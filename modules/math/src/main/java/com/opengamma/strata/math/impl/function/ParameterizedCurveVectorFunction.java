/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.Arrays;
import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * This is simply a {@link VectorFunction} backed by a {@link ParameterizedCurve}.
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
  public DoubleMatrix calculateJacobian(DoubleArray x) {
    Function<Double, DoubleArray> sense = _curve.getYParameterSensitivity(x);
    return DoubleMatrix.ofArrayObjects(
        getLengthOfRange(),
        getLengthOfDomain(),
        i -> sense.apply(_samplePoints[i]));
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
  public DoubleArray apply(DoubleArray curveParameters) {
    Function<Double, Double> func = _curve.asFunctionOfArguments(curveParameters);
    return DoubleArray.of(_samplePoints.length, i -> func.apply(_samplePoints[i]));
  }

}
