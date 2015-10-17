package suffixtree;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ArticleInfo implements TreeData{
	
	public final String	article;
	public final byte		sentenceNumber;
	
	public ArticleInfo(String article, byte sentenceNumber) {
		this.article = article;
		this.sentenceNumber = sentenceNumber;
	}
	
	public String toString() {
		return this.article + " " + this.sentenceNumber;
	}

  @Override
  public byte[] toData() {
    ByteBuffer bb = ByteBuffer.allocate(article.length()*2 + 1 + 1).order(ByteOrder.BIG_ENDIAN).put(sentenceNumber).put((byte)article.length());
    for(int i=0; i<article.length(); i++){
      bb.putChar(article.charAt(i));
    }
    return bb.array();
  }
}
