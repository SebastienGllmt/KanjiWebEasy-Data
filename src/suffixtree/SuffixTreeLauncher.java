package suffixtree;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonFactory;

import kanjicounter.KanjiBucketer;
import queryparse.AutomatonEdge;
import queryparse.BaseNode;
import queryparse.FiniteAutomaton;
import queryparse.InvalidQueryException;
import queryparse.QueryParser;
import queryparse.ReservedSymbols;
import util.NewsEasyFormatUtil;

public class SuffixTreeLauncher {

  private static final String IN_DIR = "in";
  private static final String LOOKUP_DIR = "lookup";
  private static final boolean RECREATE = false, SAVE_TO_DB = false;

  public static void genAndSaveTree(Map<Character, Integer> symbolMap) {
    long start = System.currentTimeMillis();
    symbolMap = KanjiBucketer.getBuckets();
    if (SAVE_TO_DB) {
      try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/KanjiWebEasy", "postgres", "ESMashS:VY65536")) {
        Statement statement = conn.createStatement();
        statement.executeUpdate("DELETE FROM buckets");
        for (Entry<Character, Integer> e : symbolMap.entrySet()) {
          statement.executeUpdate(String.format("INSERT INTO buckets VALUES('%c', %d)", e.getKey(), e.getValue()));
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
    }
    System.err.printf("Took %dms to generate the buckets.\n", System.currentTimeMillis() - start);

    SuffixTree<ArticleInfo> st = new SuffixTree<ArticleInfo>();

    start = System.currentTimeMillis();

    JsonFactory jsonFactory = new JsonFactory();
    long parseTime = NewsEasyFormatUtil.parseAllFiles(FileSystems.getDefault().getPath(IN_DIR), st, jsonFactory, symbolMap); // creates suffix tree
    System.err.printf("Took %dms to load files\nTook %dms to create the tree.\n", System.currentTimeMillis() - start - parseTime, parseTime);

    start = System.currentTimeMillis();
    st.assignBitsets();
    System.err.printf("Took %dms total to annotate edges with bitsets.\n", System.currentTimeMillis() - start);

    if (SAVE_TO_DB) {
      start = System.currentTimeMillis();
      try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/KanjiWebEasy", "postgres", "ESMashS:VY65536")) {
        Statement statement = conn.createStatement();
        statement.executeUpdate("DELETE FROM nodes");
        statement.executeUpdate("DELETE FROM edges");
        saveToDb(conn, st);
        long clusterStart = System.currentTimeMillis();
        statement.execute("CLUSTER edges; CLUSTER nodes");
        System.err.printf("Took %dms to cluster\n", System.currentTimeMillis() - clusterStart);
      } catch (SQLException e) {
        e.printStackTrace();
      }
      System.err.printf("Took %dms to save tree to DB.\n", System.currentTimeMillis() - start);
    }

    try {
      long parseStart = System.currentTimeMillis();
      QueryParser parser = new QueryParser("「(.)*」(.)*言", symbolMap);
      FiniteAutomaton fa = parser.parse();
      //String sentence = "「海」消すように言い";
      //int[] suffixBuckets = SuffixTreeLauncher.getSuffixBuckets(sentence, symbolMap);
      //boolean inLang = fa.isInLanguage(sentence, suffixBuckets);
      //System.out.println(inLang);
      Set<ArticleInfo> result = st.FindMatching(fa);
      for (ArticleInfo article : result) {
        System.out.println(NewsEasyFormatUtil.getSentenceFromArticleInfo(FileSystems.getDefault().getPath(LOOKUP_DIR), article, jsonFactory));
      }
      System.err.println("Found " + result.size() + " hits in " + (System.currentTimeMillis() - parseStart) + "ms");
    } catch (InvalidQueryException e) {
      e.printStackTrace();
    }
    List<String> wildcardVariations = insertWildcardsInBetween("プロ野球の山本昌投手");
    start = System.currentTimeMillis();
    for (String s : wildcardVariations) {
      Set<ArticleInfo> found = st.findAll(s.split("\\*"), 40, symbolMap);
    }
    System.err.printf("Took %dms to find all wildcards of the string", System.currentTimeMillis() - start);

    for (String s : insertWildcardsInBetween("プロ野球の山本昌投")) {
      s += "*凧"; // these should never follow
      Set<ArticleInfo> found = st.findAll(s.split("\\*"), 40, symbolMap);
      if (found.size() != 0) {
        System.err.println("Failed on " + s);
      }
    }
  }

  public static void main(String[] args) throws IOException {
    long start;

    start = System.currentTimeMillis();

    Map<Character, Integer> symbolMap = new HashMap<>();
    if (RECREATE) {
      genAndSaveTree(symbolMap);
    } else {
      try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/KanjiWebEasy", "postgres", "ESMashS:VY65536")) {
        Statement s = conn.createStatement();
        ResultSet buckets = s.executeQuery("SELECT * FROM buckets");
        while (buckets.next()) {
          symbolMap.put(buckets.getString(1).charAt(0), buckets.getInt(2));
        }
      } catch (SQLException e1) {
        e1.printStackTrace();
      }
      System.err.printf("Took %dms to generate the buckets.\n", System.currentTimeMillis() - start);

      start = System.currentTimeMillis();
      try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/KanjiWebEasy", "postgres", "ESMashS:VY65536")) {
        QueryParser parser = new QueryParser("「(.)*」(.)*言", symbolMap);
        FiniteAutomaton fa = parser.parse();
        Set<ArticleInfo> result = SuffixTreeLauncher.FindMatching(fa, conn, symbolMap, 999999, 999999);
        JsonFactory jsonFactory = new JsonFactory();
        for (ArticleInfo article : result) {
          System.out.println(NewsEasyFormatUtil.getSentenceFromArticleInfo(FileSystems.getDefault().getPath(LOOKUP_DIR), article, jsonFactory));
        }
        System.err.printf("Found %d hits in %dms on DB.\n", result.size(), System.currentTimeMillis() - start);
      } catch (SQLException | InvalidQueryException e) {
        e.printStackTrace();
      }
    }

    //System.out.println(st);

    //		start = System.currentTimeMillis();
    //		for (int i = 0; i < 100; i++) {
    //			String[] testQueries = { "は、*が*です。", "の*こ", "を*える", "が*ある", "か*か", "から*まで", "から*ように", "こと*こと*こと", "の*あぶで" };
    //			for (String query : testQueries) {
    //				//System.out.println("----" + query + "----");
    //				for (ArticleInfo info : st.findAll(query.split("\\*"), 40, symbolMap)) {
    //					//System.out.println(getSentenceFromArticleInfo(info, jsonFactory));
    //				}
    //			}
    //		}
    //		System.err.printf("Took %dms to find the string", System.currentTimeMillis() - start);

    //		String query = "「*」を*ように言";
    //		for (ArticleInfo info : st.findAll(query.split("\\*"), 40, symbolMap)) {
    //			System.out.println(getSentenceFromArticleInfo(info, jsonFactory));
    //		}
  }

  public static <D> void saveToDb(Connection conn, SuffixTree<D> st) throws SQLException {
    long start = System.currentTimeMillis();
    Statement edgeBatch = conn.createStatement();
    Statement endingsBatch = conn.createStatement();
    saveToDb(conn, st.root, edgeBatch, endingsBatch);
    System.err.printf("Done creating statements after %dms.\n", System.currentTimeMillis() - start);
    start = System.currentTimeMillis();
    edgeBatch.executeBatch();
    System.err.printf("Done executing edge batch after %dms.\n", System.currentTimeMillis() - start);
    start = System.currentTimeMillis();
    endingsBatch.executeBatch();
    System.err.printf("Done executing endings batch after %dms.\n", System.currentTimeMillis() - start);
  }

  private static <D> void saveToDb(Connection conn, Node<D> n, Statement edgeBatch, Statement endingsBatch) throws SQLException {
    if (n.dataSet != null) {
      for (D d : n.dataSet) {
        String dataString = ((ArticleInfo) d).toDatabaseText();
        endingsBatch.addBatch(String.format("INSERT INTO nodes VALUES(%d, %s)", n.getId(), dataString));
      }
    }
    for (Entry<Character, Edge<D>> entry : n.getEdgeList().entrySet()) {
      Edge<D> edge = entry.getValue();
      edgeBatch.addBatch(String.format("INSERT INTO edges VALUES(%d, %d, '%s', %d)", n.getId(), edge.child.getId(), edge.getText(), edge.childBitset));
      saveToDb(conn, edge.child, edgeBatch, endingsBatch);
    }
  }

  /**
   * Returns all ways to insert wildcards in-between letters of s
   */
  private static List<String> insertWildcardsInBetween(String s) {
    List<String> stringList = new LinkedList<>();

    if (s.length() == 0) {
      stringList.add(s);
      return stringList;
    }

    for (String sub : insertWildcardsInBetween(s.substring(1))) {
      if (sub.length() != 0) {
        stringList.add(s.charAt(0) + "*" + sub);
      }
      stringList.add(s.charAt(0) + sub);
    }

    return stringList;
  }

  public static Set<ArticleInfo> FindMatching(FiniteAutomaton fa, Connection conn, Map<Character, Integer> symbolMap, long timeout, int sufficientResults) throws SQLException {
    long startTime = System.currentTimeMillis();
    Set<ArticleInfo> dataSet = new HashSet<ArticleInfo>();
    Set<SearchNode> seenSet = new HashSet<>();
    TreeSet<Integer> dataNodes = new TreeSet<Integer>();

    Statement statement = conn.createStatement();
    ResultSet rs = statement.executeQuery("SELECT to_node, branch, child_bitset FROM edges WHERE from_node=0");
    while (rs.next()) {
      if (System.currentTimeMillis() - startTime > timeout) {
        break;
      }
      if (dataNodes.size() >= sufficientResults) {
        break;
      }
      int toNode = rs.getInt(1);
      String text = rs.getString(2);
      int childBitset = rs.getInt(3);

      List<SearchNode> currDepth = new ArrayList<>();
      currDepth.add(new SearchNode(fa.startState, 0, new DatabaseEdge(text, toNode, childBitset, KanjiBucketer.getSuffixBuckets(text, symbolMap)), 0));
      SuffixTreeLauncher.FindMatching(currDepth, seenSet, fa.finalState, conn, symbolMap, dataNodes, startTime, timeout, sufficientResults);
    }

    if (dataNodes.size() > 0) {
      StringBuilder sb = new StringBuilder("0000000000,".length() * dataNodes.size());
      sb.append('(');
      sb.append(dataNodes.first());

      Iterator<Integer> iter = dataNodes.iterator();
      iter.next();
      while (iter.hasNext()) {
        sb.append("," + iter.next());
      }
      sb.append(')');

      Statement s = conn.createStatement();
      ResultSet dataResultSet = s.executeQuery("SELECT article, sentence_num FROM nodes WHERE node IN " + sb.toString());
      while (dataResultSet.next()) {
        ArticleInfo ai = new ArticleInfo(dataResultSet.getString(1), dataResultSet.getShort(2));
        dataSet.add(ai);
      }
    }

    return dataSet;
  }

  private static void FindMatching(List<SearchNode> currDepth, Set<SearchNode> seenSet, BaseNode finalState, Connection conn, Map<Character, Integer> symbolMap, Set<Integer> dataNodes, long startTime, long timeout, int sufficientResults) throws SQLException {
    if (currDepth.size() == 0) {
      return;
    }
    if (dataNodes.size() >= sufficientResults) {
      return;
    }

    for (SearchNode sn : currDepth) {
      if (sn.currNode == finalState) {
        DfsSearchNode(sn.edge.child, dataNodes, conn);
      }
    }

    List<SearchNode> nextDepth = new ArrayList<>();

    for (SearchNode sn : currDepth) {
      if (System.currentTimeMillis() - startTime > timeout) {
        return;
      }
      nextDepth.addAll(getNextAutomatonNodes(sn, seenSet, conn, symbolMap));
    }

    SuffixTreeLauncher.FindMatching(nextDepth, seenSet, finalState, conn, symbolMap, dataNodes, startTime, timeout, sufficientResults);
  }

  private static List<SearchNode> getNextAutomatonNodes(SearchNode sn, Set<SearchNode> seenSet, Connection conn, Map<Character, Integer> symbolMap) throws SQLException {
    List<SearchNode> nextNodes = new ArrayList<>();

    for (AutomatonEdge epsilonAutomatonEdge : sn.currNode.getEpsilonEdges()) {
      SuffixTreeLauncher.addEdgeToSearch(epsilonAutomatonEdge, sn.edge, sn.edgeIndex, false, sn.parseCount, nextNodes, seenSet);
    }

    if (sn.edgeIndex == sn.edge.text.length()) {
      Statement statement = conn.createStatement();
      ResultSet rs = statement.executeQuery(String.format("SELECT to_node, branch, child_bitset FROM edges WHERE from_node=%d", sn.edge.child));
      while (rs.next()) {
        int toNode = rs.getInt(1);
        String text = rs.getString(2);
        int childBitset = rs.getInt(3);

        DatabaseEdge de = new DatabaseEdge(text, toNode, childBitset, KanjiBucketer.getSuffixBuckets(text, symbolMap));
        nextNodes.add(new SearchNode(sn.currNode, sn.parseCount, de, 0));
      }
    } else {
      // find matches for specific char
      AutomatonEdge charAutomatonEdge = sn.currNode.getAutomatonEdge(sn.edge.text.charAt(sn.edgeIndex));
      SuffixTreeLauncher.addEdgeToSearch(charAutomatonEdge, sn.edge, sn.edgeIndex, true, sn.parseCount + 1, nextNodes, seenSet);
      // find matches for general dot
      AutomatonEdge dotAutomatonEdge = sn.currNode.getAutomatonEdge(ReservedSymbols.DOT);

      // these may be equal if the current node is a NOT node that doesn't include the current char nor the dot in its exclusion list
      if (charAutomatonEdge != dotAutomatonEdge) {
        SuffixTreeLauncher.addEdgeToSearch(dotAutomatonEdge, sn.edge, sn.edgeIndex, true, sn.parseCount + 1, nextNodes, seenSet);
      }
    }

    return nextNodes;
  }

  private static void DfsSearchNode(int node, Set<Integer> dataNodes, Connection conn) throws SQLException {
    // add all data for this node    
    if (!dataNodes.add(node)) {
      return; // if we already contained this node, we already contain all its children.
    }
    // find all children node and recurse
    Statement statement = conn.createStatement();
    ResultSet rs = statement.executeQuery("SELECT * FROM get_reachable(" + node + ")");
    while (rs.next()) {
      dataNodes.add(rs.getInt(1));
    }
  }

  private static <D> void addEdgeToSearch(AutomatonEdge automatonEdge, DatabaseEdge edge, int currIndex, boolean incrementIndex, int newAutomatonIndex, List<SearchNode> nextNodes, Set<SearchNode> seenSet) {
    if (automatonEdge == null) {
      return;
    }
    // bitset not 0 implies we match at least all the bitsets required to get to the end of the automaton
    int partBitset = currIndex >= edge.text.length() ? edge.childBitset : edge.getPartBitset(currIndex);
    if (automatonEdge.bitset == 0 || (automatonEdge.bitset & partBitset) == automatonEdge.bitset) {
      SearchNode newState = new SearchNode(automatonEdge.child, newAutomatonIndex, edge, incrementIndex ? currIndex + 1 : currIndex);
      if (seenSet.add(newState)) {
        nextNodes.add(newState);
      }
    }
  }
}
