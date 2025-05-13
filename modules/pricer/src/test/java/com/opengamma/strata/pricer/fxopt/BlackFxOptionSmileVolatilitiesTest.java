/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.FLAT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LINEAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;

/**
 * Test {@link BlackFxOptionSmileVolatilities}.
 */
public class BlackFxOptionSmileVolatilitiesTest {

  private static final FxOptionVolatilitiesName NAME = FxOptionVolatilitiesName.of("Test");
  private static final DoubleArray TIME_TO_EXPIRY = DoubleArray.of(0.01, 0.252, 0.501, 1.0, 2.0, 5.0);
  private static final DoubleArray ATM = DoubleArray.of(0.175, 0.185, 0.18, 0.17, 0.16, 0.16);
  private static final DoubleArray DELTA = DoubleArray.of(0.10, 0.25);
  private static final DoubleMatrix RISK_REVERSAL = DoubleMatrix.ofUnsafe(new double[][] {
      {-0.010, -0.0050}, {-0.011, -0.0060}, {-0.012, -0.0070}, {-0.013, -0.0080}, {-0.014, -0.0090}, {-0.014, -0.0090}});
  private static final DoubleMatrix STRANGLE = DoubleMatrix.ofUnsafe(new double[][] {
      {0.0300, 0.0100}, {0.0310, 0.0110}, {0.0320, 0.0120}, {0.0330, 0.0130}, {0.0340, 0.0140}, {0.0340, 0.0140}});
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM = InterpolatedStrikeSmileDeltaTermStructure.of(
      TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE, ACT_365F);
  private static final LocalDate VAL_DATE = date(2015, 2, 17);
  private static final LocalTime VAL_TIME = LocalTime.of(13, 45);
  private static final ZoneId LONDON_ZONE = ZoneId.of("Europe/London");
  private static final ZonedDateTime VAL_DATE_TIME = VAL_DATE.atTime(VAL_TIME).atZone(LONDON_ZONE);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(EUR, USD);

  private static final BlackFxOptionSmileVolatilities VOLS =
      BlackFxOptionSmileVolatilities.of(NAME, CURRENCY_PAIR, VAL_DATE_TIME, SMILE_TERM);
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
  @Test
  public void test_builder() {
    BlackFxOptionSmileVolatilities test = BlackFxOptionSmileVolatilities.builder()
        .name(NAME)
        .currencyPair(CURRENCY_PAIR)
        .smile(SMILE_TERM)
        .valuationDateTime(VAL_DATE_TIME)
        .build();
    assertThat(test.getName()).isEqualTo(NAME);
    assertThat(test.getValuationDateTime()).isEqualTo(VAL_DATE_TIME);
    assertThat(test.getCurrencyPair()).isEqualTo(CURRENCY_PAIR);
    assertThat(test.getSmile()).isEqualTo(SMILE_TERM);
    assertThat(VOLS).isEqualTo(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_volatility() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SMILE_TERM.volatility(expiryTime, TEST_STRIKE[j], FORWARD[i]);
        double volComputed = VOLS.volatility(CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i]);
        assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE));
        // test derivatives of volatility
        double volExpiryUp = VOLS.volatility(CURRENCY_PAIR, expiryTime + EPS, TEST_STRIKE[j], FORWARD[i]);
        double volExpiryDw = VOLS.volatility(CURRENCY_PAIR, expiryTime - EPS, TEST_STRIKE[j], FORWARD[i]);
        double expiryDerivExp = 0.5 * (volExpiryUp - volExpiryDw) / EPS;
        double volStrikeUp = VOLS.volatility(CURRENCY_PAIR, expiryTime, TEST_STRIKE[j] + EPS, FORWARD[i]);
        double volStrikeDw = VOLS.volatility(CURRENCY_PAIR, expiryTime, TEST_STRIKE[j] - EPS, FORWARD[i]);
        double strikeDerivExp = 0.5 * (volStrikeUp - volStrikeDw) / EPS;
        double volForwardUp = VOLS.volatility(CURRENCY_PAIR, expiryTime, TEST_STRIKE[j], FORWARD[i] + EPS);
        double volForwardDw = VOLS.volatility(CURRENCY_PAIR, expiryTime, TEST_STRIKE[j], FORWARD[i] - EPS);
        double forwardDerivExp = 0.5 * (volForwardUp - volForwardDw) / EPS;
        ValueDerivatives volDerivatives = VOLS.firstPartialDerivatives(CURRENCY_PAIR, expiryTime, TEST_STRIKE[j], FORWARD[i]);
        assertThat(volDerivatives.getDerivative(0)).isCloseTo(expiryDerivExp, offset(EPS));
        assertThat(volDerivatives.getDerivative(1)).isCloseTo(strikeDerivExp, offset(EPS));
        assertThat(volDerivatives.getDerivative(2)).isCloseTo(forwardDerivExp, offset(EPS));
      }
    }
  }

  @Test
  public void test_volatility_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      double expiryTime = VOLS.relativeTime(TEST_EXPIRY[i]);
      for (int j = 0; j < NB_STRIKE; ++j) {
        double volExpected = SMILE_TERM.volatility(expiryTime, TEST_STRIKE[j], FORWARD[i]);
        double volComputed = VOLS.volatility(CURRENCY_PAIR.inverse(), TEST_EXPIRY[i], 1d / TEST_STRIKE[j],
            1d / FORWARD[i]);
        assertThat(volComputed).isCloseTo(volExpected, offset(TOLERANCE));
        // test derivatives of volatility
        double volExpiryUp = VOLS.volatility(
            CURRENCY_PAIR.inverse(), expiryTime + EPS, 1d / TEST_STRIKE[j], 1d / FORWARD[i]);
        double volExpiryDw = VOLS.volatility(
            CURRENCY_PAIR.inverse(), expiryTime - EPS, 1d / TEST_STRIKE[j], 1d / FORWARD[i]);
        double expiryDerivExp = 0.5 * (volExpiryUp - volExpiryDw) / EPS;
        double volStrikeUp = VOLS.volatility(
            CURRENCY_PAIR.inverse(), expiryTime, 1d / TEST_STRIKE[j] + EPS, 1d / FORWARD[i]);
        double volStrikeDw = VOLS.volatility(
            CURRENCY_PAIR.inverse(), expiryTime, 1d / TEST_STRIKE[j] - EPS, 1d / FORWARD[i]);
        double strikeDerivExp = 0.5 * (volStrikeUp - volStrikeDw) / EPS;
        double volForwardUp = VOLS.volatility(
            CURRENCY_PAIR.inverse(), expiryTime, 1d / TEST_STRIKE[j], 1d / FORWARD[i] + EPS);
        double volForwardDw = VOLS.volatility(
            CURRENCY_PAIR.inverse(), expiryTime, 1d / TEST_STRIKE[j], 1d / FORWARD[i] - EPS);
        double forwardDerivExp = 0.5 * (volForwardUp - volForwardDw) / EPS;
        ValueDerivatives volDerivatives = VOLS.firstPartialDerivatives(
            CURRENCY_PAIR.inverse(), expiryTime, 1d / TEST_STRIKE[j], 1d / FORWARD[i]);
        assertThat(volDerivatives.getDerivative(0)).isCloseTo(expiryDerivExp, offset(EPS));
        assertThat(volDerivatives.getDerivative(1)).isCloseTo(strikeDerivExp, offset(EPS));
        assertThat(volDerivatives.getDerivative(2)).isCloseTo(forwardDerivExp, offset(EPS));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_surfaceParameterSensitivity() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double timeToExpiry = VOLS.relativeTime(TEST_EXPIRY[i]);
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            VOLS.getName(), CURRENCY_PAIR, timeToExpiry, TEST_STRIKE[j], FORWARD[i], GBP, 1d);
        CurrencyParameterSensitivity computed = VOLS.parameterSensitivity(sensi).getSensitivities().get(0);
        Iterator<ParameterMetadata> itr = computed.getParameterMetadata().iterator();
        for (double value : computed.getSensitivity().toArray()) {
          FxVolatilitySurfaceYearFractionParameterMetadata meta = ((FxVolatilitySurfaceYearFractionParameterMetadata) itr.next());
          double nodeExpiry = meta.getYearFraction();
          double nodeDelta = meta.getStrike().getValue();
          double expected = nodeSensitivity(
              VOLS, CURRENCY_PAIR, TEST_EXPIRY[i], TEST_STRIKE[j], FORWARD[i], nodeExpiry, nodeDelta);
          assertThat(value).isCloseTo(expected, offset(EPS));
        }

      }
    }
  }

  @Test
  public void test_surfaceParameterSensitivity_inverse() {
    for (int i = 0; i < NB_EXPIRY; i++) {
      for (int j = 0; j < NB_STRIKE; ++j) {
        double timeToExpiry = VOLS.relativeTime(TEST_EXPIRY[i]);
        FxOptionSensitivity sensi = FxOptionSensitivity.of(
            VOLS.getName(), CURRENCY_PAIR.inverse(), timeToExpiry, 1d / TEST_STRIKE[j], 1d / FORWARD[i], GBP, 1d);
        CurrencyParameterSensitivity computed = VOLS.parameterSensitivity(sensi).getSensitivities().get(0);
        Iterator<ParameterMetadata> itr = computed.getParameterMetadata().iterator();
        for (double value : computed.getSensitivity().toArray()) {
          FxVolatilitySurfaceYearFractionParameterMetadata meta = ((FxVolatilitySurfaceYearFractionParameterMetadata) itr.next());
          double nodeExpiry = meta.getYearFraction();
          double nodeDelta = meta.getStrike().getValue();
          double expected = nodeSensitivity(VOLS, CURRENCY_PAIR.inverse(),
              TEST_EXPIRY[i], 1d / TEST_STRIKE[j], 1d / FORWARD[i], nodeExpiry, nodeDelta);
          assertThat(value).isCloseTo(expected, offset(EPS));
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    BlackFxOptionSmileVolatilities test1 =
        BlackFxOptionSmileVolatilities.of(NAME, CURRENCY_PAIR, VAL_DATE_TIME, SMILE_TERM);
    coverImmutableBean(test1);
    BlackFxOptionSmileVolatilities test2 = BlackFxOptionSmileVolatilities.of(
        FxOptionVolatilitiesName.of("Boo"),
        CURRENCY_PAIR.inverse(),
        ZonedDateTime.of(2015, 12, 21, 11, 15, 0, 0, ZoneId.of("Z")),
        SMILE_TERM);
    coverBeanEquals(test1, test2);
  }

  //-------------------------------------------------------------------------
  // bumping a node point at (nodeExpiry, nodeDelta)
  private double nodeSensitivity(
      BlackFxOptionSmileVolatilities provider,
      CurrencyPair pair,
      ZonedDateTime expiry,
      double strike,
      double forward,
      double nodeExpiry,
      double nodeDelta) {

    double strikeMod = provider.getCurrencyPair().equals(pair) ? strike : 1.0 / strike;
    double forwardMod = provider.getCurrencyPair().equals(pair) ? forward : 1.0 / forward;

    InterpolatedStrikeSmileDeltaTermStructure smileTerm =
        (InterpolatedStrikeSmileDeltaTermStructure) provider.getSmile();
    double[] times = smileTerm.getExpiries().toArray();
    int nTimes = times.length;
    SmileDeltaParameters[] volTermUp = new SmileDeltaParameters[nTimes];
    SmileDeltaParameters[] volTermDw = new SmileDeltaParameters[nTimes];
    int deltaIndex = -1;
    for (int i = 0; i < nTimes; ++i) {
      DoubleArray deltas = smileTerm.getVolatilityTerm().get(i).getDelta();
      int nDeltas = deltas.size();
      int nDeltasTotal = 2 * nDeltas + 1;
      double[] deltasTotal = new double[nDeltasTotal];
      deltasTotal[nDeltas] = 0.5d;
      for (int j = 0; j < nDeltas; ++j) {
        deltasTotal[j] = 1d - deltas.get(j);
        deltasTotal[2 * nDeltas - j] = deltas.get(j);
      }
      double[] volsUp = smileTerm.getVolatilityTerm().get(i).getVolatility().toArray();
      double[] volsDw = smileTerm.getVolatilityTerm().get(i).getVolatility().toArray();
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
    InterpolatedStrikeSmileDeltaTermStructure smileTermUp =
        InterpolatedStrikeSmileDeltaTermStructure.of(ImmutableList.copyOf(volTermUp), ACT_365F);
    InterpolatedStrikeSmileDeltaTermStructure smileTermDw =
        InterpolatedStrikeSmileDeltaTermStructure.of(ImmutableList.copyOf(volTermDw), ACT_365F);
    BlackFxOptionSmileVolatilities provUp =
        BlackFxOptionSmileVolatilities.of(NAME, CURRENCY_PAIR, VAL_DATE_TIME, smileTermUp);
    BlackFxOptionSmileVolatilities provDw =
        BlackFxOptionSmileVolatilities.of(NAME, CURRENCY_PAIR, VAL_DATE_TIME, smileTermDw);
    double volUp = provUp.volatility(pair, expiry, strike, forward);
    double volDw = provDw.volatility(pair, expiry, strike, forward);
    double totalSensi = 0.5 * (volUp - volDw) / EPS;

    double expiryTime = provider.relativeTime(expiry);
    SmileDeltaParameters singleSmile = smileTerm.smileForExpiry(expiryTime);
    double[] strikesUp = singleSmile.strike(forwardMod).toArray();
    double[] strikesDw = strikesUp.clone();
    double[] vols = singleSmile.getVolatility().toArray();
    strikesUp[deltaIndex] += EPS;
    strikesDw[deltaIndex] -= EPS;
    double volStrikeUp =
        LINEAR.bind(DoubleArray.ofUnsafe(strikesUp), DoubleArray.ofUnsafe(vols), FLAT, FLAT).interpolate(strikeMod);
    double volStrikeDw =
        LINEAR.bind(DoubleArray.ofUnsafe(strikesDw), DoubleArray.ofUnsafe(vols), FLAT, FLAT).interpolate(strikeMod);
    double sensiStrike = 0.5 * (volStrikeUp - volStrikeDw) / EPS;
    SmileDeltaParameters singleSmileUp = smileTermUp.smileForExpiry(expiryTime);
    double strikeUp = singleSmileUp.strike(forwardMod).get(deltaIndex);
    SmileDeltaParameters singleSmileDw = smileTermDw.smileForExpiry(expiryTime);
    double strikeDw = singleSmileDw.strike(forwardMod).get(deltaIndex);
    double sensiVol = 0.5 * (strikeUp - strikeDw) / EPS;

    return totalSensi - sensiStrike * sensiVol;
  }

}
