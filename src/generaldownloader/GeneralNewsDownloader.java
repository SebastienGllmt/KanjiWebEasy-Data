package generaldownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import util.NewsEasyFormatUtil;

public class GeneralNewsDownloader {

	private static final String ARTICLE_URL = "articleURL";
	private static final String TITLE = "title";
	private static final String VIEWS = "views";
	private static final String CATEGORY = "category";
	private static final String HAS_VIDEO = "hasVideo";
	private static final String[] FILE_HEADER_MAPPING = { ARTICLE_URL, TITLE, VIEWS, CATEGORY, HAS_VIDEO };
	private static final String NHK_ROOT = "http://www3.nhk.or.jp/news/";
	private static final String RES = "res/";
	private static final String CATEGORY_MAP = RES + "/" + "categoryMap.json";
	private static final String RANKING_URL = "http://www3.nhk.or.jp/news/gad/ranking/week.csv";
	private static final JSONObject CATEGORY_JSON = NewsEasyFormatUtil.createJSONFromFile(CATEGORY_MAP);
	private static final String NEWS_PATH = "news";
	private static final File VIEWS_FILE = new File(NEWS_PATH + "/" + "views.json");

	public static void main(String[] args) throws IOException {
		printGeneralArticles();
	}

	public static boolean printGeneralArticles() {
		CSVFormat csvFileFormat = CSVFormat.DEFAULT.withHeader(FILE_HEADER_MAPPING);

		File newsDir = new File(NEWS_PATH);
		if (!newsDir.exists()) {
			newsDir.mkdir();
		}

		JSONObject viewsJSON;
		if (!VIEWS_FILE.exists()) {
			try {
				VIEWS_FILE.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			viewsJSON = new JSONObject();

			// print at least empty braces that way if our program crashes it won't break on the next run
			NewsEasyFormatUtil.printJSONObject(VIEWS_FILE, viewsJSON);
		} else {
			viewsJSON = NewsEasyFormatUtil.createJSONFromFile(VIEWS_FILE);
			if (viewsJSON == null) {
				return false;
			}
		}

		String rankingsCsv = NewsEasyFormatUtil.readURLAsString(RANKING_URL);
		if (rankingsCsv == null) {
			return false;
		}
		try (CSVParser csvFileParser = CSVParser.parse(rankingsCsv, csvFileFormat)) {
			List<CSVRecord> csvRecords = csvFileParser.getRecords();

			boolean alreadySeen = false;
			for (CSVRecord record : csvRecords) {
				String articleURL = record.get(ARTICLE_URL);
				int views = Integer.parseInt(record.get(VIEWS));
				String category = CATEGORY_JSON.getString(record.get(CATEGORY));

				String articleKey = articleURL.substring("html/".length(), articleURL.indexOf(".html")).replace('/', '_');
				viewsJSON.put(articleKey, views);

				boolean recent = !hasDayElapsed(articleURL.substring("html/".length(), "html/YYYYMMdd".length()));
				alreadySeen = !saveArticleJSON(articleURL, category, recent);
				if (!alreadySeen) {
					try {
						System.out.println("Downloaded " + articleURL + " from " + category);
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}

		NewsEasyFormatUtil.printJSONObject(VIEWS_FILE, viewsJSON);
		return true;
	}

	/**
	 * We only want to download articles that are more than 1 day old, since new articles tend to be edited
	 */
	private static boolean hasDayElapsed(String date) {
		LocalDateTime japanTime = LocalDateTime.now(ZoneId.of("+09:00"));
		LocalDateTime publishTime = getArticleTime(date);

		// if it's been more than 1 day since last publish
		if (publishTime.until(japanTime, ChronoUnit.DAYS) >= 1) {
			return true;
		} else {
			return false;
		}
	}

	private static LocalDateTime getArticleTime(String date) {
		int year = Integer.parseInt(date.substring(0, "YYYY".length()));
		int month = Integer.parseInt(date.substring("YYYY".length(), "YYYYMM".length()));
		int day = Integer.parseInt(date.substring("YYYYMM".length(), date.length()));
		return LocalDateTime.of(year, month, day, 0, 0);
	}

	private static boolean saveArticleJSON(String URL, String category, boolean recent) {
		String filename = URL.substring("html/".length(), URL.indexOf(".html")).replace('/', '_');
		File dir = new File(NEWS_PATH + "/" + category);
		dir.mkdirs();

		// we want to save over recent articles in case they got updated
		File articleFile = FileSystems.getDefault().getPath(dir.getAbsolutePath(), filename + ".json").toFile();
		if (!recent && articleFile.exists()) {
			return false;
		}

		JSONObject articleJSON = null;
		int tries = 1;
		final int MAX_TRIES = 3;
		Exception downloadException = null;
		while (tries <= MAX_TRIES) {
			try {
				articleJSON = getArticleJSON(URL);
				break;
			} catch (Exception e) {
				downloadException = e;
				System.out.printf("Failed to get article from category %s at %s. Attempt %d of %d\n", category, URL, tries, MAX_TRIES);
				tries++;
			}
		}
		if(tries == 4){
			downloadException.printStackTrace();
		}

		if (articleJSON != null) {
			NewsEasyFormatUtil.printJSONObject(articleFile, articleJSON);
			return true;
		} else {
			return false;
		}
	}

	private static JSONObject getArticleJSON(String url) throws IOException {
		JSONObject articleJSON = new JSONObject();

		try {
			Document doc = Jsoup.connect(NHK_ROOT + url).get();

			String title = doc.select("span.contentTitle").text();
			String time = doc.select("span#news_date").text() + "　" + doc.select("span#news_time").text();
			String textBody = doc.select("div#news_textbody").text();
			String textMore = doc.select("div#news_textmore").text();

			articleJSON.put("title", title);
			articleJSON.put("time", time);
			articleJSON.put("text_body", textBody);
			articleJSON.put("text_more", textMore);

			JSONArray addedContent = new JSONArray();
			Elements newsAdd = doc.select("div.news_add");
			for (Element add : newsAdd) {
				// it seems the last newsAdd is always empty
				if (add.children().size() == 0) {
					continue;
				}

				Elements children = add.children();
				assert children.size() < 3;

				String subHeader, newAdd;
				if (children.size() == 2) {
					subHeader = children.get(0).text();
					newAdd = children.get(1).text();
				} else {
					subHeader = "";
					newAdd = children.get(0).text();
				}
				addedContent.put(subHeader);
				addedContent.put(newAdd);
			}

			articleJSON.put("news_add", addedContent);
		} catch (IOException e) {
			throw e;
		}

		return articleJSON;
	}
}
