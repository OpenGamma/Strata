/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * Test {@link Surfaces}.
 */
@Test
public class SurfacesTest {

  private static final String NAME = "Foo";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);

  //-------------------------------------------------------------------------
  public void iborFutureOptionNormalExpirySimpleMoneyness_string() {
    SurfaceMetadata test = Surfaces.iborFutureOptionNormalExpirySimpleMoneyness(NAME, ACT_360, MoneynessType.PRICE);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.SIMPLE_MONEYNESS)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .addInfo(SurfaceInfoType.MONEYNESS_TYPE, MoneynessType.PRICE)
        .build();
    assertEquals(test, expected);
  }

  public void iborFutureOptionNormalExpirySimpleMoneyness_surfaceName() {
    SurfaceMetadata test = Surfaces.iborFutureOptionNormalExpirySimpleMoneyness(SURFACE_NAME, ACT_360, MoneynessType.PRICE);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.SIMPLE_MONEYNESS)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .addInfo(SurfaceInfoType.MONEYNESS_TYPE, MoneynessType.PRICE)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void swaptionBlackExpiryTenor_string() {
    SurfaceMetadata test = Surfaces.swaptionBlackExpiryTenor(NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  public void swaptionBlackExpiryTenor_surfaceName() {
    SurfaceMetadata test = Surfaces.swaptionBlackExpiryTenor(SURFACE_NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void swaptionNormalExpiryTenor_string() {
    SurfaceMetadata test = Surfaces.swaptionNormalExpiryTenor(NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  public void swaptionNormalExpiryTenor_surfaceName() {
    SurfaceMetadata test = Surfaces.swaptionNormalExpiryTenor(SURFACE_NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void swaptionSabrExpiryTenor_string() {
    SurfaceMetadata test = Surfaces.swaptionSabrExpiryTenor(NAME, ACT_360, ValueType.SABR_BETA);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.SABR_BETA)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  public void swaptionSabrExpiryTenor_surfaceName() {
    SurfaceMetadata test = Surfaces.swaptionSabrExpiryTenor(SURFACE_NAME, ACT_360, ValueType.SABR_BETA);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.SABR_BETA)
        .dayCount(ACT_360)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(Surfaces.class);
  }

}
