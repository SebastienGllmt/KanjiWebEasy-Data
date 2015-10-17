package util;

import java.util.Iterator;

public class SentenceIterator implements Iterator<String> {
	
	private int					startIndex, endIndex;
	private String			articleText;
	private static char	JAPANESE_SPACE	= '\u3000';
	private static char	NORMAL_SPACE		= '\u0020';
	
	public SentenceIterator(String articleText) {
		this.startIndex = 0;
		this.endIndex = 0;
		this.articleText = articleText;
	}
	
	@Override
	public boolean hasNext() {
		this.startIndex = this.endIndex;
		this.startIndex = SentenceIterator.getStart(this.startIndex, this.articleText);
		return this.startIndex != this.articleText.length();
	}
	
	@Override
	public String next() {
		this.startIndex = this.endIndex;
		this.startIndex = SentenceIterator.getStart(this.startIndex, this.articleText);
		
		this.endIndex = SentenceIterator.getEnd(this.startIndex, this.articleText);
		
		return this.articleText.substring(this.startIndex, this.endIndex);
	}
	
	public static int getStart(int startIndex, String articleText) {
		while (startIndex != articleText.length() && (articleText.charAt(startIndex) == JAPANESE_SPACE || articleText.charAt(startIndex) == NORMAL_SPACE)) {
			startIndex++;
		}
		return startIndex;
	}
	public static int getEnd(int startIndex, String articleText) {
		int endIndex = startIndex;
		boolean inQuote = false;
		while (endIndex != articleText.length()) {
			switch (articleText.charAt(endIndex)) {
				case '「':
					inQuote = true;
					break;
				case '」':
					inQuote = false;
					break;
				case '。':
					if (!inQuote) {
						return endIndex + 1;
					}
					break;
			}
			endIndex++;
		}
		return endIndex;
	}
}
