package suffixtree;

class Edge<D> {

  /**
   * The fact that this is final is important for performance
   * Edges should not make copies for strings but instead change the start/end indices
   */
  public final SentenceInfo sentenceInfo;
  public int stringStartIndex;
  /**
   * Exclusive range
   */
  public int stringEndIndex;

  public Node<D> child;
  public int childBitset;

  /**
   * Edge label used for epsilon transitions
   */
  public static final Character EMPTY_EDGE = '\0';

  private Edge(SentenceInfo sentenceInfo, int startIndex, int endIndex, Node<D> child) {
    this.sentenceInfo = sentenceInfo;
    this.stringStartIndex = startIndex;
    this.stringEndIndex = endIndex;
    this.child = child;
    this.childBitset = 0;
  }
  
  public static <D> Edge<D> restOfSentenceEdge(SuffixTree<D> st, SentenceInfo sentenceInfo, int startIndex, D data){
    Node<D> child = Node.newInternalNode(data, st.getNextNodeId());
    st.createEpsilonSuffixLink(child);
    assert sentenceInfo.sentence.length() - startIndex > 0;
    return new Edge<D>(sentenceInfo, startIndex, sentenceInfo.sentence.length(), child);
  }

  public static <D> Edge<D> createActiveEdge(SentenceInfo sentenceInfo) {
    return new Edge<D>(sentenceInfo, 0, 0, null);
  }

  public static <D> Edge<D> splitEdge(SentenceInfo sentenceInfo, Edge<D> currEdge, int newEnd) {
    int oldEnd = currEdge.stringEndIndex;

    // assert that the edge is non-empty
    assert currEdge.getLength() > 0;

    // assert that this is shrinking the edge
    assert currEdge.stringEndIndex > newEnd;
    currEdge.stringEndIndex = newEnd;

    // we can't have shrunk the edge to 0 because we split an edge on the condition
    // the current edge and the active edge share at least 1 letter
    // therefore the curr edge gets shrunk to at least 1 letter in size
    assert currEdge.getLength() != 0;

    // return a new edge representing the 2nd half of the shrunk edge
    return new Edge<D>(sentenceInfo, currEdge.stringEndIndex, oldEnd, currEdge.child);
  }

  public boolean isEmpty() {
    return this.stringStartIndex == this.stringEndIndex;
  }

  /**
   * Note: This will return 0 if this is an empty edge leading to data
   */
  public int getLength() {
    return this.stringEndIndex - this.stringStartIndex;
  }

  public char charAt(int i) {
    return this.sentenceInfo.sentence.charAt(this.stringStartIndex + i);
  }

  public String getText() {
    return this.sentenceInfo.sentence.substring(this.stringStartIndex, this.stringEndIndex);
  }

  private int getEdgeBitset(int index) {
    // if this is an epsilon edge
    if (this.sentenceInfo.sentence.isEmpty()) {
      return 0;
    }
    if(index >= this.stringEndIndex){
      return this.childBitset;
    }
    return this.sentenceInfo.suffixBuckets[index];
  }

  /**
   * Get all the buckets that can be found by going down this edge
   * See comment on getPartBitset
   */
  public int getFullBitset() {
    return this.childBitset | this.getEdgeBitset(this.stringStartIndex);
  }

  /**
   * Get all the buckets that can be found by going down this edge starting at a give index in the edge
   * Note: if this edge is split, the getEdgeBitset at returns all buckets from the sentence.length(),...,index which is more than this edge
   * However, this is okay because if this edge is split somewhere, sentence.length(),...,k,k-1,...,0, then the part k-1,...,0 is included in childBitset so the result ends up the same
   */
  public int getPartBitset(int index) {
    return this.childBitset | this.getEdgeBitset(this.stringStartIndex + index);
  }

  public String toString() {
    String childString = this.child == null ? "" : this.child.toString();
    return String.format("--%s--> %s\n", this.sentenceInfo.sentence.substring(this.stringStartIndex, this.stringEndIndex), childString);
  }
}