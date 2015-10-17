package suffixtree;

/**
 * Reference: http://jakeboxer.com/blog/2009/12/13/the-knuth-morris-pratt-algorithm-in-my-own-words/
 */
public class KMP {
	
	public static void main(String[] args) {
		String text = "This is an example text";
		String word = "example";
		int startIndex = KMPSearch(text, word);
		System.out.println("Word found at index " + startIndex);
	}
	
	/**
	 * Finds word in text
	 * If found, returns index where word starts
	 * If not found, returns length of text
	 * 
	 * NOTE: This algorithm is good if you only search for a single word in text.
	 * If searches need to by applied multiple times, look into Suffix Tree
	 */
	public static int KMPSearch(String text, String word) {
		int tIndex = 0, wIndex = 0;
		
		int[] partialMatch = getPartialMatchTable(word);
		
		while (tIndex + wIndex < text.length()) {
			// First figure out how many letters we find in a row (denoted by wIndex)
			if (word.charAt(wIndex) == text.charAt(tIndex + wIndex)) {
				// if we found our entire word
				if (wIndex == word.length() - 1) {
					return tIndex;
				}
				wIndex++;
			} else {
				// Found mismatch. We may be able to skip forward depending on how many matches in a row we saw.
				if (wIndex == 0) {
					tIndex++;
				} else {
					/*
					 * We can skip ahead in the text input
					 * Example:
					 * bacbababaabcbab
					 * |||||
					 * abababca
					 * match of 5 characters long
					 * Looking at table from the other function, partialMatch[5]=3
					 * So we know the last 3 digits are the same as the first 3, so we can skip forward by 5-3 = 2
					 * bacbababaabcbab
					 * xx|||
					 * abababca
					 */
					
					tIndex += (wIndex - partialMatch[wIndex]);
					wIndex = partialMatch[wIndex];
				}
			}
		}
		return text.length();
	}
	
	/**
	 ** Example
	 ** char : | $ | a | b | a | b | a | b | c | a |
	 ** index: | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
	 ** value: | 0 | 0 | 0 | 1 | 2 | 3 | 4 | 0 | 1 |
	 **/
	public static int[] getPartialMatchTable(String word) {
		// T[i] is The length of the longest proper prefix in the (sub)pattern that matches a proper suffix in the same (sub)pattern.
		int[] table = new int[word.length() + 1];
		
		// special symbol at the start
		table[0] = 0;
		
		int pos = 1; // Note the first index is always 0, so we skip it
		
		for (int i = 1; i < word.length(); i++) {
			int prev = table[i - 1];
			table[pos + 1] = word.charAt(i) == word.charAt(prev) ? prev + 1 : 0;
			pos++;
		}
		
		return table;
	}
}
