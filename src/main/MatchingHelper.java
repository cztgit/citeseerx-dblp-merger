package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.citeseer.Document;
import model.dblp.Author;
import model.dblp.Title;
import model.merged.Paper;

import org.apache.solr.common.SolrDocument;

/**
 * @author Alina Maria Ciobanu
 */
public class MatchingHelper 
{	private static final String PAGE_COUNT_REGEX_1 = "[0-9]+";
    private static final String PAGE_COUNT_REGEX_2 = "([0-9]+)-([0-9]+)[a-zA-Z]*";
    private static final String PAGE_COUNT_REGEX_3 = "([0-9]+)-([0-9]+),\\s[0-9]+[a-zA-Z]*";	
	private static final String PAGE_COUNT_REGEX_4 = "(.*?),\\s([0-9]+)-([0-9]+)(.*?)";
	private static final String PAGE_COUNT_REGEX_5 = "(.*?),\\s([0-9]+)";
	private static final String PAGE_COUNT_REGEX_6 = "([0-9]+)-([0-9]+),\\s([0-9]+)-([0-9]+)";
	private static final String PAGE_COUNT_REGEX_7 = "([0-9]+):\\s([0-9]+)-([0-9]+)";
	private static final String PAGE_COUNT_REGEX_8 = "([0-9]+)\\.([0-9]+)-([0-9]+)\\.([0-9]+)";	 
	private static final String PAGE_COUNT_REGEX_9 = "([0-9]+)-([0-9]+),\\s[a-zA-Z]([0-9]+)-[a-zA-Z]([0-9]+)";
	private static final String PAGE_COUNT_REGEX_10 = "([0-9]+)-([0-9]+)&([0-9]+)";
	private static final String PAGE_COUNT_REGEX_11 = "([0-9]+),\\s([0-9]+),\\s([0-9]+)-([0-9]+),\\s([0-9]+)";
	private static final String PAGE_COUNT_REGEX_12 = "[A-Z]+\\s([0-9]+)-([0-9]+)";
	private static final String PAGE_COUNT_REGEX_13 = "([0-9]+\\w*):([0-9]+)-([0-9]+\\w*):([0-9]+)";

	
	public static final String PUNCTUATION_REGEX   = "([\\p{Punct}\\s]+)";
	public static final String PUNCTUATION_REGEX_WITHOUT_WHITESPACE   = "([\\p{Punct}]+)";
	
	private static Map<String, Integer> csPageCountMap;
	private static List<String> stopWordsList;
	
	public static Map<String, String> matchesMap;

	public static PrintWriter hits = null;
	public static PrintWriter log = null;
	
	public static int TRUE_POSITIVES = 0;
	public static int FALSE_POSITIVES = 0;
	public static int FALSE_NEGATIVES = 0;
	
	/**
	 * file separators
	 */
	private static List<String> separators = Arrays.asList("\\", "/");
	
	/**
	 * list of DBLP types of publications
	 */
	public static final List<String> elementsList;
	
	/**
	 * indexed DBLP fields
	 */
	enum Field 
	{
		ID,
		TITLE,
		NSW_TITLE,
		YEAR,
		VENUE,
		AUTHOR,
		PAGES
	};
	
	/**
	 * DBLP types of publications
	 *
	 */
	public enum Element
	{
		ARTICLE,
		BOOK,
		INCOLLECTION,
		INPROCEEDINGS,
		PHDTHESIS,
		PROCEEDINGS,
		MASTERSTHESIS,
		WWW
	};
	
	/**
	 * operator used for Solr queries
	 */
	enum Operator 
	{
		OR,
		AND
	};
	
	/**
	 * similarity measure
	 */
	enum Similarity 
	{
		JACCARD,
		COSINE
	};
	
	/**
	 * initializes input objects
	 */
	public static void init()
	{
		createFolders();
		
		BufferedReader in;
		String line;
		
		Set<String> tempListOfIds = new HashSet<String>();
	
		try 
		{
			// populate a temp list with all CiteSeer ids in the current subset
			in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Config.CITESEER_PATH)), Charset.forName("UTF8")));
			
			while ((line = in.readLine()) != null)
			{
				line = MatchingHelper.replaceFileSeparators(line);
				
				tempListOfIds.add(line.substring(line.lastIndexOf(File.separator) + 1, line.indexOf(".xml")));
			}
			
			in.close();
		}
		catch (Exception ex) 
		{
			logMessage(ex, "ERROR while reading ids in current CiteSeer subset");
		}
		
		try
		{
			// populate csPageCountMap with number of pages only for records in the current subset (tempListOfIds)
			in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Config.PAGE_COUNT_FILE_PATH)), Charset.forName("UTF8")));
			
			while ((line = in.readLine()) != null)
			{
				String[] split = line.split("\\s+");
				
				if (split.length < 2)
				{
					continue;
				}
				
				try
				{
					Integer pageCount = Integer.parseInt(split[1]);
					String filePath = MatchingHelper.replaceFileSeparators(split[0]);
					
					String id = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.indexOf(".pdf"));
						
					if (tempListOfIds.contains(id))
					{
						csPageCountMap.put(id, pageCount);
					}
				}
				catch (Exception ex)
				{
					logMessage(ex, "ERROR while populating map with number of pages for records in the current subset");
				}
			}
			
			in.close();
			
			tempListOfIds = null;
		}
		catch (Exception ex) 
		{
			logMessage(ex, "ERROR while populating map with number of pages for records in the current subset");
		}	
			
		try
		{
			// load stopwords
			if (Config.REMOVE_STOPWORDS)
			{
				in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Config.STOP_WORDS_PATH)), Charset.forName("UTF8")));
				
				while ((line = in.readLine()) != null)
				{
					stopWordsList.add(line);
				}
				
				in.close();
			}
		}
		catch (Exception ex) 
		{
			logMessage(ex, "ERROR while reading stopwords list");
		}
		
		try
		{
			// load manual matches
			if (Config.OUTPUT_STATS)
			{
				in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Config.MATCHES_FILE_PATH)), Charset.forName("UTF8")));
				
				while ((line = in.readLine()) != null)
				{
					String[] split = line.split("\\s+");
					
					if (split.length < 2)
					{
						continue;
					}
					
					matchesMap.put(split[0], split[1]);
				}
				
				in.close();
			}			
		} 
		catch (Exception ex) 
		{
			logMessage(ex, "ERROR while loading manual matches");
		}
		
		try
		{
			hits = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(Config.HITS_FILE_PATH)), Charset.forName("UTF8")));
			
			hits.write(String.format("%-25s %-15s %-25s\r\n\r\n", "doi", "hits", "time"));
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while creating hits file");
		}
		
		try
		{
			log = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(Config.LOG_FILE_PATH)), Charset.forName("UTF8")));
		}
		catch (Exception e)
		{
			System.out.println("ERROR while writing configuration parameters in log file");
		}
	}
	
	static
	{
		elementsList = Arrays.asList(
				Element.ARTICLE.toString().toLowerCase(),
				Element.BOOK.toString().toLowerCase(),
				Element.INCOLLECTION.toString().toLowerCase(),
				Element.INPROCEEDINGS.toString().toLowerCase(),
				Element.MASTERSTHESIS.toString().toLowerCase(),
				Element.PHDTHESIS.toString().toLowerCase(),
				Element.PROCEEDINGS.toString().toLowerCase(),
				Element.WWW.toString().toLowerCase());
		
		stopWordsList = new ArrayList<String>();
		matchesMap = new HashMap<String, String>();
		csPageCountMap = new HashMap<String, Integer>();
	}
	
	/**
	 * merges a CiteSeer record with an entry from DBLP
	 */
	public static Paper mergeEntries(Document citeseerRecord, SolrDocument dblpRecord) 
	{
		try
		{
			Paper paper = new Paper();
			
			Title title = new Title();
			title.setValue((String)dblpRecord.getFieldValue(Field.TITLE.toString().toLowerCase()));
			
			paper.setTitle(title);
			
			paper.setKey((String)dblpRecord.getFieldValue(Field.ID.toString().toLowerCase()));
			paper.setDoi(citeseerRecord.getDoi());
			
			String venue = (String)dblpRecord.getFieldValue(Field.VENUE.toString().toLowerCase());
			
			if (venue != null && !"".equals(venue))
			{
				paper.setVenue(venue);
			}
			else
			{
				paper.setVenue(citeseerRecord.getVenue());
			}
			
			Integer year = (Integer)dblpRecord.getFieldValue(Field.YEAR.toString().toLowerCase());
			
			if (year != null)
			{
				paper.setYear(year.toString());
			}
			else
			{
				paper.setYear(citeseerRecord.getYear());
			}
			
			paper.setAbstract(citeseerRecord.getAbstract());
			paper.setCitations(citeseerRecord.getCitations());
			
			if (dblpRecord.getFieldValue(Field.AUTHOR.toString().toLowerCase()) instanceof ArrayList<?>)
			{
				@SuppressWarnings("unchecked")
				List<String> authorStrings = (ArrayList<String>)dblpRecord.getFieldValue(Field.AUTHOR.toString().toLowerCase());
				
				for (String authorString : authorStrings)
				{
					Author author = new Author();
					author.setValue(authorString);
					paper.getAuthor().add(author);
				}
			}
			return paper;
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while merging files " + citeseerRecord.getDoi() + " and " + (String)dblpRecord.getFieldValue(Field.ID.toString().toLowerCase()));
		}
		
		return null;
	}
	
	/**
	 * creates Solr query for paper title
	 */
	public static String createTitleQuery(String title) 
	{
		try
		{
			String fieldToQuery = Config.REMOVE_STOPWORDS ? Field.NSW_TITLE.toString().toLowerCase() : Field.TITLE.toString().toLowerCase();
			
			if (Operator.AND.toString().equals(Config.OPERATOR))
			{
				if (Config.REMOVE_STOPWORDS)
				{
					title = removeStopWordsAndPunctuationFromTitle(title);
				}
				else
				{
					title = title.replaceAll(PUNCTUATION_REGEX_WITHOUT_WHITESPACE, "");
				}
				
				if ("".equals(title))
				{
					return "";
				}
				
				return fieldToQuery + ":\"" + title + "\"";
			}
			
			List<String> titleWordsList = new ArrayList<String>(Arrays.asList(title.toLowerCase().split(PUNCTUATION_REGEX)));
			
			StringBuilder titleQuery = new StringBuilder("");
			
			if (Config.REMOVE_STOPWORDS)
			{
				titleWordsList.removeAll(stopWordsList);
			}
			
			if (titleWordsList.size() == 0)
			{
				return "";
			}
			
			if (titleWordsList.size() <= Config.N_GRAMS)
			{
				titleQuery.append(fieldToQuery + ":\"");
				
				for (int i = 0; i < titleWordsList.size(); i++)
				{
					if (i < titleWordsList.size() - 1)
					{
						titleQuery.append(titleWordsList.get(i) + " ");
					}
					else
					{
						titleQuery.append(titleWordsList.get(i) + "\"");
					}
				}
				
				return titleQuery.toString();
			}
			
			for (int i = 0; i <= titleWordsList.size() - Config.N_GRAMS; i++)
			{
					StringBuilder intermediateQuery = new StringBuilder("");
					
					intermediateQuery.append(fieldToQuery + ":\"");
					
					for (int j = 0; j < Config.N_GRAMS; j++)
					{
						if  (j < Config.N_GRAMS - 1)
						{
							intermediateQuery.append(titleWordsList.get(i + j) + " ");
						}
						else
						{
							intermediateQuery.append(titleWordsList.get(i + j) + "\" ");
						}
					}
					
					titleQuery.append(intermediateQuery);
			}
			
			return titleQuery.toString().replaceAll("\" " + fieldToQuery, "\" OR " + fieldToQuery);
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while building title query");
		}
		
		return "";
	}
	
	/**
	 * removes stopwords and punctuation from title
	 */
	public static String removeStopWordsAndPunctuationFromTitle(String title)
	{
		try
		{
			ArrayList<String> titleWords = new ArrayList<String>(Arrays.asList(title.toLowerCase().split(MatchingHelper.PUNCTUATION_REGEX)));
			
			titleWords.removeAll(MatchingHelper.stopWordsList);
			
			StringBuilder newTitle = new StringBuilder("");
			
			for (String titleWord : titleWords)
			{
				newTitle.append(titleWord + " ");
			}
			
			return newTitle.toString().trim();
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while removing punctuation (and stopwords) from title '" + title + "'");
		}
		
		return "";
	}
	
	/**
	 * returns a list with the last names of the authors (last name = last token)
	 */
	public static List<String> getAuthorsLastNames(Set<String> authors)
	{
		List<String> authorsLastNames = new ArrayList<String>();
		
		try
		{
			Iterator<String> iterator = authors.iterator();
			
			while(iterator.hasNext())
			{
				String author = iterator.next();
				
				if (!author.contains(" "))
				{
					authorsLastNames.add(author);
				}
				else
				{
					String[] nameTokens = author.split("\\s+");
					
					authorsLastNames.add(nameTokens[nameTokens.length - 1]);
				}
			}
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while retrieving authors' last names");
		}
		
		return authorsLastNames;
	}
	
	/**
	 * returns the number of pages for DBLP record
	 */
	public static int getNumberOfPages(String pages)
	{
		if (pages == null)
		{
			return 0;
		}
		
		// example: 12
		if (pages.matches(PAGE_COUNT_REGEX_1))
		{
			return 1;
		}
		else
		{
			try
			{
				//example:436765:1-436765:21
				Pattern pattern = Pattern.compile(PAGE_COUNT_REGEX_13);
				Matcher matcher = pattern.matcher(pages);
					
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$4")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) + 1;
				}

				//example: 5: 1-29
				pattern = Pattern.compile(PAGE_COUNT_REGEX_7);
				matcher = pattern.matcher(pages);
					
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$3")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) + 1;
				}
			
				//example: IS 39-42
				pattern = Pattern.compile(PAGE_COUNT_REGEX_12);
				matcher = pattern.matcher(pages);
						
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$1")) + 1;
				}
				
				//example: 21.1-21.24
				pattern = Pattern.compile(PAGE_COUNT_REGEX_8);
				matcher = pattern.matcher(pages);
					
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$4")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) + 1;
				}	
				
				//example: 14-15, 20-22
				pattern = Pattern.compile(PAGE_COUNT_REGEX_6);
				matcher = pattern.matcher(pages);
					
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$1")) + 1 +
							Integer.parseInt(pages.replaceAll(pattern.toString(), "$4")) - 
							Integer.parseInt(pages.replaceAll(pattern.toString(), "$3")) + 1;
				}
				
				//example: 2, 4, 6-7, 64
				pattern = Pattern.compile(PAGE_COUNT_REGEX_11);
				matcher = pattern.matcher(pages);
						
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$4")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$3")) + 4;
				}
	
				//example: 14-15, 20
				pattern = Pattern.compile(PAGE_COUNT_REGEX_3);
				matcher = pattern.matcher(pages);
							
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$1")) + 2;
				}	
				
				//example: 31-33, B1-B22
				pattern = Pattern.compile(PAGE_COUNT_REGEX_9);
				matcher = pattern.matcher(pages);
				
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$1")) + 1 + 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$4")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$3")) + 1;
				}
				
				//example: 2-3&4
				pattern = Pattern.compile(PAGE_COUNT_REGEX_10);
				matcher = pattern.matcher(pages);
				
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$1")) + 2;
				}
					
				//example: I-VIII, 12-15
				pattern = Pattern.compile(PAGE_COUNT_REGEX_4);
				matcher = pattern.matcher(pages);
						
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$3")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) + 1;
				}
				
				//example: I-VIII, 12
				pattern = Pattern.compile(PAGE_COUNT_REGEX_5);
				matcher = pattern.matcher(pages);
							
				if (matcher.find())
				{
					return 1;
				}
							
				//example: 14-16
				pattern = Pattern.compile(PAGE_COUNT_REGEX_2);
				matcher = pattern.matcher(pages);
						
				if (matcher.find())
				{
					return Integer.parseInt(pages.replaceAll(pattern.toString(), "$2")) - 
						Integer.parseInt(pages.replaceAll(pattern.toString(), "$1")) + 1;
				}
			}
			catch(NumberFormatException ex)
			{
				logMessage(ex, "NumberFormatException while extracting page number: " + ex.getMessage());
			}
			catch (Exception ex)
			{
				logMessage(ex, "ERROR while extracting page number");
			}
			
		}
			
		return 0;
		
	}
	
	/**
	 * returns true if |citeSeerPageCount - dblpPageCount| <= pageLimit
	 * returns false otherwise and if one of the entries does not have the information set
	 */
	public static boolean pageCountMatches(Document citeSeer, SolrDocument d)
	{
		try
		{
			Integer dblpPageCount = 0;
			Integer citeSeerPageCount = 0;
			
			if (d.getFieldValue(Field.PAGES.toString().toLowerCase()) instanceof Integer)
			{
				dblpPageCount = (Integer)d.getFieldValue(Field.PAGES.toString().toLowerCase());
			}
			
			if (csPageCountMap.get(citeSeer.getDoi()) != null)
			{
				citeSeerPageCount = csPageCountMap.get(citeSeer.getDoi());
			}
			
			if (dblpPageCount == 0 || citeSeerPageCount == 0)
			{
				return false;
			}
			
			return Math.abs(dblpPageCount - citeSeerPageCount) <= Config.PAGE_LIMIT;
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while checking page count match for papers '" + citeSeer.getDoi() + "' and '" + d.getFieldValue(Field.ID.toString().toLowerCase()) + "'");
		}
		
		return false;
	}
	
	/**
	 * returns true if CiteSeer and DBLP venues are perfect match
	 */
	public static boolean venueMatches(Document citeSeer, SolrDocument dblp) 
	{
		try
		{
			String dblpVenue = "";
			
			if (dblp.getFieldValue(Field.VENUE.toString().toLowerCase()) instanceof String)
			{
				dblpVenue = (String)dblp.getFieldValue(Field.VENUE.toString().toLowerCase());
			}
			
			if (dblpVenue.equals(citeSeer.getVenue()))
			{
				return true;
			}
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while checking venue match for papers '" + citeSeer.getDoi() + "' and '" + dblp.getFieldValue(Field.ID.toString().toLowerCase()) + "'");
		}
		
		return false;
	}

	/**
	 * checks if CiteSeer authors last names list is included in DBLP authors last names list
	 */
	public static boolean isAuthorsInclusionChecked(Document citeSeer, SolrDocument dblp)
	{
		try
		{
			Set<String> citeSeerAuthors = new TreeSet<String>(Arrays.asList(citeSeer.getAuthors().split(",\\s*")));
	
			if (dblp.getFieldValue(Field.AUTHOR.toString().toLowerCase()) instanceof ArrayList<?>)
			{
				@SuppressWarnings("unchecked")
				Set<String> dblpAuthors = new TreeSet<String>((ArrayList<String>)dblp.getFieldValue(Field.AUTHOR.toString().toLowerCase())); 
	
	//			if (dblpAuthors.size() != citeSeerAuthors.size())
	//			{
	//				continue;
	//			}
					
				if (MatchingHelper.getAuthorsLastNames(dblpAuthors).containsAll(MatchingHelper.getAuthorsLastNames(citeSeerAuthors)))
				{
					return true;
				}
			}	
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while authors last names' match for papers '" + citeSeer.getDoi() + "' and '" + dblp.getFieldValue(Field.ID.toString().toLowerCase()) + "'");
		}
		return false;	
	}
	
	/**
	 * writes TP, FP, FN, precision, recall and f-measure to file
	 */
	public static void writeOutput(String outPath, String print, long globalstarttime, long globalendtime) 
	{
		FALSE_NEGATIVES = matchesMap.values().size() - TRUE_POSITIVES;
		
		double precision = 0d;
		
		if (TRUE_POSITIVES + FALSE_POSITIVES != 0)
		{
			precision = (TRUE_POSITIVES * 1.0d) / (TRUE_POSITIVES + FALSE_POSITIVES);
		}
		
		double recall = 0d;
		
		if (TRUE_POSITIVES + FALSE_NEGATIVES != 0)
		{
			recall = (TRUE_POSITIVES * 1.0d) / (TRUE_POSITIVES + FALSE_NEGATIVES);
		}
		
		double fMeasure = 0d;
		
		if (precision + recall != 0)
		{
			fMeasure = 2 * precision * recall / (precision + recall);
		}
		
		try
		{
			PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(outPath)), Charset.forName("UTF8")));
			
			out.write(print + "\r\n\r\n");
			
			out.write("TP = " + TRUE_POSITIVES + "\r\n");
			out.write("FP = " + FALSE_POSITIVES + "\r\n");
			out.write("FN = " + FALSE_NEGATIVES + "\r\n");
			out.write("Prec = " + precision + "\r\n");
			out.write("Recall = " + recall + "\r\n");
			out.write("F-Measure = " + fMeasure + "\r\n");
			out.write("Runtime(s) = " + (globalendtime-globalstarttime)/1000.);
			
			out.close();
		}
		catch (Exception ex)
		{
			logMessage(ex, "ERROR while writing output file");
		}
	}
	
	/**
	 * creates output folders
	 */
	public static void createFolders()
	{
		try
		{
			// create output folder for merged papers if it does not exist
			if (Config.WRITE_MERGED_FILES)
			{
				File outputFolder = new File(Config.MERGED_FOLDER_PATH);
				
				if (!outputFolder.exists())
				{
					outputFolder.mkdirs();
				}
			}
			// create output folder for file in which TP, FP, FN, precision, recall and F-measure are written if it does not exist
			if (Config.OUTPUT_STATS && Config.STATS_FILE_PATH.contains(File.separator))
			{
				File outputFolder = new File(Config.STATS_FILE_PATH.substring(0, Config.STATS_FILE_PATH.lastIndexOf(File.separator)));
				
				if (!outputFolder.exists())
				{
					outputFolder.mkdirs();
				}
			}
			
			// create output folder for file in which hits number and time are written if it does not exist
			if (Config.HITS_FILE_PATH.contains(File.separator))
			{
				File outputFolder = new File(Config.HITS_FILE_PATH.substring(0, Config.HITS_FILE_PATH.lastIndexOf(File.separator)));
				
				if (!outputFolder.exists())
				{
					outputFolder.mkdirs();
				}
			}	
			
			// create output folder for file in which hits number and time are written if it does not exist
			if (Config.LOG_FILE_PATH.contains(File.separator))
			{
				File outputFolder = new File(Config.LOG_FILE_PATH.substring(0, Config.LOG_FILE_PATH.lastIndexOf(File.separator)));
				
				if (!outputFolder.exists())
				{
					outputFolder.mkdirs();
				}
			}
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while creating output folders");
			System.exit(1);
		}
	}
	
	/**
	 * replaces existing file-separators with platform-specific ones
	 */
	public static String replaceFileSeparators(String path)
	{
		for (String separator : separators)
		{
			path = path.replace(separator, File.separator);
		}
		
		return path;
	}
	
	/**
	 * writes messages to console and log file
	 */
	public static void logMessage(Throwable ex, String message) 
	{
		System.out.println(message);
		
		if (log != null)
		{
			log.println(message);
		}
		
		if (Config.VERBOSITY_LEVEL >= 2 && ex != null && ex.getMessage() != null && !"".equals(ex.getMessage()))
		{
			System.out.println("\t" + ex.getMessage());
			
			if (log != null)
			{
				log.println(("\t" + ex.getMessage()));
			}
		}
		
		if (Config.VERBOSITY_LEVEL == 3 && ex != null)
		{
			ex.printStackTrace();
			
			if (log != null)
			{
				ex.printStackTrace(log);
				log.println();
			}
		}
	}
}