package jsonconvert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import util.NewsEasyFormatUtil;

public class ArticleConverter {
	
	/**
	 * The path to find misc. resources
	 */
	private static final String	RES_PATH			= "res";
	
	/**
	 * The path for the output
	 */
	private static final String	OUT_PATH			= "out";
	
	/**
	 * The path to find the example file which tells the program what the letter mapping is for word types
	 */
	private static final String	CLASSMAP_PATH	= RES_PATH + "/" + "classmap.json";
	
	/**
	 * The path to the place to put the final concatenated JSON output
	 */
	private static final String	CONCAT_OUT		= OUT_PATH + "/" + "output.json";
	
	/**
	 * The directory to find the NHK provided JSON files
	 */
	private static final String	IN_DIR				= "in";
	
	/**
	 * The directory to print the resulting JSON files. It will be a mirror of the IN_DIR structure, but with <code>IN_DIR</code> replaced by
	 * <code>TEMP_DIR</code>
	 */
	private static final String	TEMP_DIR			= "temp";
	
	/**
	 * This maps the classes used by the NHK to meaningful English words.
	 */
	public static JSONObject		classMap;
	
	public static final char		NO_CLASS			= '-';
	
	/**
	 * Recursively goes through folders to find all the files and applies the <code>parseJSONFile</code> function on them.
	 * @param dir - The directory to scan in
	 * @param globalWordList - The global word list which is used to concatenate all files together
	 */
	private static void parseAllFiles(Path dir, List<Word> globalWordList) {
		if (Files.isDirectory(dir)) {
			try {
				DirectoryStream<Path> dirStream;
				dirStream = Files.newDirectoryStream(dir);
				for (Path p : dirStream) {
					parseAllFiles(p, globalWordList);
				}
			} catch (IOException e) {
				System.err.println("Unable to access " + dir.getFileName());
				e.printStackTrace();
			}
		} else {
			parseJSONFile(dir.toFile(), globalWordList);
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		System.out.println("Program start");
		classMap = createClassMap();
		
		// Create globalWordList used to store every word in all the files combined to then create the main output
		List<Word> globalWordList = new ArrayList<>();
		
		// Parse all files currently collected and create words lists from them.
		parseAllFiles(FileSystems.getDefault().getPath(IN_DIR), globalWordList);
		
		// Create the concatenated version of all the words
		JSONObject concatOut = JSONArticle.createJSONOutput(globalWordList);
		
		// Create the file which will contain the concat version of the output
		File mergeOut = new File(CONCAT_OUT);
		
		// If it already exists, delete it and then create a new blank one to start from
		if (mergeOut.exists()) {
			mergeOut.delete();
		}
		try {
			mergeOut.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Print the final result
		NewsEasyFormatUtil.printJSONObject(mergeOut, concatOut);
		System.out.println("Program end");
		
	}
	
	/**
	 * Parse a given NHK file <code>jsonInput</code> to turn it into desired format. At the same time, it will add all words inside <code>jsonInput</code> to
	 * <code>globalWordList</code>
	 * @param jsonInput - The file to read NHK input from
	 * @param globalWordList - The global word list used to concatenate the result
	 */
	private static void parseJSONFile(File jsonInput, List<Word> globalWordList) {
		//System.out.println("Parsing " + jsonInput.getAbsolutePath());
		// Take the path for the input and create the equivalent mirror for the output path by replacing IN_DIR with OUT_DIR
		String inputPath = jsonInput.getAbsolutePath();
		int inIndex = inputPath.lastIndexOf(IN_DIR);
		String outputPath = inputPath.substring(0, inIndex) + TEMP_DIR + inputPath.substring(inIndex + IN_DIR.length(), inputPath.length());
		File outputFile = new File(outputPath);
		
		// If the required directories to put the output file in don't exist yet, create them
		if (!outputFile.getParentFile().exists()) {
			outputFile.getParentFile().mkdirs();
		}
		
		// If the output file already exists, delete it and create a new version
		if (outputFile.exists()) {
			outputFile.delete();
		}
		try {
			outputFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Create a word list from the jsonInput provided by the NHK
		List<Word> wordList = JSONArticle.createWordList(jsonInput, JSONArticle.getArticleID(jsonInput));
		
		// Add all those words to the globalWordList too
		globalWordList.addAll(wordList);
		
		// Create the output from the jsonList file in the new format. Store result in outputObject
		JSONObject outputObject = JSONArticle.createJSONOutput(wordList);
		
		// Print outputObject in the output directory
		NewsEasyFormatUtil.printJSONObject(outputFile, outputObject);
	}
	/**
	 * Creates a new JSONObject with the contents of the JSON file stored in <code>CLASSMAP_PATH<code>
	 * @return the JSONObject
	 */
	private static JSONObject createClassMap() {
		return NewsEasyFormatUtil.createJSONFromFile(CLASSMAP_PATH);
	}
}
