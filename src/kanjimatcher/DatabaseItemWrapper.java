package kanjimatcher;

public class DatabaseItemWrapper {

	public final String KEY;
	public final PairInfo<String> READING_PAIR;
	
	public DatabaseItemWrapper(String key, PairInfo<String> readingPair){
		this.KEY = key;
		this.READING_PAIR = readingPair;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((READING_PAIR == null) ? 0 : READING_PAIR.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof DatabaseItemWrapper))
			return false;
		DatabaseItemWrapper other = (DatabaseItemWrapper) obj;
		if (READING_PAIR == null) {
			if (other.READING_PAIR != null)
				return false;
		} else if (!READING_PAIR.equals(other.READING_PAIR))
			return false;
		return true;
	}
}
