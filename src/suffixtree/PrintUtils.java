package suffixtree;

public class PrintUtils {
	
	public static final String	FONT_NAME	= "meiryo"; // this font must support japanese
																										
	public static String printHeader() {
		return "digraph {\nrankdir = LR;\nedge [arrowsize=0.4,fontsize=10,fontname=\"" + FONT_NAME + "\"]\ngraph [fontname=\"" + FONT_NAME + "\"];\nnode [fontname=\"" + FONT_NAME + "\"];\n";
	}
	
	public static String makeTitle(String title){
		return String.format("labelloc=\"t\";\nlabel=\"%s\";", title);
	}
	
	public static <D extends TreeData> String printAllNodes(Node<D> n) {
		String val = n.toDotFormat();
		for (Edge<D> e : n.getEdgeList().values()) {
			val += printAllNodes(e.child);
		}
		return val;
	}
	
	public static <D extends TreeData> String printAllEdges(Node<D> n) {
		String val = "";
		for (Character c : n.getEdgeList().keySet()) {
			Edge<D> e = n.getEdgeList().get(c);
			String edgeText = e.getText();
			if (edgeText.isEmpty()) {
				val += String.format("%s -> %s [label=\"%s\",style=dashed,color=gray,weight=3]\n", n.toString(), e.child.toString(), PrintUtils.formatText(SuffixTree.EMPTY_STRING));
			} else {
				val += String.format("%s -> %s [label=\"%s\",weight=3]\n", n.toString(), e.child.toString(), PrintUtils.formatText(edgeText));
			}
			val += printAllEdges(e.child);
		}
		return val;
	}
	
	public static <D extends TreeData> String printAllSuffixLinks(Node<D> n) {
		String val = "";
		if (n.suffixLink != null) {
			val = String.format("%s -> %s [label=\"\",weight=1,style=dotted,arrowhead=empty]\n", n.toString(), n.suffixLink.toString());
		}
		for (Edge<D> e : n.getEdgeList().values()) {
			val += printAllSuffixLinks(e.child);
		}
		return val;
	}
	
	public static String formatText(Character c){
		return formatText(c.toString());
	}
	
	public static String formatText(String text) {
		StringBuilder sb = new StringBuilder(8 * text.length());
		
		for (int i = 0; i < text.length(); i++) {
			sb.append(String.format("&#%s;", Integer.toString(text.charAt(i))));
		}
		return sb.toString();
	}
}
