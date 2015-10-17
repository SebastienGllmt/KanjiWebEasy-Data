package generaldownloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.json.JSONObject;

import util.NewsEasyFormatUtil;

public class DownloadAllLauncher {

	private static final int MORNING = 6, EVENING = 22;
	private static final int SEC_IN_MIN = 60;
	private static final int MILLISEC_IN_SEC = 1000;
	private static final int SLEEP_INTERVAL = 1 * SEC_IN_MIN * MILLISEC_IN_SEC;
	private static final int DAY_INTERVAL = 1;
	private static final int NIGHT_INTERVAL = 2;
	private static final String RES = "res/";
	private static final String STAMP_NAME = "timestamp.json";
	private static final ZoneId JAPAN_TIME = ZoneId.of("+09:00");
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

	public static void main(String[] args) throws IOException {
		JSONObject stampObject;
		File stampFile = new File(RES + STAMP_NAME);
		if (stampFile.exists()) {
			stampObject = NewsEasyFormatUtil.createJSONFromFile(stampFile);
			stampObject.put("hourly", 0); // make sure we always update the hourly on startup
		} else {
			stampObject = createTimestampJSON(stampFile);
		}

		System.out.printf("Program launched at (+09:00) %s\n", LocalDateTime.now(JAPAN_TIME).format(DATE_FORMAT));

		do {
			boolean nhkIsReachable;
			try{
				new URL("http://www3.nhk.or.jp").openConnection().connect();
				nhkIsReachable = true;
			}catch(Exception e){
				nhkIsReachable = false;
			}
			if (nhkIsReachable) {
				LocalDateTime currDate = LocalDateTime.now(JAPAN_TIME);
				LocalDateTime lastDaily = LocalDateTime.ofInstant(Instant.ofEpochMilli(stampObject.getLong("daily")), JAPAN_TIME);
				LocalDateTime lastHourly = LocalDateTime.ofInstant(Instant.ofEpochMilli(stampObject.getLong("hourly")), JAPAN_TIME);

				if (lastDaily.until(currDate, ChronoUnit.DAYS) >= 1) {
					updateDaily(stampObject, currDate.format(DATE_FORMAT));
				}

				int hour = currDate.get(ChronoField.CLOCK_HOUR_OF_DAY);
				if (hour > MORNING && hour < EVENING) {
					if (lastHourly.until(currDate, ChronoUnit.HOURS) >= DAY_INTERVAL) {
						updateHourly(stampObject, "Day", currDate.format(DATE_FORMAT));
					}
				} else {
					if (lastHourly.until(currDate, ChronoUnit.HOURS) >= NIGHT_INTERVAL) {
						updateHourly(stampObject, "Night", currDate.format(DATE_FORMAT));
					}
				}
				NewsEasyFormatUtil.printJSONObject(stampFile, stampObject);
			}
			try {
				Thread.sleep(SLEEP_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (true);
	}

	private static void updateDaily(JSONObject stampObject, String currTimeFormatted) {
		System.out.printf("Daily update triggered at %s\n", currTimeFormatted);
		stampObject.put("daily", System.currentTimeMillis());
		System.out.println("Web Easy download");
		try {
			EasyArticleExtractor.parseAndSave(System.getProperty("user.dir"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Tokushu download");
		try {
			TokushuDownloader.downloadAllTokushu();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.printf("Daily update finished at %s\n", LocalDateTime.now(JAPAN_TIME).format(DATE_FORMAT));
	}

	private static void updateHourly(JSONObject stampObject, String dayOrNight, String currTimeFormatted) {
		System.out.printf("%s time update triggered at %s\n", dayOrNight, currTimeFormatted);
		stampObject.put("hourly", System.currentTimeMillis());
		System.out.println("Local News download");
		try {
			LocalNewsDownloader.getAllURL();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("General News download");
		try {
			GeneralNewsDownloader.printGeneralArticles();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.printf("%s time update finished at %s\n", dayOrNight, LocalDateTime.now(JAPAN_TIME).format(DATE_FORMAT));
	}

	private static JSONObject createTimestampJSON(File f) throws IOException {
		JSONObject lastTimestamp = new JSONObject();
		lastTimestamp.put("daily", 0);
		lastTimestamp.put("hourly", 0);
		f.createNewFile();
		NewsEasyFormatUtil.printJSONObject(f, lastTimestamp);
		return lastTimestamp;
	}
}
