package suffixtree;

public class DatabaseEdge {

  public final String text;
  public final int childBitset;
  public final int fullBitset;
  private final int[] bitset;
  public int child;
  
  public DatabaseEdge(String text, int child, int childBitset, int fullBitset, int[] bitset){
    this.text = text;
    this.child = child;
    this.childBitset = childBitset;
    this.fullBitset = fullBitset;
    this.bitset = bitset;
  }
  
  public int getFullBitset() {
    return this.childBitset | this.bitset[0];
  }

  public int getPartBitset(int index) {
    return this.childBitset | this.bitset[index];
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + child;
    result = prime * result + ((text == null) ? 0 : text.hashCode());
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
    DatabaseEdge other = (DatabaseEdge) obj;
    if (child != other.child)
      return false;
    if (text == null) {
      if (other.text != null)
        return false;
    } else if (!text.equals(other.text))
      return false;
    return true;
  }
  
}
