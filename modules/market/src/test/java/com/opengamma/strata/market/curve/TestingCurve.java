/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import com.opengamma.strata.market.param.ParameterMetadata;

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
  public Curve withMetadata(CurveMetadata metadata) {
    throw new IllegalStateException();
  }

  //-------------------------------------------------------------------------
  @Override
  public int getParameterCount() {
    return 0;
  }

  @Override
  public double getParameter(int parameterIndex) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    throw new IndexOutOfBoundsException();
  }

  @Override
  public TestingCurve withParameter(int parameterIndex, double newValue) {
    throw new IndexOutOfBoundsException();
  }

  //-------------------------------------------------------------------------
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
