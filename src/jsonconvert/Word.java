package jsonconvert;
import org.json.JSONException;
import org.json.JSONObject;


public class Word {

	public final String base;
	public final String[] kanji;
	public final String[] furigana;
	public final String conjugated;
	public final String[] type;
	public final char clazz;
	public final String articleID;
	public final int sentenceNum;

	public Word(String base, String conjugated, String[] kanji, String[] furigana, String[] type, char clazz, String articleID, int sentenceNum) {
		this.base = base;
		this.conjugated = conjugated;
		this.kanji = kanji;
		this.furigana = furigana;
		this.type = type;
		this.clazz = Character.toUpperCase(clazz);
		this.articleID = articleID;
		this.sentenceNum = sentenceNum;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Word)) {
			return false;
		}
		Word other = (Word) obj;
		if (base == null) {
			if (other.base != null) {
				return false;
			}
		} else if (!base.equals(other.base)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Gets which type of word this is based off the JSON
	 * @param wordInfo
	 * @return the string[] array representing all the types
	 * @throws JSONException
	 */
	public static String[] getType(JSONObject wordInfo) throws JSONException {
		String[] type = null;
		if (wordInfo.has("pos")) {
			String[] pos = wordInfo.getString("pos").split("-");
			int length = 0;
			for (String s : pos) {
				if (s.equals("一般") || s.equals("*")) {
					break;
				}
				length++;
			}
			type = new String[length];
			System.arraycopy(pos, 0, type, 0, length);
		} else {
			type = new String[0];
		}

		return type;
	}
	
}
