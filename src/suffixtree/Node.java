package suffixtree;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

class Node<D> {

  public Node<D> suffixLink;
  private TreeMap<Character, Edge<D>> edgeList;
  private final int ID;
  public static final byte INTERNAL = 0, LEAF = 1;

  /**
   * Note that the leaf can't be reached twice in the add to the suffix tree
   */
  public List<D> dataSet;

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

  public int getId() {
    return this.ID;
  }

  public TreeMap<Character, Edge<D>> getEdgeList() {
    if (this.edgeList == null) {
      this.edgeList = new TreeMap<Character, Edge<D>>();
    }

    return this.edgeList;
  }

  public static <D> Node<D> newInternalNode(D data, int allocatedID) {
    return new Node<D>(data, allocatedID);
  }

  public void addData(D data) {
    if (data != null) {
      if (this.dataSet == null) {
        this.dataSet = new LinkedList<D>();
      }
      this.dataSet.add(data);
    }
  }

  public String toString() {
    return "node" + this.ID;
  }

  private static final String LEAF_NODE_COLOR = "black", INTERNAL_NODE_COLOR = "grey", INTERNAL_WITH_DATA_NODE_COLOR = "red", ROOT_NODE_COLOR = "green";

  public String toDotFormat() {
    String fillColor = LEAF_NODE_COLOR;

    if (this.ID == 0) {
      fillColor = ROOT_NODE_COLOR;
    } else {
      // if is internal node
      if (this.getEdgeList().size() >= 1) {
        fillColor = INTERNAL_NODE_COLOR;
      }
      if (this.getEdgeList().size() >= 1 && this.dataSet != null) {
        fillColor = INTERNAL_WITH_DATA_NODE_COLOR;
      }
    }

    int size = this.dataSet == null ? 0 : this.dataSet.size();
      return String.format("%s [label=\"\",color=%s,label=%s,shape=circle,fontsize=10]\n", this.toString(), fillColor, size);
  }
}
