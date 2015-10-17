package kanjicounter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonFactory;

import suffixtree.ParseResult;
import util.NewsEasyFormatUtil;
import util.SentenceIterator;

public class KanjiBucketer {
	
	private static final Map<Character, Integer>	kanjiMap		= new HashMap<>();
	final static int															NUM_BUCKETS	= 32;
	private static final String										IN_DIR			= "in";
	
	public static void main(String[] args) throws IOException {
		final List<Symbol> symbols = getSymbolStats();
		getDistributedBuckets(symbols);
	}
	
	private static boolean tryFill(Bucket[] buckets, List<Symbol> symbols, double idealFilled) {
		int currBucket = 0;
		for (int i = 0; i < symbols.size(); i++) {
			Symbol currSymbol = symbols.get(i);
			if (buckets[currBucket].filled + currSymbol.freq > idealFilled) {
				currBucket++;
			}
			
			if (currBucket >= buckets.length) {
				System.out.printf("Failed at %d out of %d.\n", i, symbols.size());
				return false;
			}
			
			buckets[currBucket].add(currSymbol);
		}
		return true;
	}
	
	/**
	 * Tries to bucket nearby unicode characters together
	 * @param symbols
	 */
	private static void getBucketBuckets(List<Symbol> symbols) {
		Bucket[] buckets = new Bucket[NUM_BUCKETS];
		
		Collections.sort(symbols, (s1, s2) -> s1.symbol < s2.symbol ? 1 : (s1.symbol == s2.symbol ? 0 : -1));
		
		double idealFilled = 1.0 / NUM_BUCKETS;
		while (true) {
			for (int i = 0; i < NUM_BUCKETS; i++) {
				buckets[i] = new Bucket();
			}
			if (!tryFill(buckets, symbols, idealFilled)) {
				idealFilled += 0.001;
			} else {
				break;
			}
		}
	}
	
  /**
   * Returns an array where index i represents all the buckets seen from descending from sentence.length()-1,...,i
   */
  public static int[] getSuffixBuckets(String sentence, Map<Character, Integer> symbolMap) {
    int[] suffixBuckets = new int[sentence.length()];

    int cummulativeBucket = 0;
    for (int i = sentence.length() - 1; i >= 0; i--) {
      assert symbolMap.containsKey(sentence.charAt(i));
      cummulativeBucket |= symbolMap.get(sentence.charAt(i));
      suffixBuckets[i] = cummulativeBucket;
    }

    return suffixBuckets;
  }
	
	/**
	 * Tries to get an optimal bucketing
	 * @param symbols
	 * @return
	 */
	private static Map<Character, Integer> getDistributedBuckets(List<Symbol> symbols) {
		PriorityQueue<Bucket> buckets = new PriorityQueue<>(NUM_BUCKETS);
		Map<Character, Integer> symbolMap = new HashMap<>();
		
		int currSymbolIndex = 0;
		// initially just put the most common elements in their own buckets
		// this initializes all our buckets to start
		for (currSymbolIndex = 0; currSymbolIndex < NUM_BUCKETS; currSymbolIndex++) {
			Bucket b = new Bucket();
			b.add(symbols.get(currSymbolIndex));
			buckets.add(b);
			
			assert currSymbolIndex < 32;
			symbolMap.put(symbols.get(currSymbolIndex).symbol, 1 << currSymbolIndex);
		}
		
		while (currSymbolIndex < symbols.size()) {
			Bucket smallestBucket = buckets.poll();
			smallestBucket.add(symbols.get(currSymbolIndex));
			
			assert smallestBucket.BUCKET_ID < 32;
			symbolMap.put(symbols.get(currSymbolIndex).symbol, 1 << smallestBucket.BUCKET_ID);
			
			currSymbolIndex++;
			buckets.add(smallestBucket);
		}
		
		return symbolMap;
	}
	
	public static Map<Character, Integer> getBuckets() {
		final List<Symbol> symbols = getSymbolStats();
		return getDistributedBuckets(symbols);
	}
	
	private static List<Symbol> getSymbolStats() {
		KanjiBucketer.parseAllFiles(FileSystems.getDefault().getPath(IN_DIR), new JsonFactory());
		
		List<Entry<Character, Integer>> vals = kanjiMap.entrySet().stream().collect(Collectors.toList());
		vals = vals.stream().sorted((e1, e2) -> e1.getValue() >= e2.getValue() ? -1 : 1).collect(Collectors.toList());
		
		double sum = vals.stream().mapToInt((entry) -> entry.getValue()).sum();
		List<Symbol> freqList = vals.stream().map((entry) -> new Symbol(entry.getKey(), entry.getValue() / sum)).collect(Collectors.toList());
		
		JSONObject kanjiResult = new JSONObject();
		freqList.forEach((freq) -> kanjiResult.put(freq.symbol + "", freq.freq));
		
		File output = FileSystems.getDefault().getPath("out", "symbolFreq.json").toFile();
		
		NewsEasyFormatUtil.printJSONObject(output, kanjiResult);
		
		Path sortedOut = FileSystems.getDefault().getPath("out", "sortedSymbolFreq.txt");
		try (FileWriter fw = new FileWriter(sortedOut.toFile())) {
			for (Symbol freq : freqList) {
				fw.write(freq.symbol + " : " + freq.freq + "\r\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return freqList;
	}
	
	private static void parseAllFiles(Path dir, JsonFactory jsonFactory) {
		if (Files.isDirectory(dir)) {
			try {
				DirectoryStream<Path> dirStream;
				dirStream = Files.newDirectoryStream(dir);
				for (Path p : dirStream) {
					parseAllFiles(p, jsonFactory);
				}
			} catch (IOException e) {
				System.err.println("Unable to access " + dir.getFileName());
				e.printStackTrace();
			}
		} else {
			parseJSONFile(dir.toFile(), jsonFactory);
		}
	}
	
	private static void parseJSONFile(File jsonInput, JsonFactory jsonFactory) {
		//System.out.println("Parsing " + jsonInput.getAbsolutePath());
		
		ParseResult parseResult = NewsEasyFormatUtil.getArticleText(jsonInput, jsonFactory);
		
		// first add title
		for (int i = 0; i < parseResult.TITLE.length(); i++) {
			kanjiMap.compute(parseResult.TITLE.charAt(i), (k, v) -> v == null ? 1 : v + 1);
		}
		
		// now go and compute all sentences
		SentenceIterator si = new SentenceIterator(parseResult.ARTICLE);
		while (si.hasNext()) {
			String sentence = si.next();
			for (int i = 0; i < sentence.length(); i++) {
				kanjiMap.compute(sentence.charAt(i), (k, v) -> v == null ? 1 : v + 1);
			}
		}
	}
}

class Bucket implements Comparable<Bucket> {
	
	double							filled	= 0;
	public final int		BUCKET_ID;
	private static int	ID			= 0;
	
	public Bucket() {
		this.BUCKET_ID = ID;
		ID++;
	}
	
	@Override
	public int compareTo(Bucket o) {
		if (o.filled < this.filled) {
			return 1;
		} else if (o.filled == this.filled) {
			return 0;
		} else {
			return -1;
		}
	}
	
	public void add(Symbol s) {
		this.filled += s.freq;
	}
}

class Symbol {
	
	public char		symbol;
	public double	freq;
	
	public Symbol(char symbol, double freq) {
		this.symbol = symbol;
		this.freq = freq;
	}
	
	public String toString() {
		return new String(new char[] { this.symbol });
	}
}
