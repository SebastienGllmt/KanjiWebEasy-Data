package suffixtree;

import queryparse.BaseNode;

public class SearchNode{
  public final BaseNode currNode;
  public final int parseCount;
  public final DatabaseEdge edge;
  public final int edgeIndex;
  
  public SearchNode(BaseNode currNode, int parsedLetters, DatabaseEdge edge, int edgeIndex) {
    this.currNode = currNode;
    this.parseCount = parsedLetters;
    this.edge = edge;
    this.edgeIndex = edgeIndex;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((currNode == null) ? 0 : currNode.hashCode());
    result = prime * result + ((edge == null) ? 0 : edge.hashCode());
    result = prime * result + edgeIndex;
    result = prime * result + parseCount;
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
    SearchNode other = (SearchNode) obj;
    if (currNode == null) {
      if (other.currNode != null)
        return false;
    } else if (!currNode.equals(other.currNode))
      return false;
    if (edge == null) {
      if (other.edge != null)
        return false;
    } else if (!edge.equals(other.edge))
      return false;
    if (edgeIndex != other.edgeIndex)
      return false;
    if (parseCount != other.parseCount)
      return false;
    return true;
  }
}