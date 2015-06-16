/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;

/**
 * Test {@link BlackVolatilitySurfaceFxProvider}.
 */
@Test
public class BlackVolatilitySurfaceFxProviderTest {

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR, Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final double[] TIMES = new double[] {0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00 };
  private static final double[] STRIKES = new double[] {0.7, 0.7, 0.7, 0.8, 0.8, 0.8, 0.9, 0.9, 0.9 };
  private static final double[] VOLS = new double[] {0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014 };
  private static final InterpolatedDoublesSurface SURFACE =
      new InterpolatedDoublesSurface(TIMES, STRIKES, VOLS, INTERPOLATOR_2D);
  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(LONDON_ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);

  private static final BlackVolatilitySurfaceFxProvider PROVIDER =
      BlackVolatilitySurfaceFxProvider.of(SURFACE, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);

  private static final LocalDate[] TEST_EXPIRY =
      new LocalDate[] {date(2015, 2, 17), date(2015, 5, 17), date(2015, 6, 17), date(2017, 2, 17) };
  private static final double[] FORWARD = new double[] {0.85, 0.82, 0.77, 0.76 };
  private static final int NB_EXPIRY = TEST_EXPIRY.length;
  private static final double[] TEST_STRIKE = new double[] {0.65, 0.73, 0.85, 0.92 };
  private static final int NB_STRIKE = TEST_STRIKE.length;

  private static final double TOLERANCE = 1.0E-12;
  private static final double EPS = 1.0E-7;

  //-------------------------------------------------------------------------
  public void test_builder() {
    BlackVolatilitySurfaceFxProvider test = BlackVolatilitySurfaceFxProvider.builder()
        .currencyPair(CURRENCY_PAIR)
        .dayCount(ACT_365F)
        .surface(SURFACE)
        .valuationDateTime(VALUATION_DATE_TIME)
        .build();
    assertEquals(test.getValuationDateTime(), VALUATION_DATE_TIME);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getSurface(), SURFACE);
    assertEquals(PROVIDER, test);
  }

  //-------------------------------------------------------------------------
  public void test_volatility() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i], LocalTime.MIDNIGHT, LONDON_ZONE);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SURFACE.getZValue(expiryTime, TEST_STRIKE[j]);
        double volComputed = PROVIDER.getVolatility(CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  public void test_volatility_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i], LocalTime.MIDNIGHT, LONDON_ZONE);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SURFACE.getZValue(expiryTime, TEST_STRIKE[j]);
        double volComputed = PROVIDER
            .getVolatility(CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void test_nodeSensitivity() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], GBP, 1d);
        Map<DoublesPair, Double> computed = PROVIDER.nodeSensitivity(sensi);
        for (DoublesPair key : computed.keySet()) {
          Double x = key.getFirst();
          Double y = key.getSecond();
          double expected = nodeSensitivity(PROVIDER, CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], x, y);
          assertEquals(computed.get(key), expected, EPS);
        }
      }
    }
  }

  public void test_nodeSensitivity_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], GBP, 1d);
        Map<DoublesPair, Double> computed = PROVIDER.nodeSensitivity(sensi);
        for (DoublesPair key : computed.keySet()) {
          Double x = key.getFirst();
          Double y = key.getSecond();
          double expected = nodeSensitivity(
              PROVIDER, CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], x, y);
          assertEquals(computed.get(key), expected, EPS);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackVolatilitySurfaceFxProvider test1 =
        BlackVolatilitySurfaceFxProvider.of(SURFACE, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    coverImmutableBean(test1);
    BlackVolatilitySurfaceFxProvider test2 = BlackVolatilitySurfaceFxProvider.of(
        SURFACE,
        CURRENCY_PAIR.inverse(),
        ACT_360,
        ZonedDateTime.of(2015, 12, 21, 11, 15, 0, 0, ZoneId.of("Z")));
    coverBeanEquals(test1, test2);
  }

  //-------------------------------------------------------------------------
  // bumping a node point at (nodeExpiry, nodeStrike)
  private double nodeSensitivity(BlackVolatilitySurfaceFxProvider provider, CurrencyPair pair, LocalDate expiry,
      double strike, double forward, double nodeExpiry, double nodeStrike) {
    InterpolatedDoublesSurface surface =  provider.getSurface();
    Double [] xData = surface.getXData().clone();
    Double [] yData = surface.getYData().clone();
    Double [] zDataUp = surface.getZData().clone();
    Double [] zDataDw = surface.getZData().clone();
    int nData = xData.length;
    int index = -1;
    for (int i = 0; i < nData; ++i) {
      if (Math.abs(xData[i] - nodeExpiry) < TOLERANCE && Math.abs(yData[i] - nodeStrike) < TOLERANCE) {
        index = i;
      }
    }
    zDataUp[index] += EPS;
    zDataDw[index] -= EPS;
    InterpolatedDoublesSurface surfaceUp = new InterpolatedDoublesSurface(xData, yData, zDataUp, INTERPOLATOR_2D);
    InterpolatedDoublesSurface surfaceDw = new InterpolatedDoublesSurface(xData, yData, zDataDw, INTERPOLATOR_2D);
    BlackVolatilitySurfaceFxProvider provUp =
        BlackVolatilitySurfaceFxProvider.of(surfaceUp, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    BlackVolatilitySurfaceFxProvider provDw =
        BlackVolatilitySurfaceFxProvider.of(surfaceDw, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    double volUp = provUp.getVolatility(pair, expiry, strike, forward);
    double volDw = provDw.getVolatility(pair, expiry, strike, forward);
    return 0.5 * (volUp - volDw) / EPS;
  }
}
