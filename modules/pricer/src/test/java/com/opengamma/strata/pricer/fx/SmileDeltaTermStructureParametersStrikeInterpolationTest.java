/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Triple;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;
import com.opengamma.strata.math.impl.interpolation.LinearInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;

/**
 * Tests related to the construction of term structure of smile data from delta.
 * Tests related to the interpolation of volatility.
 */
@Test
public class SmileDeltaTermStructureParametersStrikeInterpolationTest {

  private static final double[] TIME_TO_EXPIRY = {0.10, 0.25, 0.50, 1.00, 2.00, 3.00 };
  private static final double[] ATM = {0.175, 0.185, 0.18, 0.17, 0.16, 0.17 };
  private static final DoubleArray DELTA = DoubleArray.of(0.10, 0.25);
  private static final double[][] RISK_REVERSAL = new double[][] {
      {-0.010, -0.0050},
      {-0.011, -0.0060},
      {-0.012, -0.0070},
      {-0.013, -0.0080},
      {-0.014, -0.0090},
      {-0.014, -0.0090}};
  private static final double[][] STRANGLE = new double[][] {
      {0.0300, 0.0100},
      {0.0310, 0.0110},
      {0.0320, 0.0120},
      {0.0330, 0.0130},
      {0.0340, 0.0140},
      {0.0340, 0.0140}};
  private static final int NB_EXP = TIME_TO_EXPIRY.length;
  private static final SmileDeltaParameters[] VOLATILITY_TERM = new SmileDeltaParameters[NB_EXP];
  static {
    for (int loopexp = 0; loopexp < NB_EXP; loopexp++) {
      VOLATILITY_TERM[loopexp] = SmileDeltaParameters.of(
          TIME_TO_EXPIRY[loopexp], 
          ATM[loopexp],
          DELTA, 
          DoubleArray.copyOf(RISK_REVERSAL[loopexp]),
          DoubleArray.copyOf(STRANGLE[loopexp]));
    }
  }
  private static final Interpolator1D INTERPOLATOR_STRIKE = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.LINEAR, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final Interpolator1D INTERPOLATOR_TIME = CombinedInterpolatorExtrapolatorFactory.getInterpolator(Interpolator1DFactory.TIME_SQUARE, Interpolator1DFactory.FLAT_EXTRAPOLATOR,
      Interpolator1DFactory.FLAT_EXTRAPOLATOR);
  private static final SmileDeltaTermStructureParametersStrikeInterpolation SMILE_TERM = new SmileDeltaTermStructureParametersStrikeInterpolation(VOLATILITY_TERM, INTERPOLATOR_STRIKE);

  private static final double TOLERANCE_VOL = 1.0E-10;

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullVolatility() {
    new SmileDeltaTermStructureParametersStrikeInterpolation(null, INTERPOLATOR_STRIKE);
  }

  public void getter() {
    assertEquals("Smile by delta term structure: volatility", VOLATILITY_TERM, SMILE_TERM.getVolatilityTerm());
  }

  public void constructor() {
    SmileDeltaTermStructureParametersStrikeInterpolation smileTerm2 =
        new SmileDeltaTermStructureParametersStrikeInterpolation(
            SMILE_TERM.getName(), TIME_TO_EXPIRY, DELTA.toArray(), ATM, RISK_REVERSAL, STRANGLE);
    assertEquals("Smile by delta term structure: constructor", SMILE_TERM, smileTerm2);
  }

  public void constructor2() {
    double[][] vol = new double[NB_EXP][];
    for (int loopexp = 0; loopexp < NB_EXP; loopexp++) {
      vol[loopexp] = VOLATILITY_TERM[loopexp].getVolatility().toArray();
    }
    SmileDeltaTermStructureParametersStrikeInterpolation smileTermVol =
        new SmileDeltaTermStructureParametersStrikeInterpolation(
            SMILE_TERM.getName(), TIME_TO_EXPIRY, DELTA.toArray(), vol);
    assertEquals("Smile by delta term structure: constructor", SMILE_TERM, smileTermVol);
  }

  /**
   * Tests the volatility at a point of the grid.
   */
  public void volatilityAtPoint() {
    double forward = 1.40;
    double timeToExpiration = 0.50;
    double[] strikes = SMILE_TERM.getVolatilityTerm()[2].getStrike(forward);
    double volComputed = SMILE_TERM.getVolatility(timeToExpiration, strikes[1], forward);
    double volExpected = SMILE_TERM.getVolatilityTerm()[2].getVolatility().get(1);
    assertEquals("Smile by delta term structure: volatility at a point", volExpected, volComputed, TOLERANCE_VOL);
  }

  /**
   * Tests the interpolation in the strike dimension at a time of the grid.
   */
  public void volatilityStrikeInterpolation() {
    double forward = 1.40;
    double timeToExpiration = 0.50;
    double strike = 1.50;
    double[] strikes = SMILE_TERM.getVolatilityTerm()[2].getStrike(forward);
    double[] vol = SMILE_TERM.getVolatilityTerm()[2].getVolatility().toArray();
    ArrayInterpolator1DDataBundle volatilityInterpolation = new ArrayInterpolator1DDataBundle(strikes, vol);
    LinearInterpolator1D interpolator = new LinearInterpolator1D();
    double volExpected = interpolator.interpolate(volatilityInterpolation, strike);
    double volComputed = SMILE_TERM.getVolatility(timeToExpiration, strike, forward);
    assertEquals("Smile by delta term structure: volatility interpolation on strike", volExpected, volComputed, TOLERANCE_VOL);
  }

  /**
   * Tests the extrapolation below the first expiration.
   */
  public void volatilityBelowFirstExpiry() {
    double forward = 1.40;
    double timeToExpiration = 0.05;
    double strike = 1.45;
    SmileDeltaParameters smile = SmileDeltaParameters.of(
        timeToExpiration,
        ATM[0],
        DELTA,
        DoubleArray.copyOf(RISK_REVERSAL[0]),
        DoubleArray.copyOf(STRANGLE[0]));
    double[] strikes = smile.getStrike(forward);
    double[] vol = smile.getVolatility().toArray();
    ArrayInterpolator1DDataBundle volatilityInterpolation = new ArrayInterpolator1DDataBundle(strikes, vol);
    double volExpected = INTERPOLATOR_STRIKE.interpolate(volatilityInterpolation, strike);
    double volComputed = SMILE_TERM.getVolatility(timeToExpiration, strike, forward);
    assertEquals("Smile by delta term structure: volatility interpolation on strike", volExpected, volComputed, TOLERANCE_VOL);
  }

  /**
   * Tests the extrapolation above the last expiration.
   */
  public void volatilityAboveLastExpiry() {
    double forward = 1.40;
    double timeToExpiration = 5.00;
    double strike = 1.45;
    SmileDeltaParameters smile = SmileDeltaParameters.of(
        timeToExpiration,
        ATM[NB_EXP - 1],
        DELTA,
        DoubleArray.copyOf(RISK_REVERSAL[NB_EXP - 1]),
        DoubleArray.copyOf(STRANGLE[NB_EXP - 1]));
    double[] strikes = smile.getStrike(forward);
    double[] vol = smile.getVolatility().toArray();
    ArrayInterpolator1DDataBundle volatilityInterpolation = new ArrayInterpolator1DDataBundle(strikes, vol);
    double volExpected = INTERPOLATOR_STRIKE.interpolate(volatilityInterpolation, strike);
    double volComputed = SMILE_TERM.getVolatility(timeToExpiration, strike, forward);
    assertEquals("Smile by delta term structure: volatility interpolation on strike", volExpected, volComputed, TOLERANCE_VOL);
  }

  /**
   * Tests the interpolation in the time and strike dimensions.
   */
  public void volatilityTimeInterpolation() {
    double forward = 1.40;
    double timeToExpiration = 0.75;
    double strike = 1.50;
    double[] vol050 = SMILE_TERM.getVolatilityTerm()[2].getVolatility().toArray();
    double[] vol100 = SMILE_TERM.getVolatilityTerm()[3].getVolatility().toArray();
    double[] vol = new double[vol050.length];
    for (int loopvol = 0; loopvol < vol050.length; loopvol++) {
      vol[loopvol] = Math.sqrt(((vol050[loopvol] * vol050[loopvol] * TIME_TO_EXPIRY[2] + vol100[loopvol] * vol100[loopvol] * TIME_TO_EXPIRY[3]) / 2.0) / timeToExpiration);
    }
    SmileDeltaParameters smile = SmileDeltaParameters.of(timeToExpiration, DELTA, DoubleArray.copyOf(vol));
    double[] strikes = smile.getStrike(forward);
    ArrayInterpolator1DDataBundle volatilityInterpolation = new ArrayInterpolator1DDataBundle(strikes, vol);
    LinearInterpolator1D interpolator = new LinearInterpolator1D();
    double volExpected = interpolator.interpolate(volatilityInterpolation, strike);
    double volComputed = SMILE_TERM.getVolatility(timeToExpiration, strike, forward);
    assertEquals("Smile by delta term structure: volatility interpolation on strike", volExpected, volComputed, TOLERANCE_VOL);
    double volTriple = SMILE_TERM.getVolatility(Triple.of(timeToExpiration, strike, forward));
    assertEquals("Smile by delta term structure: volatility interpolation on strike", volComputed, volTriple, TOLERANCE_VOL);
    SmileDeltaTermStructureParametersStrikeInterpolation smileTerm2 =
        new SmileDeltaTermStructureParametersStrikeInterpolation(
            TIME_TO_EXPIRY, DELTA.toArray(), ATM, RISK_REVERSAL, STRANGLE, INTERPOLATOR_STRIKE, INTERPOLATOR_TIME);
    double volComputed2 = smileTerm2.getVolatility(timeToExpiration, strike, forward);
    assertEquals("Smile by delta term structure: volatility interpolation on strike", volComputed, volComputed2, TOLERANCE_VOL);
  }

  /**
   * Tests the interpolation and its derivative with respect to the data by comparison to finite difference.
   */
  public void volatilityAjoint() {
    double forward = 1.40;
    double[] timeToExpiration = new double[] {0.75, 1.00, 2.50};
    double[] strike = new double[] {1.50, 1.70, 2.20};
    double[] tolerance = new double[] {3e-2, 1e-1, 1e-5};
    int nbTest = strike.length;
    double shift = 0.00001;
    for (int looptest = 0; looptest < nbTest; looptest++) {
      double vol = SMILE_TERM.getVolatility(timeToExpiration[looptest], strike[looptest], forward);
      double[][] bucketTest = new double[TIME_TO_EXPIRY.length][2 * DELTA.size() + 1];
      VolatilityAndBucketedSensitivities volComputed =
          SMILE_TERM.getVolatilityAndSensitivities(timeToExpiration[looptest], strike[looptest], forward);
      DoubleMatrix bucketSensi = volComputed.getSensitivities();
      assertEquals("Smile by delta term structure: volatility adjoint", vol, volComputed.getVolatility(), 1.0E-10);
      SmileDeltaParameters[] volData = new SmileDeltaParameters[TIME_TO_EXPIRY.length];
      double[] volBumped = new double[2 * DELTA.size() + 1];
      for (int loopexp = 0; loopexp < TIME_TO_EXPIRY.length; loopexp++) {
        for (int loopsmile = 0; loopsmile < 2 * DELTA.size() + 1; loopsmile++) {
          System.arraycopy(SMILE_TERM.getVolatilityTerm(), 0, volData, 0, TIME_TO_EXPIRY.length);
          System.arraycopy(
              SMILE_TERM.getVolatilityTerm()[loopexp].getVolatility().toArray(), 0, volBumped, 0, 2 * DELTA.size() + 1);
          volBumped[loopsmile] += shift;
          volData[loopexp] = SmileDeltaParameters.of(TIME_TO_EXPIRY[loopexp], DELTA, DoubleArray.copyOf(volBumped));
          SmileDeltaTermStructureParametersStrikeInterpolation smileTermBumped =
              new SmileDeltaTermStructureParametersStrikeInterpolation(volData, INTERPOLATOR_STRIKE);
          bucketTest[loopexp][loopsmile] = (smileTermBumped.getVolatility(timeToExpiration[looptest], strike[looptest], forward) - volComputed.getVolatility()) / shift;
          // FIXME: the strike sensitivity to volatility is missing. To be corrected when [PLAT-1396] is fixed.
          assertEquals("Smile by delta term structure: (test: " + looptest + ") volatility bucket sensitivity " + loopexp + " - " + loopsmile, bucketTest[loopexp][loopsmile],
              bucketSensi.get(loopexp, loopsmile), tolerance[looptest]);
        }
      }
    }
  }

}
