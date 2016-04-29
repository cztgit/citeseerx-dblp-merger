package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Alina Maria Ciobanu
 */
public class Config 
{	
	// input parameters

	public static double THRESHOLD = 0.7;
	public static boolean BUILD_INDEX = true;
	public static boolean REMOVE_STOPWORDS = true;
	public static boolean WRITE_MERGED_FILES = true;
	public static boolean OUTPUT_STATS = true;
	public static int N_GRAMS = 3;
	public static int PAGE_LIMIT = 1;		
	public static String OPERATOR = MatchingHelper.Operator.OR.toString().toUpperCase();
	public static String SIMILARITY = MatchingHelper.Similarity.JACCARD.toString().toUpperCase();

	// matching features to use

	public static boolean MATCH_TITLE;			
	public static boolean MATCH_AUTHORS;			
	public static boolean MATCH_PAGE_COUNT;		
	public static boolean MATCH_VENUE;			

	// paths
	
	// SOLR url
	public static String BASE_URL = "http://localhost:8983/solr/collection1"; 

	// DBLP xml file
	public static String DBLP_PATH = "";

	// file containing the paths for the CiteSeer xml files
	public static String CITESEER_PATH = "";
	
	// folder in which all output files are stored
	public static String OUTPUT_FOLDER_PATH = "";

	// folder in which merged papers are stored, when 'writeMergedFiles' option = true
	public static String MERGED_FOLDER_PATH = "";

	// file in which TP, FP, FN, precision, recall and F-measure are written, when 'writeMergedFiles' option = false
	public static String STATS_FILE_PATH = "";

	// file in which the number of DBLP hits for each CiteSeer record is written
	public static String HITS_FILE_PATH = "";

	// file in which stop words are listed
	public static String STOP_WORDS_PATH = "";

	// file in which manual matches are listed, when 'writeMergedFiles' option = false
	public static String MATCHES_FILE_PATH = "";

	// file in which the number of pages for records in the current subset is listed
	public static String PAGE_COUNT_FILE_PATH = "";
	
	// file in which the log is written
	public static String LOG_FILE_PATH = "";
	
	// debug
	
	public static int VERBOSITY_LEVEL = 1;

	// config fields enum
	static class Parameters 
	{
		static List<String> THRESHOLD = Arrays.asList("-t", "--threshold", "THRESHOLD");
		static List<String> REMOVE_STOPWORDS = Arrays.asList("-sw", "--stopwords", "REMOVE_STOPWORDS");
		static List<String> WRITE_MERGED_FILES = Arrays.asList("-mf", "--merge_files", "WRITE_MERGED_FILES");
		static List<String> OUTPUT_FOLDER_PATH = Arrays.asList("-of", "--output_folder", "OUTPUT_FOLDER_PATH");
		static List<String> OUTPUT_STATS = Arrays.asList("-st", "--stats", "OUTPUT_STATS");
		static List<String> OPERATOR = Arrays.asList("-o", "--operator", "OPERATOR");
		static List<String> SIMILARITY = Arrays.asList("-sim", "--similarity", "SIMILARITY");
		static List<String> N_GRAMS = Arrays.asList("-n", "--n_grams", "N_GRAMS");
		static List<String> PAGE_LIMIT = Arrays.asList("-pl", "--page_limit", "PAGE_LIMIT");
		static List<String> BUILD_INDEX = Arrays.asList("-i", "--build_index", "BUILD_INDEX");
		static List<String> MATCH_TITLE = Arrays.asList("MATCH_TITLE");
		static List<String> MATCH_AUTHORS = Arrays.asList("MATCH_AUTHORS");
		static List<String> MATCH_PAGE_COUNT = Arrays.asList("MATCH_PAGE_COUNT");
		static List<String> MATCH_VENUE = Arrays.asList("MATCH_VENUE");
		static List<String> BASE_URL = Arrays.asList("-u", "--url", "BASE_URL");
		static List<String> DBLP_PATH = Arrays.asList("-dp", "--dblp_path", "DBLP_PATH");
		static List<String> CITESEER_PATH = Arrays.asList("-cp", "--citeseer_path", "CITESEER_PATH");
		static List<String> MERGED_FOLDER_PATH = Arrays.asList("-mp", "--merged_path", "MERGED_FOLDER_PATH");
		static List<String> STATS_FILE_PATH = Arrays.asList("-sp", "--stats_path", "STATS_FILE_PATH");
		static List<String> HITS_FILE_PATH= Arrays.asList("-hp", "--hits_path", "HITS_FILE_PATH");
		static List<String> STOP_WORDS_PATH= Arrays.asList("-swp", "--stopwords_path", "STOP_WORDS_PATH");
		static List<String> MATCHES_FILE_PATH = Arrays.asList("-mmp", "--manual_matches_path", "MATCHES_FILE_PATH");
		static List<String> PAGE_COUNT_FILE_PATH= Arrays.asList("-pcp", "--page_count_path", "PAGE_COUNT_FILE_PATH");
		static List<String> VERBOSITY_LEVEL = Arrays.asList("-v", "--verbosity", "VERBOSITY_LEVEL");
		static List<String> FEATURES = Arrays.asList("-f", "--features");
		static List<String> LOG_FILE_PATH = Arrays.asList("-l", "--log_file", "LOG_FILE_PATH");
		static List<String> CONFIG_FILE_PATH = Arrays.asList("-c", "--config_file");
		static List<String> HELP = Arrays.asList("-h", "--help");
	}
	
	public static String init(String[] args)
	{
		Map<String, String> argsMap = new HashMap<String, String>();
		
		for (int i = 0; i < args.length; i += 2)
		{
			String parameter = args[i];
			
			if (Parameters.HELP.contains(parameter))
			{
				System.out.println(help());
				System.exit(0);
			}
			
			if (i == args.length - 1)
			{
				System.out.println("WARNING incorrect arguments");
				
				break;
			}
			
			String value = args[i + 1];
			
			argsMap.put(parameter, value);
		}
		
		String configFilePath = argsMap.get(Parameters.CONFIG_FILE_PATH.get(0));
		
		if (configFilePath == null || "".equals(configFilePath))
		{
			configFilePath = argsMap.get(Parameters.CONFIG_FILE_PATH.get(1));
		}
		
		if (configFilePath == null || "".equals(configFilePath))
		{
			System.out.println("WARNING no config file provided");
		}
		else
		{
			initFromFile(configFilePath);
		}
		
		initFromArgs(argsMap);
		
		// prepend root folder path to output paths
		if (!"".equals(OUTPUT_FOLDER_PATH))
		{
			MERGED_FOLDER_PATH = OUTPUT_FOLDER_PATH + File.separator + MERGED_FOLDER_PATH;
			
			if (!"".equals(STATS_FILE_PATH))
			{
				STATS_FILE_PATH = OUTPUT_FOLDER_PATH + File.separator + STATS_FILE_PATH;
			}
			
			if (!"".equals(HITS_FILE_PATH))
			{
				HITS_FILE_PATH = OUTPUT_FOLDER_PATH + File.separator + HITS_FILE_PATH;
			}
			
			if (!"".equals(LOG_FILE_PATH))
			{
				LOG_FILE_PATH = OUTPUT_FOLDER_PATH + File.separator + LOG_FILE_PATH;
			}
		}
		
		return print();
	}
	
	public static void initFromFile(String configFilePath)
	{
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(configFilePath), Charset.forName("UTF8")));
			
			String line;
			
			while ((line = in.readLine()) != null)
			{
				if (line.startsWith("#") || "".equals(line.trim()))
				{
					continue;
				}
				
				String[] split = line.split("[\\s]+");
				
				String parameter = "";
				String value = "";
				
				if (split.length >= 2)
				{
					parameter = split[0];
					value = split[1];
				}
				else
				{
					parameter = split[0];
					System.out.println("Value not found in config file for parameter '" + parameter + "'\n");
				}
				
				setParameter(parameter, value);
			}
			
			in.close(); 
		}
		catch (FileNotFoundException ex)
		{
			System.out.println("WARNING config file could not be found");
			System.out.println(ex.getMessage() != null ? ex.getMessage() : "");
		}
		catch (IOException ex)
		{
			System.out.println("ERROR while parsing config file");
			System.out.println(ex.getMessage() != null ? ex.getMessage() : "");
		}
		catch (Exception ex)
		{
			System.out.println("ERROR while initializing configuration");
			System.out.println(ex.getMessage() != null ? ex.getMessage() : "");
		}
	}
	
	public static void initFromArgs(Map<String, String> argsMap)
	{
		for (Entry<String, String> entry : argsMap.entrySet())
		{
			if (!Parameters.CONFIG_FILE_PATH.contains(entry.getKey()) && !Parameters.HELP.contains(entry.getKey()))
			{
				setParameter(entry.getKey(), entry.getValue());
			}
		}
	}
	
	public static void setParameter(String parameter, String value)
	{
		if (Parameters.THRESHOLD.contains(parameter))
		{
			try
			{
				THRESHOLD = Double.parseDouble(value);
			}
			catch (NumberFormatException ex)
			{
				System.out.println("Number expected for THRESHOLD value");
				System.exit(1);
			}
		}
		else if (Parameters.REMOVE_STOPWORDS.contains(parameter))
		{
			REMOVE_STOPWORDS = Boolean.parseBoolean(value);
		}
		else if (Parameters.WRITE_MERGED_FILES.contains(parameter))
		{
			WRITE_MERGED_FILES = Boolean.parseBoolean(value);
		}
		else if (Parameters.OUTPUT_STATS.contains(parameter))
		{
			OUTPUT_STATS = Boolean.parseBoolean(value);
		}
		else if (Parameters.SIMILARITY.contains(parameter))
		{
			if (MatchingHelper.Similarity.JACCARD.toString().equals(value.toUpperCase()) || MatchingHelper.Similarity.COSINE.toString().equals(value.toUpperCase()))
			{
				SIMILARITY = value.toUpperCase();
			}
			else
			{
				System.out.println("'JACCARD'/'COSINE' expected for SIMILARITY value");
				System.exit(1);
			}
		}
		else if (Parameters.OPERATOR.contains(parameter))
		{
			if (MatchingHelper.Operator.AND.toString().equals(value.toUpperCase()) || MatchingHelper.Operator.OR.toString().equals(value.toUpperCase()))
			{
				OPERATOR = value.toUpperCase();
			}
			else
			{
				System.out.println("'AND'/'OR' expected for OPERATOR value");
				System.exit(1);
			}
		}
		else if (Parameters.N_GRAMS.contains(parameter))
		{
			if (!"".equals(value) && !"0".equals(value))
			{
				try
				{
					N_GRAMS = Integer.parseInt(value);
				}
				catch (NumberFormatException ex)
				{
					System.out.println("Integer expected for N_GRAMS value");
					System.exit(1);
				}
			}
		}
		else if (Parameters.PAGE_LIMIT.contains(parameter))
		{
			if (!"".equals(parameter))
			{
				try
				{
					PAGE_LIMIT = Integer.parseInt(value);
				}
				catch (NumberFormatException ex)
				{
					System.out.println("Integer expected for PAGE_LIMIT value");
					System.exit(1);
				}
			}
		}
		else if (Parameters.BUILD_INDEX.contains(parameter))
		{
			BUILD_INDEX = Boolean.parseBoolean(value);
		}
		else if (Parameters.MATCH_TITLE.contains(parameter))
		{
			MATCH_TITLE = Boolean.parseBoolean(value);
		}
		else if (Parameters.MATCH_AUTHORS.contains(parameter))
		{
			MATCH_AUTHORS = Boolean.parseBoolean(value);
		}
		else if (Parameters.MATCH_PAGE_COUNT.contains(parameter))
		{
			MATCH_PAGE_COUNT = Boolean.parseBoolean(value);
		}
		else if (Parameters.MATCH_VENUE.contains(parameter))
		{
			MATCH_VENUE = Boolean.parseBoolean(value);
		}
		else if (Parameters.BASE_URL.contains(parameter))
		{
			BASE_URL = value;
		}
		else if (Parameters.DBLP_PATH.contains(parameter))
		{
			DBLP_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.CITESEER_PATH.contains(parameter))
		{
			CITESEER_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.MERGED_FOLDER_PATH.contains(parameter))
		{
			MERGED_FOLDER_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.STATS_FILE_PATH.contains(parameter))
		{
			STATS_FILE_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.HITS_FILE_PATH.contains(parameter))
		{
			HITS_FILE_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.STOP_WORDS_PATH.contains(parameter))
		{
			STOP_WORDS_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.MATCHES_FILE_PATH.contains(parameter))
		{
			MATCHES_FILE_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.PAGE_COUNT_FILE_PATH.contains(parameter))
		{
			PAGE_COUNT_FILE_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.VERBOSITY_LEVEL.contains(parameter))
		{
			if (!"1".equals(value) && !"2".equals(value) && !"3".equals(value))
			{
				System.out.println("'1'/'2'/'3' expected for VERBOSITY_LEVEL value.\n");
			}
			else
			{
				VERBOSITY_LEVEL = Integer.parseInt(value);
			}
		}
		else if (Parameters.OUTPUT_FOLDER_PATH.contains(parameter))
		{
			OUTPUT_FOLDER_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.LOG_FILE_PATH.contains(parameter))
		{
			LOG_FILE_PATH = MatchingHelper.replaceFileSeparators(value);
		}
		else if (Parameters.FEATURES.contains(parameter))
		{
			MATCH_AUTHORS = value.contains("a");
			MATCH_PAGE_COUNT = value.contains("p");
			MATCH_TITLE = value.contains("t");
			MATCH_VENUE = value.contains("v");
		}
		else
		{
			System.out.println("Unexpected parameter '" + parameter +"'\n");
		}
	}
	
	public static String print()
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		String format = "%-25s %-25s\r\n\r\n";
		
		stringBuilder.append("Configuration\r\n\r\n");
		stringBuilder.append(String.format(format, Parameters.THRESHOLD.get(2) + ": ", THRESHOLD));
		stringBuilder.append(String.format(format, Parameters.SIMILARITY.get(2) + ": ", SIMILARITY));
		
		String buildIndexString = (BUILD_INDEX + "").toUpperCase();
		if (BUILD_INDEX)
		{
			buildIndexString += ", from file '" + DBLP_PATH + "'";
		}
		
		stringBuilder.append(String.format(format, Parameters.BUILD_INDEX.get(2) + ": ", buildIndexString));
		
		String removeStopWordsString = (REMOVE_STOPWORDS + "").toUpperCase();
		if (REMOVE_STOPWORDS)
		{
			removeStopWordsString += ", from file '" + STOP_WORDS_PATH + "'";
		}
		
		stringBuilder.append(String.format(format, Parameters.REMOVE_STOPWORDS.get(2) + ": ", removeStopWordsString));
		
		String writeMergedFilesString = (WRITE_MERGED_FILES + "").toUpperCase();
		if (WRITE_MERGED_FILES)
		{
			writeMergedFilesString += ", in folder '" + MERGED_FOLDER_PATH + "'";
		}
		
		stringBuilder.append(String.format(format, Parameters.WRITE_MERGED_FILES.get(2) + ": ", writeMergedFilesString));
		
		String outputStatsString = (OUTPUT_STATS + "").toUpperCase();
		if (OUTPUT_STATS)
		{
			outputStatsString += ", in file '" + STATS_FILE_PATH + "', using manual matches from file '" + MATCHES_FILE_PATH + "'";
		}
		
		stringBuilder.append(String.format(format, Parameters.OUTPUT_STATS.get(2) + ": ", outputStatsString));
		
		String operatorString = OPERATOR.toUpperCase();
		if ("OR".equals(operatorString))
		{
			operatorString += ", using " + N_GRAMS + "-grams";
		}
		
		stringBuilder.append(String.format(format, Parameters.OPERATOR.get(2) + ": ", operatorString));
				
		String matchingFeaturesString = "";
		
		if (MATCH_TITLE)
		{
			matchingFeaturesString += Parameters.MATCH_TITLE.get(0).toString().substring(6) + ", ";
		}
		if (MATCH_AUTHORS)
		{
			matchingFeaturesString += Parameters.MATCH_AUTHORS.get(0).toString().substring(6) + ", ";
		}
		if (MATCH_PAGE_COUNT)
		{
			matchingFeaturesString += Parameters.MATCH_PAGE_COUNT.get(0).toString().substring(6) + " (with page limit " + PAGE_LIMIT + "), ";
		}
		if (MATCH_VENUE)
		{
			matchingFeaturesString+= Parameters.MATCH_VENUE.get(0).toString().substring(6) + ", ";
		}
		
		stringBuilder.append(String.format(format, "MATCHING FEATURES: ", 
				matchingFeaturesString.contains(",") ? matchingFeaturesString.substring(0, matchingFeaturesString.lastIndexOf(",")) : ""));
		
		stringBuilder.append(String.format(format, Parameters.BASE_URL.get(2) + ": ", BASE_URL));
		stringBuilder.append(String.format(format, Parameters.CITESEER_PATH.get(2) + ": ", "'" + CITESEER_PATH + "'"));
		if (MATCH_PAGE_COUNT)
		{
			stringBuilder.append(String.format(format, Parameters.PAGE_COUNT_FILE_PATH.get(2) + ": ", "'" + PAGE_COUNT_FILE_PATH + "'"));
		}
		
		stringBuilder.append(String.format(format, Parameters.HITS_FILE_PATH.get(2) + ": ", "'" + HITS_FILE_PATH + "'"));
		stringBuilder.append(String.format(format, Parameters.LOG_FILE_PATH.get(2) + ": ", "'" + LOG_FILE_PATH + "'"));
		stringBuilder.append(String.format(format, Parameters.VERBOSITY_LEVEL.get(2) + ": ", VERBOSITY_LEVEL));
		
		return stringBuilder.toString();
	}
	
	public static String help()
	{
		StringBuilder help = new StringBuilder();
		
		String format = "%-5s %-100s\r\n";
		help.append("Command line parameters\r\n\r\n");
		help.append(String.format(format, "abbr.", "full") + "\r\n");
		help.append(String.format(format, Parameters.THRESHOLD.get(0), Parameters.THRESHOLD.get(1)));
		help.append(String.format(format, Parameters.REMOVE_STOPWORDS.get(0), Parameters.REMOVE_STOPWORDS.get(1)));
		help.append(String.format(format, Parameters.WRITE_MERGED_FILES.get(0), Parameters.WRITE_MERGED_FILES.get(1)));
		help.append(String.format(format, Parameters.OUTPUT_FOLDER_PATH.get(0), Parameters.OUTPUT_FOLDER_PATH.get(1)));
		help.append(String.format(format, Parameters.OUTPUT_STATS.get(0), Parameters.OUTPUT_STATS.get(1)));
		help.append(String.format(format, Parameters.OPERATOR.get(0), Parameters.OPERATOR.get(1)));
		help.append(String.format(format, Parameters.SIMILARITY.get(0), Parameters.SIMILARITY.get(1)));
		help.append(String.format(format, Parameters.N_GRAMS.get(0), Parameters.N_GRAMS.get(1)));
		help.append(String.format(format, Parameters.PAGE_LIMIT.get(0), Parameters.PAGE_LIMIT.get(1)));
		help.append(String.format(format, Parameters.BUILD_INDEX.get(0), Parameters.BUILD_INDEX.get(1)));
		help.append(String.format(format, Parameters.BASE_URL.get(0), Parameters.BASE_URL.get(1)));
		help.append(String.format(format, Parameters.DBLP_PATH.get(0), Parameters.DBLP_PATH.get(1)));
		help.append(String.format(format, Parameters.CITESEER_PATH.get(0), Parameters.CITESEER_PATH.get(1)));
		help.append(String.format(format, Parameters.MERGED_FOLDER_PATH.get(0), Parameters.MERGED_FOLDER_PATH.get(1)));
		help.append(String.format(format, Parameters.STATS_FILE_PATH.get(0), Parameters.STATS_FILE_PATH.get(1)));
		help.append(String.format(format, Parameters.HITS_FILE_PATH.get(0), Parameters.HITS_FILE_PATH.get(1)));
		help.append(String.format(format, Parameters.STOP_WORDS_PATH.get(0), Parameters.STOP_WORDS_PATH.get(1)));
		help.append(String.format(format, Parameters.MATCHES_FILE_PATH.get(0), Parameters.MATCHES_FILE_PATH.get(1)));
		help.append(String.format(format, Parameters.PAGE_COUNT_FILE_PATH.get(0), Parameters.PAGE_COUNT_FILE_PATH.get(1)));
		help.append(String.format(format, Parameters.VERBOSITY_LEVEL.get(0), Parameters.VERBOSITY_LEVEL.get(1)));
		help.append(String.format(format, Parameters.FEATURES.get(0), Parameters.FEATURES.get(1)));
		help.append(String.format(format, Parameters.LOG_FILE_PATH.get(0), Parameters.LOG_FILE_PATH.get(1)));
		help.append(String.format(format, Parameters.CONFIG_FILE_PATH.get(0), Parameters.CONFIG_FILE_PATH.get(1)));
		
		return help.toString();
	}
}