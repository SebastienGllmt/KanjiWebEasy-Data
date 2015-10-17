package util;

public class WordAnalyzer {
	
	public static final int HIRAGANA_START = 0x3041;
	public static final int HIRAGANA_END = 0x3094;
	public static final int KATAKANA_START = 0x30A1;
	public static final int KATAKANA_END = 0x30FA;
	public static final int NUMBER_START = 0xFF10;
	public static final int NUMBER_END = 0xFF19;
	public static final int KANJI_START = 0x4E00;
	public static final int KANJI_END = 0x9FAF;
	public static final int FULLWIDTH_ALPHA_START = 0xFF20;
	public static final int FULLWIDTH_ALPHA_END = 0xFF5A;
	
	public static boolean isJapWord(String word) {
		if (word == null || word.isEmpty())
			return false;
		char firstLetter = word.charAt(0);
		if (isHiragana(firstLetter) || isKatakana(firstLetter) || isNumber(firstLetter) || isKanji(firstLetter)) {
			return true;
		}
		return false;
	}
	
	public static boolean containsNumber(String word) {
		for (int j = 0; j < word.length(); j++) {
			if (WordAnalyzer.isNumber(word.charAt(j))) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean containsKanji(String word) {
		for (int j = 0; j < word.length(); j++) {
			if (WordAnalyzer.isKanji(word.charAt(j))) {
				return true;
			}
		}
		return false;
	}
	
	public static String removeFuriganalessChars(String word) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (isNumber(c) || isKanji(c) || isFullWidthAlpha(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	public static boolean isHiragana(char c) {
		return c >= HIRAGANA_START && c <= HIRAGANA_END;
	}
	
	public static boolean isKatakana(char c) {
		return c >= KATAKANA_START && c <= KATAKANA_END;
	}
	
	public static boolean isNumber(char c) {
		return c >= NUMBER_START && c <= NUMBER_END;
	}
	
	public static boolean isKanji(char c) {
		return c >= KANJI_START && c <= KANJI_END;
	}
	
	public static boolean isFullWidthAlpha(char c) {
		return c >= FULLWIDTH_ALPHA_START && c <= FULLWIDTH_ALPHA_END;
	}
}
