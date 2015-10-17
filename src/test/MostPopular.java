package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONObject;

import util.NewsEasyFormatUtil;

public class MostPopular {

	public static final String VIEWS_PATH = "news/views.json";
	private static final int MIN_VIEWS = 50_000;
	
	public static void main(String[] args){
		JSONObject views = NewsEasyFormatUtil.createJSONFromFile(VIEWS_PATH);
		List<Article> articles = new ArrayList<>();
		views.keySet().stream().forEach(k -> articles.add(new Article(k, views.getInt(k))));
		
		Collections.sort(articles);
		
		articles.stream().filter(a -> a.views >= MIN_VIEWS).forEach(System.out::println);
	}
}

class Article implements Comparable<Article>{
	
	public String name;
	public int views;
	
	public Article(String name, int views){
		this.name = name;
		this.views = views;
	}

	@Override
	public int compareTo(Article o) {
		return this.views < o.views ? 1 : -1;
	}
	
	@Override
	public String toString(){
		return this.name + " " + this.views;
	}
}
