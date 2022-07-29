package edu.pku.code2graph.mining;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashSet;
import java.util.Set;

public class Confidence {
  static public double add(double a, double b) {
    double sum = a + b;
    double product = a * b;
    if (product <= 0) {
      return sum / (1 - Math.min(Math.abs(a), Math.abs(b)));
    } else {
      return sum - product * Math.signum(sum);
    }
  }

  static public Pair<String, String> splitLast(String input, char delimiter) {
    int index = input.lastIndexOf(delimiter);
    if (index == -1) return new ImmutablePair<>(input, "");
    return new ImmutablePair<>(input.substring(0, index), input.substring(index + 1));
  }

  static public boolean intersects(String s1, String s2) {
    Set<String> words1 = slices(s1);
    Set<String> words2 = slices(s2);
    for (String word1 : words1) {
      if (words2.contains(word1)) return true;
    }
    return false;
  }

  static public Set<String> slices(String input) {
    Set<String> result = new HashSet<>();
    for (String word : input.split("[^0-9a-zA-Z]|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])|(?=[0-9]([a-z]|[A-Z]{2}))")) {
      if (word.length() > 1) result.add(word.toLowerCase());
    }
    return result;
  }

  static public int lcs(String s1, String s2) {
    int[] dp = new int[s2.length() + 1];
    int max = 0;
    for (int i = 1; i <= s1.length(); i++) {
      char c1 = s1.charAt(i - 1);
      for (int j = s2.length(); j > 0; j--) {
        char c2 = s2.charAt(j - 1);
        if (c1 == c2) {
          dp[j] = dp[j - 1] + 1;
          max = Math.max(max, dp[j]);
        } else {
          dp[j] = 0;
        }
      }
    }
    return max;
  }
}
