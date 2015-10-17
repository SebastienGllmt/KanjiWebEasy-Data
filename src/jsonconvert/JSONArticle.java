package jsonconvert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JSONArticle {
	
	/**
	 * PRECONDITION: This file has the correct file name of form news*date*_*ID* Gets the article id from the filename of a given file.
	 * @param articleFile - The article file to get the ID from
	 * @return the articleID
	 */
	public static String getArticleID(File articleFile) {
		return articleFile.getName().substring(0, articleFile.getName().lastIndexOf("."));
	}
	
	/**
	 * From a given jsonFile provided by the NHK, it will create a word list This will serve as an intermediate representation of the information for later
	 * output See <code>createJSONOutput</code> method for next step.
	 * @param jsonFile - The NHK provided json file to get the words from
	 * @param articleID - The articleID to associate with these words
	 * @return the list of words generated
	 */
	public static ArrayList<Word> createWordList(File jsonFile, String articleID) {
		// This method simply creates a JSONObject from jsonFile then calls the other version of createWordList
		try (InputStream is = jsonFile.toURI().toURL().openStream()) {
			JSONTokener tokener = new JSONTokener(is);
			JSONObject root = new JSONObject(tokener);
			return createWordList(root, articleID);
		} catch (JSONException | IOException e) {
			System.err.print("Failed to create word list from " + jsonFile.getAbsolutePath());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * From a given json object created from an NHK json file, it will create a word list This will serve as an intermediate representation of the information
	 * for later output See <code>createJSONOutput</code> method for next step.
	 * @param root - The json file to get the words from
	 * @param articleID - The articleID to associate with these words
	 * @return the list of words generated
	 */
	public static ArrayList<Word> createWordList(JSONObject root, String articleID) {
		
		ArrayList<Word> wordList = new ArrayList<Word>();
		boolean inQuote = false;
		int sentenceNum = 0;
		
		try {
			// Get the word (and symbol) array used to store every word in the article
			JSONArray words = root.getJSONArray("morph");
			for (int i = 0; i < words.length(); i++) {
				
				JSONObject wordInfo = words.getJSONObject(i);
				
				// Create the word object from the information provided
				Word word = WordPartFactory.createWord(wordInfo, articleID, sentenceNum);
				
				// If there's a new paragraph and we're still at the very start or the article (sentence 0) or the title (sentence 1)
				if (word.conjugated.equals(WordPartFactory.PARAGRAPH_START) && sentenceNum <= 1) {
					sentenceNum++;
				}
				if (word.conjugated.equals("「")) {
					inQuote = true;
				}
				if (word.conjugated.equals("」")) {
					inQuote = false;
				}
				if (word.conjugated.equals("。") && !inQuote) {
					sentenceNum++;
				}
				
				wordList.add(word);
			}
		} catch (JSONException e) {
			System.err.println("Error in creating word list");
			e.printStackTrace();
		}
		
		return wordList;
	}
	
	public static JSONObject createJSONOutput(List<Word> wordList) {
		JSONObject rootObject = new JSONObject();
		try {
			rootObject.put("words", new JSONObject());
			
			for (int i = 0; i < wordList.size(); i++) {
				
				Word currWord = wordList.get(i);
				
				// Skip <S> and </S> which indicate start/end of article respectively.
				if (currWord.clazz == WordPartFactory.PARAGRAPH_CLASS) {
					continue;
				}
				
				JSONObject rootWordObject = rootObject.getJSONObject("words");
				
				WordPartFactory.addWord(rootWordObject, currWord);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return rootObject;
	}
}
