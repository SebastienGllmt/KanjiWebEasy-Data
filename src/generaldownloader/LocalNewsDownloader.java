package generaldownloader;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LocalNewsDownloader {

	public static final String L_NEWS = "http://www3.nhk.or.jp/lnews/";
	public static final String PAGE_DIR = "general/";
	public static final String RES_DIR = "res/";

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		getAllURL();
	}

	public static boolean getAllURL() {
		JSONObject regionData;
		try {
			regionData = ArticleHTMLParser.getRegionData();

			for (String region : regionData.keySet()) {
				JSONObject areas = regionData.getJSONObject(region);

				for (String area : areas.keySet()) {
					String areaURL = areas.getString(area);

					if (areaURL.contains("lnews")) {
						handleXML(area, areaURL);
					} else {
						LocalDate currDate = LocalDate.now(ZoneId.of("+09:00"));

						final int DAYS_IN_WEEK = 7;
						int dayOffset = 0;
						for (int i = 0; i < DAYS_IN_WEEK + dayOffset; i++) {
							String date = currDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
							String newURL = areaURL + date + "/newslist.xml";

							try {
								boolean somethingDownloaded = handleXML(area, newURL);

								// if we didn't download anything, it's because we've already scanned this day, and so we've probably scanned every day before it
								if (!somethingDownloaded) {
									break;
								}
							} catch (IOException e) {
								// we get an IOException if we try and parse a day that's too early
								// that is to say it's maybe 1AM or something and they haven't uploaded a newslist for that day yet
								// in that case, we may need to look back in the history one more day
								if (i == DAYS_IN_WEEK) {
									break;
								}
								dayOffset++;
							}

							currDate = currDate.minusDays(1);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static boolean handleXML(String areaName, String xmlURL) throws IOException {
		boolean downloaded = false;

		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xmlURL);

			NodeList links = doc.getElementsByTagName("link");

			for (int i = 0; i < links.getLength(); i++) {
				Node urlNode = links.item(i);
				String articleURL = urlNode.getTextContent();
				if (articleURL.contains("lnews")) {
					// the first link to the file is not an article
					if (i == 0) {
						continue;
					}
					downloaded |= downloadArticle(areaName, articleURL);
				} else {
					downloaded |= downloadArticle(areaName, xmlURL.substring(0, xmlURL.lastIndexOf('/') + 1) + articleURL);
				}
			}
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}

		return downloaded;
	}

	private static boolean downloadArticle(String areaName, String URL) {
		boolean gotArticle = false;
		int tries = 1;
		final int MAX_TRIES = 3;
		while (tries <= MAX_TRIES) {
			try {
				if (URL.contains("lnews")) {
					gotArticle = ArticleHTMLParser.savePlainWebFormat(areaName, URL);
				} else {
					gotArticle = ArticleHTMLParser.saveNewsWebFormat(areaName, URL);
				}
				break; // we didn't crash
			} catch (Exception e) {
				e.printStackTrace();
				System.out.printf("Failed to get article from %s at %s. Attempt %d of %d\n", areaName, URL, tries, MAX_TRIES); 
				tries++;
			}
		}

		if (gotArticle) {
			try {
				System.out.println("Downloaded " + URL + " from " + areaName);
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return gotArticle;
	}
}
