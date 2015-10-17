package queryparse;

public class AutomatonEdge {
	public BaseNode child;
	public int bitset;

	public static final Character EMPTY_EDGE = '\0';

	public AutomatonEdge(BaseNode child, int bitset) {
		this.child = child;
		this.bitset = bitset;
	}

	@Override
	public int hashCode() {
		return ((this.child == null) ? 0 : this.child.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AutomatonEdge other = (AutomatonEdge) obj;
		if (this.child == null) {
			if (other.child != null)
				return false;
		} else if (!this.child.equals(other.child))
			return false;
		return true;
	}
}
