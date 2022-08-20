package edu.pku.code2graph.mining;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Credit {
  public double value = 0.;
  public List<Record> history = new ArrayList<>();

  static public double add(double a, double b) {
    double sum = a + b;
    double product = a * b;
    if (product <= 0) {
      return sum / (1 - Math.min(Math.abs(a), Math.abs(b)));
    } else {
      return sum - product * Math.signum(sum);
    }
  }

  public void add(Record record) {
    this.value = add(this.value, record.value);
    this.history.add(record);
  }

  static public Pair<String, String> splitExtension(String input) {
    int index = input.lastIndexOf('.');
    if (index == -1) return new ImmutablePair<>(input, "");
    return new ImmutablePair<>(input.substring(0, index), input.substring(index + 1));
  }

  static public String getLastSegment(String input) {
    int index, last = input.length() - 1;
    do {
      index = input.lastIndexOf('/', last);
      if (index <= 0 || input.charAt(index - 1) != '\\') {
        break;
      } else {
        last = index - 1;
      }
    } while (true);
    return input.substring(index + 1);
  }

  static public Double similarity(String s1, String s2) {
    Pair<Set<String>, Integer> slices1 = slices(s1);
    Pair<Set<String>, Integer> slices2 = slices(s2);
    int length = 0;
    for (String word : slices1.getLeft()) {
      if (!slices2.getLeft().contains(word)) continue;
      length += word.length();
    }
    if (length == 0) return 0.;
    return 2. * length / (slices1.getRight() + slices2.getRight());
  }

  static public Pair<Set<String>, Integer> slices(String input) {
    Set<String> result = new HashSet<>();
    int length = 0;
    for (String word : input.split("[^0-9a-zA-Z]|(?<=[a-z])(?=[A-Z])|(?=[A-Z][a-z])|(?=[0-9]([a-z]|[A-Z]{2}))")) {
      if (word.length() > 1) {
        result.add(word.toLowerCase());
        length += word.length();
      }
    }
    return new ImmutablePair<>(result, length);
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

  public static class Record {
    public final double value;
    public final String commit;
    public final double similarity;
    public final double density;

    public Record(String commit, double similarity, double density) {
      this.value = similarity / density;
      this.commit = commit;
      this.similarity = similarity;
      this.density = density;
    }

    @Override
    public String toString() {
      return "Record{" +
          "value=" + value +
          ", commit=" + commit +
          ", similarity=" + similarity +
          ", density=" + density +
          '}';
    }
  }
}
