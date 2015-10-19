package suffixtree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import queryparse.AutomatonEdge;
import queryparse.BaseNode;
import queryparse.FiniteAutomaton;
import queryparse.ReservedSymbols;

public class SuffixTree<D> {

  public static final String EMPTY_STRING = "ɛ";
  private static final char END_CHAR = '‖';
  public final Node<D> root;
  private Node<D> mostRecentInternalNode = null;
  private Node<D> mostRecentEpsilonNode = null;
  private int allocatedNodes;

  public SuffixTree() {
    this.root = Node.newInternalNode(null, this.getNextNodeId());
  }

  public int getNextNodeId() {
    this.allocatedNodes++;
    return this.allocatedNodes - 1;
  }

  public void addString(SentenceInfo sentenceInfo, D data) {
    ActivePoint<D> activePoint = new ActivePoint<D>(sentenceInfo, this.root);

    // the current edge we're looking at. When we try and add a new char, we look to see if it's already part of this edge
    Edge<D> currEdge = null;

    for (int i = 0; i < sentenceInfo.sentence.length() + 1; i++) {
      char currChar;

      // at the end we need to make our current character a char that hasn't appeared anywhere else in the text
      if (i == sentenceInfo.sentence.length()) {
        currChar = END_CHAR;
      } else {
        currChar = sentenceInfo.sentence.charAt(i);
      }

      // first assume that our current edge still matches our active edge
      activePoint.edge.stringEndIndex++;
      activePoint.remaining++;

      // make sure we don't accidentally link nodes created in different rounds.
      this.resetSuffixLinkRound();

      while (activePoint.remaining > 0) {

        // if we're at the special end char, we don't want to add a branch to our tree for it
        if ((activePoint.remaining == 1 && currChar == END_CHAR)) {
          // if we get to this point, there is only 1 letter left to insert
          assert activePoint.getActiveLength() == 1;

          // having a suffix link from a node that is 1 letter away from the root is the same as not having such a suffix link
          // this is because our algorithm states that "on insertion, either follow the suffix link or go back to the root"
          // and if the suffix link and root are the same, then the result is the same
          // we choose not to add the suffix link in this case for better visual clarity on the graphs.
          if (activePoint.node != this.root) {
            this.createSuffixLink(activePoint.node);
          }
          activePoint.remaining--;
          break;
        }

        /*
         * The string might end with multiples characters that are the same so we need to add an extra epsilon edge
         * To show how we handle this, imagine the case ababb. When we're adding suffixes we will get to the point where we have to add both "bb" and "b" (remaining = 2)
         * the insertion of bb will go fine, but one we get to the insertion of just "b", it's a subset of "bb"
         * this means our algorithm will follow the path from a node (root or result of following a suffix link) until the first "b" of a "bb" edge then stop.
         * What we want is that when we get to the first "b" of "bb", we add data to the node to indicate the presence of the suffix "b"
         * 
         * This case also occurs during the creation of generalized suffix trees
         * Notably if the string we're trying to insert is already contained entirely within the tree
         * Note that the suffix links in this case are extra helpful since if one string is entirely contained, all its suffixes are entirely contained also
         */
        if (activePoint.edge.stringStartIndex + activePoint.edgeOffset == sentenceInfo.sentence.length()) {
          activePoint.node.addData(data);
          // we have to tie in the active point to the epsilon suffix chain here as if we added a new edge as before, this may have already been connected (but may not)
          // see argument outlined when we split an edge as to why if it's already connected, we're just connecting it to the same node
          // we don't want to add epsilon edges to the root
          if (activePoint.node != this.root) {
            // see argument outlined when we split an edge as to why if it's already connected, we're just connecting it to the same node
            this.createEpsilonSuffixLink(activePoint.node);
          }

          activePoint.updateAfterAdd(this, activePoint.node);
          continue;
        }

        // re-target our current edge based off our active point
        // note: the current edge here may have been something else -- possible incorrect data. It was not necessarily null
        currEdge = activePoint.node.getEdgeList().get(sentenceInfo.sentence.charAt(activePoint.edge.stringStartIndex + activePoint.edgeOffset));
        if (currEdge == null) {
          // we've never seen this substring before, so add it to our tree.
          int startIndex = activePoint.edge.stringStartIndex + activePoint.edgeOffset;

          activePoint.node.getEdgeList().put(currChar, Edge.restOfSentenceEdge(this, sentenceInfo, startIndex, data));

          if (activePoint.node == this.root) {
            // note: if we got here it's because the active node's edge list doesn't contain the suffix starting with a given character
            // if that active node is the root, then the entire tree has never seen a suffix starting with a given character
            // therefore it must be the last char remaining, since remaining can only increment as long as we see characters we've seen before
            assert activePoint.remaining == 1;
          }
          activePoint.updateAfterAdd(this, activePoint.node);
          continue;
        }

        // if our active edge contains all of our current edge, move our active point over to the next node
        // note: if our active edge extends all the way to the special end char, this is always true
        if (activePoint.getActiveLength() > currEdge.getLength()) {
          activePoint.edgeOffset += currEdge.getLength();
          activePoint.node = currEdge.child;
          continue;
        }

        // assert that if we got to this point, the active edge and the current edge share at least 1 char (notably the first one)
        // This is because we picked our current edge such that this property held.
        // Otherwise, we inserted a new edge
        assert currEdge.sentenceInfo.sentence.charAt(currEdge.stringStartIndex) == sentenceInfo.sentence.charAt(activePoint.edge.stringStartIndex + activePoint.edgeOffset);

        // how far we are into the current edge.
        // NOTE: We're comparing with the sentence of the current edge, which may not be the sentence we're currently adding
        int currEdgeIndex = currEdge.stringStartIndex + activePoint.getActiveLength() - 1;

        if (currEdge.sentenceInfo.sentence.charAt(currEdgeIndex) == currChar) {
          // if we matched the next character and have no insertions to do, then break
          // but before that, if we have nothing left to add, we just need to place any leftover suffix links
          // recall once we break we will reset the suffix link round
          this.createSuffixLink(activePoint.node);
          break;
        } else {
          // if our assumption that our current edge still matches our active edge is wrong
          // we must split the current edge into two parts and add our new branch

          Edge<D> nextEdge = sentenceInfo.sentence.length() - i > 0 ? Edge.restOfSentenceEdge(this, sentenceInfo, i, data) : null;
          splitEdge(sentenceInfo, data, currChar, activePoint, currEdge, nextEdge);
          continue;
        }
      }
    }
    assert activePoint.remaining == 0;

    // this call is actually kind of pointless since the suffix link round is already reset for every letter of an input
    // put this here anyways that way we make sure that the data structure
    // doesn't leave any trace of the last round data to avoid bugs in the future
    this.resetSuffixLinkRound();

    // clear this between sentences
    this.resetEpsilonSuffixLinkRound();
  }

  /**
   * splits before the current character we're looking at
   */
  private void splitEdge(SentenceInfo sentenceInfo, D data, char currChar, ActivePoint<D> activePoint, Edge<D> currEdge, Edge<D> newEdge) {
    // shrink our current edge to only contain the shared portion of both strings
    // NOTE: We're splitting on the sentence of the current edge, which may not be the sentence we're currently adding
    // this edge is at least 1 in size. See comment block inside split edge function for more detail
    int newEnd = currEdge.stringStartIndex + activePoint.getActiveLength() - 1;

    // Note: Our current suffix bucket is not the same that was used to create the edge we're about to split
    // This is why we can not create reliable bitsets in reasonable time if we try and perfectly annotate the edges as we build the tree
    // instead, we will annotate the edges in a 2nd pass of the data structure after construction
    Edge<D> splitEdge = Edge.splitEdge(currEdge.sentenceInfo, currEdge, newEnd);

    currEdge.stringEndIndex = currEdge.stringStartIndex + activePoint.getActiveLength() - 1;

    // create our new internal node which represents the fork between our new element and the branch of the tree previously there
    Node<D> newNode = Node.newInternalNode(null, this.getNextNodeId());
    if (newEdge == null) {
      // to indicate that some suffix ended exactly on the forking point, we add it to the data list
      newNode.addData(data);

      /*
       * since the internal node represents the end of some suffix, we have to remember to link it up with all other epsilon branches
       * the problem is that when we call updateAfterAdd at the end of this function call, we are queuing up newNode to be used as a suffix link
       * so if this would result in newNode having a suffix link pointing to something that isn't the next epsilon transition then we would have a problem.
       * However,
       * To prove that they would have been linked regardless,
       * Note that the only way we can get here is if our current suffix S matched an existing edge E all the way up until the end of the sentence
       * So it will create a new internal node N here
       * By properties of the suffix tree, E without its first character must also be a branch of the tree (i.e. "abc" -> "bc" is in the tree)
       * Therefore S without its first letter will match E without its first character all the way to the end of the sentence also
       * So it will create another new internal node M here with an epsilon transition on it
       * Therefore by suffix links, N -> M
       * Therefore they would have been linked anyway, as desired.
       */
      createEpsilonSuffixLink(newNode);
    } else {
      assert newEdge.getLength() != 0;
      // note: the first letter in the edge is always the first character of the current suffix we're adding
      newNode.getEdgeList().put(currChar, newEdge);
    }
    newNode.getEdgeList().put(splitEdge.sentenceInfo.sentence.charAt(splitEdge.stringStartIndex), splitEdge);

    currEdge.child = newNode;

    activePoint.updateAfterAdd(this, newNode);
  }

  /**
   * Creates a suffix link connection to n if one is possible.
   * 
   * @param n
   */
  public void createSuffixLink(Node<D> newNode) {
    if (this.mostRecentInternalNode != null) {
      // note: it's possible for n and mostRecentInternalNode.suffix link to be the same
      // this function call is still meaningful since mostRecent will update to become n
      // however, we want to assert that we don't accidentally change the suffix link from something meaningful to a new value
      // this is done with the following implication

      // suffix link is not null implies suffix link is n
      assert this.mostRecentInternalNode.suffixLink == null || this.mostRecentInternalNode.suffixLink == newNode;

      this.mostRecentInternalNode.suffixLink = newNode;
    }
    this.mostRecentInternalNode = newNode;
  }

  /**
   * Creates a suffix link connection to n if one is possible.
   * 
   * @param n
   */
  public void createEpsilonSuffixLink(Node<D> newNode) {
    if (this.mostRecentEpsilonNode != null) {
      // assert suffix link not null -> linking to the same thing it was already linking to.
      //See comment for the same implication on regular suffix links
      assert this.mostRecentEpsilonNode.suffixLink == null || this.mostRecentEpsilonNode.suffixLink == newNode;

      this.mostRecentEpsilonNode.suffixLink = newNode;
    }
    this.mostRecentEpsilonNode = newNode;
  }

  public void resetSuffixLinkRound() {
    this.mostRecentInternalNode = null;
  }

  public void resetEpsilonSuffixLinkRound() {
    this.mostRecentEpsilonNode = null;
  }

  public String toString() {
    String header = PrintUtils.printHeader();
    String nodes = PrintUtils.printAllNodes(this.root);
    String edges = PrintUtils.printAllEdges(this.root);
    String suffixes = PrintUtils.printAllSuffixLinks(this.root);
    return header + "// nodes\n" + nodes + "// edges\n" + edges + "// suffix links\n" + suffixes + "}\n";
  }

  public void assignBitsets() {
    for (Edge<D> e : this.root.getEdgeList().values()) {
      assignBitsets(e);
    }
  }

  private void assignBitsets(Edge<D> parentEdge) {
    // if this is just an edge leading to data, we've reached the end of this branch
    if (parentEdge.getLength() == 0) {
      return;
    }

    Node<D> child = parentEdge.child;
    int bitset = 0;
    for (Edge<D> e : child.getEdgeList().values()) {
      assignBitsets(e);
      bitset |= e.getFullBitset();
    }

    parentEdge.childBitset = bitset;
  }

  /*
   * TREE SEARCH
   */

  public Set<D> findAll(String key, int max) {
    return findAll(new String[] { key }, max, null);
  }

  public Set<D> findAll(String[] keys, int max, Map<Character, Integer> symbolMap) {
    keys = removeEmptyWildcards(keys);
    if (keys.length == 0) {
      return new HashSet<>();
    }

    int[][] wildcardBuckets = new int[keys.length][];
    for (int i = keys.length - 1; i >= 0; i--) {
      wildcardBuckets[i] = new int[keys[i].length()];
      // the current bucket is the bucket that comes after it and more
      if (i != keys.length - 1) {
        wildcardBuckets[i][keys[i].length() - 1] = wildcardBuckets[i + 1][0];
      }
      // put all chars in current wildcard to this bucket
      for (int j = keys[i].length() - 1; j >= 0; j--) {
        if (j != keys[i].length() - 1) {
          wildcardBuckets[i][j] = wildcardBuckets[i][j + 1];
        }
        Integer charBitset = symbolMap.get(keys[i].charAt(j));
        if (charBitset == null) {
          // some letter in the input isn't even in a bucket
          return new HashSet<>();
        }
        wildcardBuckets[i][j] |= charBitset;
      }
    }

    // how far into key[0] we've traversed
    int keyIndex = 0;
    int lastKeyIndex = 0;

    Node<D> currNode = this.root;
    Edge<D> currEdge = null;
    while (keyIndex < keys[0].length()) {
      currEdge = currNode.getEdgeList().get(keys[0].charAt(keyIndex));
      if (currEdge == null) {
        return new HashSet<>();
      }
      if ((currEdge.getFullBitset() & wildcardBuckets[0][keyIndex]) == 0) {
        return new HashSet<>();
      }
      lastKeyIndex = keyIndex;
      keyIndex = updateKeyIndex(keys[0], keyIndex, currEdge);

      currNode = currEdge.child;
    }

    Set<D> articles = new HashSet<>();

    // traverse the remaining part of the first edge matching all of key[0] to see if we can match anything else in our wildcard from there
    if (keys.length > 1) {
      int edgeIndex = (keyIndex - lastKeyIndex);
      if ((currEdge.getPartBitset(edgeIndex) & wildcardBuckets[1][0]) != 0) {
        for (KeyLocation kl : searchEdge(currEdge, edgeIndex, 0, keys, 1)) {
          LeafSearchBFS(currNode, articles, keys, kl.keyOffset, kl.keyIndex, max, wildcardBuckets);
        }
      }
    }
    // case where nothing was matched
    LeafSearchBFS(currNode, articles, keys, 0, 1, max, wildcardBuckets);
    return articles;
  }

  private static <D> void LeafSearchBFS(Node<D> currNode, Set<D> articles, String[] keys, int keyOffset, int keyIndex, int max, int[][] wildcardBuckets) {
    if (articles.size() == max) {
      return;
    }
    if (currNode.dataSet != null) {
      // if we've matched all keys
      if (keyIndex >= keys.length) {
        if (articles.size() + currNode.dataSet.size() > max) {
          articles.addAll(currNode.dataSet);
        } else {
          articles.addAll(currNode.dataSet.stream().limit(max - articles.size()).collect(Collectors.toSet()));
        }
      }
    }
    for (Edge<D> e : currNode.getEdgeList().values()) {
      // if we've already matched all patterns
      if (keyIndex >= keys.length) {
        LeafSearchBFS(e.child, articles, keys, keyOffset, keyIndex, max, wildcardBuckets);
      } else {
        if (e.getLength() == 0) {
          continue;
        }
        // the current edge must lead to at least all the bits in the wildcard bucket
        if ((e.getFullBitset() & wildcardBuckets[keyIndex][keyOffset]) == 0) {
          continue;
        }

        // have to find branches with our pattern
        List<KeyLocation> locations = searchEdge(e, e.stringStartIndex, keyOffset, keys, keyIndex);
        // go down all paths where we matched part of a wildcard on the current edge
        for (KeyLocation kl : locations) {
          LeafSearchBFS(e.child, articles, keys, kl.keyOffset, kl.keyIndex, max, wildcardBuckets);
        }
        // go down all paths assuming we didn't match a wildcard on our current edge.
        // Note we can only do this if we didn't partially match a key already (aka if the key offset is 0)
        if (keyOffset == 0) {
          LeafSearchBFS(e.child, articles, keys, keyOffset, keyIndex, max, wildcardBuckets);
        }
      }
    }
  }

  private static <D> List<KeyLocation> searchEdge(Edge<D> currEdge, int edgeIndex, int keyOffset, String[] keys, int keyIndex) {
    if (keyIndex >= keys.length) {
      return new ArrayList<KeyLocation>();
    }
    List<KeyLocation> splitPoints = new ArrayList<KeyLocation>();

    for (int i = edgeIndex; i < currEdge.stringEndIndex; i++) {
      // if we're currently considering a key offset, the edge must start with the remaining part of our key
      if (keyOffset > 0 && i > edgeIndex) {
        break;
      }
      int scanCount;
      int keyPos;
      for (keyPos = keyOffset, scanCount = 0; keyPos < keys[keyIndex].length(); keyPos++, scanCount++) {
        if (i + scanCount == currEdge.stringEndIndex) {
          splitPoints.add(new KeyLocation(keyIndex, keyOffset + scanCount));
          // look for the same string that starts at least 1 after the one we already found
          splitPoints.addAll(searchEdge(currEdge, i + 1, keyOffset, keys, keyIndex));
          break;
        }
        if (currEdge.sentenceInfo.sentence.charAt(i + scanCount) != keys[keyIndex].charAt(keyOffset + scanCount)) {
          break;
        }
      }
      if (keyPos == keys[keyIndex].length()) {
        // we match keyIndex, so specify the point we're at is keyIndex+1
        splitPoints.add(new KeyLocation(keyIndex + 1, 0));
        //look for the same string that starts at least 1 after the one we already found
        splitPoints.addAll(searchEdge(currEdge, i + 1, keyOffset, keys, keyIndex));
        // look for the next key starting right after the string we found
        splitPoints.addAll(searchEdge(currEdge, i + scanCount, 0, keys, keyIndex + 1));
      }
    }

    return splitPoints;
  }

  private static <D> int updateKeyIndex(String key, int keyIndex, Edge<D> currEdge) {
    int currEdgeIndex = currEdge.stringStartIndex;
    while (currEdgeIndex != currEdge.stringEndIndex) {
      if (currEdge.sentenceInfo.sentence.charAt(currEdgeIndex) == key.charAt(keyIndex)) {
        keyIndex++;
        if (keyIndex == key.length()) {
          break;
        }
        currEdgeIndex++;
      }
    }
    return keyIndex;
  }

  private static String[] removeEmptyWildcards(String[] keys) {
    int count = 0;
    for (int i = 0; i < keys.length; i++) {
      if (!keys[i].isEmpty()) {
        count++;
      }
    }

    // if no empty, just return the original array
    if (count == 0) {
      return keys;
    }

    String[] newKeys = new String[count];

    for (int j = 0, i = 0; i < keys.length; i++) {
      if (!keys[i].isEmpty()) {
        newKeys[j] = keys[i];
        j++;
      }
    }
    return newKeys;
  }

  /**
   * Finds all sentences that match a given finite automaton
   */
  public Set<D> FindMatching(FiniteAutomaton fa) {
    Set<D> dataSet = new HashSet<D>();

    Set<SuffixAutomatonState<D>> seenSet = new HashSet<>();
    // go through every edge starting at the root of the tree and see if we find a match on it
    this.root.getEdgeList().forEach((k, e) -> {
      SuffixAutomatonState<D> currState = new SuffixAutomatonState<>(fa.startState, 0, e, 0);
      List<SuffixAutomatonState<D>> currDepth = new ArrayList<>();
      currDepth.add(currState);
      FindMatching(currDepth, seenSet, dataSet, fa.finalState);
    });

    return dataSet;
  }

  /**
   * Performs a BFS on the suffix tree
   * 
   * @param currDepth
   *          Nodes at our current BFS depth
   * @param seenSet
   *          Nodes already seen before
   * @param results
   *          Set of branches that matched successfully
   * @param finalState
   *          The final state of our automaton we're trying to match
   * @return
   */
  private static <D> void FindMatching(List<SuffixAutomatonState<D>> currDepth, Set<SuffixAutomatonState<D>> seenSet, Set<D> results, BaseNode finalState) {
    if (currDepth.size() == 0) {
      return;
    }

    // if any state at our current depth matches the final state, add all branches of that tree as a result
    for (SuffixAutomatonState<D> state : currDepth) {
      if (state.currNode == finalState) {
        DfsSearchNode(state.e.child, results);
      }
    }
    
    List<SuffixAutomatonState<D>> nextDepth = new ArrayList<>();

    // add all states that we can get to following our input to our next BFS depth
    for (SuffixAutomatonState<D> st : currDepth) {
      nextDepth.addAll(SuffixTree.getNextAutomatonNodes(st, seenSet));
    }

    // go down to next BFS level
    FindMatching(nextDepth, seenSet, results, finalState);
  }

  /**
   * Performs a DFS search starting at a node and stores all results into a set
   */
  private static <D> void DfsSearchNode(Node<D> n, Set<D> dataSet) {
    if (n.dataSet != null) {
      for (D d : n.dataSet) {
        dataSet.add(d);
      }
    }
    n.getEdgeList().forEach((k, e) -> DfsSearchNode(e.child, dataSet));
  }

  public static <D> List<SuffixAutomatonState<D>> getNextAutomatonNodes(SuffixAutomatonState<D> state, Set<SuffixAutomatonState<D>> seenSet) {
    List<SuffixAutomatonState<D>> nextNodes = new ArrayList<>();
    // first add any epsilon edges from the automaton
    for (AutomatonEdge epsilonAutomatonEdge : state.currNode.getEpsilonEdges()) {
      SuffixTree.addEdgeToSearch(epsilonAutomatonEdge, state.e, state.edgeIndex, false, state.parseCount, nextNodes, seenSet);
    }

    if (state.e.stringStartIndex + state.edgeIndex == state.e.stringEndIndex) {
      Node<D> child = state.e.child;
      child.getEdgeList().forEach((k, e) -> {
        SuffixAutomatonState<D> newState = new SuffixAutomatonState<>(state.currNode, state.parseCount, e, 0);
        nextNodes.add(newState);
      });
    } else {
      // find matches for specific char
      AutomatonEdge charAutomatonEdge = state.currNode.getAutomatonEdge(state.e.charAt(state.edgeIndex));
      SuffixTree.addEdgeToSearch(charAutomatonEdge, state.e, state.edgeIndex, true, state.parseCount + 1, nextNodes, seenSet);
      // find matches for general dot
      AutomatonEdge dotAutomatonEdge = state.currNode.getAutomatonEdge(ReservedSymbols.DOT);

      // these may be equal if the current node is a NOT node that doesn't include the current char nor the dot in its exclusion list
      if (charAutomatonEdge != dotAutomatonEdge) {
        SuffixTree.addEdgeToSearch(dotAutomatonEdge, state.e, state.edgeIndex, true, state.parseCount + 1, nextNodes, seenSet);
      }
    }

    return nextNodes;
  }

  private static <D> void addEdgeToSearch(AutomatonEdge automatonEdge, Edge<D> edge, int currIndex, boolean incrementIndex, int newAutomatonIndex, List<SuffixAutomatonState<D>> nextNodes, Set<SuffixAutomatonState<D>> seenSet) {
    if (automatonEdge == null) {
      return;
    }
    // bitset not 0 implies we match at least all the bitsets required to get to the end of the automaton
    if (automatonEdge.bitset == 0 || (automatonEdge.bitset & edge.getPartBitset(currIndex)) == automatonEdge.bitset) {
      SuffixAutomatonState<D> newState = new SuffixAutomatonState<>(automatonEdge.child, newAutomatonIndex, edge, incrementIndex ? currIndex + 1 : currIndex);
      if (seenSet.add(newState)) {
        nextNodes.add(newState);
      }
    }
  }
}

class KeyLocation {

  public int keyIndex;
  public int keyOffset;

  public KeyLocation(int keyIndex, int keyOffset) {
    this.keyIndex = keyIndex;
    this.keyOffset = keyOffset;
  }
}
