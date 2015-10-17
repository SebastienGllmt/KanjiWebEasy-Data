package queryparse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeSet;

import kanjicounter.KanjiBucketer;

public class QueryParser {

  public static void main(String[] args) {
    File folder = new File(QUERY_OUT);
    if (folder.exists()) {
      deleteDir(folder);
    }

    System.out.println("Generating buckets");
    Map<Character, Integer> symbolMap = KanjiBucketer.getBuckets();
    System.out.println("Done generating buckets");

        // test simple query
        assert testParse("", true, symbolMap);
        assert testParse("は", false, symbolMap);
    
        // test braces
        assert testParse("()", false, symbolMap);
        assert testParse("()()()", false, symbolMap);
        assert testParse(")", true, symbolMap);
        assert testParse("(", true, symbolMap);
        assert testParse(")(", true, symbolMap);
    
        // test braces with elements
        assert testParse("(は)(は)", false, symbolMap);
        assert testParse("は(は)", false, symbolMap);
        assert testParse("は(は)は", false, symbolMap);
        assert testParse("(は)は", false, symbolMap);
        assert testParse("(は)", false, symbolMap);
        assert testParse("(は", true, symbolMap);
        assert testParse("は)", true, symbolMap);
    
        // test optional
        assert testParse("?", true, symbolMap);
        assert testParse("は?", true, symbolMap);
        assert testParse("(は)?", false, symbolMap);
        assert testParse("(はが)?", false, symbolMap);
        assert testParse("(はが)?えき", false, symbolMap);
        assert testParse("は??", true, symbolMap);
    
        // test plus
        assert testParse("+", false, symbolMap);
        assert testParse("+は", false, symbolMap);
        assert testParse("は+", false, symbolMap);
        assert testParse("は+えき", false, symbolMap);
        assert testParse("(は)+", false, symbolMap);
        assert testParse("(は|が)+えき", false, symbolMap);
        assert testParse("は++", true, symbolMap);
    
        // test wildcard
        assert testParse("*", false, symbolMap);
        assert testParse("*は", false, symbolMap);
        assert testParse("は*", false, symbolMap);
        assert testParse("は*えき", false, symbolMap);
        assert testParse("(は)*", false, symbolMap);
        assert testParse("(は|が)*えき", false, symbolMap);
        assert testParse("は**", true, symbolMap);
    
        // test any group
        assert testParse(".", false, symbolMap);
        assert testParse("..", false, symbolMap);
        assert testParse(".*", false, symbolMap);
        assert testParse("(.)", false, symbolMap);
        assert testParse("(.)?", false, symbolMap);
        assert testParse(".えき", false, symbolMap);
    
        // test not group
        assert testParse("[", true, symbolMap);
        assert testParse("]", true, symbolMap);
        assert testParse("[]", true, symbolMap);
        assert testParse("[は]", false, symbolMap);
        assert testParse("[はが]", false, symbolMap);
        assert testParse("[はが]えき", false, symbolMap);
        assert testParse("[は|が]", true, symbolMap);
        assert testParse("[は", true, symbolMap);
        assert testParse("は]", true, symbolMap);
        assert testParse("[は]が", false, symbolMap);
    
        // test or group
        assert testParse("|", true, symbolMap);
        assert testParse("は|", true, symbolMap);
        assert testParse("|が", true, symbolMap);
        assert testParse("||", true, symbolMap);
        assert testParse("は|が", false, symbolMap);
        assert testParse("(は|が)えき", false, symbolMap);
        assert testParse("はは|がが", false, symbolMap);
        assert testParse("(は)|が", false, symbolMap);
        assert testParse("は|(は|が)", false, symbolMap);
        
    assert testParse("(((は|が)?)*)+", false, symbolMap);

    try {
      String sentence = "の「２」か";
      String query = "(.)「(.)」(.)";
      int[] buckets = KanjiBucketer.getSuffixBuckets(sentence, symbolMap);
      QueryParser parser = new QueryParser(query, symbolMap);
      FiniteAutomaton fa = parser.parse();
      System.out.printf("Is %s in lang %s\n", sentence, query);
      boolean inLang = fa.isInLanguage(sentence, buckets);
      System.out.println(inLang);
    } catch (InvalidQueryException e) {
      e.printStackTrace();
    }
  }

  public static boolean testParse(String line, boolean expectException, Map<Character, Integer> symbolMap) {
    boolean PRINT = true;
    try {
      QueryParser parser = new QueryParser(line, symbolMap);
      FiniteAutomaton result = parser.parse();
      if (PRINT) {
        System.out.printf("%s -> %s\n", line, result == null ? "null" : result.toString());
      }
      if (result != null) {
        printTest(line, result);
      }
      return !expectException;
    } catch (InvalidQueryException e) {
      if (PRINT) {
        System.err.printf("%s -> %s\n", line, e.getMessage());
      }
      return expectException;
    }
  }

  private static int testCount = 0;
  private static String FILE_PREFIX = "test";
  private static String QUERY_OUT = "query";

  private static void deleteDir(File folder) {
    File[] files = folder.listFiles();
    if (files != null) { //some JVMs return null for empty dirs
      for (File f : files) {
        f.delete();
      }
    }
    folder.delete();
  }

  private static void printTest(String query, FiniteAutomaton fa) {
    File folder = new File(QUERY_OUT);
    if (!folder.exists()) {
      folder.mkdir();
    }
    String filename = String.format("%s/%s%d.dot", QUERY_OUT, FILE_PREFIX, testCount);
    File f = new File(filename);
    System.out.println(f.getAbsolutePath());
    try {
      f.createNewFile();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    try (PrintStream out = new PrintStream(new FileOutputStream(f))) {
      out.print(fa.toDotFormat(query));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    testCount++;
  }

  private String line;
  private int index;
  Map<Character, Integer> symbolMap;

  public QueryParser(String line, Map<Character, Integer> symbolMap) {
    this.line = line;
    this.index = line.length() - 1;
    this.symbolMap = symbolMap;
  }

  public FiniteAutomaton parse() throws InvalidQueryException {
    return this.parseBackward(new StdNode(), 0, 0);
  }

  private FiniteAutomaton parseBackward(final BaseNode finalState, int nextBitset, int depth) throws InvalidQueryException {
    if (line == null || line.isEmpty() || this.index < 0) {
      throw new InvalidQueryException("Can't have empty automaton");
    }
    if (finalState == null) {
      throw new IllegalArgumentException("final state can't be null");
    }
    BaseNode currState = finalState;
    int bitset = nextBitset;

    while (this.index >= 0) {
      char c = this.line.charAt(this.index);
      this.index--;
      switch (c) {
      case ReservedSymbols.OPEN_PAREN:
        if (depth == 0) {
          throw new InvalidQueryException(String.format("Found %c with unmatched %c", ReservedSymbols.OPEN_PAREN, ReservedSymbols.CLOSE_PAREN));
        }

        return new FiniteAutomaton(currState, finalState);
      case ReservedSymbols.CLOSE_PAREN:
        currState = parseBackward(currState, bitset, depth + 1).startState;
        break;
      case ReservedSymbols.OPTIONAL:
        if (this.index == -1) {
          throw new InvalidQueryException("Found optional with nothing before it");
        }
        char nextSymbol = this.line.charAt(this.index);
        this.index--;
        if (nextSymbol != ReservedSymbols.CLOSE_PAREN) {
          throw new InvalidQueryException("Can only use optional on groups");
        }
        FiniteAutomaton group = parseBackward(currState, bitset, depth + 1);
        group.startState.addEpsilonEdge(new AutomatonEdge(currState, bitset));
        currState = group.startState;
        break;
      case ReservedSymbols.CLOSE_SQUARE:
        int notGroupEndIndex = this.index;
        char next;
        do {
          if (this.index == -1) {
            throw new InvalidQueryException(String.format("Found %c with no matching %c", ReservedSymbols.CLOSE_SQUARE, ReservedSymbols.OPEN_SQUARE));
          }
          next = this.line.charAt(this.index);
          this.index--;
        } while (this.index >= 0 && !ReservedSymbols.isReserved(next));
        if (next != ReservedSymbols.OPEN_SQUARE) {
          if (ReservedSymbols.isReserved(next)) {
            throw new InvalidQueryException(String.format("Can not have reserved symbol inside %c%c", ReservedSymbols.OPEN_SQUARE, ReservedSymbols.CLOSE_SQUARE));
          } else {
            throw new InvalidQueryException(String.format("Found %c with no matching %c", ReservedSymbols.CLOSE_SQUARE, ReservedSymbols.OPEN_SQUARE));
          }
        }
        if (this.index + 1 == notGroupEndIndex) {
          throw new InvalidQueryException("Can not have empty NOT group");
        }

        TreeSet<Character> excludedSet = new TreeSet<>();
        String excludedChars = this.line.substring(this.index + 2, notGroupEndIndex + 1);
        excludedChars.chars().forEach(excludedChar -> excludedSet.add((char) excludedChar));
        NotNode notNode = new NotNode(new AutomatonEdge(currState, bitset), excludedSet);

        currState = notNode;
        break;
      case ReservedSymbols.OPEN_SQUARE:
        throw new InvalidQueryException(String.format("Can not have %c before matching %c", ReservedSymbols.OPEN_SQUARE, ReservedSymbols.CLOSE_SQUARE));
      case ReservedSymbols.PLUS:
      case ReservedSymbols.REPEAT:
        if (this.index < 0) {
          // if we try and repeat over nothing, just make it repeat a dot all before continuing
          if (c == ReservedSymbols.PLUS) {
            StdNode newBase = new StdNode();
            newBase.addAutomatonEdge(ReservedSymbols.DOT, new AutomatonEdge(currState, bitset));
            currState.addEpsilonEdge(new AutomatonEdge(newBase, bitset));
            currState = newBase;
          } else {
            StdNode start = new StdNode();
            StdNode end = new StdNode();
            start.addAutomatonEdge(ReservedSymbols.DOT, new AutomatonEdge(end, bitset));
            end.addEpsilonEdge(new AutomatonEdge(currState, bitset));
            currState.addEpsilonEdge(new AutomatonEdge(start, bitset));
          }
          break;
        } else {
          // first figure out what we're applying the * to
          FiniteAutomaton subGroup;
          char nextChar = this.line.charAt(this.index);
          this.index--;
          if (nextChar == ReservedSymbols.CLOSE_PAREN) { // applying * to an entire group
            subGroup = parseBackward(currState, bitset, depth + 1);
          } else {
            // applying * to just a single letter
            if (ReservedSymbols.isReserved(nextChar) && nextChar != ReservedSymbols.DOT) {
              throw new InvalidQueryException(String.format("Can not have reserved symbol before %c", ReservedSymbols.REPEAT));
            } else {
              int nextCharBitset = nextChar == ReservedSymbols.DOT ? 0 : this.symbolMap.getOrDefault(nextChar, 0);
              if (c == ReservedSymbols.PLUS) {
                StdNode start = new StdNode();
                start.addAutomatonEdge(nextChar, new AutomatonEdge(currState, bitset | nextCharBitset));

                subGroup = new FiniteAutomaton(start, currState);
              } else {
                StdNode start = new StdNode();
                StdNode end = new StdNode();
                start.addAutomatonEdge(nextChar, new AutomatonEdge(end, bitset | nextCharBitset));

                subGroup = new FiniteAutomaton(start, end);
              }
            }
          }

          if (c == ReservedSymbols.PLUS) {
            // make the final nodes point back to the start
            subGroup.finalState.addEpsilonEdge(new AutomatonEdge(subGroup.startState, bitset | subGroup.startState.getBitset()));

            // make the start node a final node
            // note: since the previous final node now points to the start node, it's as if they were both final nodes
            subGroup.finalState = subGroup.startState;

            currState = subGroup.startState;
          } else {
            subGroup.finalState.addEpsilonEdge(new AutomatonEdge(currState, bitset));
            currState.addEpsilonEdge(new AutomatonEdge(subGroup.startState, subGroup.startState.getBitset() | bitset));
          }
          break;
        }
      case ReservedSymbols.OR:
        if (currState == finalState) {
          throw new InvalidQueryException(String.format("%c must be followed by something", ReservedSymbols.OR));
        }
        // keep in mind the bitset being passed into the leftOr is that which was passed in when this function was called
        // that means it's either the bitset of one depth up or 0. Or in other words, the bitset of the part after this or
        FiniteAutomaton leftOr = parseBackward(new StdNode(), nextBitset, depth);
        FiniteAutomaton rightOr = new FiniteAutomaton(currState, finalState);
        FiniteAutomaton merged = QueryParser.or(leftOr, rightOr, finalState, nextBitset);

        return merged; // either we got to the end of the line or we need to go up one depth because we ran into an open group symbol
      case ReservedSymbols.DOT:
      default:
        if (c != ReservedSymbols.DOT) {
          bitset |= this.symbolMap.getOrDefault(c, 0);
        }
        StdNode newCurr = new StdNode();
        newCurr.addAutomatonEdge(c, new AutomatonEdge(currState, bitset));
        currState = newCurr;
      }
    }

    if (depth != 0) {
      throw new InvalidQueryException(String.format("Found %c with unmatched %c", ReservedSymbols.OPEN_PAREN, ReservedSymbols.CLOSE_PAREN));
    }
    return new FiniteAutomaton(currState, finalState);
  }

  public static FiniteAutomaton or(FiniteAutomaton leftAutomaton, FiniteAutomaton rightAutomaton, final BaseNode finalState, int nextBitset) {
    // set start state
    StdNode startState = new StdNode();
    int leftBitset = leftAutomaton.startState.getBitset();
    int rightBitset = rightAutomaton.startState.getBitset();

    startState.addEpsilonEdge(new AutomatonEdge(leftAutomaton.startState, leftBitset));
    startState.addEpsilonEdge(new AutomatonEdge(rightAutomaton.startState, rightBitset));

    // create final states
    if (finalState != leftAutomaton.finalState) {
      leftAutomaton.finalState.addEpsilonEdge(new AutomatonEdge(finalState, nextBitset));
    }
    if (finalState != rightAutomaton.finalState) {
      rightAutomaton.finalState.addEpsilonEdge(new AutomatonEdge(finalState, nextBitset));
    }

    return new FiniteAutomaton(startState, finalState);
  }
}