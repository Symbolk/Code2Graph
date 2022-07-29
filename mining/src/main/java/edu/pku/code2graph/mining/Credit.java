package edu.pku.code2graph.mining;

import java.util.ArrayList;
import java.util.List;

public class Credit {
  public double value = 0.;
  public List<Record> history = new ArrayList<>();

  public void add(double value) {
    this.value = Confidence.add(this.value, value);
  }

  public void add(Record record) {
    this.value = Confidence.add(this.value, record.value);
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
