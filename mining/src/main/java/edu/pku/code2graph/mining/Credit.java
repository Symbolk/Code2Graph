package edu.pku.code2graph.mining;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

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

  public static class Record {
    public final double value;
    public final String commit;
    public final String source1;
    public final String source2;
    public final double similarity;
    public final double density;

    public Record(String commit, String source1, String source2, double similarity, double density) {
      this.value = similarity / density;
      this.commit = commit;
      this.source1 = source1;
      this.source2 = source2;
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
