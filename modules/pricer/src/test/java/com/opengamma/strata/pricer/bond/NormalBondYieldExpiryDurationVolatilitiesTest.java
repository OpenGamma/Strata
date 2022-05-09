/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.surface.ConstantSurface;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceName;

/**
 * Test {@link NormalBondYieldExpiryDurationVolatilities}
 */
public class NormalBondYieldExpiryDurationVolatilitiesTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2022, 4, 22);
  private static final LocalTime VALUATION_TIME = LocalTime.of(14, 30);
  private static final ZoneId VALUATION_ZONE = ZoneId.of("Europe/Brussels");
  private static final SurfaceName SURFACE_NAME = SurfaceName.of("EUR Govt Vol");
  private static final SurfaceMetadata SURFACE_METADATA = DefaultSurfaceMetadata.builder()
      .surfaceName(SURFACE_NAME)
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.YEAR_FRACTION)
      .zValueType(ValueType.NORMAL_VOLATILITY)
      .dayCount(ACT_365F).build();
  private static final double YIELD_VOL = 0.0050;
  private static final Surface VOL_SURFACE = ConstantSurface.of(SURFACE_METADATA, YIELD_VOL);
  private static final ZonedDateTime VALUATION_DATE_TIME = VAL_DATE.atTime(VALUATION_TIME).atZone(VALUATION_ZONE);
  private static final BondYieldVolatilities VOLATILITIES_FLAT = NormalBondYieldExpiryDurationVolatilities
      .of(EUR, VALUATION_DATE_TIME, VOL_SURFACE);

  @Test
  public void test_of() {
    NormalBondYieldExpiryDurationVolatilities test = NormalBondYieldExpiryDurationVolatilities
        .of(EUR, VALUATION_DATE_TIME, VOL_SURFACE);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getValuationDateTime()).isEqualTo(VALUATION_DATE_TIME);
    assertThat(test.getSurface()).isEqualTo(VOL_SURFACE);
    assertThat(test.getVolatilityType()).isEqualTo(ValueType.NORMAL_VOLATILITY);
  }

  @Test
  public void relativeTime() {
    LocalDate expiryDate = LocalDate.of(2023, 4, 22);
    ZonedDateTime expiry = ZonedDateTime.of(expiryDate, VALUATION_TIME, VALUATION_ZONE);
    double expiryTimeComputed = VOLATILITIES_FLAT.relativeTime(expiry);
    double expiryTimeExpected = ACT_365F.relativeYearFraction(VAL_DATE, expiryDate);
    assertThat(expiryTimeComputed).isEqualTo(expiryTimeExpected);
  }

  @Test
  public void volatility() {
    double volatilityComputed = VOLATILITIES_FLAT.volatility(1.0, 2.0, 0.01, 0.015);
    double volatilityExpected = VOL_SURFACE.zValue(1.0, 2.0);
    assertThat(volatilityComputed).isEqualTo(volatilityExpected);
  }

  @Test
  public void parameterSensitivity() {
    double expiry = 1.0;
    double duration = 2.0;
    double strike = 0.01;
    double forward = 0.015;
    double sensitivity = 123456.7;
    BondYieldSensitivity point = BondYieldSensitivity.of(
        BondVolatilitiesName.of(SURFACE_NAME.getName()),
        expiry, duration, strike, forward, EUR, sensitivity);
    CurrencyParameterSensitivities psComputed = VOLATILITIES_FLAT.parameterSensitivity(point);
    UnitParameterSensitivity unitSensitivity = VOL_SURFACE.zValueParameterSensitivity(expiry, duration);
    CurrencyParameterSensitivities psExpected =
        CurrencyParameterSensitivities.of(unitSensitivity.multipliedBy(EUR, sensitivity));
    assertThat(psComputed.equalWithTolerance(psExpected, 1.0E-6)).isTrue();
  }

}
