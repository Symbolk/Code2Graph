package edu.pku.code2graph.mining;

import java.util.ArrayList;
import java.util.List;

public class Comparison {
  public final double similarity;

  public Comparison(String s1, String s2) {
    Slice slice1 = new Slice(s1);
    Slice slice2 = new Slice(s2);
    int[] dp = new int[slice1.words.size() + 1];
    int max = 0, ii = 0, jj = 0;
    for (int i = 1; i <= slice1.words.size(); i++) {
      String c1 = slice1.words.get(i - 1);
      for (int j = slice2.words.size(); j > 0; j--) {
        String c2 = slice2.words.get(j - 1);
        if (c1.equals(c2)) {
          dp[j] = dp[j - 1] + c1.length();
          if (dp[j] >= max) {
            max = dp[j];
            ii = i;
            jj = j;
          }
        } else {
          dp[j] = 0;
        }
      }
    }
    similarity = 2. * max / (slice1.length + slice2.length);
  }

  static public class Slice {
    public final List<String> words = new ArrayList<>();
    public final int length;

    public Slice(String input) {
      int length = 0;
      for (String word : input.split("[^0-9a-zA-Z]|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])|(?=[0-9]([a-z]|[A-Z]{2}))")) {
        if (word.length() > 0) {
          words.add(word.toLowerCase());
          length += word.length();
        }
      }
      this.length = length;
    }
  }
}
