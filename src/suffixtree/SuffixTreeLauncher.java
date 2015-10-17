package suffixtree;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;

import kanjicounter.KanjiBucketer;
import queryparse.FiniteAutomaton;
import queryparse.InvalidQueryException;
import queryparse.QueryParser;
import util.NewsEasyFormatUtil;

public class SuffixTreeLauncher {

  private static final String IN_DIR = "in";

  public static void main(String[] args) throws IOException {
    long start;

    start = System.currentTimeMillis();
    Map<Character, Integer> symbolMap = KanjiBucketer.getBuckets();
    System.err.printf("Took %dms to generate the buckets.\n", System.currentTimeMillis() - start);

    SuffixTree<ArticleInfo> st = new SuffixTree<ArticleInfo>();

    start = System.currentTimeMillis();
    JsonFactory jsonFactory = new JsonFactory();

    long parseTime = NewsEasyFormatUtil.parseAllFiles(FileSystems.getDefault().getPath(IN_DIR), st, jsonFactory, symbolMap);
    System.err.printf("Took %dms to load files\nTook %dms to create the tree.\n", System.currentTimeMillis() - start - parseTime, parseTime);

    start = System.currentTimeMillis();
    st.assignBitsets();
    System.err.printf("Took %dms total to annotate edges with bitsets.\n", System.currentTimeMillis() - start);

    System.exit(0);
    start = System.currentTimeMillis();
    try(Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/KanjiWebEasy", "postgres", "ESMashS:VY65536")){
      Statement statement = conn.createStatement();
      statement.executeUpdate("DELETE FROM \"Edge\"");
      st.saveToDb(conn);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    System.err.printf("Took %dms to save tree to DB.\n", System.currentTimeMillis() - start);
    System.exit(0);
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

    try {
      long parseStart = System.currentTimeMillis();
      //QueryParser parser = new QueryParser("「[暑](.)*」(.)*(く|す)ように言[わ]", symbolMap);
      QueryParser parser = new QueryParser("や(.)+や(.)+など", symbolMap);
      FiniteAutomaton fa = parser.parse();
      //String sentence = "「海」消すように言い";
      //int[] suffixBuckets = SuffixTreeLauncher.getSuffixBuckets(sentence, symbolMap);
      //boolean inLang = fa.isInLanguage(sentence, suffixBuckets);
      //System.out.println(inLang);
      Set<ArticleInfo> result = st.FindMatching(fa);
      for (ArticleInfo article : result) {
        System.out.println(NewsEasyFormatUtil.getSentenceFromArticleInfo(FileSystems.getDefault().getPath(IN_DIR), article, jsonFactory));
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
}
