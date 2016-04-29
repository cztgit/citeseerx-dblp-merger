package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import javax.xml.bind.JAXBContext;

import main.MatchingHelper.Field;
import model.ModelHelper;
import model.citeseer.Document;
import model.merged.Paper;

import org.apache.solr.common.SolrDocument;

/**
 * @author Alina Maria Ciobanu
 */
public class MainClass 
{		
	private static final String XML_EXTENSION = ".xml";
	
	public static void main(String[] args) throws Exception
	{
		long globalstarttime = System.currentTimeMillis();
		long globalendtime = System.currentTimeMillis();
		
		String print = Config.init(args);
		
		MatchingHelper.init();
		
		MatchingHelper.logMessage(null, print + "\r\n");
		
		SolrHandler solrHandler = new SolrHandler();

		BufferedReader in = null;
		
		try
		{
			// delete all previously indexed documents
			//solrHandler.deleteAllIndexedPublications();
			
			// index DBLP entries
			if (Config.BUILD_INDEX)
			{
				System.out.println("indexing dblp...");
				solrHandler.indexDblp(Config.DBLP_PATH);
				System.out.println("indexing dblp -- done");
				System.exit(0);
			}
			
			JAXBContext jaxbContext = JAXBContext.newInstance(Document.class);
			
			in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(Config.CITESEER_PATH))));
			
			String filePath;
						
			while ((filePath = in.readLine()) != null)
			{
				if (filePath.endsWith(XML_EXTENSION)) 
				{
					try
					{
						// read CiteSeer record
						Document record = ModelHelper.readCiteseerRecord(filePath, jaxbContext);
						
						if (record == null)
						{
							MatchingHelper.logMessage(null, "Skipping file '" + filePath + "'");
							continue;
						}
						
						SolrHandler.currentNumberOfHits = 0;
						long startTime = System.currentTimeMillis();
						
						// get matching documents
						SolrDocument result = solrHandler.getMatchingDocuments(record, Config.THRESHOLD);
						
						long stopTime = System.currentTimeMillis();
						
						// write number of hits and time for getting matching document (seconds)
						MatchingHelper.hits.write(String.format("%-25s %-15s %-25s\r\n", record.getDoi(), SolrHandler.currentNumberOfHits, 1.0 * (stopTime - startTime) / 1000));
						
						if (result != null)
						{
							if (Config.WRITE_MERGED_FILES)
							{
								// write merged file
								Paper mergedFile = MatchingHelper.mergeEntries(record, result);
								
								if (mergedFile != null)
								{
									ModelHelper.writeModel(mergedFile, Config.MERGED_FOLDER_PATH + File.separator + record.getDoi() + XML_EXTENSION);
								}
							}
	
							if (Config.OUTPUT_STATS) // we check TP, FP, FN against manual matches
							{
								if (MatchingHelper.matchesMap.get(record.getDoi()) != null)
								{
									String correctMatchId = MatchingHelper.matchesMap.get(record.getDoi());
									
									String matchId = (String)result.getFieldValue(Field.ID.toString().toLowerCase());
									
									if (matchId.equals(correctMatchId))
									{
										MatchingHelper.TRUE_POSITIVES++;
									}
									else
									{
										MatchingHelper.FALSE_POSITIVES++;
									}
								}
								else
								{
									MatchingHelper.FALSE_POSITIVES++;
								}
							}
						}
					}
					catch (Exception ex)
					{
						MatchingHelper.logMessage(ex, "ERROR while processing file '" + filePath + "'");
					}
				}
			}
			
			globalendtime = System.currentTimeMillis();
				
			if (Config.OUTPUT_STATS)
			{
				MatchingHelper.writeOutput(Config.STATS_FILE_PATH, print, globalstarttime, globalendtime);
			}
		}
		catch (FileNotFoundException ex)
		{
			MatchingHelper.logMessage(ex, "ERROR CiteSeer folder not found");
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while matching records");
		}
		catch (OutOfMemoryError err) 
		{
			MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
	        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
	       	        
	        String errorMessage = "ERROR out of memory\r\n" +
	        		"Amount of used memory (MB): " + heapUsage.getUsed() / 1024 * 1024 + "\r\n" + 
	        		"Maximum amount of memory (MB): " + heapUsage.getMax() / 1024 * 1024;
	        
	        MatchingHelper.logMessage(err, errorMessage);
		}
		finally
		{	
			if (in != null)
			{
				in.close();
			}
			
			if (MatchingHelper.hits != null)
			{
				MatchingHelper.hits.close();
			}	
			
			globalendtime = System.currentTimeMillis();
			MatchingHelper.logMessage(null, "Program finishes. Runtime (s): "+(globalendtime-globalstarttime)/1000.);
			
			if (MatchingHelper.log != null)
			{
				MatchingHelper.log.close();
			}
		}
	}
}