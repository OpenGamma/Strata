/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;

/**
 * Tests related to the construction of term structure of smile data from delta.
 * Tests related to the interpolation of volatility.
 */
public class InterpolatedStrikeSmileDeltaTermStructureTest {

  private static final DoubleArray TIME_TO_EXPIRY = DoubleArray.of(0.10, 0.25, 0.50, 1.00, 2.00, 3.00);
  private static final DoubleArray ATM = DoubleArray.of(0.175, 0.185, 0.18, 0.17, 0.16, 0.17);
  private static final DoubleArray DELTA = DoubleArray.of(0.10, 0.25);
  private static final DoubleMatrix RISK_REVERSAL = DoubleMatrix.copyOf(new double[][] {
      {-0.010, -0.0050},
      {-0.011, -0.0060},
      {-0.012, -0.0070},
      {-0.013, -0.0080},
      {-0.014, -0.0090},
      {-0.014, -0.0090}});
  private static final DoubleMatrix STRANGLE = DoubleMatrix.copyOf(new double[][] {
      {0.0300, 0.0100},
      {0.0310, 0.0110},
      {0.0320, 0.0120},
      {0.0330, 0.0130},
      {0.0340, 0.0140},
      {0.0340, 0.0140}});
  private static final int NB_EXP = TIME_TO_EXPIRY.size();
  private static final List<SmileDeltaParameters> VOLATILITY_TERM = new ArrayList<>(NB_EXP);
  private static final Percentage DERIV_TOL = Percentage.withPercentage(0.001);

  static {
    for (int loopexp = 0; loopexp < NB_EXP; loopexp++) {
      VOLATILITY_TERM.add(SmileDeltaParameters.of(
          TIME_TO_EXPIRY.get(loopexp),
          ATM.get(loopexp),
          DELTA,
          DoubleArray.copyOf(RISK_REVERSAL.toArray()[loopexp]),
          DoubleArray.copyOf(STRANGLE.toArray()[loopexp])));
    }
  }
  private static final CurveInterpolator INTERPOLATOR_STRIKE = CurveInterpolators.LINEAR;
  private static final CurveExtrapolator FLAT = CurveExtrapolators.FLAT;
  private static final InterpolatedStrikeSmileDeltaTermStructure SMILE_TERM =
      InterpolatedStrikeSmileDeltaTermStructure.of(VOLATILITY_TERM, ACT_360, INTERPOLATOR_STRIKE, FLAT, FLAT);

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  @Test
  public void getter() {
    assertThat(SMILE_TERM.getVolatilityTerm()).as("Smile by delta term structure: volatility").isEqualTo(VOLATILITY_TERM);
  }

  //-------------------------------------------------------------------------
  @Test
  public void constructor() {
    InterpolatedStrikeSmileDeltaTermStructure smileTerm1 =
        InterpolatedStrikeSmileDeltaTermStructure.of(
            TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE, ACT_360);
    assertThat(smileTerm1).as("Smile by delta term structure: constructor").isEqualTo(SMILE_TERM);
    InterpolatedStrikeSmileDeltaTermStructure smileTerm2 =
        InterpolatedStrikeSmileDeltaTermStructure.of(
            TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, STRANGLE, ACT_360, INTERPOLATOR_STRIKE, FLAT, FLAT);
    assertThat(smileTerm2).as("Smile by delta term structure: constructor").isEqualTo(SMILE_TERM);
  }

  @Test
  public void constructor2() {
    double[][] vol = new double[NB_EXP][];
    for (int loopexp = 0; loopexp < NB_EXP; loopexp++) {
      vol[loopexp] = VOLATILITY_TERM.get(loopexp).getVolatility().toArray();
    }
    InterpolatedStrikeSmileDeltaTermStructure smileTermVol1 = InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, DELTA, DoubleMatrix.copyOf(vol), ACT_360);
    assertThat(smileTermVol1).as("Smile by delta term structure: constructor").isEqualTo(SMILE_TERM);
    InterpolatedStrikeSmileDeltaTermStructure smileTermVol2 = InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, DELTA, DoubleMatrix.copyOf(vol), ACT_360, INTERPOLATOR_STRIKE, FLAT, FLAT);
    assertThat(smileTermVol2).as("Smile by delta term structure: constructor").isEqualTo(SMILE_TERM);
  }

  @Test
  public void testWrongDataSize() {
    double[][] vol = new double[NB_EXP][];
    for (int loopexp = 0; loopexp < NB_EXP; loopexp++) {
      vol[loopexp] = VOLATILITY_TERM.get(loopexp).getVolatility().toArray();
    }
    DoubleArray timeShort = DoubleArray.of(0.10, 0.25, 0.50, 1.00, 2.00);
    DoubleArray deltaLong = DoubleArray.of(0.10, 0.2, 0.25);
    DoubleArray delta0 = DoubleArray.of();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        timeShort, DELTA, DoubleMatrix.copyOf(vol), ACT_360));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, deltaLong, DoubleMatrix.copyOf(vol), ACT_360));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, delta0, DoubleMatrix.copyOf(vol), ACT_360));
    DoubleMatrix shortMat = DoubleMatrix.copyOf(new double[][] {
        {0.0300, 0.0100}, {0.0310, 0.0110}, {0.0320, 0.0120}, {0.0330, 0.0130}, {0.0340, 0.0140}});
    DoubleMatrix vec = DoubleMatrix.copyOf(new double[][] {
        {0.0300}, {0.0310}, {0.0320}, {0.0330}, {0.0340}, {0.0340}});
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        timeShort, DELTA, ATM, RISK_REVERSAL, STRANGLE, ACT_360));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, deltaLong, ATM, RISK_REVERSAL, STRANGLE, ACT_360));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, DELTA, ATM, shortMat, STRANGLE, ACT_360));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, shortMat, ACT_360));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> InterpolatedStrikeSmileDeltaTermStructure.of(
        TIME_TO_EXPIRY, DELTA, ATM, RISK_REVERSAL, vec, ACT_360));
  }

  //-------------------------------------------------------------------------
  /**
   * Tests the volatility at a point of the grid.
   */
  @Test
  public void volatilityAtPoint() {
    double forward = 1.40;
    double timeToExpiry = 0.50;
    double[] strikes = SMILE_TERM.getVolatilityTerm().get(2).strike(forward).toArray();
    double volComputed = SMILE_TERM.volatility(timeToExpiry, strikes[1], forward);
    double volExpected = SMILE_TERM.getVolatilityTerm().get(2).getVolatility().get(1);
    assertThat(volComputed).as("Smile by delta term structure: volatility at a point").isCloseTo(volExpected, offset(TOLERANCE_VOL));
  }

  /**
   * Tests the interpolation in the strike dimension at a time of the grid.
   */
  @Test
  public void volatilityStrikeInterpolation() {
    double forward = 1.40;
    double timeToExpiry = 0.50;
    double strike = 1.50;
    DoubleArray strikes = SMILE_TERM.getVolatilityTerm().get(2).strike(forward);
    DoubleArray vol = SMILE_TERM.getVolatilityTerm().get(2).getVolatility();
    BoundCurveInterpolator interpolator = CurveInterpolators.LINEAR.bind(strikes, vol);
    double volExpected = interpolator.interpolate(strike);
    double volComputed = SMILE_TERM.volatility(timeToExpiry, strike, forward);
    assertThat(volComputed).as("Smile by delta term structure: vol interpolation on strike").isCloseTo(volExpected, offset(TOLERANCE_VOL));
  }

  /**
   * Tests the extrapolation below the first expiry.
   */
  @Test
  public void volatilityBelowFirstExpiry() {
    double forward = 1.40;
    double timeToExpiry = 0.05;
    double strike = 1.45;
    SmileDeltaParameters smile = SmileDeltaParameters.of(
        timeToExpiry,
        ATM.get(0),
        DELTA,
        DoubleArray.copyOf(RISK_REVERSAL.toArray()[0]),
        DoubleArray.copyOf(STRANGLE.toArray()[0]));
    DoubleArray strikes = smile.strike(forward);
    DoubleArray vol = smile.getVolatility();
    double volExpected = INTERPOLATOR_STRIKE.bind(strikes, vol, FLAT, FLAT).interpolate(strike);
    double volComputed = SMILE_TERM.volatility(timeToExpiry, strike, forward);
    assertThat(volComputed).as("Smile by delta term structure: vol interpolation on strike").isCloseTo(volExpected, offset(TOLERANCE_VOL));
  }

  /**
   * Tests the extrapolation above the last expiry.
   */
  @Test
  public void volatilityAboveLastExpiry() {
    double forward = 1.40;
    double timeToExpiry = 5.00;
    double strike = 1.45;
    SmileDeltaParameters smile = SmileDeltaParameters.of(
        timeToExpiry,
        ATM.toArray()[NB_EXP - 1],
        DELTA,
        DoubleArray.copyOf(RISK_REVERSAL.toArray()[NB_EXP - 1]),
        DoubleArray.copyOf(STRANGLE.toArray()[NB_EXP - 1]));
    DoubleArray strikes = smile.strike(forward);
    DoubleArray vol = smile.getVolatility();
    double volExpected = INTERPOLATOR_STRIKE.bind(strikes, vol, FLAT, FLAT).interpolate(strike);
    double volComputed = SMILE_TERM.volatility(timeToExpiry, strike, forward);
    assertThat(volComputed).as("Smile by delta term structure: vol interpolation on strike").isCloseTo(volExpected, offset(TOLERANCE_VOL));
  }

  /**
   * Tests the interpolation in the time and strike dimensions.
   */
  @Test
  public void volatilityTimeInterpolation() {
    double forward = 1.40;
    double timeToExpiry = 0.75;
    double strike = 1.50;
    double[] vol050 = SMILE_TERM.getVolatilityTerm().get(2).getVolatility().toArray();
    double[] vol100 = SMILE_TERM.getVolatilityTerm().get(3).getVolatility().toArray();
    double[] vol = new double[vol050.length];
    for (int loopvol = 0; loopvol < vol050.length; loopvol++) {
      vol[loopvol] = Math.sqrt(((vol050[loopvol] * vol050[loopvol] * TIME_TO_EXPIRY.get(2) +
          vol100[loopvol] * vol100[loopvol] * TIME_TO_EXPIRY.get(3)) / 2.0) / timeToExpiry);
    }
    SmileDeltaParameters smile = SmileDeltaParameters.of(timeToExpiry, DELTA, DoubleArray.copyOf(vol));
    DoubleArray strikes = smile.strike(forward);
    double volExpected = INTERPOLATOR_STRIKE.bind(strikes, DoubleArray.copyOf(vol), FLAT, FLAT).interpolate(strike);
    double volComputed = SMILE_TERM.volatility(timeToExpiry, strike, forward);
    assertThat(volComputed).as("Smile by delta term structure: vol interpolation on strike").isCloseTo(volExpected, offset(TOLERANCE_VOL));
    double volTriple = SMILE_TERM.volatility(timeToExpiry, strike, forward);
    assertThat(volTriple).as("Smile by delta term structure: vol interpolation on strike").isCloseTo(volComputed, offset(TOLERANCE_VOL));
    InterpolatedStrikeSmileDeltaTermStructure smileTerm2 =
        InterpolatedStrikeSmileDeltaTermStructure.of(VOLATILITY_TERM, ACT_360);
    double volComputed2 = smileTerm2.volatility(timeToExpiry, strike, forward);
    assertThat(volComputed2).as("Smile by delta term structure: vol interp on strike").isCloseTo(volComputed, offset(TOLERANCE_VOL));
  }

  /**
   * Tests the interpolation and its derivative with respect to the data by comparison to finite difference.
   */
  @Test
  public void volatilityAjoint() {
    double forward = 1.40;
    double[] timeToExpiry = new double[] {0.75, 1.00, 2.50};
    double[] strike = new double[] {1.50, 1.70, 2.20};
    double[] tolerance = new double[] {3e-2, 1e-1, 1e-5};
    int nbTest = strike.length;
    double shift = 0.00001;
    for (int looptest = 0; looptest < nbTest; looptest++) {
      double vol = SMILE_TERM.volatility(timeToExpiry[looptest], strike[looptest], forward);
      double[][] bucketTest = new double[TIME_TO_EXPIRY.size()][2 * DELTA.size() + 1];
      VolatilityAndBucketedSensitivities volComputed =
          SMILE_TERM.volatilityAndSensitivities(timeToExpiry[looptest], strike[looptest], forward);
      DoubleMatrix bucketSensi = volComputed.getSensitivities();
      assertThat(volComputed.getVolatility()).as("Smile by delta term structure: volatility adjoint").isCloseTo(vol, offset(1.0E-10));
      SmileDeltaParameters[] volData = new SmileDeltaParameters[TIME_TO_EXPIRY.size()];
      double[] volBumped = new double[2 * DELTA.size() + 1];
      for (int loopexp = 0; loopexp < TIME_TO_EXPIRY.size(); loopexp++) {
        for (int loopsmile = 0; loopsmile < 2 * DELTA.size() + 1; loopsmile++) {
          System.arraycopy(SMILE_TERM.getVolatilityTerm().toArray(), 0, volData, 0, TIME_TO_EXPIRY.size());
          System.arraycopy(SMILE_TERM.getVolatilityTerm().get(loopexp).getVolatility().toArray(), 0, volBumped, 0,
              2 * DELTA.size() + 1);
          volBumped[loopsmile] += shift;
          volData[loopexp] = SmileDeltaParameters.of(TIME_TO_EXPIRY.get(loopexp), DELTA, DoubleArray.copyOf(volBumped));
          InterpolatedStrikeSmileDeltaTermStructure smileTermBumped =
              InterpolatedStrikeSmileDeltaTermStructure.of(ImmutableList.copyOf(volData), ACT_360);
          bucketTest[loopexp][loopsmile] = (smileTermBumped.volatility(timeToExpiry[looptest], strike[looptest],
              forward) - volComputed.getVolatility()) / shift;
          assertThat(bucketSensi.get(loopexp, loopsmile)).as("Smile by delta term structure: (test: " + looptest + ") volatility bucket sensitivity " +
              loopexp + " - " + loopsmile).isCloseTo(bucketTest[loopexp][loopsmile], offset(tolerance[looptest]));
        }
      }
    }
  }

  @Test
  void partialFirstDerivatives() {
    double forward = 1.70;
    double timeToExpiry = 1.3;
    double strike = 1.5;
    double eps = 1e-6;
    double volExpected = SMILE_TERM.volatility(timeToExpiry, strike, forward);

    double volHighTime = SMILE_TERM.volatility(timeToExpiry + eps, strike, forward);
    double timeDerivativeExpected = (volHighTime - volExpected) / (eps);
    ValueDerivatives valueDerivativesComputed = SMILE_TERM.partialFirstDerivatives(timeToExpiry, strike, forward);
    assertThat(valueDerivativesComputed.getValue()).isCloseTo(volExpected, offset(TOLERANCE_VOL));
    assertThat(valueDerivativesComputed.getDerivative(0)).isCloseTo(timeDerivativeExpected, DERIV_TOL);

    double volHighStrike = SMILE_TERM.volatility(timeToExpiry, strike + eps, forward);
    double strikeDerivativeExpected = (volHighStrike - volExpected) / eps;
    assertThat(valueDerivativesComputed.getDerivative(1)).isCloseTo(strikeDerivativeExpected, DERIV_TOL);

  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(SMILE_TERM);
    InterpolatedStrikeSmileDeltaTermStructure other = InterpolatedStrikeSmileDeltaTermStructure.of(
        DoubleArray.of(0.1, 0.5),
        DoubleArray.of(0.25),
        DoubleMatrix.copyOf(new double[][] {{0.15, 0.1, 0.12}, {0.1, 0.07, 0.08}}),
        ACT_360,
        CurveInterpolators.NATURAL_SPLINE,
        CurveExtrapolators.LINEAR,
        CurveExtrapolators.LINEAR,
        CurveInterpolators.NATURAL_SPLINE,
        CurveExtrapolators.LINEAR,
        CurveExtrapolators.LINEAR);
    coverBeanEquals(SMILE_TERM, other);
  }

  @Test
  public void test_serialization() {
    assertSerialization(SMILE_TERM);
  }

}
