package kanjimatcher;

public class PairInfo<T> {
	
	public final T ADDITIONAL_INFO;
	public final T KANJI, FURIGANA;
	
	public PairInfo(T kanji, T furigana, T additional){
		this.KANJI = kanji;
		this.FURIGANA = furigana;
		this.ADDITIONAL_INFO = additional;
	}
	
	public PairInfo(T kanji, T furigana){
		this(kanji, furigana, null);
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((FURIGANA == null) ? 0 : FURIGANA.hashCode());
		result = prime * result + ((KANJI == null) ? 0 : KANJI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PairInfo))
			return false;
		@SuppressWarnings("unchecked")
		PairInfo<T> other = (PairInfo<T>) obj;
		if (FURIGANA == null) {
			if (other.FURIGANA != null)
				return false;
		} else if (!FURIGANA.equals(other.FURIGANA))
			return false;
		if (KANJI == null) {
			if (other.KANJI != null)
				return false;
		} else if (!KANJI.equals(other.KANJI))
			return false;
		return true;
	}
}