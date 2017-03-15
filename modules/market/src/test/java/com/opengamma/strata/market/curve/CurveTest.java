/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.LabelParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link Curve}.
 */
@Test
public class CurveTest {

  private static final CurveName CURVE_NAME = CurveName.of("Curve");
  private static final LabelParameterMetadata PARAM_META = LabelParameterMetadata.of("TestParam");

  public void test_withPerturbation() {
    Curve test = new TestingCurve(2d);
    assertEquals(test.withPerturbation((i, v, m) -> v + 1).getParameter(0), 3d);
  }

  public void test_createParameterSensitivity_unit() {
    Curve test = new TestingCurve(2d);
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getMarketDataName(), CURVE_NAME);
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterCount(), 1);
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterMetadata(), ImmutableList.of(PARAM_META));
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getSensitivity(), DoubleArray.of(2d));
  }

  public void test_createParameterSensitivity_currency() {
    Curve test = new TestingCurve(2d);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getMarketDataName(), CURVE_NAME);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterCount(), 1);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterMetadata(), ImmutableList.of(PARAM_META));
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getCurrency(), USD);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getSensitivity(), DoubleArray.of(2d));
  }

  //-------------------------------------------------------------------------
  static class TestingCurve implements Curve {

    final double value;

    TestingCurve(double value) {
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
      return PARAM_META;
    }

    @Override
    public CurveMetadata getMetadata() {
      return DefaultCurveMetadata.of(CURVE_NAME);
    }

    @Override
    public Curve withMetadata(CurveMetadata metadata) {
      return this;
    }

    @Override
    public Curve withParameter(int parameterIndex, double newValue) {
      return new TestingCurve(newValue);
    }

    @Override
    public double yValue(double x) {
      return value;
    }

    @Override
    public UnitParameterSensitivity yValueParameterSensitivity(double x) {
      return UnitParameterSensitivity.of(CURVE_NAME, DoubleArray.filled(1));
    }

    @Override
    public double firstDerivative(double x) {
      return 0;
    }
  }

}
