package edu.pku.code2graph.mining;

import edu.pku.code2graph.model.Layer;
import edu.pku.code2graph.model.URI;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cochange {
  public final double similarity;
  public Candidate candidate;
  public Change change1;
  public Change change2;
  private Slice slice1;
  private Slice slice2;
  private int maxLength = 0;
  private int maxCount = 0;
  public int leftIndex = 0;
  public int rightIndex = 0;

  public Cochange(Change change1, Change change2) {
    this.change1 = change1;
    this.change2 = change2;
    this.slice1 = new Slice(change1.identifier);
    this.slice2 = new Slice(change2.identifier);
    int[] dpCount = new int[slice2.size() + 1];
    int[] dpLength = new int[slice2.size() + 1];
    for (int i = 1; i <= slice1.size(); i++) {
      String c1 = slice1.get(i - 1).text;
      for (int j = slice2.size(); j > 0; j--) {
        String c2 = slice2.get(j - 1).text;
        if (c1.equals(c2)) {
          dpCount[j] = dpCount[j - 1] + 1;
          dpLength[j] = dpLength[j - 1] + c1.length();
          if (dpLength[j] >= maxLength) {
            maxLength = dpLength[j];
            maxCount = dpCount[j];
            leftIndex = i - dpCount[j];
            rightIndex = j - dpCount[j];
          }
        } else {
          dpCount[j] = 0;
          dpLength[j] = 0;
        }
      }
    }
    similarity = 2. * maxLength / (slice1.totalLength + slice2.totalLength);
  }

  public void draft() {
    String pattern1 = pattern(change1.source, slice1.interpolate(leftIndex, maxCount, "(name)"));
    String pattern2 = pattern(change2.source, slice2.interpolate(rightIndex, maxCount, "(name)"));
    candidate = new Candidate(pattern1, pattern2);
  }

  private String pattern(String source, String identifier) {
    URI uri = new URI(source);
    for (int i = 0; i < uri.layers.size(); i++) {
      Layer layer = uri.layers.get(i);
      String value = i == uri.layers.size() - 1 ? identifier : "*";
      if (i == 0) {
        Pair<String, String> division = Credit.splitExtension(layer.get("identifier"));
        layer.put("identifier", value + "." + division.getRight());
      } else {
        layer.put("identifier", value);
      }
    }
    return uri.toString();
  }

  static public class Word {
    public final String text;
    public final int start;
    public final int end;

    public Word(String text, int start, int end) {
      this.text = text;
      this.start = start;
      this.end = end;
    }

    @Override
    public String toString() {
      return text + "(" + start + "," + end + ")";
    }
  }

  static public class Slice extends ArrayList<Word> {
    public int totalLength = 0;
    public final String source;
    private int index = 0;

    public Slice(String source) {
      this.source = source;
      Pattern pattern = Pattern.compile("[^0-9a-zA-Z]|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])|(?=[0-9]([a-z]|[A-Z]{2}))");
      Matcher matcher = pattern.matcher(source);
      while (matcher.find()) {
        addWord(matcher.start());
        index = matcher.end();
      }
      addWord(source.length());
    }

    private void addWord(int position) {
      if (position == index) return;
      add(new Word(source.substring(index, position).toLowerCase(), index, position));
      totalLength += position - index;
    }

    public String interpolate(int index, int count, String replacement) {
      return source.substring(0, get(index).start) + replacement + source.substring(get(index + count - 1).end);
    }
  }
}
