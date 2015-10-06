/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * This is simply a {@link VectorFunction} backed by a {@link ParameterizedCurve}
 */
public class ParameterizedCurveVectorFunction extends VectorFunction {

  private final double[] _samplePoints;
  private final ParameterizedCurve _curve;

  /**
   * Creates an instance with a sampled (parameterised) curve.
   * 
   * @param samplePoints  the points where we sample the curve 
   * @param curve  a parameterised curve 
   */
  public ParameterizedCurveVectorFunction(double[] samplePoints, ParameterizedCurve curve) {
    ArgChecker.notEmpty(samplePoints, "samplePoints");
    ArgChecker.notNull(curve, "curve");
    _samplePoints = Arrays.copyOf(samplePoints, samplePoints.length);
    _curve = curve;
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleMatrix2D calculateJacobian(DoubleMatrix1D x) {
    Function1D<Double, DoubleMatrix1D> sense = _curve.getYParameterSensitivity(x);
    int n = getLengthOfRange();
    DoubleMatrix2D jac = new DoubleMatrix2D(n, getLengthOfDomain());
    for (int i = 0; i < n; i++) {
      jac.getData()[i] = sense.evaluate(_samplePoints[i]).toArray();
    }
    return jac;
  }

  @Override
  public int getLengthOfDomain() {
    return _curve.getNumberOfParameters();
  }

  @Override
  public int getLengthOfRange() {
    return _samplePoints.length;
  }

  /**
   * Build a curve given the parameters, then return its value at the sample points.
   * 
   * @param curveParameters  the curve parameters 
   * @return the curve value at the sample points 
   */
  @Override
  public DoubleMatrix1D evaluate(DoubleMatrix1D curveParameters) {
    Function1D<Double, Double> func = _curve.asFunctionOfArguments(curveParameters);
    int n = _samplePoints.length;
    double[] y = new double[n];
    for (int i = 0; i < n; i++) {
      y[i] = func.evaluate(_samplePoints[i]);
    }
    return new DoubleMatrix1D(y);
  }

}
