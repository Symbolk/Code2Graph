package edu.pku.code2graph.mining;

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
}
