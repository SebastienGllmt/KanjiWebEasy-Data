package generaldownloader;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TokushuDownloader {

	private static final String NEWS_PATH = "news";
	private static final String NEWS_URL = "http://www3.nhk.or.jp/news/";
	private static final String WEB_PATH = "/web_tokushu/";
	private static final String BUSINESS_PATH = "/business_tokushu/";

	public static void main(String[] args) {
		downloadAllTokushu();
	}

	public static boolean downloadAllTokushu() {
		boolean oneFailed = false;
		oneFailed |= !handleXML(WEB_PATH);
		oneFailed |= !handleXML(BUSINESS_PATH);
		return oneFailed;
	}

	private static boolean handleXML(String path) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(NEWS_URL + path + "/sidemenu.xml");

			NodeList items = doc.getElementsByTagName("file");

			for (int i = 0; i < items.getLength(); i++) {
				Node itemNode = items.item(i);
				String date = itemNode.getTextContent();
				String diskPath = NEWS_PATH + path;
				String webPath = NEWS_URL + path;

				boolean isNewArticle = false;
				int tries = 1;
				final int MAX_TRIES = 3;
				while (tries <= MAX_TRIES) {
					try {
						isNewArticle = ArticleHTMLParser.saveTokushuFormat(diskPath, webPath, date);
						break;
					} catch (Exception e) {
						e.printStackTrace();
						System.out.printf("Failed to get tokushu from %s. Attempt %d of %d\n", webPath, tries, MAX_TRIES); 
						tries++;
					}
				}
				if (!isNewArticle) {
					//return true;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
			return false;
		} catch (SAXException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
