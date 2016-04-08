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

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * Test {@link BlackVolatilitySurfaceFxProvider}.
 */
@Test
public class BlackVolatilitySurfaceFxProviderTest {

  private static final Interpolator1D LINEAR_FLAT = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.LINEAR.getName(), CurveExtrapolators.FLAT.getName(), CurveExtrapolators.FLAT.getName());

  private static final GridInterpolator2D INTERPOLATOR_2D = new GridInterpolator2D(LINEAR_FLAT, LINEAR_FLAT);
  private static final DoubleArray TIMES = DoubleArray.of(0.25, 0.50, 1.00, 0.25, 0.50, 1.00, 0.25, 0.50, 1.00);
  private static final DoubleArray STRIKES = DoubleArray.of(0.7, 0.7, 0.7, 0.8, 0.8, 0.8, 0.9, 0.9, 0.9);
  private static final DoubleArray VOLS = DoubleArray.of(0.011, 0.012, 0.013, 0.012, 0.013, 0.014, 0.010, 0.012, 0.014);
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(DefaultSurfaceMetadata.of("Test"), TIMES, STRIKES, VOLS, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);

  private static final BlackVolatilitySurfaceFxProvider PROVIDER =
      BlackVolatilitySurfaceFxProvider.of(SURFACE, CURRENCY_PAIR, ACT_365F, VAL_DATE_TIME);

  private static final LocalTime TIME = LocalTime.of(11, 45);
  private static final ZonedDateTime[] TEST_EXPIRY = new ZonedDateTime[] {
    date(2015, 2, 17).atTime(LocalTime.MIDNIGHT).atZone(LONDON_ZONE),
    date(2015, 9, 17).atTime(TIME).atZone(LONDON_ZONE),
    date(2016, 6, 17).atTime(TIME).atZone(LONDON_ZONE),
    date(2018, 7, 17).atTime(TIME).atZone(LONDON_ZONE) };
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
        .valuationDateTime(VAL_DATE_TIME)
        .build();
    assertEquals(test.getValuationDateTime(), VAL_DATE_TIME);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getSurface(), SURFACE);
    assertEquals(PROVIDER, test);
  }

  //-------------------------------------------------------------------------
  public void test_volatility() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SURFACE.zValue(expiryTime, TEST_STRIKE[j]);
        double volComputed = PROVIDER.getVolatility(CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  public void test_volatility_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SURFACE.zValue(expiryTime, TEST_STRIKE[j]);
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
        SurfaceCurrencyParameterSensitivity computed = PROVIDER.surfaceParameterSensitivity(sensi);
        for (int k = 0; k < SURFACE.getParameterCount(); k++) {
          double value = computed.getSensitivity().get(k);
          double nodeExpiry = SURFACE.getXValues().get(k);
          double nodeStrike = SURFACE.getYValues().get(k);
          double expected = nodeSensitivity(
              PROVIDER, CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], nodeExpiry, nodeStrike);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  public void test_nodeSensitivity_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], GBP, 1d);
        SurfaceCurrencyParameterSensitivity computed = PROVIDER.surfaceParameterSensitivity(sensi);
        for (int k = 0; k < SURFACE.getParameterCount(); k++) {
          double value = computed.getSensitivity().get(k);
          double nodeExpiry = SURFACE.getXValues().get(k);
          double nodeStrike = SURFACE.getYValues().get(k);
          double expected = nodeSensitivity(PROVIDER, CURRENCY_PAIR.inverse(),
              TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], nodeExpiry, nodeStrike);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackVolatilitySurfaceFxProvider test1 =
        BlackVolatilitySurfaceFxProvider.of(SURFACE, CURRENCY_PAIR, ACT_365F, VAL_DATE_TIME);
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
  private double nodeSensitivity(
      BlackVolatilitySurfaceFxProvider provider,
      CurrencyPair pair,
      ZonedDateTime expiry,
      double strike,
      double forward,
      double nodeExpiry,
      double nodeStrike) {

    NodalSurface surface = provider.getSurface();
    DoubleArray xValues = surface.getXValues();
    DoubleArray yValues = surface.getYValues();
    DoubleArray zValues = surface.getZValues();
    int nData = xValues.size();
    int index = -1;
    for (int i = 0; i < nData; ++i) {
      if (Math.abs(xValues.get(i) - nodeExpiry) < TOLERANCE && Math.abs(yValues.get(i) - nodeStrike) < TOLERANCE) {
        index = i;
      }
    }
    NodalSurface surfaceUp = surface.withZValues(zValues.with(index, zValues.get(index) + EPS));
    NodalSurface surfaceDw = surface.withZValues(zValues.with(index, zValues.get(index) - EPS));
    BlackVolatilitySurfaceFxProvider provUp =
        BlackVolatilitySurfaceFxProvider.of(surfaceUp, CURRENCY_PAIR, ACT_365F, VAL_DATE_TIME);
    BlackVolatilitySurfaceFxProvider provDw =
        BlackVolatilitySurfaceFxProvider.of(surfaceDw, CURRENCY_PAIR, ACT_365F, VAL_DATE_TIME);
    double volUp = provUp.getVolatility(pair, expiry, strike, forward);
    double volDw = provDw.getVolatility(pair, expiry, strike, forward);
    return 0.5 * (volUp - volDw) / EPS;
  }

}
