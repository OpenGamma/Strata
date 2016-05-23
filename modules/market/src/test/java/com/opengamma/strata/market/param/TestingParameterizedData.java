/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

/**
 * Testing implementation.
 */
public class TestingParameterizedData implements ParameterizedData {

  private final double value;

  public TestingParameterizedData(double value) {
    this.value = value;
  }

  @Override
  public int getParameterCount() {
    return 1;
  }

  @Override
  public double getParameter(int parameterIndex) {
    return value;
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return ParameterMetadata.empty();
  }

  @Override
  public ParameterizedData withParameter(int parameterIndex, double newValue) {
    return new TestingParameterizedData(newValue);
  }

}
