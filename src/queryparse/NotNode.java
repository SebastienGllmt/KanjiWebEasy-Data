package queryparse;

import java.util.Set;

class NotNode extends BaseNode {

	Set<Character> excludedSet;
	private AutomatonEdge edge;

	public NotNode(AutomatonEdge e, Set<Character> excludedSet) {
		this.excludedSet = excludedSet;
		this.edge = e;
	}

	@Override
	public AutomatonEdge getAutomatonEdge(Character c) {
	  if(c == ReservedSymbols.DOT) return null;
		return this.excludedSet.contains(c) ? null : this.edge;
	}
	
	@Override
	public int getBitset(){
		int epsilonBitset = super.getBitset();
		if(epsilonBitset != 0){
			return epsilonBitset & this.edge.bitset;
		}
		return this.edge.bitset;
	}
}