package suffixtree;

public class SentenceInfo {
	
	public final String								sentence;
	public final int[]								suffixBuckets;
	public static final SentenceInfo	EMPTY_SENTENCE	= new SentenceInfo("", null);
	
	public SentenceInfo(String sentence, int[] suffixBuckets) {
		this.sentence = sentence;
		this.suffixBuckets = suffixBuckets;
	}
}
