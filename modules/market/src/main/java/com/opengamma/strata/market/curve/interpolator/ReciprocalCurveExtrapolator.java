/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.interpolation.ProductPiecewisePolynomialInterpolator1D;

/**
 * Extrapolator implementation that uses a polynomial function.
 * <p>
 * Given a data set {x[i], y[i]}, extrapolate {x[i], x[i] * y[i]} by a linear function by
 * using polynomial coefficients obtained in {@link ProductPiecewisePolynomialInterpolator1D}.
 * <p>
 * Even if the interpolator is clamped at (0,0), this extrapolator does not ensure the
 * resulting extrapolation curve goes through the origin.
 * Thus a reference value is returned for Math.abs(value) < SMALL, where SMALL is defined in the super class.
 */
final class ReciprocalCurveExtrapolator
    extends AbstractProductPolynomialCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "Reciprocal";
  /**
   * The function.
   */
  private static final PiecewisePolynomialFunction1D FUNCTION = new LinearlFunction1D();
  /**
   * The extrapolator instance.
   */
  public static final CurveExtrapolator INSTANCE = new ReciprocalCurveExtrapolator();

  /**
   * Restricted constructor.
   */
  private ReciprocalCurveExtrapolator() {
    super(FUNCTION);
  }

  // resolve instance
  private Object readResolve() {
    return INSTANCE;
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String toString() {
    return NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Function to use.
   */
  static class LinearlFunction1D extends PiecewisePolynomialFunction1D {

    @Override
    public DoubleArray evaluate(PiecewisePolynomialResult pp, double xKey) {
      ArgChecker.notNull(pp, "pp");
      DoubleArray knots = pp.getKnots();
      int nKnots = knots.size();
      DoubleMatrix coefMatrix = pp.getCoefMatrix();
      int dim = pp.getDimensions();
      double[] res = new double[dim];
      int indicator = FunctionUtils.getLowerBoundIndex(knots, xKey);
      if (indicator == nKnots - 1) {
        indicator--; //there is 1 less interval than knots 
      }
      for (int j = 0; j < dim; ++j) {
        DoubleArray coefs = coefMatrix.row(dim * indicator + j);
        res[j] = getValue(coefs, xKey, knots.get(indicator));

        ArgChecker.isFalse(Double.isInfinite(res[j]), "Too large input");
        ArgChecker.isFalse(Double.isNaN(res[j]), "Too large input");
      }

      return DoubleArray.copyOf(res);
    }

    @Override
    public DoubleArray differentiate(PiecewisePolynomialResult pp, double xKey) {
      ArgChecker.notNull(pp, "pp");
      DoubleArray knots = pp.getKnots();
      int nKnots = pp.getNumberOfIntervals() + 1;
      int nCoefs = pp.getOrder();
      int dim = pp.getDimensions();
      double[] res = new double[dim];
      int indicator = FunctionUtils.getLowerBoundIndex(knots, xKey);
      if (indicator == nKnots - 1) {
        indicator--; //there is 1 less interval than knots 
      }
      DoubleMatrix coefMatrix = pp.getCoefMatrix();
      for (int j = 0; j < dim; ++j) {
        DoubleArray coefs = coefMatrix.row(dim * indicator + j);
        res[j] = coefs.get(nCoefs - 2);
      }
      return DoubleArray.copyOf(res);
    }

    @Override
    public DoubleArray differentiateTwice(PiecewisePolynomialResult pp, double xKey) {
      ArgChecker.notNull(pp, "pp");
      int nKnots = pp.getNumberOfIntervals() + 1;
      int dim = pp.getDimensions();
      double[] result = new double[dim * (nKnots - 1)];
      return DoubleArray.copyOf(result);
    }

    @Override
    protected double getValue(double[] coefs, double x, double leftknot) {
      int nCoefs = coefs.length;
      double res = coefs[nCoefs - 2] * (x - leftknot) + coefs[nCoefs - 1];
      return res;
    }
  }

}
