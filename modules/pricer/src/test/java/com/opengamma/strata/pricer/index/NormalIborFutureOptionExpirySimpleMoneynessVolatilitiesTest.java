/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.model.MoneynessType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;

/**
 * Tests {@link NormalIborFutureOptionExpirySimpleMoneynessVolatilities}
 */
@Test
public class NormalIborFutureOptionExpirySimpleMoneynessVolatilitiesTest {

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.25, 0.25, 0.25, 0.5, 0.5, 0.5, 0.5, 1, 1, 1, 1);
  private static final DoubleArray MONEYNESS_PRICES =
      DoubleArray.of(-0.02, -0.01, 0, 0.01, -0.02, -0.01, 0, 0.01, -0.02, -0.01, 0, 0.01);
  private static final DoubleArray NORMAL_VOL_PRICES =
      DoubleArray.of(0.01, 0.011, 0.012, 0.010, 0.011, 0.012, 0.013, 0.012, 0.012, 0.013, 0.014, 0.014);
  private static final DoubleArray MONEYNESS_RATES =
      DoubleArray.of(-0.01, 0, 0.01, 0.02, -0.01, 0, 0.01, 0.02, -0.01, 0, 0.01, 0.02);
  private static final DoubleArray NORMAL_VOL_RATES =
      DoubleArray.of(0.010, 0.012, 0.011, 0.01, 0.012, 0.013, 0.012, 0.011, 0.014, 0.014, 0.013, 0.012);
  private static final InterpolatedNodalSurface PARAMETERS_PRICE = InterpolatedNodalSurface.of(
      Surfaces.normalVolatilityByExpirySimpleMoneyness("Price", ACT_365F, MoneynessType.PRICE),
      TIMES,
      MONEYNESS_PRICES,
      NORMAL_VOL_PRICES,
      INTERPOLATOR_2D);
  private static final InterpolatedNodalSurface PARAMETERS_RATE = InterpolatedNodalSurface.of(
      Surfaces.normalVolatilityByExpirySimpleMoneyness("Rate", ACT_365F, MoneynessType.RATES),
      TIMES,
      MONEYNESS_RATES,
      NORMAL_VOL_RATES,
      INTERPOLATOR_2D);

  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);

  private static final NormalIborFutureOptionExpirySimpleMoneynessVolatilities VOL_SIMPLE_MONEY_PRICE =
      NormalIborFutureOptionExpirySimpleMoneynessVolatilities.of(EUR_EURIBOR_3M, VAL_DATE_TIME, PARAMETERS_PRICE);

  private static final NormalIborFutureOptionExpirySimpleMoneynessVolatilities VOL_SIMPLE_MONEY_RATE =
      NormalIborFutureOptionExpirySimpleMoneynessVolatilities.of(EUR_EURIBOR_3M, VAL_DATE_TIME, PARAMETERS_RATE);

  private static final ZonedDateTime[] TEST_EXPIRY = new ZonedDateTime[] {
      dateUtc(2015, 2, 17), dateUtc(2015, 5, 17), dateUtc(2015, 6, 17), dateUtc(2017, 2, 17)};
  private static final int NB_TEST = TEST_EXPIRY.length;
  private static final LocalDate[] TEST_FIXING =
      new LocalDate[] {date(2015, 2, 17), date(2015, 5, 17), date(2015, 5, 17), date(2015, 5, 17)};
  private static final double[] TEST_STRIKE_PRICE = {0.985, 0.985, 0.985, 0.985};
  private static final double[] TEST_FUTURE_PRICE = {0.98, 0.985, 1.00, 1.01};

  private static final double TOLERANCE_VOL = 1.0E-10;
  private static final double TOLERANCE_DELTA = 1.0E-2;

  //-------------------------------------------------------------------------
  public void test_basics() {
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getValuationDate(), VAL_DATE_TIME.toLocalDate());
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getValuationDateTime(), VAL_DATE_TIME);
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getIndex(), EUR_EURIBOR_3M);
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getName(), IborFutureOptionVolatilitiesName.of("Price"));
  }

  public void test_volatility_price() {
    for (int i = 0; i < NB_TEST; i++) {
      double timeToExpiry = VOL_SIMPLE_MONEY_RATE.relativeTime(TEST_EXPIRY[i]);
      double volExpected = PARAMETERS_PRICE.zValue(timeToExpiry, TEST_STRIKE_PRICE[i] - TEST_FUTURE_PRICE[i]);
      double volComputed = VOL_SIMPLE_MONEY_PRICE.volatility(
          TEST_EXPIRY[i], TEST_FIXING[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void test_volatility_rate() {
    for (int i = 0; i < NB_TEST; i++) {
      double timeToExpiry = VOL_SIMPLE_MONEY_RATE.relativeTime(TEST_EXPIRY[i]);
      double volExpected = PARAMETERS_RATE.zValue(timeToExpiry, TEST_FUTURE_PRICE[i] - TEST_STRIKE_PRICE[i]);
      double volComputed = VOL_SIMPLE_MONEY_RATE.volatility(
          TEST_EXPIRY[i], TEST_FIXING[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  //-------------------------------------------------------------------------
  public void test_parameterSensitivity() {
    double expiry = ACT_365F.relativeYearFraction(VAL_DATE, LocalDate.of(2015, 8, 14));
    LocalDate fixing = LocalDate.of(2016, 9, 14);
    double strikePrice = 1.0025;
    double futurePrice = 0.9975;
    double sensitivity = 123456;
    IborFutureOptionSensitivity point = IborFutureOptionSensitivity.of(
        VOL_SIMPLE_MONEY_RATE.getName(), expiry, fixing, strikePrice, futurePrice, EUR, sensitivity);
    CurrencyParameterSensitivities ps = VOL_SIMPLE_MONEY_RATE.parameterSensitivity(point);
    double shift = 1.0E-6;
    double v0 = VOL_SIMPLE_MONEY_RATE.volatility(expiry, fixing, strikePrice, futurePrice);
    for (int i = 0; i < NORMAL_VOL_RATES.size(); i++) {
      DoubleArray v = NORMAL_VOL_RATES.with(i, NORMAL_VOL_RATES.get(i) + shift);
      InterpolatedNodalSurface param = InterpolatedNodalSurface.of(
          Surfaces.normalVolatilityByExpirySimpleMoneyness("Rate", ACT_365F, MoneynessType.RATES),
          TIMES,
          MONEYNESS_RATES,
          v,
          INTERPOLATOR_2D);
      NormalIborFutureOptionExpirySimpleMoneynessVolatilities vol =
          NormalIborFutureOptionExpirySimpleMoneynessVolatilities.of(EUR_EURIBOR_3M, VAL_DATE_TIME, param);
      double vP = vol.volatility(expiry, fixing, strikePrice, futurePrice);
      double s = ps.getSensitivity(PARAMETERS_RATE.getName(), EUR).getSensitivity().get(i);
      assertEquals(s, (vP - v0) / shift * sensitivity, TOLERANCE_DELTA);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    NormalIborFutureOptionExpirySimpleMoneynessVolatilities test =
        NormalIborFutureOptionExpirySimpleMoneynessVolatilities.of(EUR_EURIBOR_3M, VAL_DATE_TIME, PARAMETERS_RATE);
    coverImmutableBean(test);
    NormalIborFutureOptionExpirySimpleMoneynessVolatilities test2 =
        NormalIborFutureOptionExpirySimpleMoneynessVolatilities.of(
            EUR_EURIBOR_6M, VAL_DATE_TIME.plusDays(1), PARAMETERS_PRICE);
    coverBeanEquals(test, test2);
  }

}
