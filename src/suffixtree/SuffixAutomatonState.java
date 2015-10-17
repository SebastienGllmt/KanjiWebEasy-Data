package suffixtree;

import queryparse.BaseNode;

public class SuffixAutomatonState<D extends TreeData> {

	public BaseNode currNode;
	public int parseCount;
	public Edge<D> e;
	public int edgeIndex;

	public SuffixAutomatonState(BaseNode currNode, int parsedLetters, Edge<D> e, int edgeIndex) {
		this.currNode = currNode;
		this.parseCount = parsedLetters;
		this.e = e;
		this.edgeIndex = edgeIndex;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.currNode == null) ? 0 : this.currNode.hashCode());
		result = prime * result + ((this.e == null) ? 0 : this.e.hashCode());
		result = prime * result + this.edgeIndex;
		result = prime * result + this.parseCount;
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
		SuffixAutomatonState<?> other = (SuffixAutomatonState<?>) obj;
		if (this.currNode == null) {
			if (other.currNode != null)
				return false;
		} else if (!this.currNode.equals(other.currNode))
			return false;
		if (this.e == null) {
			if (other.e != null)
				return false;
		} else if (!this.e.equals(other.e))
			return false;
		if (this.edgeIndex != other.edgeIndex)
			return false;
		if (this.parseCount != other.parseCount)
			return false;
		return true;
	}
}
