package queryparse;

import java.util.Map.Entry;
import java.util.TreeMap;

class StdNode extends BaseNode {

	protected TreeMap<Character, AutomatonEdge> edgeMap;

	public StdNode() {
		this.edgeMap = new TreeMap<>();
	}

	public AutomatonEdge getAutomatonEdge(Character c) {
		return this.edgeMap.get(c);
	}

	public void addAutomatonEdge(Character c, AutomatonEdge e) {
		this.edgeMap.put(c, e);
	}

	@Override
	public int getBitset() {
		int bitset = 0;
		boolean hasSet = false;
		for(Entry<Character, AutomatonEdge> e : this.edgeMap.entrySet()){
			if (e.getKey() != ReservedSymbols.DOT) {
				if (hasSet) {
					bitset &= e.getValue().bitset;
				} else {
					bitset = e.getValue().bitset;
				}
			}
		}

		int epsilonBitset = super.getBitset();
		if (epsilonBitset != 0) {
			bitset &= epsilonBitset;
		}

		return bitset;
	}
}