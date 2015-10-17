package jsonconvert;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import util.WordAnalyzer;

public class WordPartFactory {
	
	private static Logger				creationErrorLogger;
	
	static {
		creationErrorLogger = Logger.getLogger(WordPartFactory.class.getName());
		creationErrorLogger.setLevel(Level.SEVERE);
	}
	
	public static final String	PARAGRAPH_START	= "<S>";
	public static final String	PARAGRAPH_END		= "</S>";
	public static final char		PARAGRAPH_CLASS	= '?';
	
	public static void addWord(JSONObject rootWordObject, Word currWord) throws JSONException {
		JSONObject wordObject = rootWordObject.optJSONObject(currWord.base);
		if (wordObject == null) {
			createJSONWord(rootWordObject, currWord);
		} else {
			JSONObject readings = wordObject.getJSONObject("readings");
			
			String furiganaKey = getFullReading(currWord.furigana);
			
			JSONObject furiganaInner;
			if (readings.has(furiganaKey)) {
				furiganaInner = readings.getJSONObject(furiganaKey);
				JSONObject examples = furiganaInner.getJSONObject("examples");
				createExampleObject(examples, currWord.articleID, currWord.sentenceNum);
			} else {
				furiganaInner = createFuriganaInner(currWord);
				readings.put(furiganaKey, furiganaInner);
			}
		}
	}
	
	public static void createJSONWord(JSONObject rootWordObject, Word currWord) throws JSONException {
		JSONObject innerWord = new JSONObject();
		
		String fullReading = getFullReading(currWord.furigana);
		
		JSONObject readings = new JSONObject();
		readings.put(fullReading, createFuriganaInner(currWord));
		
		innerWord.put("readings", readings);
		
		rootWordObject.put(currWord.base, innerWord);
	}
	
	private static String getFullReading(String[] furigana) {
		String fullReading = "";
		for (String s : furigana) {
			fullReading += s;
		}
		return fullReading;
	}
	
	public static JSONObject createFuriganaInner(Word currWord) throws JSONException {
		JSONObject furiganaInner = new JSONObject();
		furiganaInner.put("furigana", currWord.furigana);
		furiganaInner.put("kanji", currWord.kanji);
		
		JSONObject examples = new JSONObject();
		createExampleObject(examples, currWord.articleID, currWord.sentenceNum);
		furiganaInner.put("examples", examples);
		
		furiganaInner.put("type", currWord.type);
		furiganaInner.put("class", new String(new char[] { currWord.clazz }));
		
		return furiganaInner;
	}
	
	private static void createExampleObject(JSONObject examples, String articleID, int sentenceNum) throws JSONException {
		JSONArray sentences = examples.optJSONArray(articleID);
		if (sentences == null) {
			sentences = new JSONArray(new int[] { sentenceNum });
			examples.put(articleID, sentences);
		} else {
			sentences.put(sentenceNum);
		}
	}
	
	/**
	 * Creates a list of words based off a web token
	 * @param wordInfo
	 * @return list of words to add
	 * @throws JSONException
	 */
	public static Word createWord(JSONObject wordInfo, String articleID, int sentenceNum) throws JSONException {
		String base;
		
		String conjugated = wordInfo.getString("word");
		
		if (!wordInfo.has("base")) {
			// Check if these are the special words representing the start and end of an article respectively
			// The choice of <S> and </S> was because those are the ones the NHK use
			if (conjugated.equals(WordPartFactory.PARAGRAPH_START) || conjugated.equals(WordPartFactory.PARAGRAPH_END)) {
				return new Word(conjugated, conjugated, new String[0], new String[0], new String[0], PARAGRAPH_CLASS, articleID, sentenceNum);
			} else {
				base = conjugated;
				creationErrorLogger.info("Found a word " + conjugated + " with no base that was not article start or article end. Assumed 'conjugated' as 'base'");
			}
		} else {
			base = wordInfo.getString("base");
			if (base.isEmpty()) {
				creationErrorLogger.info("Found a word " + conjugated + " with empty base. Assumed 'conjugated' as 'base'");
				base = conjugated;
			}
		}
		
		String[] furigana = null, kanji = null;
		
		JSONArray ruby = wordInfo.getJSONArray("ruby");
		
		kanji = new String[ruby.length()];
		furigana = new String[ruby.length()];
		for (int i = 0; i < ruby.length(); i++) {
			JSONObject kanjiAndReading = ruby.getJSONObject(i);
			
			// If is kanji and has furigana
			if (kanjiAndReading.has("r")) {
				furigana[i] = kanjiAndReading.getString("r");
				kanji[i] = kanjiAndReading.getString("s");
			} else {
				furigana[i] = kanjiAndReading.getString("s");
				if (WordAnalyzer.containsKanji(furigana[i])) {
					creationErrorLogger.severe(articleID + " Found word " + base + " (" + furigana[i] + ") with no furigana. Instead found " + furigana[i]);
					// These exceptions are rare and manually fixed
					/*
					 * History log of issues fixed:
					 * 
					 * news20141030_k10015760731000 Found word 東京 (東京) with no furigana. Instead found 東京
					 * news20150130_k10015018801000 Found word 歩く (歩き) with no furigana. Instead found 歩き
					 * news20150130_k10015018801000 Found word 回る (回る) with no furigana. Instead found 回る
					 */
				} else {
					kanji[i] = "";
				}
				
				// If it contains a number that doesn't have a furigana reading
				if (WordAnalyzer.containsNumber(furigana[i])) {
					String[] oldKanji = kanji;
					kanji[i] = WordAnalyzer.removeFuriganalessChars(furigana[i]);
					if (wordInfo.has("kana")) {
						String katakana = wordInfo.getString("kana");
						if (!katakana.isEmpty()) {
							if (!katakana.equals("*")) {
								if (!katakana.startsWith(Arrays.stream(furigana).filter(s -> s != null).reduce("", (a, b) -> a + b))) {
									String[] oldFurigana = furigana;
									kanji = new String[] { WordAnalyzer.removeFuriganalessChars(conjugated) };
									furigana = new String[] { katakana };
									creationErrorLogger.info("Found word base: " + base + "  conj: " + conjugated + " containing # inside furigana." + " Converted furigana " + Arrays.toString(oldFurigana) + " to " + Arrays.toString(furigana) + " and " + Arrays.toString(oldKanji) + " to " + Arrays.toString(kanji));
									break;
								} else {
									creationErrorLogger.info("Found word base: " + base + "  conj: " + conjugated + " containing # inside furigana and word had furigana=katakana." + " furigana left as " + Arrays.toString(furigana) + " and kanji left as " + Arrays.toString(kanji));
								}
							} else {
								creationErrorLogger.warning("Found word base: " + base + "  conj: " + conjugated + " containing # inside furigana and word had * as kata." + " furigana left as " + Arrays.toString(furigana) + " and kanji left as " + Arrays.toString(kanji));
							}
						} else {
							creationErrorLogger.warning("Found word base: " + base + "  conj: " + conjugated + " containing # inside furigana and word had empty kata." + " furigana left as " + Arrays.toString(furigana) + " and kanji left as " + Arrays.toString(kanji));
						}
					} else {
						creationErrorLogger.warning("Found word base: " + base + "  conj: " + conjugated + " containing # inside furigana and word had no kata to substitute with." + " furigana left as " + Arrays.toString(furigana) + " and kanji left as " + Arrays.toString(kanji));
					}
				}
			}
		}
		
		String[] type = Word.getType(wordInfo);
		
		char clazz;
		if (wordInfo.has("class")) {
			clazz = wordInfo.getString("class").charAt(0);
		} else {
			clazz = ArticleConverter.NO_CLASS;
			creationErrorLogger.warning("Found a word " + conjugated + " (" + base + ") " + "with no class");
		}
		
		if (base.equals("*")) {
			// If is number
			if (clazz == 'B') {
				creationErrorLogger.warning("Found a number with no base " + conjugated + " (" + clazz + "). Assumed 'conjugated' as 'base'");
				base = conjugated;
			} else {
				creationErrorLogger.info("Found a word with no base " + conjugated + " (" + clazz + "). Assumed 'conjugated' as 'base'");
				base = conjugated;
			}
		}
		
		if (base.equals("い")) {
			creationErrorLogger.info("Marked い as grammar point");
			clazz = 'B';
		}
		
		if (base.equals("ＡＫＢ４８")) {
			if (furigana.length == 1 && furigana[0].equals("エーケービー")) {
				if (kanji.length == 1 && kanji[0].equals("ＡＫＢ４８")) {
					kanji[0] = "ＡＫＢ";
					creationErrorLogger.info("Removed extra kanji from AKB48");
				}
			}
		}
		
		return new Word(base, conjugated, kanji, furigana, type, clazz, articleID, sentenceNum);
	}
}
