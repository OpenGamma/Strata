/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
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
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.FxOptionSensitivity;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.FxVolatilitySurfaceYearFractionNodeMetadata;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;

/**
 * Test {@link BlackVolatilitySmileFxProvider}.
 */
@Test
public class BlackVolatilitySmileFxProviderTest {

  private static final Interpolator1D LINEAR_FLAT =
      CombinedInterpolatorExtrapolator.of(CurveInterpolators.LINEAR, CurveExtrapolators.FLAT, CurveExtrapolators.FLAT);

  private static final double[] TIME_TO_EXPIRY = new double[] {0.01, 0.252, 0.501, 1.0, 2.0, 5.0 };
  private static final double[] ATM = new double[] {0.175, 0.185, 0.18, 0.17, 0.16, 0.16 };
  private static final double[] DELTA = new double[] {0.10, 0.25 };
  private static final double[][] RISK_REVERSAL = new double[][] { {-0.010, -0.0050 }, {-0.011, -0.0060 },
    {-0.012, -0.0070 }, {-0.013, -0.0080 }, {-0.014, -0.0090 }, {-0.014, -0.0090 } };
  private static final double[][] STRANGLE = new double[][] { {0.0300, 0.0100 }, {0.0310, 0.0110 }, {0.0320, 0.0120 },
    {0.0330, 0.0130 }, {0.0340, 0.0140 }, {0.0340, 0.0140 } };
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM =
      new SmileDeltaTermStructureParametersStrikeInterpolation(TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE);
  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);
  private static final LocalTime VALUATION_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VALUATION_DATE_TIME = VALUATION_DATE.atTime(VALUATION_TIME).atZone(LONDON_ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);

  private static final BlackVolatilitySmileFxProvider PROVIDER =
      BlackVolatilitySmileFxProvider.of(SMILE_TERM, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
  private static final LocalTime TIME = LocalTime.of(11, 45);
  private static final ZonedDateTime[] TEST_EXPIRY = new ZonedDateTime[] {
    date(2015, 2, 18).atTime(LocalTime.MIDNIGHT).atZone(LONDON_ZONE),
    date(2015, 9, 17).atTime(TIME).atZone(LONDON_ZONE),
    date(2016, 6, 17).atTime(TIME).atZone(LONDON_ZONE),
    date(2018, 7, 17).atTime(TIME).atZone(LONDON_ZONE) };
  private static final double[] FORWARD = new double[] {1.4, 1.395, 1.39, 1.38, 1.35 };
  private static final int NB_EXPIRY = TEST_EXPIRY.length;
  private static final double[] TEST_STRIKE = new double[] {1.1, 1.28, 1.45, 1.62, 1.8 };
  private static final int NB_STRIKE = TEST_STRIKE.length;

  private static final double TOLERANCE = 1.0E-12;
  private static final double EPS = 1.0E-7;

  //-------------------------------------------------------------------------
  public void test_builder() {
    BlackVolatilitySmileFxProvider test = BlackVolatilitySmileFxProvider.builder()
        .currencyPair(CURRENCY_PAIR)
        .dayCount(ACT_365F)
        .smile(SMILE_TERM)
        .valuationDateTime(VALUATION_DATE_TIME)
        .build();
    assertEquals(test.getValuationDateTime(), VALUATION_DATE_TIME);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getSmile(), SMILE_TERM);
    assertEquals(PROVIDER, test);
  }

  //-------------------------------------------------------------------------
  public void test_volatility() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SMILE_TERM.getVolatility(expiryTime, TEST_STRIKE[j], FORWARD[i]);
        double volComputed = PROVIDER.getVolatility(CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i]);
        assertEquals(volComputed, volExpected, TOLERANCE);
      }
    }
  }

  public void test_volatility_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = PROVIDER.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SMILE_TERM.getVolatility(expiryTime, TEST_STRIKE[j], FORWARD[i]);
        double volComputed = PROVIDER.getVolatility(CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j],
            1d / FORWARD[i]);
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
          double nodeDelta = meta.getStrike().getValue();
          double expected = nodeSensitivity(
              PROVIDER, CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], nodeExpiry, nodeDelta);
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
          FxVolatilitySurfaceYearFractionNodeMetadata meta = ((FxVolatilitySurfaceYearFractionNodeMetadata) itr.next());
          double nodeExpiry = meta.getYearFraction();
          double nodeDelta = meta.getStrike().getValue();
          double expected = nodeSensitivity(PROVIDER, CURRENCY_PAIR.inverse(),
              TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], nodeExpiry, nodeDelta);
          assertEquals(value, expected, EPS);
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BlackVolatilitySmileFxProvider test1 =
        BlackVolatilitySmileFxProvider.of(SMILE_TERM, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    coverImmutableBean(test1);
    BlackVolatilitySmileFxProvider test2 = BlackVolatilitySmileFxProvider.of(
        SMILE_TERM,
        CURRENCY_PAIR.inverse(),
        ACT_360,
        ZonedDateTime.of(2015, 12, 21, 11, 15, 0, 0, ZoneId.of("Z")));
    coverBeanEquals(test1, test2);
  }

  //-------------------------------------------------------------------------
  // bumping a node point at (nodeExpiry, nodeDelta)
  private double nodeSensitivity(
      BlackVolatilitySmileFxProvider provider,
      CurrencyPair pair,
      ZonedDateTime expiry,
      double strike,
      double forward,
      double nodeExpiry,
      double nodeDelta) {

    double strikeMod = provider.getCurrencyPair().equals(pair) ? strike : 1.0 / strike;
    double forwardMod = provider.getCurrencyPair().equals(pair) ? forward : 1.0 / forward;

    SmileDeltaTermStructureParametersStrikeInterpolation smileTerm = provider.getSmile();
    double[] times = smileTerm.getTimeToExpiry();
    int nTimes = times.length;
    SmileDeltaParameters[] volTermUp = new SmileDeltaParameters[nTimes];
    SmileDeltaParameters[] volTermDw = new SmileDeltaParameters[nTimes];
    int deltaIndex = -1;
    for (int i = 0; i < nTimes; ++i) {
      DoubleArray deltas = smileTerm.getVolatilityTerm()[i].getDelta();
      int nDeltas = deltas.size();
      int nDeltasTotal = 2 * nDeltas + 1;
      double[] deltasTotal = new double[nDeltasTotal];
      for (int j = 0; j < nDeltas; ++j) {
        deltasTotal[j] = 1d - deltas.get(j);
        deltasTotal[2 * nDeltas - j] = deltas.get(j);
      }
      double[] volsUp = smileTerm.getVolatilityTerm()[i].getVolatility().toArray();
      double[] volsDw = smileTerm.getVolatilityTerm()[i].getVolatility().toArray();
      if (Math.abs(times[i] - nodeExpiry) < TOLERANCE) {
        for (int j = 0; j < nDeltasTotal; ++j) {
          if (Math.abs(deltasTotal[j] - nodeDelta) < TOLERANCE) {
            deltaIndex = j;
            volsUp[j] += EPS;
            volsDw[j] -= EPS;
          }
        }
      }
      volTermUp[i] = SmileDeltaParameters.of(times[i], deltas, DoubleArray.copyOf(volsUp));
      volTermDw[i] = SmileDeltaParameters.of(times[i], deltas, DoubleArray.copyOf(volsDw));
    }
    SmileDeltaTermStructureParametersStrikeInterpolation smileTermUp =
        new SmileDeltaTermStructureParametersStrikeInterpolation(volTermUp);
    SmileDeltaTermStructureParametersStrikeInterpolation smileTermDw =
        new SmileDeltaTermStructureParametersStrikeInterpolation(volTermDw);
    BlackVolatilitySmileFxProvider provUp =
        BlackVolatilitySmileFxProvider.of(smileTermUp, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    BlackVolatilitySmileFxProvider provDw =
        BlackVolatilitySmileFxProvider.of(smileTermDw, CURRENCY_PAIR, ACT_365F, VALUATION_DATE_TIME);
    double volUp = provUp.getVolatility(pair, expiry, strike, forward);
    double volDw = provDw.getVolatility(pair, expiry, strike, forward);
    double totalSensi = 0.5 * (volUp - volDw) / EPS;

    double expiryTime = provider.relativeTime(expiry);
    SmileDeltaParameters singleSmile = smileTerm.getSmileForTime(expiryTime);
    double[] strikesUp = singleSmile.getStrike(forwardMod);
    double[] strikesDw = strikesUp.clone();
    double[] vols = singleSmile.getVolatility().toArray();
    strikesUp[deltaIndex] += EPS;
    strikesDw[deltaIndex] -= EPS;
    double volStrikeUp = LINEAR_FLAT.interpolate(LINEAR_FLAT.getDataBundleFromSortedArrays(strikesUp, vols), strikeMod);
    double volStrikeDw = LINEAR_FLAT.interpolate(LINEAR_FLAT.getDataBundleFromSortedArrays(strikesDw, vols), strikeMod);
    double sensiStrike = 0.5 * (volStrikeUp - volStrikeDw) / EPS;
    SmileDeltaParameters singleSmileUp = smileTermUp.getSmileForTime(expiryTime);
    double strikeUp = singleSmileUp.getStrike(forwardMod)[deltaIndex];
    SmileDeltaParameters singleSmileDw = smileTermDw.getSmileForTime(expiryTime);
    double strikeDw = singleSmileDw.getStrike(forwardMod)[deltaIndex];
    double sensiVol = 0.5 * (strikeUp - strikeDw) / EPS;

    return totalSensi - sensiStrike * sensiVol;
  }

}
