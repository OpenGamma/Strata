/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import com.google.common.base.Preconditions;

/**
 * Testing implementation.
 */
public class TestingParameterizedData2 implements ParameterizedData {

  private final double value1;
  private final double value2;

  public TestingParameterizedData2(double value1, double value2) {
    this.value1 = value1;
    this.value2 = value2;
  }

  @Override
  public int getParameterCount() {
    return 2;
  }

  @Override
  public double getParameter(int parameterIndex) {
    Preconditions.checkElementIndex(parameterIndex, 2);
    return parameterIndex == 0 ? value1 : value2;
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    Preconditions.checkElementIndex(parameterIndex, 2);
    return ParameterMetadata.empty();
  }

  @Override
  public ParameterizedData withParameter(int parameterIndex, double newValue) {
    if (parameterIndex == 0) {
      return new TestingParameterizedData2(newValue, value2);
    }
    return new TestingParameterizedData2(value1, newValue);
  }

}
