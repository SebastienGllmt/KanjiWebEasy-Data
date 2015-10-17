package generaldownloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import util.NewsEasyFormatUtil;

public class EasyArticleExtractor {
	
	public static final String	HOME_PAGE	= "http://www3.nhk.or.jp/news/easy/";
	public static final String	JSON_PATH	= HOME_PAGE + "news-list.json";
	public static final String	PAGE_DIR	= "in/";
	
	private static JSONObject getNewsJSON() {
		String articlesInfo = NewsEasyFormatUtil.readURLAsString(JSON_PATH);
		if(articlesInfo == null){
			return null;
		}
		articlesInfo = articlesInfo.substring(articlesInfo.indexOf('['));
		JSONTokener tokener = new JSONTokener(articlesInfo);
		return new JSONArray(tokener).getJSONObject(0);
	}
	
	public static boolean parseAndSave(String rootDir) {
		File pageFolder = new File(rootDir, PAGE_DIR);
		if (!pageFolder.exists()) {
			pageFolder.mkdir();
		}
		
		JSONObject root = getNewsJSON();
		if(root == null){
			return false;
		}
		
		Iterator<String> dateIterator = root.keys();
		while (dateIterator.hasNext()) {
			String dateKey = dateIterator.next();
			String shortDate = dateKey.replace("-", "");
			
			File dateFolder = new File(rootDir, PAGE_DIR + shortDate);
			if (!dateFolder.exists()) {
				dateFolder.mkdir();
			}
			
			JSONArray articlesOnDay = root.getJSONArray(dateKey);
			for (int i = 0; i < articlesOnDay.length(); i++) {
				JSONObject article = articlesOnDay.getJSONObject(i);
				String newsId = article.getString("news_id");
				
				final String ARTICLE_URL = HOME_PAGE + newsId + "/" + newsId;
				
				File articleDir = new File(dateFolder, newsId);
				if (!articleDir.exists()) {
					articleDir.mkdir();
				} else {
					continue; // this article was already scanned before
				}
				
				String filename = "news" + shortDate + "_" + newsId;
				String fullPath = articleDir.getAbsolutePath() + "/" + filename + ".json";
				
				try {
					String relativePath = articleDir.getPath() + "/" + filename + ".json";
					if (saveToDisk(ARTICLE_URL + ".out.json", fullPath)) {
						System.out.println("Saved to " + relativePath);
					} else {
						System.out.println("Failed to find article. In JSON but not published yet or not internet connection. " + relativePath);
					}
				} catch (MalformedURLException e1) {
					e1.printStackTrace();
				}
				
				// Sleep to avoid DOSing
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		return true;
	}
	
	private static boolean saveToDisk(String webpath, String filename) throws MalformedURLException {
		URL website = new URL(webpath);
		try (ReadableByteChannel rbc = Channels.newChannel(website.openStream()); FileOutputStream fos = new FileOutputStream(filename);) {
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
