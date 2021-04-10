import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.PriorityQueue;

import static org.assertj.core.api.Assertions.assertThat;

public class UtilTest {
  static class PairComparator implements Comparator<Pair<String, Double>> {
    @Override
    public int compare(Pair<String, Double> p1, Pair<String, Double> p2) {
      if (p1.getRight() < p2.getRight()) {
        return 1;
      } else if (p1.getRight() > p2.getRight()) {
        return -1;
      }
      return 0;
    }
  }

  @Test
  public void testPriorityQueue() {
    PriorityQueue<Pair<String, Double>> pq = new PriorityQueue<>(new PairComparator());
    pq.add(Pair.of("1", 0.7));
    pq.add(Pair.of("4", 0.9));
    pq.add(Pair.of("4", 0.5));
    pq.add(Pair.of("4", 0.6));

    int k = 3;
    while (!pq.isEmpty() && k > 0) {
      System.out.println(pq.poll());
      k--;
    }
    assertThat(pq.size()).isEqualTo(1);

    pq.add(Pair.of("1", 1.0));
    k = 1;
    while (!pq.isEmpty() && k > 0) {
      System.out.println(pq.poll());
      k--;
    }
    assertThat(pq.size()).isEqualTo(1);
  }
}
