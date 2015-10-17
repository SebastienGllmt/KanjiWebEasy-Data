package queryparse;

public class StateTracker {

	public BaseNode currNode;
	public int currIndex;
	
	public StateTracker(BaseNode currNode, int currIndex) {
		this.currNode = currNode;
		this.currIndex = currIndex;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.currIndex;
		result = prime * result + ((this.currNode == null) ? 0 : this.currNode.hashCode());
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
		StateTracker other = (StateTracker) obj;
		if (this.currIndex != other.currIndex)
			return false;
		if (this.currNode == null) {
			if (other.currNode != null)
				return false;
		} else if (!this.currNode.equals(other.currNode))
			return false;
		return true;
	}
}