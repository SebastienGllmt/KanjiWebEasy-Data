package suffixtree;

class Edge<D extends TreeData> {

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

  public static <D extends TreeData> Edge<D> createActiveEdge(SentenceInfo sentenceInfo) {
    return new Edge<D>(sentenceInfo, 0, 0, null);
  }

  public static <D extends TreeData> Edge<D> createNewEdge(SuffixTree<D> tree, SentenceInfo sentenceInfo, int startIndex, int endIndex, D data) {

    // NOTE: If endIndex == startIndex then this function doesn't create an epsilon suffix link
    // However, one should be created. This is  left up to the caller of this function.
    if (endIndex - startIndex == 0) {
      // if this is an epsilon transition just add it
      return new Edge<D>(SentenceInfo.EMPTY_SENTENCE, 0, 0, Node.newLeafNode(data, tree.getNextNodeId()));
    } else {
      // otherwise, add the edge but then add an epsilon transition at the end of it
      Node<D> child = Node.newInternalNode(tree.getNextNodeId());
      Edge<D> e = new Edge<D>(sentenceInfo, startIndex, endIndex, child);

      // note: can't already have an edge since we just created it
      child.getEdgeList().put(EMPTY_EDGE, new Edge<D>(SentenceInfo.EMPTY_SENTENCE, 0, 0, Node.newLeafNode(data, tree.getNextNodeId())));
      tree.createEpsilonSuffixLink(child);
      return e;
    }
  }

  public static <D extends TreeData> Edge<D> splitEdge(SentenceInfo sentenceInfo, Edge<D> currEdge, int newEnd) {
    int oldEnd = currEdge.stringEndIndex;

    // assert that the edge is non-empty
    assert currEdge.getLength() > 0;
    // the child of this edge must have at least one child (at least the epsilon transition)
    assert currEdge.child.getEdgeList().size() > 0;
    // if it has only one edge, it must be the epsilon transition
    assert!(currEdge.child.getEdgeList().size() == 1) || currEdge.child.getEdgeList().containsKey(EMPTY_EDGE);

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

  public char chatAt(int i) {
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
    return this.childBitset | this.getEdgeBitset(index);
  }

  public String toString() {
    String childString = this.child == null ? "" : this.child.toString();
    return String.format("--%s--> %s\n", this.sentenceInfo.sentence.substring(this.stringStartIndex, this.stringEndIndex), childString);
  }
}