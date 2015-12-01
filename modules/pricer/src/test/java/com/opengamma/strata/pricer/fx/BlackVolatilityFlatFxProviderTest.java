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
import java.util.Iterator;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.FxVolatilitySurfaceYearFractionNodeMetadata;

/**
 * Test {@link BlackVolatilityFlatFxProvider}.
 */
@Test
public class BlackVolatilityFlatFxProviderTest {

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DoubleArray TIMES = DoubleArray.of(0.5, 1.0, 3.0);
  private static final DoubleArray VOLS = DoubleArray.of(0.05, 0.09, 0.16);
  private static final CurveMetadata METADATA = DefaultCurveMetadata.of("TestCurve");
  private static final InterpolatedNodalCurve CURVE = InterpolatedNodalCurve.of(METADATA, TIMES, VOLS, INTERPOLATOR);
  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(LONDON_ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);
  private static final BlackVolatilityFlatFxProvider PROVIDER =
      BlackVolatilityFlatFxProvider.of(CURVE, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);

  private static final LocalTime TIME = LocalTime.of(11, 45);
  private static final ZonedDateTime[] TEST_EXPIRY = new ZonedDateTime[] {
    date(2015, 2, 17).atTime(LocalTime.MIDNIGHT).atZone(LONDON_ZONE),
    date(2015, 9, 17).atTime(TIME).atZone(LONDON_ZONE),
    date(2016, 6, 17).atTime(TIME).atZone(LONDON_ZONE),
    date(2018, 7, 17).atTime(TIME).atZone(LONDON_ZONE) };
  private static final double[] FORWARD = new double[] {0.85, 0.82, 0.75, 0.68 };
  private static final int NB_EXPIRY = TEST_EXPIRY.length;
  private static final double[] TEST_STRIKE = new double[] {0.67, 0.81, 0.92 };
  private static final int NB_STRIKE = TEST_STRIKE.length;

  private static final double TOLERANCE = 1.0E-12;
  private static final double EPS = 1.0E-7;

  //-------------------------------------------------------------------------
  public void test_builder() {
    BlackVolatilityFlatFxProvider test = BlackVolatilityFlatFxProvider.builder()
        .currencyPair(CURRENCY_PAIR)
        .dayCount(ACT_365F)
        .curve(CURVE)
        .valuationDateTime(VALUATION_DATE_TIME)
        .build();
    assertEquals(test.getValuationDateTime(), VALUATION_DATE_TIME);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getCurve(), CURVE);
    assertEquals(PROVIDER, test);
  }

  //-------------------------------------------------------------------------
  public void test_volatility() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = CURVE.yValue(expiryTime);
        double volComputed = PROVIDER.getVolatility(CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  public void test_volatility_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = CURVE.yValue(expiryTime);
        double volComputed = PROVIDER
            .getVolatility(CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void test_surfaceParameterSensitivity() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], GBP, 1d);
        SurfaceCurrencyParameterSensitivity computed = PROVIDER.surfaceParameterSensitivity(sensi);
        Iterator<SurfaceParameterMetadata> itr = computed.getMetadata().getParameterMetadata().get().iterator();
        for (double value : computed.getSensitivity().toArray()) {
          FxVolatilitySurfaceYearFractionNodeMetadata meta = ((FxVolatilitySurfaceYearFractionNodeMetadata) itr.next());
          double nodeExpiry = meta.getYearFraction();
          double expected = nodeSensitivity(
              PROVIDER, CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], nodeExpiry);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  public void test_surfaceParameterSensitivity_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], GBP, 1d);
        SurfaceCurrencyParameterSensitivity computed = PROVIDER.surfaceParameterSensitivity(sensi);
        Iterator<SurfaceParameterMetadata> itr = computed.getMetadata().getParameterMetadata().get().iterator();
        for (double value : computed.getSensitivity().toArray()) {
          double nodeExpiry = ((FxVolatilitySurfaceYearFractionNodeMetadata) itr.next()).getYearFraction();
          double expected = nodeSensitivity(
              PROVIDER, CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], nodeExpiry);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackVolatilityFlatFxProvider test1 =
        BlackVolatilityFlatFxProvider.of(CURVE, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    coverImmutableBean(test1);
    BlackVolatilityFlatFxProvider test2 = BlackVolatilityFlatFxProvider.of(
        CURVE,
        CURRENCY_PAIR.inverse(),
        ACT_360,
        ZonedDateTime.of(2015, 12, 21, 11, 15, 0, 0, ZoneId.of("Z")));
    coverBeanEquals(test1, test2);
  }

  //-------------------------------------------------------------------------
  // bumping a node point at nodeExpiry
  private double nodeSensitivity(
      BlackVolatilityFlatFxProvider provider,
      CurrencyPair pair,
      ZonedDateTime expiry,
      double strike,
      double forward,
      double nodeExpiry) {

    NodalCurve curve = provider.getCurve();
    DoubleArray xValues = curve.getXValues();
    DoubleArray yValues = curve.getYValues();

    int nData = xValues.size();
    int index = -1;
    for (int i = 0; i < nData; ++i) {
      if (Math.abs(xValues.get(i) - nodeExpiry) < TOLERANCE) {
        index = i;
      }
    }
    NodalCurve curveUp = curve.withYValues(yValues.with(index, yValues.get(index) + EPS));
    NodalCurve curveDw = curve.withYValues(yValues.with(index, yValues.get(index) - EPS));
    BlackVolatilityFlatFxProvider provUp =
        BlackVolatilityFlatFxProvider.of(curveUp, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    BlackVolatilityFlatFxProvider provDw =
        BlackVolatilityFlatFxProvider.of(curveDw, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    double volUp = provUp.getVolatility(pair, expiry, strike, forward);
    double volDw = provDw.getVolatility(pair, expiry, strike, forward);
    return 0.5 * (volUp - volDw) / EPS;
  }

}
