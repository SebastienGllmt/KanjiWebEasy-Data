package kanjimatcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PairReader {
	private int index;
	private List<DatabaseItemWrapper> allPairs;

	public PairReader(JSONObject inputRoot, Set<PairInfo<String>> preprocessedPairs) {
		this.allPairs = generateAllPairs(inputRoot, preprocessedPairs);
		this.index = 0;
	}
	
	public int size(){
		return allPairs.size();
	}

	public boolean hasNext() {
		return index < allPairs.size();
	}

	public DatabaseItemWrapper next() {
		if (!hasNext()) {
			return null;
		}
		return allPairs.get(index++);
	}
	
	public DatabaseItemWrapper getCurr(){
		if(index > 0){
			return allPairs.get(index-1);
		}else{
			return allPairs.get(0);
		}
	}

	@SuppressWarnings("unchecked")
	public static List<DatabaseItemWrapper> generateAllPairs(JSONObject inputRoot, Set<PairInfo<String>> preprocessedPairs) {
		Set<DatabaseItemWrapper> allPairs = new HashSet<>();
		inputRoot.keys().forEachRemaining(key -> {
			try {
				String type = (String) key;
				JSONObject words = inputRoot.getJSONObject(type).getJSONObject("words");
				words.keys().forEachRemaining(wordKey -> {
					String word = (String) wordKey;
					try {
						JSONObject wordObject = words.getJSONObject(word);
						
						JSONArray kanjiArray = wordObject.getJSONArray("kanji");
						JSONObject readings = wordObject.getJSONObject("readings");
						readings.keys().forEachRemaining(readingKey -> {
							String reading = (String) readingKey;

							try {
								JSONObject readingInner = readings.getJSONObject(reading);
								
								JSONArray furiganaArray = readingInner.getJSONArray("furigana");
								
								for(int i=0; i<kanjiArray.length(); i++){
									String kanji = kanjiArray.getString(i);
									if(kanji.isEmpty()){
										continue;
									}
									
									String furigana = furiganaArray.getString(i);
									
									PairInfo<String> pair = new PairInfo<>(kanji, furigana);
									if(!preprocessedPairs.contains(pair)){
										allPairs.add(new DatabaseItemWrapper(word, pair));
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}							
						});
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		return new ArrayList<DatabaseItemWrapper>(allPairs);
	}

	@SuppressWarnings("unchecked")
	public static Set<PairInfo<String>> readPreprocessedKeys(JSONObject knownJSON) {
		Set<PairInfo<String>> knownReadings = new HashSet<>();

		JSONObject words;
		try {
			words = knownJSON.getJSONObject("words");
			words.keys().forEachRemaining(wordKey -> {
				String word = (String) wordKey;
				try {
					JSONObject readings = words.getJSONObject(word);
					readings.keys().forEachRemaining(readingKey -> {
						String reading = (String) readingKey;

						PairInfo<String> existingPair = new PairInfo<>(word, reading);
						knownReadings.add(existingPair);
					});
				} catch (Exception e) {
					e.printStackTrace();
				}

			});
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		return knownReadings;
	}
}