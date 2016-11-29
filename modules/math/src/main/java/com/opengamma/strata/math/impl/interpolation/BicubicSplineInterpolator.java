/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;

/**
 *  Given a set of data (x0Values_i, x1Values_j, yValues_{ij}), derive the piecewise bicubic function, f(x0,x1) = sum_{i=0}^{3} sum_{j=0}^{3} coefMat_{ij} (x0-x0Values_i)^{3-i} (x1-x1Values_j)^{3-j},
 *  for the region x0Values_i < x0 < x0Values_{i+1}, x1Values_j < x1 < x1Values_{j+1}  such that f(x0Values_a, x1Values_b) = yValues_{ab} where a={i,i+1}, b={j,j+1}. 
 *  1D piecewise polynomial interpolation methods are called to determine first derivatives and cross derivative at data points
 *  Note that the value of the cross derivative at {ij} is not "accurate" if yValues_{ij} = 0.
 */
public class BicubicSplineInterpolator extends PiecewisePolynomialInterpolator2D {

  private static final double ERROR = 1.e-13;

  private PiecewisePolynomialInterpolator[] _method;
  private static DoubleMatrix INV_MAT;

  static {
    double[][] invMat = new double[16][16];
    invMat[0] = new double[] {1., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
    invMat[1] = new double[] {0., 0., 0., 0., 1., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
    invMat[2] = new double[] {-3., 3., 0., 0., -2., -1., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
    invMat[3] = new double[] {2., -2., 0., 0., 1., 1., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0.};
    invMat[4] = new double[] {0., 0., 0., 0., 0., 0., 0., 0., 1., 0., 0., 0., 0., 0., 0., 0.};
    invMat[5] = new double[] {0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 0., 1., 0., 0., 0.};
    invMat[6] = new double[] {0., 0., 0., 0., 0., 0., 0., 0., -3., 3., 0., 0., -2., -1., 0., 0.};
    invMat[7] = new double[] {0., 0., 0., 0., 0., 0., 0., 0., 2., -2., 0., 0., 1., 1., 0., 0.};
    invMat[8] = new double[] {-3., 0., 3., 0., 0., 0., 0., 0., -2., 0., -1., 0., 0., 0., 0., 0.};
    invMat[9] = new double[] {0., 0., 0., 0., -3., 0., 3., 0., 0., 0., 0., 0., -2., 0., -1., 0.};
    invMat[10] = new double[] {9., -9., -9., 9., 6., 3., -6., -3., 6., -6., 3., -3., 4., 2., 2., 1.};
    invMat[11] = new double[] {-6., 6., 6., -6., -3., -3., 3., 3., -4., 4., -2., 2., -2., -2., -1., -1.};
    invMat[12] = new double[] {2., 0., -2., 0., 0., 0., 0., 0., 1., 0., 1., 0., 0., 0., 0., 0.};
    invMat[13] = new double[] {0., 0., 0., 0., 2., 0., -2., 0., 0., 0., 0., 0., 1., 0., 1., 0.};
    invMat[14] = new double[] {-6., 6., 6., -6., -4., -2., 4., 2., -3., 3., -3., 3., -2., -1., -2., -1.};
    invMat[15] = new double[] {4., -4., -4., 4., 2., 2., -2., -2., 2., -2., 2., -2., 1., 1., 1., 1.};
    INV_MAT = DoubleMatrix.ofUnsafe(invMat);
  }

  /**
   * Constructor which can take different methods for x0 and x1
   * @param method Choose 2 of {@link PiecewisePolynomialInterpolator}
   */
  public BicubicSplineInterpolator(final PiecewisePolynomialInterpolator[] method) {
    ArgChecker.notNull(method, "method");
    ArgChecker.isTrue(method.length == 2, "two methods should be chosen");

    _method = new PiecewisePolynomialInterpolator[2];
    for (int i = 0; i < 2; ++i) {
      _method[i] = method[i];
    }
  }

  /**
   * Constructor using the same interpolation method for x0 and x1
   * @param method {@link PiecewisePolynomialInterpolator}
   */
  public BicubicSplineInterpolator(final PiecewisePolynomialInterpolator method) {
    _method = new PiecewisePolynomialInterpolator[] {method, method };
  }

  @Override
  public PiecewisePolynomialResult2D interpolate(final double[] x0Values, final double[] x1Values, final double[][] yValues) {

    ArgChecker.notNull(x0Values, "x0Values");
    ArgChecker.notNull(x1Values, "x1Values");
    ArgChecker.notNull(yValues, "yValues");

    final int nData0 = x0Values.length;
    final int nData1 = x1Values.length;

    DoubleMatrix yValuesMatrix = DoubleMatrix.copyOf(yValues);
    final PiecewisePolynomialFunction1D func = new PiecewisePolynomialFunction1D();
    double[][] diff0 = new double[nData1][nData0];
    double[][] diff1 = new double[nData0][nData1];
    double[][] cross = new double[nData0][nData1];

    PiecewisePolynomialResult result0 = _method[0].interpolate(x0Values, OG_ALGEBRA.getTranspose(yValuesMatrix).toArray());
    diff0 = func.differentiate(result0, x0Values).toArray();

    PiecewisePolynomialResult result1 = _method[1].interpolate(x1Values, yValuesMatrix.toArray());
    diff1 = func.differentiate(result1, x1Values).toArray();

    final int order = 4;

    for (int i = 0; i < nData0; ++i) {
      for (int j = 0; j < nData1; ++j) {
        if (yValues[i][j] == 0.) {
          if (diff0[j][i] == 0.) {
            cross[i][j] = diff1[i][j];
          } else {
            if (diff1[i][j] == 0.) {
              cross[i][j] = diff0[j][i];
            } else {
              cross[i][j] = Math.signum(diff0[j][i] * diff1[i][j]) * Math.sqrt(Math.abs(diff0[j][i] * diff1[i][j]));
            }
          }
        } else {
          cross[i][j] = diff0[j][i] * diff1[i][j] / yValues[i][j];
        }
      }
    }

    DoubleMatrix[][] coefMat = new DoubleMatrix[nData0 - 1][nData1 - 1];
    for (int i = 0; i < nData0 - 1; ++i) {
      for (int j = 0; j < nData1 - 1; ++j) {
        double[] diffsVec = new double[16];
        for (int l = 0; l < 2; ++l) {
          for (int m = 0; m < 2; ++m) {
            diffsVec[l + 2 * m] = yValues[i + l][j + m];
          }
        }
        for (int l = 0; l < 2; ++l) {
          for (int m = 0; m < 2; ++m) {
            diffsVec[4 + l + 2 * m] = diff0[j + m][i + l];
          }
        }
        for (int l = 0; l < 2; ++l) {
          for (int m = 0; m < 2; ++m) {
            diffsVec[8 + l + 2 * m] = diff1[i + l][j + m];
          }
        }
        for (int l = 0; l < 2; ++l) {
          for (int m = 0; m < 2; ++m) {
            diffsVec[12 + l + 2 * m] = cross[i + l][j + m];
          }
        }
        final DoubleArray diffs = DoubleArray.copyOf(diffsVec);
        final DoubleArray ansVec = ((DoubleArray) OG_ALGEBRA.multiply(INV_MAT, diffs));

        double ref = 0.;
        double[][] coefMatTmp = new double[order][order];
        for (int l = 0; l < order; ++l) {
          for (int m = 0; m < order; ++m) {
            coefMatTmp[order - l - 1][order - m - 1] =
                ansVec.get(l + m * (order)) /
                    Math.pow((x0Values[i + 1] - x0Values[i]), l) /
                    Math.pow((x1Values[j + 1] - x1Values[j]), m);
            ArgChecker.isFalse(Double.isNaN(coefMatTmp[order - l - 1][order - m - 1]), "Too large/small input");
            ArgChecker.isFalse(Double.isInfinite(coefMatTmp[order - l - 1][order - m - 1]), "Too large/small input");
            ref += coefMatTmp[order - l - 1][order - m - 1] * Math.pow((x0Values[i + 1] - x0Values[i]), l) * Math.pow((x1Values[j + 1] - x1Values[j]), m);
          }
        }
        final double bound = Math.max(Math.abs(ref) + Math.abs(yValues[i + 1][j + 1]), 0.1);
        ArgChecker.isTrue(Math.abs(ref - yValues[i + 1][j + 1]) < ERROR * bound, "Input is too large/small or data points are too close");
        coefMat[i][j] = DoubleMatrix.copyOf(coefMatTmp);
      }
    }

    return new PiecewisePolynomialResult2D(
        DoubleArray.copyOf(x0Values),
        DoubleArray.copyOf(x1Values),
        coefMat,
        new int[] {order, order});
  }

}
