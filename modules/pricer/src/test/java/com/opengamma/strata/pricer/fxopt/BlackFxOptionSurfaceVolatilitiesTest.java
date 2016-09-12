/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.DefaultSurfaceMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.interpolator.GridSurfaceInterpolator;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;

/**
 * Test {@link BlackFxOptionSurfaceVolatilities}.
 */
@Test
public class BlackFxOptionSurfaceVolatilitiesTest {

  private static final SurfaceInterpolator INTERPOLATOR_2D = GridSurfaceInterpolator.of(LINEAR, LINEAR);
  private static final DoubleArray TIMES = DoubleArray.of(0.25, 0.25, 0.25, 0.50, 0.50, 0.50, 1.00, 1.00, 1.00);
  private static final DoubleArray STRIKES = DoubleArray.of(0.7, 0.8, 0.9, 0.7, 0.8, 0.9, 0.7, 0.8, 0.9);
  private static final DoubleArray VOL_ARRAY = DoubleArray.of(0.011, 0.012, 0.010, 0.012, 0.013, 0.011, 0.013, 0.014, 0.014);
  private static final SurfaceMetadata METADATA = DefaultSurfaceMetadata.builder()
      .surfaceName("Test")
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.STRIKE)
      .zValueType(ValueType.BLACK_VOLATILITY)
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalSurface SURFACE =
      InterpolatedNodalSurface.of(METADATA, TIMES, STRIKES, VOL_ARRAY, INTERPOLATOR_2D);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);

  private static final BlackFxOptionSurfaceVolatilities VOLS =
      BlackFxOptionSurfaceVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, SURFACE);

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
    BlackFxOptionSurfaceVolatilities test = BlackFxOptionSurfaceVolatilities.builder()
        .currencyPair(CURRENCY_PAIR)
        .surface(SURFACE)
        .valuationDateTime(VAL_DATE_TIME)
        .build();
    assertEquals(test.getValuationDateTime(), VAL_DATE_TIME);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getSurface(), SURFACE);
    assertEquals(VOLS, test);
  }

  //-------------------------------------------------------------------------
  public void test_volatility() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SURFACE.zValue(expiryTime, TEST_STRIKE[j]);
        double volComputed = VOLS.volatility(CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  public void test_volatility_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SURFACE.zValue(expiryTime, TEST_STRIKE[j]);
        double volComputed = VOLS
            .volatility(CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void test_nodeSensitivity() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double timeToExpiry = VOLS.relativeTime(TEST_EXPIRY[i]);
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            VOLS.getName(), CURRENCY_PAIR, timeToExpiry, TEST_STRIKE[j], FORWARD[i], GBP, 1d);
        CurrencyParameterSensitivities computed = VOLS.parameterSensitivity(sensi);
        for (int k = 0; k < SURFACE.getParameterCount(); k++) {
          double value = computed.getSensitivities().get(0).getSensitivity().get(k);
          double nodeExpiry = SURFACE.getXValues().get(k);
          double nodeStrike = SURFACE.getYValues().get(k);
          double expected = nodeSensitivity(
              VOLS, CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], nodeExpiry, nodeStrike);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  public void test_nodeSensitivity_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double timeToExpiry = VOLS.relativeTime(TEST_EXPIRY[i]);
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            VOLS.getName(), CURRENCY_PAIR.inverse(), timeToExpiry, 1d / TEST_STRIKE[j], 1d / FORWARD[i], GBP, 1d);
        CurrencyParameterSensitivities computed = VOLS.parameterSensitivity(sensi);
        for (int k = 0; k < SURFACE.getParameterCount(); k++) {
          double value = computed.getSensitivities().get(0).getSensitivity().get(k);
          double nodeExpiry = SURFACE.getXValues().get(k);
          double nodeStrike = SURFACE.getYValues().get(k);
          double expected = nodeSensitivity(VOLS, CURRENCY_PAIR.inverse(),
              TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], nodeExpiry, nodeStrike);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackFxOptionSurfaceVolatilities test1 = BlackFxOptionSurfaceVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, SURFACE);
    coverImmutableBean(test1);
    BlackFxOptionSurfaceVolatilities test2 = BlackFxOptionSurfaceVolatilities.of(
        CURRENCY_PAIR.inverse(),
        ZonedDateTime.of(2015, 12, 21, 11, 15, 0, 0, ZoneId.of("Z")),
        SURFACE);
    coverBeanEquals(test1, test2);
  }

  //-------------------------------------------------------------------------
  // bumping a node point at (nodeExpiry, nodeStrike)
  private double nodeSensitivity(
      BlackFxOptionSurfaceVolatilities provider,
      CurrencyPair pair,
      ZonedDateTime expiry,
      double strike,
      double forward,
      double nodeExpiry,
      double nodeStrike) {

    NodalSurface surface = (NodalSurface) provider.getSurface();
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
    Surface surfaceUp = surface.withZValues(zValues.with(index, zValues.get(index) + EPS));
    Surface surfaceDw = surface.withZValues(zValues.with(index, zValues.get(index) - EPS));
    BlackFxOptionSurfaceVolatilities provUp = BlackFxOptionSurfaceVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, surfaceUp);
    BlackFxOptionSurfaceVolatilities provDw = BlackFxOptionSurfaceVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, surfaceDw);
    double volUp = provUp.volatility(pair, expiry, strike, forward);
    double volDw = provDw.volatility(pair, expiry, strike, forward);
    return 0.5 * (volUp - volDw) / EPS;
  }

}
