package main;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import main.MatchingHelper.Field;
import model.citeseer.Document;
import model.dblp.Article;
import model.dblp.Author;
import model.dblp.Book;
import model.dblp.Dblp;
import model.dblp.Incollection;
import model.dblp.Inproceedings;
import model.dblp.Mastersthesis;
import model.dblp.Phdthesis;
import model.dblp.Proceedings;
import model.dblp.Publication;
import model.dblp.Title;
import model.dblp.Www;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

/**
 * @author Alina Maria Ciobanu
 */
public class SolrHandler 
{
	//index commit batch size 
	private static final int DOCS_LIST_SIZE = 10000;
	//max number of docs to index
	private static final int INDEXED_MAX_SIZE = 10000000;
	
	private static final String DBLP 		 = "dblp";
	private static final String DELETE_QUERY = "*:*";
	
	public static int currentNumberOfHits = 0;
	
	private int currentNumberOfDocs = 0;

	private SolrServer server;

	public SolrHandler()
	{
		server = new HttpSolrServer(Config.BASE_URL);
	}

	/**
	 * reads DBLP XML file and indexes each entry
	 * <br> indexed fields are: key, title, year, venue, authors
	 */
	public void indexDblp(String filePath) 
	{
		Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		
		try 
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(Dblp.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

			XMLInputFactory inputFactory = XMLInputFactory.newInstance();

			InputStream in = new FileInputStream(filePath);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(new InputStreamReader(in, Charset.forName("UTF8")));

			EventFilter filter = new EventFilter() 
			{
				public boolean accept(XMLEvent event) 
				{
					if (event.isStartElement())
					{
						if (((StartElement)event).getName().getLocalPart().equals(DBLP))
						{
							return false;
						}
					}
					return event.isStartElement();
				}
			};

			XMLEventReader filteredEventReader = inputFactory.createFilteredReader(eventReader, filter);
			XMLEvent event;

			while ((event = filteredEventReader.peek()) != null) 
			{
				if (event.isStartElement())
				{
					String startElementName = ((StartElement)event).getName().getLocalPart();

					if	(MatchingHelper.elementsList.contains(startElementName))
					{
						Publication p = (Publication)unmarshaller.unmarshal(eventReader);
						
						System.out.println("currentNumberOfDocs : "+currentNumberOfDocs);
						if (currentNumberOfDocs++ > INDEXED_MAX_SIZE)
						{
							System.out.println("currentNumberOfDocs : "+currentNumberOfDocs);
							server.shutdown();
							
							server = new HttpSolrServer(Config.BASE_URL);
							
							currentNumberOfDocs = 0;
						}
						else if (p.getTitle() != null)
						{
							//skip Home Page
							if (p.getTitle().getValue() != "Home Page")
								{
									System.out.println(p.getTitle().getValue());
									SolrInputDocument solrDocument = creatSolrInputDocument(p);
									docs.add(solrDocument);
								}
						}
					}
				}
				else
				{
					eventReader.next();
				}
				
				if (docs.size() == DOCS_LIST_SIZE)
				{
					indexDocuments(docs);
					docs.clear();
				}
			}
			
			if (docs.size() > 0)
			{
				indexDocuments(docs);
			}
		} 
		catch (Exception ex) 
		{
			MatchingHelper.logMessage(ex, "ERROR while parsing DBLP file");
			ex.printStackTrace();
		}  
	}
	
	/**
	 * indexes all input documents
	 */
	private void indexDocuments(Collection<SolrInputDocument> docs) 
	{
		try
		{
			server.add(docs);
			server.optimize();
			server.commit();
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while indexing DBLP files");
		}
	}

	/**
	 * deletes all previously indexed documents, not used through this package
	 */
	public void deleteAllIndexedPublications() 
	{
		try
		{
			server.deleteByQuery(DELETE_QUERY);
			server.commit();
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while deleting all previously indexed publications");
		}
	}

	/**
	 * creates Solr document from DBLP entry
	 * <br>indexed fields are: key, title (with and without stopwords), year, venue, authors, page count
	 */
	private SolrInputDocument creatSolrInputDocument(Publication publication) 
	{
		SolrInputDocument doc = new SolrInputDocument();

		String key = "";
		
		try
		{
			if (publication instanceof Article)
			{
				key = ((Article) publication).getKey();
			}
			else if (publication instanceof Book)
			{
				key = ((Book) publication).getKey();
			}
			else if (publication instanceof Incollection)
			{
				key = ((Incollection) publication).getKey();
			}
			else if (publication instanceof Inproceedings)
			{
				key = ((Inproceedings) publication).getKey();
			}
			else if (publication instanceof Mastersthesis)
			{
				key = ((Mastersthesis) publication).getKey();
			}
			else if (publication instanceof Phdthesis)
			{
				key = ((Phdthesis) publication).getKey();
			}
			else if (publication instanceof Proceedings)
			{
				key = ((Proceedings) publication).getKey();
			}
			else if (publication instanceof Www)
			{
				key = ((Www) publication).getKey();
			}
			
			doc.addField(Field.ID.toString().toLowerCase(), key);
			
			Title title = publication.getTitle();
			
			if (title == null)
			{
				MatchingHelper.logMessage(null, "Paper '" + key + "' does not have a title");
			}
			else
			{
				doc.addField(Field.TITLE.toString().toLowerCase(), title.getValue());
			}
			
			doc.addField(Field.NSW_TITLE.toString().toLowerCase(), MatchingHelper.removeStopWordsAndPunctuationFromTitle(publication.getTitle().getValue()));
			doc.addField(Field.YEAR.toString().toLowerCase(), publication.getYear());
			
			if (publication instanceof Inproceedings || publication instanceof Proceedings || publication instanceof Incollection)
			{
				doc.addField(Field.VENUE.toString().toLowerCase(), publication.getBooktitle());
			}
			else if (publication instanceof Article)
			{
				doc.addField(Field.VENUE.toString().toLowerCase(), publication.getJournal());
			}
			
			doc.addField(Field.PAGES.toString().toLowerCase(), MatchingHelper.getNumberOfPages(publication.getPages()));
	
			for (Author author : publication.getAuthor())
			{
				doc.addField(Field.AUTHOR.toString().toLowerCase(), author.getValue());
			}
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while creating Solr document from DBLP entry '" + key + "'");
		}
		
		return doc;
	}

	/**
	 * creates Solr query and returns all hits from DBLP indexed files 
	 */
	public SolrDocument getMatchingDocuments(Document record, double threshold) 
	{
		SolrDocument bestMatch = null;
		double highestJaccardSimilarity = 0;
		
		String titleQuery = MatchingHelper.createTitleQuery(record.getTitle());
		
		if (titleQuery.length() == 0)
		{
			return bestMatch;
		}
		
		SolrQuery query = new SolrQuery();
		query.setRows(Integer.MAX_VALUE);
		query.setQuery(titleQuery);
		
		SolrDocumentList results = new SolrDocumentList();
		
		try
		{
			QueryResponse rsp = server.query(query);
			results.addAll(rsp.getResults());

			currentNumberOfHits = results.size();
		}
		catch (SolrServerException ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while getting response from Solr server");
			System.exit(1);
		}
		
		try
		{
			for (SolrDocument result : results)
			{
				if (result.getFieldValue(Field.TITLE.toString().toLowerCase()) instanceof String)
				{
					double similarity = 1.0;
										
					if (Config.MATCH_TITLE)
					{
						if (MatchingHelper.Similarity.COSINE.toString().equals(Config.SIMILARITY.toUpperCase()))
						{
							similarity = Similarity.computeCosineSimilarity(record.getTitle(), (String)result.getFieldValue(Field.TITLE.toString().toLowerCase()));
						}
						else
						{
							similarity = Similarity.computeJaccardSimilarity(record.getTitle(), (String)result.getFieldValue(Field.TITLE.toString().toLowerCase()));
						}
						
						if (similarity < threshold)
						{
							continue;
						}
					}
						
					if (Config.MATCH_AUTHORS && !MatchingHelper.isAuthorsInclusionChecked(record, result))
					{
						continue;
					}
					
					if (Config.MATCH_PAGE_COUNT && !MatchingHelper.pageCountMatches(record, result))
					{
						continue;
					}
					
					if (Config.MATCH_VENUE && !MatchingHelper.venueMatches(record, result))
					{
						continue;
					}
						
					if (similarity > highestJaccardSimilarity)
					{
						bestMatch = result;
						highestJaccardSimilarity = similarity;
					}
				}
			}
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while getting best DBLP match for paper '" + record.getDoi() + "'");
		}
		
		return bestMatch;
	}


	/**
	 * returns all documents (DBLP indexed entries) for which the last names of the authors list includes the CiteSeer authors last names' list  
	 */
	public SolrDocumentList getAuthorsMatchingResults(String title, String authors, SolrDocumentList closeResults) 
	{
		SolrDocumentList closerResults = new SolrDocumentList();
		
		try
		{
			Set<String> citeSeerAuthors = new TreeSet<String>(Arrays.asList(authors.split(",\\s*")));
	
			for (SolrDocument dblpRecord : closeResults)
			{
				if (dblpRecord.getFieldValue(Field.AUTHOR.toString().toLowerCase()) instanceof ArrayList<?>)
				{
					@SuppressWarnings("unchecked")
					Set<String> dblpAuthors = new TreeSet<String>((ArrayList<String>)dblpRecord.getFieldValue(Field.AUTHOR.toString().toLowerCase())); 
	
	//				if (dblpAuthors.size() != citeSeerAuthors.size())
	//				{
	//					continue;
	//				}
					
					if (MatchingHelper.getAuthorsLastNames(dblpAuthors).containsAll(MatchingHelper.getAuthorsLastNames(citeSeerAuthors)))
					{
						closerResults.add(dblpRecord);
					}
				}
			}
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while selecting DBLP hits for which the authors match");
		}
		
		return closerResults;
	}
}