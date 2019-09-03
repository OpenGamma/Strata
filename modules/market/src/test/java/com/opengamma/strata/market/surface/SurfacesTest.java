/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.surface;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.model.MoneynessType;

/**
 * Test {@link Surfaces}.
 */
public class SurfacesTest {

  private static final String NAME = "Foo";
  private static final SurfaceName SURFACE_NAME = SurfaceName.of(NAME);

  //-------------------------------------------------------------------------
  @Test
  public void blackVolatilityByExpiryTenor_string() {
    SurfaceMetadata test = Surfaces.blackVolatilityByExpiryTenor(NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void blackVolatilityByExpiryTenor_surfaceName() {
    SurfaceMetadata test = Surfaces.blackVolatilityByExpiryTenor(SURFACE_NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void blackVolatilityByExpiryStrike_string() {
    SurfaceMetadata test = Surfaces.blackVolatilityByExpiryStrike(NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void blackVolatilityByExpiryStrike_surfaceName() {
    SurfaceMetadata test = Surfaces.blackVolatilityByExpiryStrike(SURFACE_NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void blackVolatilityByExpiryLogMoneyness_string() {
    SurfaceMetadata test = Surfaces.blackVolatilityByExpiryLogMoneyness(NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.LOG_MONEYNESS)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void blackVolatilityByExpiryLogMoneyness_surfaceName() {
    SurfaceMetadata test = Surfaces.blackVolatilityByExpiryLogMoneyness(SURFACE_NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.LOG_MONEYNESS)
        .zValueType(ValueType.BLACK_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void normalVolatilityByExpiryTenor_string() {
    SurfaceMetadata test = Surfaces.normalVolatilityByExpiryTenor(NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void normalVolatilityByExpiryTenor_surfaceName() {
    SurfaceMetadata test = Surfaces.normalVolatilityByExpiryTenor(SURFACE_NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void normalVolatilityByExpiryStrike_string() {
    SurfaceMetadata test = Surfaces.normalVolatilityByExpiryStrike(NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void normalVolatilityByExpiryStrike_surfaceName() {
    SurfaceMetadata test = Surfaces.normalVolatilityByExpiryStrike(SURFACE_NAME, ACT_360);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.STRIKE)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void normalVolatilityByExpirySimpleMoneyness_string() {
    SurfaceMetadata test = Surfaces.normalVolatilityByExpirySimpleMoneyness(NAME, ACT_360, MoneynessType.PRICE);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.SIMPLE_MONEYNESS)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .addInfo(SurfaceInfoType.MONEYNESS_TYPE, MoneynessType.PRICE)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void normalVolatilityByExpirySimpleMoneyness_surfaceName() {
    SurfaceMetadata test = Surfaces.normalVolatilityByExpirySimpleMoneyness(SURFACE_NAME, ACT_360, MoneynessType.PRICE);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.SIMPLE_MONEYNESS)
        .zValueType(ValueType.NORMAL_VOLATILITY)
        .dayCount(ACT_360)
        .addInfo(SurfaceInfoType.MONEYNESS_TYPE, MoneynessType.PRICE)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void sabrParameterByExpiryTenor_string() {
    SurfaceMetadata test = Surfaces.sabrParameterByExpiryTenor(NAME, ACT_360, ValueType.SABR_BETA);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.SABR_BETA)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void sabrParameterByExpiryTenor_surfaceName() {
    SurfaceMetadata test = Surfaces.sabrParameterByExpiryTenor(SURFACE_NAME, ACT_360, ValueType.SABR_BETA);
    SurfaceMetadata expected = DefaultSurfaceMetadata.builder()
        .surfaceName(SURFACE_NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.YEAR_FRACTION)
        .zValueType(ValueType.SABR_BETA)
        .dayCount(ACT_360)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(Surfaces.class);
  }

}
