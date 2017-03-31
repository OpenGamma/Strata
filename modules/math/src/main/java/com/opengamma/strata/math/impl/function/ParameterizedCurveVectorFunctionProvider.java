/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * A provider of a {@link ParameterizedCurveVectorFunction}.
 */
public class ParameterizedCurveVectorFunctionProvider extends DoublesVectorFunctionProvider {

  private final ParameterizedCurve _pCurve;

  /**
   * Creates an instance backed by a {@link ParameterizedCurve}.
   * 
   * @param pCurve  the parameterised curve 
   */
  public ParameterizedCurveVectorFunctionProvider(ParameterizedCurve pCurve) {
    ArgChecker.notNull(pCurve, "pCurve");
    _pCurve = pCurve;
  }

  //-------------------------------------------------------------------------
  /**
   * Produces a {@link VectorFunction} which builds a {@link ParameterizedCurve} from the input vector
   * (treated as curve parameters), then samples the curve at the smaplePoints, to produce the output vector.
   * 
   * @param samplePoints the points where we sample the curve 
   * @return a {@link ParameterizedCurveVectorFunction}
   */
  @Override
  public VectorFunction from(double[] samplePoints) {
    return new ParameterizedCurveVectorFunction(samplePoints, _pCurve);
  }

}
