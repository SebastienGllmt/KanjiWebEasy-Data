package queryparse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import suffixtree.PrintUtils;

public class FiniteAutomaton {

  public BaseNode finalState;
  public BaseNode startState;

  private static final String EDGE_FORMAT = "node%d -> node%d [label=\"%s (%s)\", style=%s, weight=3]\n";
  private static final String NODE_FORMAT = "node%d [label=\"\", %sshape=%s]\n";

  private static final int BITSET_BASE = 8;
  private static final int PADDING_LEN = (int) Math.ceil(32.0 / (Math.log(BITSET_BASE) / Math.log(2)));

  public FiniteAutomaton(BaseNode startState, BaseNode finalState) {
    this.startState = startState;
    this.finalState = finalState;
  }

  /**
   * Checks if the finite automaton matches s ENTIRELY (only matching a prefix or suffix is not enough)
   */
  public boolean isInLanguage(String s, int[] bitset) {
    StateTracker firstState = new StateTracker(this.startState, 0);
    List<StateTracker> startList = new ArrayList<>(1);
    startList.add(firstState);
    Set<StateTracker> seenSet = new HashSet<StateTracker>();
    seenSet.add(firstState);
    return isInLanguage(startList, s, bitset, seenSet);
  }

  private boolean isInLanguage(List<StateTracker> currDepth, String s, int[] bitset, Set<StateTracker> seenSet) {
    if (currDepth.size() == 0) {
      return false;
    }
    for (StateTracker state : currDepth) {
      if (state.currNode == this.finalState) {
        if (state.currIndex == s.length()) {
          return true;
        }
      }
    }
    List<StateTracker> nextDepth = new ArrayList<StateTracker>();
    for (StateTracker state : currDepth) {
      Character currChar;
      int currBitset;
      if (state.currIndex == s.length()) {
        currChar = null;
        currBitset = 0;
      } else {
        currChar = s.charAt(state.currIndex);
        currBitset = bitset[state.currIndex];
      }
      nextDepth.addAll(getNext(state, currChar, currBitset, seenSet));
    }

    return isInLanguage(nextDepth, s, bitset, seenSet);
  }

  /**
   * get all states that we can get to by either taking in a letter as input or
   * following an epsilon transition as long as the bitset matches
   */
  public static List<StateTracker> getNext(StateTracker state, Character currLetter, int bitset, Set<StateTracker> seenSet) {
    List<StateTracker> nextNodes = new ArrayList<>();
    for (AutomatonEdge epsilonAutomatonEdge : state.currNode.getEpsilonEdges()) {
      FiniteAutomaton.checkBitsetAndAdd(epsilonAutomatonEdge, bitset, state.currIndex, nextNodes, seenSet);
    }

    if (currLetter != null) {
      AutomatonEdge charAutomatonEdge = state.currNode.getAutomatonEdge(currLetter);
      FiniteAutomaton.checkBitsetAndAdd(charAutomatonEdge, bitset, state.currIndex + 1, nextNodes, seenSet);
      AutomatonEdge dotAutomatonEdge = state.currNode.getAutomatonEdge(ReservedSymbols.DOT);

      // these may be equal if the current node is a NOT node that doesn't include the current char nor the dot in its exclusion list
      if (charAutomatonEdge != dotAutomatonEdge) {
        FiniteAutomaton.checkBitsetAndAdd(dotAutomatonEdge, bitset, state.currIndex + 1, nextNodes, seenSet);
      }
    }

    return nextNodes;
  }

  private static void checkBitsetAndAdd(AutomatonEdge edge, int bitset, int newIndex, List<StateTracker> nextNodes, Set<StateTracker> seenSet) {
    if (edge == null) {
      return;
    }
    if (edge.bitset == 0 || (edge.bitset & bitset) == edge.bitset) {
      StateTracker newState = new StateTracker(edge.child, newIndex);
      if (seenSet.add(newState)) {
        nextNodes.add(newState);
      }
    }
  }

  public String toString() {
    Set<BaseNode> seenSet = new HashSet<BaseNode>();
    HashMap<BaseNode, Integer> idMap = new HashMap<>();
    idMap.put(null, 0); // the counter
    return PrintUtils.printHeader() + toString(this.startState, seenSet, idMap) + "}";
  }

  public String toDotFormat(String regex) {
    Set<BaseNode> seenSet = new HashSet<BaseNode>();
    HashMap<BaseNode, Integer> idMap = new HashMap<>();
    idMap.put(null, 0); // the counter
    return PrintUtils.printHeader() + PrintUtils.makeTitle(regex) + toString(this.startState, seenSet, idMap) + "}";
  }

  private String toString(BaseNode n, Set<BaseNode> seenSet, HashMap<BaseNode, Integer> idMap) {
    seenSet.add(n);
    String thisNode = printFirstTime(n, idMap);
    if (n instanceof StdNode) {
      StdNode stdNode = (StdNode) n;
      for (Entry<Character, AutomatonEdge> e : stdNode.edgeMap.entrySet()) {
        String bitset = Integer.toString(e.getValue().bitset, BITSET_BASE);
        bitset = StringUtils.leftPad(bitset, PADDING_LEN, '0');
        thisNode += String.format(EDGE_FORMAT, getId(n, idMap), getId(e.getValue().child, idMap), PrintUtils.formatText(e.getKey()), bitset, "solid");
        if (!seenSet.contains(e.getValue().child)) {
          thisNode += toString(e.getValue().child, seenSet, idMap);
        }
      }
    } else if (n instanceof NotNode) {
      NotNode notNode = (NotNode) n;
      StringBuilder notString = new StringBuilder(notNode.excludedSet.size() + 2);
      notString.append('[');
      notNode.excludedSet.forEach(c -> notString.append(c));
      notString.append(']');

      // assume \0 will always not be in the excluded chars
      BaseNode child = notNode.getAutomatonEdge('\0').child;

      String bitset = Integer.toString(notNode.getBitset(), BITSET_BASE);
      bitset = StringUtils.leftPad(bitset, PADDING_LEN, '0');
      thisNode += String.format(EDGE_FORMAT, getId(n, idMap), getId(child, idMap), PrintUtils.formatText(notString.toString()), bitset, "solid");
      if (!seenSet.contains(child)) {
        thisNode += toString(child, seenSet, idMap);
      }
    } else {
      // do nothing
    }

    for (AutomatonEdge e : n.getEpsilonEdges()) {
      String bitset = Integer.toString(e.bitset, BITSET_BASE);
      bitset = StringUtils.leftPad(bitset, PADDING_LEN, '0');
      thisNode += String.format(EDGE_FORMAT, getId(n, idMap), getId(e.child, idMap), PrintUtils.formatText("ɛ"), bitset, "dashed");
      if (!seenSet.contains(e.child)) {
        thisNode += toString(e.child, seenSet, idMap);
      }
    }
    return thisNode;
  }

  private static int getId(BaseNode node, HashMap<BaseNode, Integer> idMap) {
    return idMap.computeIfAbsent(node, k -> (idMap.compute(null, (none, v) -> v + 1)));
  }

  private String printFirstTime(BaseNode n, HashMap<BaseNode, Integer> idMap) {
    String shape = this.startState == n ? "triangle" : "box";
    String color = this.finalState == n ? "style=filled, fillcolor=red, " : "";

    return String.format(NODE_FORMAT, getId(n, idMap), color, shape);
  }
}