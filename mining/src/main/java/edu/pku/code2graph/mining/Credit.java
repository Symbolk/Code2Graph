package edu.pku.code2graph.mining;

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
