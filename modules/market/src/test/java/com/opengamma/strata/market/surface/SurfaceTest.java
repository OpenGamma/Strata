/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.LabelParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.UnitParameterSensitivity;

/**
 * Test {@link Surface}.
 */
@Test
public class SurfaceTest {

  private static final SurfaceName SURFACE_NAME = SurfaceName.of("Surface");
  private static final LabelParameterMetadata PARAM_META = LabelParameterMetadata.of("TestParam");

  public void test_withPerturbation() {
    Surface test = new TestingSurface(2d);
    assertEquals(test.withPerturbation((i, v, m) -> v + 1).getParameter(0), 3d);
  }

  public void test_createParameterSensitivity_unit() {
    Surface test = new TestingSurface(2d);
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getMarketDataName(), SURFACE_NAME);
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterCount(), 1);
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getParameterMetadata(), ImmutableList.of(PARAM_META));
    assertEquals(test.createParameterSensitivity(DoubleArray.of(2d)).getSensitivity(), DoubleArray.of(2d));
  }

  public void test_createParameterSensitivity_currency() {
    Surface test = new TestingSurface(2d);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getMarketDataName(), SURFACE_NAME);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterCount(), 1);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getParameterMetadata(), ImmutableList.of(PARAM_META));
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getCurrency(), USD);
    assertEquals(test.createParameterSensitivity(USD, DoubleArray.of(2d)).getSensitivity(), DoubleArray.of(2d));
  }

  //-------------------------------------------------------------------------
  static class TestingSurface implements Surface {

    final double value;

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
  }

}
