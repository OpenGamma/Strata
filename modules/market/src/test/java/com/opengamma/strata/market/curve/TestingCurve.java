/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

/**
 * Testing curve implementation.
 * <p>
 * Does not implement {@link NodalCurve}.
 */
public class TestingCurve implements Curve {

  private final CurveMetadata metadata;

  public TestingCurve() {
    this(DefaultCurveMetadata.of("Test"));
  }

  public TestingCurve(CurveMetadata metadata) {
    this.metadata = metadata;
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveMetadata getMetadata() {
    return metadata;
  }

  @Override
  public int getParameterCount() {
    throw new IllegalStateException();
  }

  @Override
  public double yValue(double x) {
    throw new IllegalStateException();
  }

  @Override
  public CurveUnitParameterSensitivity yValueParameterSensitivity(double x) {
    throw new IllegalStateException();
  }

  @Override
  public double firstDerivative(double x) {
    throw new IllegalStateException();
  }

}
