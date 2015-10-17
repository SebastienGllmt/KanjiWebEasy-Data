package suffixtree;

class ActivePoint<D extends TreeData> {
	
	/**
	 * represents the node holding our active edge
	 */
	Node<D>	node;
	
	/**
	 * represents the part of an edge we're trying to split
	 */
	Edge<D>	edge;
	
	/**
	 * represents how many elements are left to add and at the same time the length of the largest element to add
	 */
	int			remaining;
	
	/**
	 * How much we have to offset the content of the active edge to perform the insertion
	 * This is because sometimes the active edge gets larger than the current edge, so we have to switch the active node
	 * however, we can't shrink the active edge otherwise we lose some data
	 * ex: if we see current edge "ab" with active edge "abc", shrinking "abc" to just "c" means we lost adding the edge "bc".
	 * Therefore edge offset will be set to 2 instead.
	 */
	int			edgeOffset;
	
	public ActivePoint(SentenceInfo sentenceInfo, Node<D> root) {
		this.edge = Edge.createActiveEdge(sentenceInfo);
		this.edgeOffset = 0;
		this.remaining = 0;
		this.node = root;
	}
	/**
	 * Updates the active point using the suffix link rules.
	 * @param suffixLinkNode - the node that should become the suffix link
	 * @param root
	 */
	public void updateAfterAdd(SuffixTree<D> tree, Node<D> internalNode) {
		tree.createSuffixLink(internalNode);
		this.edge.stringStartIndex++;
		this.remaining--;
		
		// note: this.node != internalNode in some cases. Such as when you split an edge halfway
		// in that case, the internal node is the node where the split happens, but the active node is the node with an edge leading to the internal node
		if (this.node != tree.root) {
			// assert edgeOffset isn't 0 since if our node isn't the root, then we must have traveled through the tree, which means we incremented our offset
			assert this.edgeOffset != 0;
			
			/*
			 * Note: suffix link is null implies the edgeOffset is 1
			 * Proof: Suppose edgeOffset > 1.
			 * Therefore our active length > 1 since recall edgeOffset += currEdge length if activeLength > currEdge length
			 * Suppose our active edge is "abc" (length=3)
			 * This means that after we do an insertion for "abc", we then have to do "bc" then just "c"
			 * Recall all of these get connected by a suffix link
			 * Therefore the only one receiving no suffix link is "c", which is of length 1
			 * Therefore edgeOffset = 1 at the point where there is no suffix link.
			 * A contradiction
			 */
			assert this.node.suffixLink != null || this.edgeOffset == 1;
			
			this.edgeOffset--; // active length shouldn't change, so we decrement the edge offset to balance out incrementing the start index
			
			this.node = this.node.suffixLink == null ? tree.root : this.node.suffixLink;
		} else {
			this.edgeOffset = 0;
		}
	}
	public int getActiveLength() {
		assert this.edge.getLength() == this.remaining;
		return this.remaining - this.edgeOffset;
	}
}