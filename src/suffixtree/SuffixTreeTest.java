package suffixtree;

public class SuffixTreeTest {

  public static void main(String[] args){
    //    /*
    //     * Single element tests
    //     * abc
    //     * aba
    //     * aaa
    //     * abax
    //     * abab
    //     * ababb
    //     * abcabx
    //     * abcabxabz
    //     * abcabxabcd
    //     * cdddcdc
    //     * banana
    //     * mississippi
    //     */
    //    
    //    /*
    //     * multiple element tests
    //     * abc, def
    //     * abc, abc
    //     * cdddcdc, acacc
    //     * abab, aab
    //     * abab, baba
    //     * tagcacg, ttagacg, cacgtag
    //     * "。", "す。", "す。"
    //     * "ンンの", "プン", "プン"
    //     */
    
    SuffixTree<ArticleInfo> st = new SuffixTree<ArticleInfo>();
    String test1 = "たあがさあさが";
    String test2 = "たたあがあさが";
    String test3 = "さあさがたあが";
    st.addString(new SentenceInfo(test1, null), new ArticleInfo(null, (byte) 0));
    st.addString(new SentenceInfo(test2, null), new ArticleInfo(null, (byte) 0));
    st.addString(new SentenceInfo(test3, null), new ArticleInfo(null, (byte) 0));
    
    System.out.println(st);
  }
  
}
