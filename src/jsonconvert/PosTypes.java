package jsonconvert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PosTypes {
	
	private static final String ROOT_PATH = "D://Users/Sebastien/Desktop/prog/JavaProg/NHKExtractor/res/";
	private static final Map<String, MapObject> posSet = new HashMap<>();
	
	public static void main(String[] args) {
		visit(new File(ROOT_PATH));
		
		ArrayList<MapResult> results = new ArrayList<>();
		for (String type : posSet.keySet()) {
			results.add(new MapResult(type, posSet.get(type).examples, posSet.get(type).count));
		}
		Collections.sort(results);
		for (MapResult mr : results) {
			System.out.println(mr);
		}
	}
	
	private static void visit(File f) {
		if (f.isDirectory()) {
			File[] children = f.listFiles();
			for (File child : children) {
				visit(child);
			}
			return;
		}
		
		getPos(f);
	}
	
	private static void getPos(File f) {
		try (InputStream is = new FileInputStream(f); Reader r = new InputStreamReader(is, "UTF-8")) {
			StringBuilder sb = new StringBuilder();
			
			int ch = r.read();
			while (ch >= 0) {
				sb.append((char) ch);
				ch = r.read();
			}
			
			JSONTokener tokener = new JSONTokener(sb.toString());
			JSONObject root = new JSONObject(tokener);
			JSONArray words = root.getJSONArray("morph");
			for (int i = 0; i < words.length(); i++) {
				JSONObject word = words.getJSONObject(i);
				String[] types = Word.getType(word);
				for (String type : types) {
					String base = (String) word.get("base");
					MapObject oldMap = posSet.get(type);
					MapObject newMap = new MapObject(base, oldMap);
					posSet.put(type, newMap);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	static class MapObject {
		public List<String> examples;
		public int count;
		
		public MapObject(String word, MapObject old) {
			if (old != null) {
				this.count = old.count + 1;
				
				this.examples = old.examples;
				if (this.examples.size() < 5) {
					if (!this.examples.contains(word)) {
						this.examples.add(word);
					}
				}
			} else {
				this.count = 1;
				this.examples = new ArrayList<>();
				this.examples.add(word);
			}
			
		}
	}
	
	static class MapResult implements Comparable<MapResult> {
		public String type;
		public List<String> example;
		public int count;
		
		public MapResult(String type, List<String> example, int count) {
			this.type = type;
			this.example = example;
			this.count = count;
		}
		
		@Override
		public String toString() {
			String result = this.type;
			result += " (" + count + ")";
			for (String ex : example) {
				result += "\n\t" + ex;
			}
			return result;
		}
		
		@Override
		public int compareTo(MapResult o) {
			if (this.count < o.count) {
				return 1;
			} else if (this.count == o.count) {
				return 0;
			} else {
				return -1;
			}
		}
	}
	
}
