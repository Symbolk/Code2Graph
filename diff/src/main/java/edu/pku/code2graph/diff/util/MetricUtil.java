package edu.pku.code2graph.diff.util;

import edu.pku.code2graph.model.ElementNode;
import edu.pku.code2graph.model.Range;
import edu.pku.code2graph.model.RelationNode;
import info.debatty.java.stringsimilarity.Cosine;
import info.debatty.java.stringsimilarity.MetricLCS;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.apache.commons.text.similarity.JaccardSimilarity;

import java.util.*;

public class MetricUtil {

  public static double weightElement(ElementNode n1, ElementNode n2) {
    List<Double> metrics = new ArrayList<>();
    // internal
    metrics.add(prefixString(n1.getQualifiedName(), n2.getQualifiedName()));
    metrics.add(lcsString(n1.getSnippet(), n2.getSnippet()));
    metrics.add(distanceRange(n1.getRange(), n2.getRange()));

    // context
    //    similarity += cosineVector()

    return myAvg(metrics);
  }

  public static double weightRelation(RelationNode n1, RelationNode n2) {
    return 0D;
  }

  /**
   * Estimate how close two ranges are in the code text
   *
   * @param r1
   * @param r2
   * @return
   */
  private static double distanceRange(Range r1, Range r2) {
    if (r1 == null || r2 == null) {
      return 0D;
    }
    int a =
        Math.max(
            Math.abs(r1.getStartLine() - r2.getStartLine()),
            Math.abs(r1.getEndLine() - r2.getEndLine()));
    int b = myMax(r1.getStartLine(), r1.getEndLine(), r2.getStartLine(), r2.getEndLine());
    return formatDouble(1.0 - (double) a / b);
  }

  public static double lcsString(String s1, String s2) {
    MetricLCS lcs = new MetricLCS();
    return formatDouble(lcs.distance(s1, s2));
  }

  /**
   * Split qualified names with ., and compute similarity with common prefix, order matters
   *
   * @param s1
   * @param s2
   * @return
   */
  public static double prefixString(String s1, String s2) {
    String[] a1 = s1.split("\\.");
    String[] a2 = s2.split("\\.");

    int min = Math.min(a1.length, a2.length);
    int max = Math.max(a1.length, a2.length);
    int cnt = 0;

    for (int i = 0; i < min; ++i) {
      if (a1[i].equals(a2[i])) {
        cnt += 1;
      } else {
        break;
      }
    }
    return formatDouble((double) cnt / min);
  }

  public static double cosineString(String s1, String s2) {
    Cosine cosine = new Cosine();
    return formatDouble(cosine.similarity(s1, s2));
  }

  public static double jaccardString(String s1, String s2) {
    JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();
    return formatDouble(jaccardSimilarity.apply(s1, s2));
  }

  /**
   * Jaccard = Intersection/Union [0,1], order does not matter
   *
   * @param s1
   * @param s2
   * @return
   */
  public static double jaccardSet(Set s1, Set s2) {
    Set<String> union = new HashSet<>();
    union.addAll(s1);
    union.addAll(s2);
    Set<String> intersection = new HashSet<>(s1);
    intersection.retainAll(s2);
    if (union.size() <= 0) {
      return 0D;
    } else {
      return formatDouble((double) intersection.size() / union.size());
    }
  }

  public static int intersectSize(Set s1, Set s2) {
    Set<String> intersection = new HashSet<>(s1);
    intersection.retainAll(s2);
    return intersection.size();
  }

  /**
   * Compute the cosine similarity of two vectors
   *
   * @param v1
   * @param v2
   * @param length length of the complete set of feature labels
   * @return
   */
  private static double cosineVector(
      Map<Integer, Integer> v1, Map<Integer, Integer> v2, int length) {
    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;

    for (Integer index = 0; index < length; ++index) {
      Integer a = v1.get(index);
      Integer b = v2.get(index);
      dotProduct += a * b;
      norm1 += Math.pow(a, 2);
      norm2 += Math.pow(b, 2);
    }
    norm1 = (Math.sqrt(norm1));
    norm2 = (Math.sqrt(norm2));

    double product = norm1 * norm2;
    return formatDouble(product == 0.0 ? 0.0 : dotProduct / product);
  }

  private static double cosineVector(Map<CharSequence, Integer> v1, Map<CharSequence, Integer> v2) {
    CosineSimilarity cosineSimilarity = new CosineSimilarity();
    return formatDouble(cosineSimilarity.cosineSimilarity(v1, v2));
  }

  /**
   * Format the double value to leave 2 digits after .
   *
   * @param value
   * @return
   */
  public static double formatDouble(double value) {
    return Math.ceil(value * 100) / 100;
  }

  private static int myMax(Integer... vals) {
    return Collections.max(Arrays.asList(vals));
  }

  private static double myAvg(List<Double> numbers) {
    double sum = 0;
    for (int i = 0; i < numbers.size(); i++) {
      sum = sum + numbers.get(i);
    }
    return formatDouble(sum / numbers.size());
  }

  public static double getMean(List<Double> numList) {
    if (numList.isEmpty()) {
      return 0D;
    }
    if (numList.size() == 1) {
      return numList.get(0);
    }

    Double average = numList.stream().mapToDouble(val -> val).average().orElse(0.0);
    return formatDouble(average);
  }

  public static double getMedian(List<Double> numList) {
    if (numList.isEmpty()) {
      return 0D;
    }
    if (numList.size() == 1) {
      return numList.get(0);
    }

    Double[] numArray = numList.toArray(new Double[0]);
    Arrays.sort(numArray);
    int middle = ((numArray.length) / 2);
    if (numArray.length % 2 == 0) {
      double medianA = numArray[middle];
      double medianB = numArray[middle - 1];
      return formatDouble((medianA + medianB) / 2);
    } else {
      return numArray[middle + 1];
    }
  }
}
