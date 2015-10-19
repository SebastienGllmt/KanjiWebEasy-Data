package suffixtree;

public class ArticleInfo {

  public final String article;
  public final short sentenceNumber;

  public ArticleInfo(String article, short sentenceNumber) {
    this.article = article;
    this.sentenceNumber = sentenceNumber;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((article == null) ? 0 : article.hashCode());
    result = prime * result + sentenceNumber;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ArticleInfo other = (ArticleInfo) obj;
    if (article == null) {
      if (other.article != null)
        return false;
    } else if (!article.equals(other.article))
      return false;
    if (sentenceNumber != other.sentenceNumber)
      return false;
    return true;
  }

  public String toString() {
    return this.article + " " + this.sentenceNumber;
  }

  public String toDatabaseText() {
    return String.format("'%s', %d", article, sentenceNumber);
  }
}
