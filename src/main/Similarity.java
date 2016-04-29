package main;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Alina Maria Ciobanu
 */
public class Similarity 
{
	/**
	 * computes Jaccard similarity between CiteSeer and DBLP titles
	 */
	public static double computeJaccardSimilarity(String citeseerTitle, String dblpTitle) 
	{
		try
		{
			List<String> citeseerTitleWords = Arrays.asList(citeseerTitle.split(MatchingHelper.PUNCTUATION_REGEX));
			List<String> dblpTitleWords = Arrays.asList(dblpTitle.split(MatchingHelper.PUNCTUATION_REGEX));
	
			Set<String> wordsSet = new HashSet<String>();
			Set<String> dblpSet = new HashSet<String>();
			Set<String> citeseerSet = new HashSet<String>();
	
			for (String word : citeseerTitleWords)
			{
				wordsSet.add(word.toLowerCase());
				citeseerSet.add(word.toLowerCase());
			}
	
			for (String word : dblpTitleWords)
			{
				wordsSet.add(word.toLowerCase());
				dblpSet.add(word.toLowerCase());
			}
	
			int nrOfCommonWords = 0;
	
			for (String word : wordsSet)
			{
				if (citeseerSet.contains(word) && dblpSet.contains(word))
				{
					nrOfCommonWords++;
				}
			}
	
			return wordsSet.size() != 0 ? nrOfCommonWords/(double)wordsSet.size() : 0;
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while computing Jaccard similarity for titles '" + citeseerTitle + "' and '" + dblpTitle + "'");
		}
		
		return 0;
	}
	
	/**
	 * computes cosine similarity between CiteSeer and DBLP titles
	 */
	public static double computeCosineSimilarity(String citeseerTitle, String dblpTitle)
	{
		try
		{
			Set<String> dictionary = getDictionary(dblpTitle, citeseerTitle);
			
			int[] dblpArray = getFrequencyArray(dblpTitle, dictionary);
			int[] citeseerArray = getFrequencyArray(citeseerTitle, dictionary);
			
			double dblpNorm = Math.sqrt(computeNorm(dblpArray));
	
			double citeseerNorm = Math.sqrt(computeNorm(citeseerArray));
	
			double score = 0.0;
			
			if(dblpNorm * citeseerNorm != 0.0)
			{
				score = computeInnerProduct(dblpArray, citeseerArray) / (dblpNorm * citeseerNorm);
			}
	
			return score;
		}
		catch (Exception ex)
		{
			MatchingHelper.logMessage(ex, "ERROR while computing cosine similarity for titles '" + citeseerTitle + "' and '" + dblpTitle + "'");
		}
		
		return 0;
	} 
	
	/**
	 * computes norm
	 */
	public static final int computeNorm(int[] v) 
	{
		int norm = 0;
		
		for(int i = 0; i < v.length; i++)
		{
			norm += v[i] * v[i];
		}
		return norm;
	}
	
	/**
	 * computes inner product
	 */
	public static final int computeInnerProduct(int[] v1, int[] v2) 
	{
		int r = 0;
		
		if (v1.length != v2.length)
		{
			MatchingHelper.logMessage(null, "Incorrect vectors' size");
			return 0;
		}
		
		for(int i = 0; i < v1.length; i++)
		{
			r += v1[i] * v2[i];
		}
		
		return r;
	}
	
	/**
	 * retrieves word dictionary for DBLP and CiteSeer titles
	 */
	public static Set<String> getDictionary(String dblpTitle, String citeseerTitle)
	{
		Set<String> wordsSet = new HashSet<String>();
		
		wordsSet.addAll(Arrays.asList(dblpTitle.toLowerCase().split(MatchingHelper.PUNCTUATION_REGEX)));
		wordsSet.addAll(Arrays.asList(citeseerTitle.toLowerCase().split(MatchingHelper.PUNCTUATION_REGEX)));
		
		return wordsSet;
	}
	
	/**
	 * builds word frequency map for a given title
	 */
	public static Map<String, Integer> buildFrequencyMap(String title)
	{
		Map<String, Integer> freqMap = new HashMap<String, Integer>();
		
		String[] words = title.toLowerCase().split(MatchingHelper.PUNCTUATION_REGEX);
		
		for (String word : words)
		{
			if (freqMap.get(word) == null)
			{
				freqMap.put(word, 1);
			}
			else
			{
				freqMap.put(word, freqMap.get(word) + 1);
			}
		}
		
		return freqMap;
	}
	
	/**
	 * builds frequency array for a given title for all the words in the dictionary
	 */
	public static int[] getFrequencyArray(String title, Set<String> dictionary)
	{
		int[] vector = new int[dictionary.size()];

		Map<String, Integer> freqMap = buildFrequencyMap(title);
		
		Iterator<String> it = dictionary.iterator();
		
		int i = 0;
		
		while (it.hasNext())
		{
			Integer freq = freqMap.get(it.next());
			vector[i++] = freq != null ? freq : 0; 
		}
		
		return vector;
	}
}