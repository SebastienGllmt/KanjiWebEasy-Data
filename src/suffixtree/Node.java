package suffixtree;

import java.util.LinkedList;
import java.util.TreeMap;

class Node<D extends TreeData> {

  public Node<D> suffixLink;
  private TreeMap<Character, Edge<D>> edgeList;
  private final int ID;
  public static final byte INTERNAL = 0, LEAF = 1;

  /**
   * Note that the leaf can't be reached twice in the add to the suffix tree
   */
  //public final List<D> dataSet;

  private Node(D data, int allocatedID) {
    this.edgeList = null;

    this.ID = allocatedID;
    this.suffixLink = null;

    if (data != null) {
      this.dataSet = new LinkedList<D>();
      this.dataSet.add(data);
    } else {
      this.dataSet = null;
    }
  }
  
  public int getId(){
    return this.ID;
  }

  public TreeMap<Character, Edge<D>> getEdgeList() {
    if (this.edgeList == null) {
      this.edgeList = new TreeMap<Character, Edge<D>>();
    }

    return this.edgeList;
  }

  public static <D extends TreeData> Node<D> newLeafNode(D data, int allocatedID) {
    return new Node<D>(data, allocatedID);
  }

  public static <D extends TreeData> Node<D> newInternalNode(int allocatedID) {
    return new Node<D>(null, allocatedID);
  }

  public void addData(D data) {
    if (data != null) {
      this.dataSet.add(data);
    }else{
      assert false;
    }
  }

  public String toString() {
    return "node" + this.ID;
  }

  public boolean isLeaf() {
    return this.edgeList == null || this.edgeList.size() == 0;
  }

  private static final String LEAF_NODE_COLOR = "black", INTERNAL_NODE_COLOR = "lightgrey", PHANTOM_NODE_COLOR = "red", ROOT_NODE_COLOR = "green", END_NODE_COLOR = "pink";

  public String toDotFormat() {
    String fillColor = LEAF_NODE_COLOR;

    if (this.ID == 0) {
      fillColor = ROOT_NODE_COLOR;
    } else {
      // if is internal node
      if (this.getEdgeList().size() > 1) {
        fillColor = INTERNAL_NODE_COLOR;
      }
      // if only has 2 branches, one of which is an epsilon transition
      if (this.getEdgeList().size() == 2 && this.getEdgeList().get(Edge.EMPTY_EDGE) != null) {
        // then this is a phantom edge
        fillColor = PHANTOM_NODE_COLOR;
      }
    }

    // if this node only has an epsilon transition after it
    if (this.getEdgeList().size() == 1) {
      fillColor = END_NODE_COLOR;
    }

    if (isLeaf()) {
      int size = this.dataSet == null ? 0 : this.dataSet.size();
      return String.format("%s [label=\"\",label=%s,shape=circle,fontsize=10]\n", this.toString(), size);
    } else {
      return String.format("%s [label=\"\",fillcolor=%s,shape=point]\n", this.toString(), fillColor);
    }
  }
}
