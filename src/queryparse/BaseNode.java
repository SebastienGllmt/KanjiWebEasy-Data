package queryparse;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public abstract class BaseNode {
	private Set<AutomatonEdge> epsilonEdges;

	public BaseNode() {
		this.epsilonEdges = new HashSet<>();
	}

	public void addEpsilonEdge(AutomatonEdge e) {
	  // avoid epsilon transitions to ourselves.
	  if(e.child == this){
	    return;
	  }
		this.epsilonEdges.add(e);
	}

	public Set<AutomatonEdge> getEpsilonEdges() {
		return this.epsilonEdges;
	}

	public abstract AutomatonEdge getAutomatonEdge(Character c);

	public int getBitset() {
		int bitset = 0;
		Iterator<AutomatonEdge> iter = getEpsilonEdges().iterator();
		if(!iter.hasNext()){
			return bitset;
		}
		bitset = iter.next().bitset;
		while (iter.hasNext()) {
			bitset &= iter.next().bitset;
			if (bitset == 0) {
				break;
			}
		}

		return bitset;
	}
}