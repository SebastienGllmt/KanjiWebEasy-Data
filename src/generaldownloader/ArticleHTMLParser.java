package generaldownloader;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import util.NewsEasyFormatUtil;

public class ArticleHTMLParser {
	
	public static final File		regionFile	= FileSystems.getDefault().getPath("res", "regionFile.json").toFile();
	public static final String	LNEWS_PATH	= "news/lnews";
	
	public static JSONObject getRegionData() throws IOException {
		JSONObject urlLookup = new JSONObject();
		
		Document doc = Jsoup.connect("http://www3.nhk.or.jp/lnews/").get();
		Elements regions = doc.select("h2");
		for (Element region : regions) {
			JSONObject regionJSON = new JSONObject();
			
			Elements areas = region.parent().parent().children().select("a.areaName");
			for (Element area : areas) {
				String url = area.attr("href");
				String name = area.text();
				if (url.contains("lnews")) {
					String englishName = url.substring("http://www3.nhk.or.jp/lnews/".length(), url.length() - 1);
					url = url + "nhk_" + englishName + ".xml";
				}
				regionJSON.put(name, url);
			}
			
			urlLookup.put(region.text(), regionJSON);
		}
		
		NewsEasyFormatUtil.printJSONObject(regionFile, urlLookup);
		
		return urlLookup;
	}
	
	public static boolean saveNewsWebFormat(String areaName, String URL) {
		String filename = URL.substring(URL.indexOf("news/") + "news/".length(), URL.indexOf(".html")).replace('/', '_');
		File dir = new File(LNEWS_PATH + "/" + areaName);
		dir.mkdirs();
		
		File articleFile = FileSystems.getDefault().getPath(dir.getAbsolutePath(), filename + ".json").toFile();
		if (articleFile.exists()) {
			return false;
		}
		
		try {
			Document doc = Jsoup.connect(URL).get();
			String title = doc.select("span.hero_title").text();
			String article = doc.select("p#news_textbody").text();
			String time = doc.select("p.time").text();
			
			JSONObject articleJSON = new JSONObject();
			articleJSON.put("title", title);
			articleJSON.put("article", article);
			articleJSON.put("time", time);
			
			NewsEasyFormatUtil.printJSONObject(articleFile, articleJSON);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public static boolean savePlainWebFormat(String areaName, String URL) throws IOException {
		String filename = URL.substring(URL.lastIndexOf('/') + 1, URL.indexOf(".html"));
		
		File dir = new File(LNEWS_PATH + "/" + areaName);
		dir.mkdirs();
		
		File articleFile = FileSystems.getDefault().getPath(dir.getAbsolutePath(), filename + ".json").toFile();
		if (articleFile.exists()) {
			return false;
		}
		
		try {
			Document doc = Jsoup.connect(URL).get();
			
			Elements mainDiv = doc.select("div#main");
			String title = mainDiv.select("h2.detail").get(0).text();
			Elements paragraphs = mainDiv.get(0).select("p");
			String article = paragraphs.get(0).text();
			String time = paragraphs.get(1).text();
			
			JSONObject articleJSON = new JSONObject();
			articleJSON.put("title", title);
			articleJSON.put("article", article);
			articleJSON.put("time", time);
			
			NewsEasyFormatUtil.printJSONObject(articleFile, articleJSON);
		} catch (IOException e) {
			throw e;
		}
		return true;
	}
	
	public static boolean saveTokushuFormat(String diskPath, String webPath, String urlDate) throws IOException {
		File dir = new File(diskPath);
		dir.mkdirs();
		
		File articleFile = FileSystems.getDefault().getPath(dir.getAbsolutePath(), urlDate.replace(".html", ".json")).toFile();
		if (articleFile.exists()) {
			return false;
		}
		
		System.out.println(webPath + urlDate);
		try {
			Document doc = Jsoup.connect(webPath + urlDate).get();
			Element article = doc.select("article").first();
			
			String title = "";
			String date = "";
			String intro = "";
			JSONObject root = new JSONObject();
			JSONArray articleDivision = new JSONArray();
			JSONArray currDivision = null;
			for (Element e : article.children()) {
				if (e.className().equals("leadTxt")) {
					title = e.select("h1.title").text();
					date = e.select("p.date").text();
					intro = e.child(e.children().size() - 1).text(); // skip information about the reporter
					
					root.put("title", title);
					root.put("date", date);
					root.put("intro", intro);
				} else if (e.nodeName().equals("h2")) {
					JSONObject divisionObj = new JSONObject();
					currDivision = new JSONArray();
					divisionObj.put(e.text(), currDivision);
					articleDivision.put(divisionObj);
				} else if (e.nodeName().equals("p")) {
					if (!e.className().equals("image")) {
						currDivision.put(e.text());
					}
				}
			}
			root.put("article", articleDivision);
			NewsEasyFormatUtil.printJSONObject(articleFile, root);
		} catch (IOException e) {
			throw e;
		}
		return true;
	}
}
