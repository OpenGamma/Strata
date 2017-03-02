/*
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
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;

/**
 * Test {@link BlackFxOptionFlatVolatilities}.
 */
@Test
public class BlackFxOptionFlatVolatilitiesTest {

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DoubleArray TIMES = DoubleArray.of(0.5, 1.0, 3.0);
  private static final DoubleArray VOL_ARRAY = DoubleArray.of(0.05, 0.09, 0.16);
  private static final CurveMetadata METADATA = DefaultCurveMetadata.builder()
      .curveName("Test")
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.BLACK_VOLATILITY)
      .dayCount(ACT_365F)
      .build();
  private static final InterpolatedNodalCurve CURVE = InterpolatedNodalCurve.of(METADATA, TIMES, VOL_ARRAY, INTERPOLATOR);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, GBP);
  private static final BlackFxOptionFlatVolatilities VOLS =
      BlackFxOptionFlatVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, CURVE);

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
    BlackFxOptionFlatVolatilities test = BlackFxOptionFlatVolatilities.builder()
        .currencyPair(CURRENCY_PAIR)
        .curve(CURVE)
        .valuationDateTime(VAL_DATE_TIME)
        .build();
    assertEquals(test.getValuationDateTime(), VAL_DATE_TIME);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getName(), FxOptionVolatilitiesName.of(CURVE.getName().getName()));
    assertEquals(test.getCurve(), CURVE);
    assertEquals(VOLS, test);
  }

  //-------------------------------------------------------------------------
  public void test_volatility() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = CURVE.yValue(expiryTime);
        double volComputed = VOLS.volatility(CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  public void test_volatility_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = CURVE.yValue(expiryTime);
        double volComputed = VOLS
            .volatility(CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void test_parameterSensitivity() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double timeToExpiry = VOLS.relativeTime(TEST_EXPIRY[i]);
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            VOLS.getName(), CURRENCY_PAIR, timeToExpiry, TEST_STRIKE[j], FORWARD[i], GBP, 1d);
        CurrencyParameterSensitivities computed = VOLS.parameterSensitivity(sensi);
        for (int k = 0; k < TIMES.size(); k++) {
          double value = computed.getSensitivities().get(0).getSensitivity().get(k);
          double nodeExpiry = TIMES.get(k);
          double expected = nodeSensitivity(
              VOLS, CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], nodeExpiry);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  public void test_parameterSensitivity_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double timeToExpiry = VOLS.relativeTime(TEST_EXPIRY[i]);
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            VOLS.getName(), CURRENCY_PAIR.inverse(), timeToExpiry, 1d / TEST_STRIKE[j], 1d / FORWARD[i], GBP, 1d);
        CurrencyParameterSensitivities computed = VOLS.parameterSensitivity(sensi);
        for (int k = 0; k < TIMES.size(); k++) {
          double value = computed.getSensitivities().get(0).getSensitivity().get(k);
          double nodeExpiry = TIMES.get(k);
          double expected = nodeSensitivity(
              VOLS, CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], nodeExpiry);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackFxOptionFlatVolatilities test1 = BlackFxOptionFlatVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, CURVE);
    coverImmutableBean(test1);
    BlackFxOptionFlatVolatilities test2 = BlackFxOptionFlatVolatilities.of(
        CURRENCY_PAIR.inverse(), ZonedDateTime.of(2015, 12, 21, 11, 15, 0, 0, ZoneId.of("Z")), CURVE);
    coverBeanEquals(test1, test2);
  }

  //-------------------------------------------------------------------------
  // bumping a node point at nodeExpiry
  private double nodeSensitivity(
      BlackFxOptionFlatVolatilities provider,
      CurrencyPair pair,
      ZonedDateTime expiry,
      double strike,
      double forward,
      double nodeExpiry) {

    NodalCurve curve = (NodalCurve) provider.getCurve();
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
    BlackFxOptionFlatVolatilities provUp = BlackFxOptionFlatVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, curveUp);
    BlackFxOptionFlatVolatilities provDw = BlackFxOptionFlatVolatilities.of(CURRENCY_PAIR, VAL_DATE_TIME, curveDw);
    double volUp = provUp.volatility(pair, expiry, strike, forward);
    double volDw = provDw.volatility(pair, expiry, strike, forward);
    return 0.5 * (volUp - volDw) / EPS;
  }

}
