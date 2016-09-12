/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Extrapolator implementation that always throws an exception.
 * <p>
 * This is used to prevent extrapolation from being used.
 */
final class ExceptionCurveExtrapolator
    implements CurveExtrapolator, BoundCurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The extrapolator name.
   */
  public static final String NAME = "Exception";
  /**
   * The extrapolator instance.
   */
  public static final ExceptionCurveExtrapolator INSTANCE = new ExceptionCurveExtrapolator();

  /**
   * Restricted constructor.
   */
  private ExceptionCurveExtrapolator() {
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
  public BoundCurveExtrapolator bind(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public double leftExtrapolate(double xValue) {
    throw new UnsupportedOperationException("Extrapolation is not permitted");
  }

  @Override
  public double leftExtrapolateFirstDerivative(double xValue) {
    throw new UnsupportedOperationException("Extrapolation is not permitted");
  }

  @Override
  public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
    throw new UnsupportedOperationException("Extrapolation is not permitted");
  }

  //-------------------------------------------------------------------------
  @Override
  public double rightExtrapolate(double xValue) {
    throw new UnsupportedOperationException("Extrapolation is not permitted");
  }

  @Override
  public double rightExtrapolateFirstDerivative(double xValue) {
    throw new UnsupportedOperationException("Extrapolation is not permitted");
  }

  @Override
  public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
    throw new UnsupportedOperationException("Extrapolation is not permitted");
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return NAME;
  }

}
