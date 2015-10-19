package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import kanjicounter.KanjiBucketer;
import suffixtree.ArticleInfo;
import suffixtree.ParseResult;
import suffixtree.SentenceInfo;
import suffixtree.SuffixTree;

public class NewsEasyFormatUtil {
	
	/**
	 * The indent used for printing the resulting JSON outputs
	 */
	private static final int		INDENT					= 2;
	private static final String	SENTENCE_START	= "<S>";
	private static final String	SENTENCE_END		= "</S>";
	private static final String	WORD_TOKEN			= "word";
	private static final String	TEXT_KEY				= "text";
	private static final String	NEWS_ID_KEY			= "newsid";
	private static final String	MORPH_KEY				= "morph";
	
	public static String readURLAsString(String url) {
		try {
			return new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static JSONObject createJSONFromFile(String path) {
		return createJSONFromFile(new File(path));
	}
	
	public static JSONObject createJSONFromFile(File file) {
		JSONObject outputObject = null;
		
		try (FileInputStream fis = new FileInputStream(file)) {
			outputObject = new JSONObject(new JSONTokener(fis));
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
		return outputObject;
	}
	
	/**
	 * Print a jsonObject to a given file with the indent stored in the final field <code>INDENT</code>
	 * @param output - The file to output to
	 * @param json - The json file to create the output from
	 */
	public static void printJSONObject(File output, JSONObject json) {
		try (PrintStream newJSONPrinter = new PrintStream(output, "UTF-8")) {
			newJSONPrinter.print(json.toString(INDENT));
		} catch (FileNotFoundException | JSONException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
	}
	
	public static String getArticleID(File articleFile) {
		return articleFile.getName().substring(0, articleFile.getName().lastIndexOf("."));
	}
	
	public static String getFileDate(String articleName) {
		return articleName.substring("news".length(), "news00000000".length());
	}
	
	public static String getArticleKey(String articleName) {
		return articleName.substring("news00000000_".length(), articleName.length());
	}
	
	public static ParseResult getArticleText(File jsonInput, JsonFactory jsonFactory) {
		String articleText = "";
		String articleTitle = "";
		
		try (JsonParser jp = jsonFactory.createParser(jsonInput)) {
			jp.nextToken(); // consume the start token
			while (jp.nextToken() != JsonToken.END_OBJECT) {
				String fieldname = jp.getCurrentName();
				jp.nextToken(); // now that we've gotten the field name, consume the token to get the value
				
				if (NEWS_ID_KEY.equals(fieldname)) {
					// do nothing
				} else if (TEXT_KEY.equals(fieldname)) {
					articleText = jp.getText();
				} else if (MORPH_KEY.equals(fieldname)) {
					articleTitle = getTitle(jp);
					
					// if we got here, we already got the article text and we're ready to end parsing
					assert !articleText.isEmpty();
					break;
				} else {
					System.err.println("Found unknown token " + fieldname);
					assert false; // should have been the first result
				}
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		assert !articleText.isEmpty();
		assert !articleTitle.isEmpty();
		
		// remove the title from the article string
		int titleEndIndex = articleTitle.length();
		while (articleText.charAt(titleEndIndex) == ' ' || articleText.charAt(titleEndIndex) == '\u3000') {
			titleEndIndex++;
		}
		articleText = articleText.substring(titleEndIndex);
		
		return new ParseResult(articleTitle, articleText);
	}
	
	private static String getTitle(JsonParser jp) throws JsonParseException, IOException {
		StringBuilder title = new StringBuilder(30);
		
		String currToken = "";
		while (true) {
			currToken = jp.nextFieldName();
			
			// if this is not a title (maybe a start array or something) just skip it
			if (currToken == null) {
				continue;
			}
			
			jp.nextToken();  // consume token for field title
			if (WORD_TOKEN.equals(currToken)) {
				String word = jp.getText();
				if (SENTENCE_START.equals(word)) {
					continue;
				}
				if (SENTENCE_END.equals(word)) {
					break;
				} else {
					title.append(word);
				}
			}
		}
		
		return title.toString();
	}
	
  public static String getSentenceFromArticleInfo(Path dir, ArticleInfo article, JsonFactory jsonFactory) {
    String date = NewsEasyFormatUtil.getFileDate(article.article);
    String articleKey = NewsEasyFormatUtil.getArticleKey(article.article);
    String path = String.format("%s/%s/%s/%s.json", dir, date, articleKey, article.article);

    File jsonInput = new File(path);

    ParseResult parseResult = NewsEasyFormatUtil.getArticleText(jsonInput, jsonFactory);

    if (article.sentenceNumber == 1) {
      return parseResult.TITLE;
    }

    int sentenceNum = 2;
    SentenceIterator si = new SentenceIterator(parseResult.ARTICLE);
    while (si.hasNext()) {
      String sentence = si.next();
      if (sentenceNum == article.sentenceNumber) {
        return sentence;
      }
      sentenceNum++;
    }

    assert false;
    return "";
  }

  public static long parseAllFiles(Path dir, SuffixTree<ArticleInfo> st, JsonFactory jsonFactory, Map<Character, Integer> symbolMap) {
    long treeBuildTime = 0;
    if (Files.isDirectory(dir)) {
      try {
        DirectoryStream<Path> dirStream;
        dirStream = Files.newDirectoryStream(dir);
        for (Path p : dirStream) {
          treeBuildTime += parseAllFiles(p, st, jsonFactory, symbolMap);
        }
      } catch (IOException e) {
        System.err.println("Unable to access " + dir.getFileName());
        e.printStackTrace();
      }
    } else {
      treeBuildTime += parseJSONFile(dir.toFile(), st, jsonFactory, symbolMap);
    }
    
    return treeBuildTime;
  }

  private static long parseJSONFile(File jsonInput, SuffixTree<ArticleInfo> st, JsonFactory jsonFactory, Map<Character, Integer> symbolMap) {
    String articleID = NewsEasyFormatUtil.getArticleID(jsonInput);

    ParseResult parseResult = NewsEasyFormatUtil.getArticleText(jsonInput, jsonFactory);

    st.addString(new SentenceInfo(parseResult.TITLE, KanjiBucketer.getSuffixBuckets(parseResult.TITLE, symbolMap)), new ArticleInfo(articleID, (short) 1));

    long treeBuildTime = 0;
    byte sentenceNum = 2;
    SentenceIterator si = new SentenceIterator(parseResult.ARTICLE);
    while (si.hasNext()) {
      long now = System.currentTimeMillis();
      String sentence = si.next();
      int[] suffixBuckets = KanjiBucketer.getSuffixBuckets(sentence, symbolMap);
      st.addString(new SentenceInfo(sentence, suffixBuckets), new ArticleInfo(articleID, sentenceNum));
      treeBuildTime += System.currentTimeMillis() - now;
      sentenceNum++;
    }
    
    return treeBuildTime;
  }
}
