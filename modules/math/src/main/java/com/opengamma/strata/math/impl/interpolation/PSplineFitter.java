/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.math.impl.statistics.leastsquare.GeneralizedLeastSquare;
import com.opengamma.strata.math.impl.statistics.leastsquare.GeneralizedLeastSquareResults;

/**
 * P-Spline fitter.
 */
@SuppressWarnings("deprecation")
public class PSplineFitter {

  private final BasisFunctionGenerator _generator = new BasisFunctionGenerator();
  private final GeneralizedLeastSquare _gls = new GeneralizedLeastSquare();

  /**
   * Fits a curve to x-y data.
   * @param x The independent variables 
   * @param y The dependent variables 
   * @param sigma The error (or tolerance) on the y variables 
   * @param xa The lowest value of x 
   * @param xb The highest value of x 
   * @param nKnots Number of knots (note, the actual number of basis splines and thus fitted weights, equals nKnots + degree-1)
   * @param degree The degree of the basis function - 0 is piecewise constant, 1 is a sawtooth function (i.e. two straight lines joined in the middle), 2 gives three 
   *   quadratic sections joined together, etc. For a large value of degree, the basis function tends to a gaussian 
   * @param lambda The weight given to the penalty function 
   * @param differenceOrder applies the penalty the nth order difference in the weights, so a differenceOrder of 2 will penalise large 2nd derivatives etc
   * @return The results of the fit
   */
  public GeneralizedLeastSquareResults<Double> solve(List<Double> x, List<Double> y, List<Double> sigma, double xa, double xb, int nKnots, int degree, double lambda, int differenceOrder) {
    List<Function<Double, Double>> bSplines = _generator.generateSet(xa, xb, nKnots, degree);
    return _gls.solve(x, y, sigma, bSplines, lambda, differenceOrder);
  }

  /**
   * Given a set of data {x_i ,y_i} where each x_i is a vector and the y_i are scalars, we wish to find a function (represented
   * by B-splines) that fits the data while maintaining smoothness in each direction.
   * @param x The independent (vector) variables, as List&lt;double[]>
   * @param y The dependent variables, as List&lt;Double> y
   * @param sigma The error (or tolerance) on the y variables 
   * @param xa  The lowest value of x in each dimension 
   * @param xb The highest value of x in each dimension 
   * @param nKnots Number of knots in each dimension (note, the actual number of basis splines and thus fitted weights,
   *   equals nKnots + degree-1)
   * @param degree The degree of the basis function in each dimension - 0 is piecewise constant, 1 is a sawtooth function
   *   (i.e. two straight lines joined in the middle), 2 gives three quadratic sections joined together, etc. For a large
   *   value of degree, the basis function tends to a gaussian 
   * @param lambda The weight given to the penalty function in each dimension 
   * @param differenceOrder applies the penalty the nth order difference in the weights, so a differenceOrder of 2
   *   will penalize large 2nd derivatives etc. A difference differenceOrder can be used in each dimension 
   * @return The results of the fit
   */
  public GeneralizedLeastSquareResults<double[]> solve(List<double[]> x, List<Double> y, List<Double> sigma, double[] xa, double[] xb, int[] nKnots, int[] degree, double[] lambda,
      int[] differenceOrder) {
    List<Function<double[], Double>> bSplines = _generator.generateSet(xa, xb, nKnots, degree);

    final int dim = xa.length;
    int[] sizes = new int[dim];
    for (int i = 0; i < dim; i++) {
      sizes[i] = nKnots[i] + degree[i] - 1;
    }
    return _gls.solve(x, y, sigma, bSplines, sizes, lambda, differenceOrder);
  }

}
