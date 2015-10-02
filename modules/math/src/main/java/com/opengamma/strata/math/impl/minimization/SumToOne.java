/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * For a set of N-1 "fit" parameters, produces N "model" parameters that sum to one
 */
public class SumToOne {
  private static final double TOL = 1e-9;
  private static final Map<Integer, int[][]> SETS = new HashMap<>();

  private final int[][] _set;
  private final int _n;

  /**
   *For a set of N-1 "fit" parameters, produces N "model" parameters that sum to one
   * @param n The number of "model" parameters, N
   */
  public SumToOne(final int n) {
    _set = getSet(n);
    _n = n;
  }

  /**
   * Transform from the N-1 "fit" parameters to the N "model" parameters
   * @param fitParms The N-1 "fit" parameters
   * @return The N "model" parameters
   */
  public double[] transform(final double[] fitParms) {
    ArgChecker.isTrue(fitParms.length == _n - 1, "length of fitParms is {}, but must be {} ", fitParms.length, _n - 1);
    final double[] s2 = new double[_n - 1];
    final double[] c2 = new double[_n - 1];
    for (int j = 0; j < _n - 1; j++) {
      double temp = Math.sin(fitParms[j]);
      temp *= temp;
      s2[j] = temp;
      c2[j] = 1.0 - temp;
    }

    final double[] res = new double[_n];
    for (int i = 0; i < _n; i++) {
      double prod = 1.0;
      for (int j = 0; j < _n - 1; j++) {
        if (_set[i][j] == 1) {
          prod *= s2[j];
        } else if (_set[i][j] == -1) {
          prod *= c2[j];
        }
      }
      res[i] = prod;
    }
    return res;
  }

  /**
   * Transform from the N-1 "fit" parameters to the N "model" parameters
   * @param fitParms The N-1 "fit" parameters
   * @return The N "model" parameters
   */
  public DoubleMatrix1D transform(final DoubleMatrix1D fitParms) {
    return new DoubleMatrix1D(transform(fitParms.getData()));
  }

  /**
   * Inverse transform from the N "model" parameters to the N-1 "fit" parameters. Used mainly to find the start position of a optimisation routine
   * @param modelParms The N "model" parameters. <b>These must sum to one</b>
   * @return The N-1 "fit" parameters
   */
  public double[] inverseTransform(final double[] modelParms) {
    ArgChecker.isTrue(modelParms.length == _n, "length of modelParms is {}, but must be {} ", modelParms.length, _n);

    final double[] res = new double[_n - 1];
    final double[] cum = new double[_n + 1];

    double sum = 0.0;
    for (int i = 0; i < _n; i++) {
      sum += modelParms[i];
      cum[i + 1] = sum;
    }
    ArgChecker.isTrue(Math.abs(sum - 1.0) < TOL, "sum of elements is {}. Must be 1.0", sum);

    cal(cum, 1.0, 0, _n, 0, res);

    for (int i = 0; i < _n - 1; i++) {
      res[i] = Math.asin(Math.sqrt(res[i]));
    }
    return res;
  }

  /**
   * Inverse transform from the N "model" parameters to the N-1 "fit" parameters. Used mainly to find the start position of a optimisation routine
   * @param modelParms The N "model" parameters. <b>These must sum to one</b>
   * @return The N-1 "fit" parameters
   */
  public DoubleMatrix1D inverseTransform(final DoubleMatrix1D modelParms) {
    return new DoubleMatrix1D(inverseTransform(modelParms.getData()));
  }

  /**
   * The N by N-1 Jacobian matrix between the N "model" parameters (that sum to one) and the N-1 "fit" parameters
   * @param fitParms  The N-1 "fit" parameters
   * @return The N by N-1 Jacobian matrix
   */
  public double[][] jacobian(final double[] fitParms) {
    ArgChecker.isTrue(fitParms.length == _n - 1, "length of fitParms is {}, but must be {} ", fitParms.length, _n - 1);
    final double[] sin = new double[_n - 1];
    final double[] cos = new double[_n - 1];
    for (int j = 0; j < _n - 1; j++) {
      sin[j] = Math.sin(fitParms[j]);
      cos[j] = Math.cos(fitParms[j]);
    }

    final double[] a = new double[_n];
    for (int i = 0; i < _n; i++) {
      double prod = 1.0;
      for (int j = 0; j < _n - 1; j++) {
        if (_set[i][j] == 1) {
          prod *= sin[j];
        } else if (_set[i][j] == -1) {
          prod *= cos[j];
        }
      }
      a[i] = 2 * prod * prod;
    }

    final double[][] res = new double[_n][_n - 1];
    for (int i = 0; i < _n; i++) {
      for (int j = 0; j < _n - 1; j++) {
        if (_set[i][j] == 1 && a[i] != 0.0) {
          res[i][j] = a[i] * cos[j] / sin[j];
        } else if (_set[i][j] == -1 && a[i] != 0.0) {
          res[i][j] = -a[i] * sin[j] / cos[j];
        }
      }
    }
    return res;
  }

  /**
   * The N by N-1 Jacobian matrix between the N "model" parameters (that sum to one) and the N-1 "fit" parameters
   * @param fitParms  The N-1 "fit" parameters
   * @return The N by N-1 Jacobian matrix
   */
  public DoubleMatrix2D jacobian(final DoubleMatrix1D fitParms) {
    return new DoubleMatrix2D(jacobian(fitParms.getData()));
  }

  private void cal(final double[] cum, final double factor, final int d, final int n, final int p1, final double[] res) {
    if (n == 1) {
      return;
    }
    final int n1 = n / 2;
    final int n2 = n - n1;
    final double s = (cum[p1 + n1] - cum[p1]) * factor;
    final double c = 1 - s;
    res[d] = s;
    cal(cum, factor / s, d + 1, n1, p1, res);
    cal(cum, factor / c, d + n1, n2, p1 + n1, res);
  }

  protected static int[][] getSet(final int n) {
    ArgChecker.isTrue(n > 1, "need n>1");
    if (SETS.containsKey(n)) {
      return SETS.get(n);
    }
    int[][] res = new int[n][];
    switch (n) {
      case 2:
        res[0] = new int[] {1 };
        res[1] = new int[] {-1 };
        break;
      case 3:
        res[0] = new int[] {1, 0 };
        res[1] = new int[] {-1, 1 };
        res[2] = new int[] {-1, -1 };
        break;
      case 4:
        res[0] = new int[] {1, 1, 0 };
        res[1] = new int[] {1, -1, 0 };
        res[2] = new int[] {-1, 0, 1 };
        res[3] = new int[] {-1, 0, -1 };
        break;
      default:
        final int n1 = n / 2;
        final int n2 = n - n1;
        final int[][] set1 = getSet(n1);
        final int[][] set2 = (n1 == n2 ? set1 : getSet(n2));
        res = new int[n][n - 1];

        for (int i = 0; i < n1; i++) {
          res[i][0] = 1;
          System.arraycopy(set1[i], 0, res[i], 1, n1 - 1);
        }
        for (int i = 0; i < n2; i++) {
          res[i + n1][0] = -1;
          System.arraycopy(set2[i], 0, res[i + n1], n1, n2 - 1);
        }
    }
    SETS.put(n, res);
    return res;
  }

}
