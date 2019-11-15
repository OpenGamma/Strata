/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.LabelParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link Curve}.
 */
public class CurveTest {

  private static final CurveName CURVE_NAME = CurveName.of("Curve");
  private static final LabelParameterMetadata PARAM_META = LabelParameterMetadata.of("TestParam");

  @Test
  public void test_withPerturbation() {
    Curve test = new TestingCurve(2d);
    assertThat(test.withPerturbation((i, v, m) -> v + 1).getParameter(0)).isEqualTo(3d);
  }

  @Test
  public void test_createParameterSensitivity_unit() {
    Curve test = new TestingCurve(2d);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getMarketDataName()).isEqualTo(CURVE_NAME);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterCount()).isEqualTo(1);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterMetadata()).containsExactly(PARAM_META);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getSensitivity()).isEqualTo(DoubleArray.of(2d));
  }

  @Test
  public void test_createParameterSensitivity_currency() {
    Curve test = new TestingCurve(2d);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getMarketDataName()).isEqualTo(CURVE_NAME);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterCount()).isEqualTo(1);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterMetadata()).containsExactly(PARAM_META);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getCurrency()).isEqualTo(USD);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getSensitivity()).isEqualTo(DoubleArray.of(2d));
  }

  //-------------------------------------------------------------------------
  static class TestingCurve implements Curve {

    private final double value;

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
