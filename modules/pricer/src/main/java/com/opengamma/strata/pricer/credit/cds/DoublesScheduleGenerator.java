package com.opengamma.strata.pricer.credit.cds;

import java.util.Arrays;

import com.opengamma.strata.collect.array.DoubleArray;

public class DoublesScheduleGenerator {

  private static final double TOL = 1d / 730d;

  public static double[] getIntegrationsPoints(
      double start,
      double end,
      DoubleArray discountCurveNodes,
      DoubleArray creditCurveNodes) {

    double[] set1 = truncateSetExclusive(start, end, discountCurveNodes.toArray());
    double[] set2 = truncateSetExclusive(start, end, creditCurveNodes.toArray());
    int n1 = set1.length;
    int n2 = set2.length;
    int n = n1 + n2;
    double[] set = new double[n];
    System.arraycopy(set1, 0, set, 0, n1);
    System.arraycopy(set2, 0, set, n1, n2);
    Arrays.sort(set);

    double[] temp = new double[n + 2];
    temp[0] = start;
    int pos = 0;
    for (int i = 0; i < n; i++) {
      if (different(temp[pos], set[i])) {
        temp[++pos] = set[i];
      }
    }
    if (different(temp[pos], end)) {
      pos++;
    }
    temp[pos] = end; // add the end point (this may replace the last entry in temp if that is not significantly different)

    int resLength = pos + 1;
    if (resLength == n + 2) {
      return temp; // everything was unique
    }

    double[] res = new double[resLength];
    System.arraycopy(temp, 0, res, 0, resLength);
    return res;
  }

  private static boolean different(double a, double b) {
    return Math.abs(a - b) > TOL;
  }

  private static double[] truncateSetExclusive(double lower, double upper, double[] set) {
    // this is private, so assume inputs are fine

    int n = set.length;
    if (upper < set[0] || lower > set[n - 1]) {
      return new double[0];
    }

    int lIndex;
    if (lower < set[0]) {
      lIndex = 0;
    } else {
      int temp = Arrays.binarySearch(set, lower);
      lIndex = temp >= 0 ? temp + 1 : -(temp + 1);
    }

    int uIndex;
    if (upper > set[n - 1]) {
      uIndex = n;
    } else {
      int temp = Arrays.binarySearch(set, lIndex, n, upper);
      uIndex = temp >= 0 ? temp : -(temp + 1);
    }

    int m = uIndex - lIndex;
    if (m == n) {
      return set;
    }

    double[] trunc = new double[m];
    System.arraycopy(set, lIndex, trunc, 0, m);
    return trunc;
  }

  public static double[] truncateSetInclusive(final double lower, final double upper, final double[] set) {
    // this is private, so assume inputs are fine
    final double[] temp = truncateSetExclusive(lower, upper, set);
    final int n = temp.length;
    if (n == 0) {
      return new double[] {lower, upper};
    }
    final boolean addLower = different(lower, temp[0]);
    final boolean addUpper = different(upper, temp[n - 1]);
    if (!addLower && !addUpper) { // replace first and last entries of set
      temp[0] = lower;
      temp[n - 1] = upper;
      return temp;
    }

    final int m = n + (addLower ? 1 : 0) + (addUpper ? 1 : 0);
    final double[] res = new double[m];
    System.arraycopy(temp, 0, res, (addLower ? 1 : 0), n);
    res[0] = lower;
    res[m - 1] = upper;

    return res;
  }
}
