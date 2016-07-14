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
    Preconditions.checkElementIndex(parameterIndex, 1);
    return value;
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    Preconditions.checkElementIndex(parameterIndex, 1);
    return ParameterMetadata.empty();
  }

  @Override
  public ParameterizedData withParameter(int parameterIndex, double newValue) {
    return new TestingParameterizedData(newValue);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TestingParameterizedData) {
      TestingParameterizedData other = (TestingParameterizedData) obj;
      return Double.doubleToRawLongBits(value) == Double.doubleToRawLongBits(other.value);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Double.valueOf(value).hashCode();
  }

  @Override
  public String toString() {
    return Double.toString(value);
  }

}
