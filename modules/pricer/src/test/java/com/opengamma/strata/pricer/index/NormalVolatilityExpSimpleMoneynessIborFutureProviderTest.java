/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.collect.TestHelper.dateUtc;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.IborFutureOptionSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * Tests {@link NormalVolatilityExpSimpleMoneynessIborFutureProvider}
 */
@Test
public class NormalVolatilityExpSimpleMoneynessIborFutureProviderTest {

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolator.of(CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray TIMES =
      DoubleArray.of(0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00);
  private static final DoubleArray MONEYNESS_PRICES =
      DoubleArray.of(-0.02, -0.02, -0.02, -0.01, -0.01, -0.01, 0.00, 0.00, 0.00, 0.01, 0.01, 0.01);
  private static final DoubleArray MONEYNESS_RATES =
      DoubleArray.of(0.02, 0.02, 0.02, 0.01, 0.01, 0.01, 0.00, 0.00, 0.00, -0.01, -0.01, -0.01);
  private static final DoubleArray NORMAL_VOL =
      DoubleArray.of(0.01, 0.011, 0.012, 0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014);
  private static final InterpolatedNodalSurface PARAMETERS_PRICE = InterpolatedNodalSurface.of(
      DefaultSurfaceMetadata.of("Price"), TIMES, MONEYNESS_PRICES, NORMAL_VOL, INTERPOLATOR_2D);
  private static final InterpolatedNodalSurface PARAMETERS_RATE = InterpolatedNodalSurface.of(
      DefaultSurfaceMetadata.of("Rate"), TIMES, MONEYNESS_RATES, NORMAL_VOL, INTERPOLATOR_2D);

  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(LONDON_ZONE);

  private static final NormalVolatilityExpSimpleMoneynessIborFutureProvider VOL_SIMPLE_MONEY_PRICE =
      NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
          PARAMETERS_PRICE, true, EUR_EURIBOR_3M, ACT_365F, VALUATION_DATE_TIME);

  private static final NormalVolatilityExpSimpleMoneynessIborFutureProvider VOL_SIMPLE_MONEY_RATE =
      NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
          PARAMETERS_RATE, false, EUR_EURIBOR_3M, ACT_365F, VALUATION_DATE_TIME);

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
  public void test_valuationDate() {
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getValuationDateTime(), VALUATION_DATE_TIME);
  }

  public void test_futureIndex() {
    assertEquals(VOL_SIMPLE_MONEY_PRICE.getFutureIndex(), EUR_EURIBOR_3M);
  }

  public void volatility_price() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOL_SIMPLE_MONEY_RATE.relativeTime(TEST_EXPIRY[i]);
      double volExpected = PARAMETERS_PRICE.zValue(expiryTime, TEST_STRIKE_PRICE[i] - TEST_FUTURE_PRICE[i]);
      double volComputed = VOL_SIMPLE_MONEY_PRICE.getVolatility(
          TEST_EXPIRY[i], TEST_FIXING[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  public void volatility_rate() {
    for (int i = 0; i < NB_TEST; i++) {
      double expiryTime = VOL_SIMPLE_MONEY_RATE.relativeTime(TEST_EXPIRY[i]);
      double volExpected = PARAMETERS_RATE.zValue(expiryTime, TEST_FUTURE_PRICE[i] - TEST_STRIKE_PRICE[i]);
      double volComputed = VOL_SIMPLE_MONEY_RATE.getVolatility(
          TEST_EXPIRY[i], TEST_FIXING[i], TEST_STRIKE_PRICE[i], TEST_FUTURE_PRICE[i]);
      assertEquals(volComputed, volExpected, TOLERANCE_VOL);
    }
  }

  //-------------------------------------------------------------------------
  public void node_sensitivity() {
    ZonedDateTime expiry = LocalDate.of(2015, 8, 14).atTime(11, 0).atZone(LONDON_ZONE);
    LocalDate fixing = LocalDate.of(2016, 9, 14);
    double strikePrice = 1.0025;
    double futurePrice = 0.9975;
    double sensitivity = 123456;
    IborFutureOptionSensitivity point =
        IborFutureOptionSensitivity.of(EUR_EURIBOR_3M, expiry, fixing, strikePrice, futurePrice, sensitivity);
    Map<DoublesPair, Double> ps = VOL_SIMPLE_MONEY_RATE.nodeSensitivity(point);
    double shift = 1.0E-6;
    double v0 = VOL_SIMPLE_MONEY_RATE.getVolatility(expiry, fixing, strikePrice, futurePrice);
    for (int i = 0; i < NORMAL_VOL.size(); i++) {
      DoubleArray v = NORMAL_VOL.with(i, NORMAL_VOL.get(i) + shift);
      InterpolatedNodalSurface param = InterpolatedNodalSurface.of(
          DefaultSurfaceMetadata.of("Rate"), TIMES, MONEYNESS_RATES, v, INTERPOLATOR_2D);
      NormalVolatilityExpSimpleMoneynessIborFutureProvider vol = NormalVolatilityExpSimpleMoneynessIborFutureProvider
          .of(param, false, EUR_EURIBOR_3M, ACT_365F, VALUATION_DATE_TIME);
      double vP = vol.getVolatility(expiry, fixing, strikePrice, futurePrice);
      double s = ps.get(DoublesPair.of(TIMES.get(i), MONEYNESS_RATES.get(i)));
      assertEquals(s, (vP - v0) / shift * sensitivity, TOLERANCE_DELTA);
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    NormalVolatilityExpSimpleMoneynessIborFutureProvider test =
        NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
            PARAMETERS_RATE, false, EUR_EURIBOR_3M, ACT_365F, VALUATION_DATE_TIME);
    coverImmutableBean(test);

    NormalVolatilityExpSimpleMoneynessIborFutureProvider test2 =
        NormalVolatilityExpSimpleMoneynessIborFutureProvider.of(
            PARAMETERS_RATE, true, EUR_EURIBOR_6M, ACT_360, VALUATION_DATE_TIME.plusDays(1));
    coverBeanEquals(test, test2);
  }

}
