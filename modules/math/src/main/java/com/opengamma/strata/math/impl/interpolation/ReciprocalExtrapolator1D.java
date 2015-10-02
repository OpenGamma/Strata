/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.FunctionUtils;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Given a data set {x[i], y[i]}, extrapolate {x[i], x[i] * y[i]} by a linear function by
 * using polynomial coefficients obtained in {@link ProductPiecewisePolynomialInterpolator1D}.
 * 
 * Even if the interpolator is clamped at (0,0), this extrapolator does not ensure the
 * resulting extrapolation curve goes through the origin.
 * Thus a reference value is returned for Math.abs(value) < SMALL, where SMALL is defined in the super class.
 */
public class ReciprocalExtrapolator1D
    extends ProductPolynomialExtrapolator1D {

  /** The extrapolator name. */
  public static final String NAME = "Reciprocal";

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private static final PiecewisePolynomialFunction1D FUNC = new LinearlFunction1D();

  /**
   * Construct the extrapolator
   */
  public ReciprocalExtrapolator1D() {
    super(FUNC);
  }

  //-------------------------------------------------------------------------
  @Override
  public String getName() {
    return NAME;
  }

  private static class LinearlFunction1D extends PiecewisePolynomialFunction1D {

    @Override
    public DoubleMatrix1D evaluate(PiecewisePolynomialResult pp, double xKey) {
      ArgChecker.notNull(pp, "pp");
      double[] knots = pp.getKnots().getData();
      int nKnots = knots.length;
      DoubleMatrix2D coefMatrix = pp.getCoefMatrix();
      int dim = pp.getDimensions();
      double[] res = new double[dim];
      int indicator = FunctionUtils.getLowerBoundIndex(knots, xKey);
      if (indicator == nKnots - 1) {
        indicator--; //there is 1 less interval that knots 
      }
      for (int j = 0; j < dim; ++j) {
        double[] coefs = coefMatrix.getRowVector(dim * indicator + j).getData();
        res[j] = getValue(coefs, xKey, knots[indicator]);

        ArgChecker.isFalse(Double.isInfinite(res[j]), "Too large input");
        ArgChecker.isFalse(Double.isNaN(res[j]), "Too large input");
      }

      return new DoubleMatrix1D(res);
    }

    @Override
    public DoubleMatrix1D differentiate(PiecewisePolynomialResult pp, double xKey) {
      ArgChecker.notNull(pp, "pp");
      double[] knots = pp.getKnots().getData();
      int nKnots = pp.getNumberOfIntervals() + 1;
      int nCoefs = pp.getOrder();
      int dim = pp.getDimensions();
      double[] res = new double[dim];
      int indicator = FunctionUtils.getLowerBoundIndex(knots, xKey);
      if (indicator == nKnots - 1) {
        indicator--; //there is 1 less interval that knots 
      }
      DoubleMatrix2D coefMatrix = pp.getCoefMatrix();
      for (int j = 0; j < dim; ++j) {
        double[] coefs = coefMatrix.getRowVector(dim * indicator + j).getData();
        res[j] = coefs[nCoefs - 2];
      }
      return new DoubleMatrix1D(res);
    }

    @Override
    public DoubleMatrix1D differentiateTwice(PiecewisePolynomialResult pp, double xKey) {
      ArgChecker.notNull(pp, "pp");
      int nKnots = pp.getNumberOfIntervals() + 1;
      int dim = pp.getDimensions();
      double[] result = new double[dim * (nKnots - 1)];
      return new DoubleMatrix1D(result);
    }

    @Override
    protected double getValue(double[] coefs, double x, double leftknot) {
      int nCoefs = coefs.length;
      double res = coefs[nCoefs - 2] * (x - leftknot) + coefs[nCoefs - 1];
      return res;
    }
  }
}
