/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.LabelParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link Surface}.
 */
public class SurfaceTest {

  private static final SurfaceName SURFACE_NAME = SurfaceName.of("Surface");
  private static final LabelParameterMetadata PARAM_META = LabelParameterMetadata.of("TestParam");

  @Test
  public void test_withPerturbation() {
    Surface test = new TestingSurface(2d);
    assertThat(test.withPerturbation((i, v, m) -> v + 1).getParameter(0)).isEqualTo(3d);
  }

  @Test
  public void test_createParameterSensitivity_unit() {
    Surface test = new TestingSurface(2d);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getMarketDataName()).isEqualTo(SURFACE_NAME);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterCount()).isEqualTo(1);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterMetadata()).containsExactly(PARAM_META);
    assertThat(test.createParameterSensitivity(DoubleArray.of(2d)).getSensitivity()).isEqualTo(DoubleArray.of(2d));
  }

  @Test
  public void test_createParameterSensitivity_currency() {
    Surface test = new TestingSurface(2d);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getMarketDataName()).isEqualTo(SURFACE_NAME);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterCount()).isEqualTo(1);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterMetadata()).containsExactly(PARAM_META);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getCurrency()).isEqualTo(USD);
    assertThat(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getSensitivity()).isEqualTo(DoubleArray.of(2d));
  }

  //-------------------------------------------------------------------------
  static class TestingSurface implements Surface {

    private final double value;

    TestingSurface(double value) {
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
    public SurfaceMetadata getMetadata() {
      return DefaultSurfaceMetadata.of(SURFACE_NAME);
    }

    @Override
    public Surface withMetadata(SurfaceMetadata metadata) {
      return this;
    }

    @Override
    public Surface withParameter(int parameterIndex, double newValue) {
      return new TestingSurface(newValue);
    }

    @Override
    public double zValue(double x, double y) {
      return value;
    }

    @Override
    public UnitParameterSensitivity zValueParameterSensitivity(double x, double y) {
      return createParameterSensitivity(DoubleArray.filled(1));
    }

    @Override
    public ValueDerivatives firstPartialDerivatives(double x, double y) {
      return null;
    }
  }

}
