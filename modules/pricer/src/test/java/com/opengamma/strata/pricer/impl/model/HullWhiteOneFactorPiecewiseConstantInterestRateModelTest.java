/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.model;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.pricer.impl.rate.model.HullWhiteOneFactorPiecewiseConstantInterestRateModel;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParameters;

/**
 * Test {@link HullWhiteOneFactorPiecewiseConstantInterestRateModel}.
 */
@Test
public class HullWhiteOneFactorPiecewiseConstantInterestRateModelTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double MEAN_REVERSION = 0.01;
  private static final DoubleArray VOLATILITY = DoubleArray.of(0.01, 0.011, 0.012, 0.013, 0.014);
  private static final DoubleArray VOLATILITY_TIME = DoubleArray.of(0.5, 1.0, 2.0, 5.0);
  private static final HullWhiteOneFactorPiecewiseConstantParameters MODEL_PARAMETERS =
      HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
  private static final HullWhiteOneFactorPiecewiseConstantInterestRateModel MODEL =
      HullWhiteOneFactorPiecewiseConstantInterestRateModel.DEFAULT;
  private static final DoubleArray DCF_FIXED = DoubleArray.of(0.50, 0.48);
  private static final DoubleArray ALPHA_FIXED = DoubleArray.of(0.02, 0.04);
  private static final DoubleArray DCF_IBOR = DoubleArray.of(-1.0, -0.01, 0.01, -0.01, 0.95);
  private static final DoubleArray ALPHA_IBOR = DoubleArray.of(0.00, 0.01, 0.02, 0.03, 0.04);
  private static final double TOLERANCE_RATE = 1.0E-10;
  private static final double TOLERANCE_RATE_DELTA = 1.0E-8;
  private static final double TOLERANCE_RATE_DELTA2 = 1.0E-7;
  private static final double TOLERANCE_ALPHA = 1E-8;
  private static final IborIndex EURIBOR3M = IborIndices.EUR_EURIBOR_3M;

  /**
   * Tests the class getters.
   */
  public void getter() {
    assertEquals(MEAN_REVERSION, MODEL_PARAMETERS.getMeanReversion());
    for (int loopperiod = 0; loopperiod < VOLATILITY.size(); loopperiod++) {
      assertEquals(VOLATILITY.get(loopperiod), MODEL_PARAMETERS.getVolatility().get(loopperiod));
    }
    double[] volTime = MODEL_PARAMETERS.getVolatilityTime().toArray();
    for (int loopperiod = 0; loopperiod < VOLATILITY_TIME.size(); loopperiod++) {
      assertEquals(VOLATILITY_TIME.get(loopperiod), volTime[loopperiod + 1]);
    }
  }

  /**
   * Tests the class setters.
   */
  public void setter() {
    double volReplaced = 0.02;
    HullWhiteOneFactorPiecewiseConstantParameters param1 = MODEL_PARAMETERS.withLastVolatility(volReplaced);
    assertEquals(volReplaced, param1.getVolatility().get(param1.getVolatility().size() - 1));
    HullWhiteOneFactorPiecewiseConstantParameters param2 =
        MODEL_PARAMETERS.withLastVolatility(VOLATILITY.get(VOLATILITY.size() - 1));
    for (int loopperiod = 0; loopperiod < param2.getVolatility().size(); loopperiod++) {
      assertEquals(VOLATILITY.get(loopperiod), param2.getVolatility().get(loopperiod));
    }
  }

  /**
   * Tests the equal and hash code methods.
   */
  public void equalHash() {
    HullWhiteOneFactorPiecewiseConstantParameters newParameter =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION, VOLATILITY, VOLATILITY_TIME);
    assertTrue(MODEL_PARAMETERS.equals(newParameter));
    assertTrue(MODEL_PARAMETERS.hashCode() == newParameter.hashCode());
    HullWhiteOneFactorPiecewiseConstantParameters modifiedParameter =
        HullWhiteOneFactorPiecewiseConstantParameters.of(MEAN_REVERSION + 0.01, VOLATILITY, VOLATILITY_TIME);
    assertFalse(MODEL_PARAMETERS.equals(modifiedParameter));
  }

  /**
   * Test the future convexity adjustment factor v a hard-coded value.
   */
  public void futureConvexityFactor() {
    LocalDate SPOT_DATE = LocalDate.of(2012, 9, 19);
    LocalDate LAST_TRADING_DATE = EURIBOR3M.calculateFixingFromEffective(SPOT_DATE, REF_DATA);
    LocalDate REFERENCE_DATE = LocalDate.of(2010, 8, 18);
    double tradeLastTime = DayCounts.ACT_ACT_ISDA.relativeYearFraction(REFERENCE_DATE, LAST_TRADING_DATE);
    double fixStartTime = DayCounts.ACT_ACT_ISDA.relativeYearFraction(REFERENCE_DATE, SPOT_DATE);
    double fixEndTime = DayCounts.ACT_ACT_ISDA.relativeYearFraction(
        REFERENCE_DATE, EURIBOR3M.calculateMaturityFromEffective(SPOT_DATE, REF_DATA));
    double factor = MODEL.futuresConvexityFactor(MODEL_PARAMETERS, tradeLastTime, fixStartTime, fixEndTime);
    double expectedFactor = 1.000079130767980;
    assertEquals(expectedFactor, factor, TOLERANCE_RATE);
    // Derivative with respect to volatility parameters
    int nbSigma = MODEL_PARAMETERS.getVolatility().size();
    ValueDerivatives factorDeriv =
        MODEL.futuresConvexityFactorAdjoint(MODEL_PARAMETERS, tradeLastTime, fixStartTime, fixEndTime);
    double factor2 = factorDeriv.getValue();
    double[] sigmaBar = factorDeriv.getDerivatives().toArray();
    assertEquals(factor, factor2, TOLERANCE_RATE);
    double[] sigmaBarExpected = new double[nbSigma];
    double shift = 1E-6;
    for (int loops = 0; loops < nbSigma; loops++) {
      double[] volBumped = VOLATILITY.toArray();
      volBumped[loops] += shift;
      HullWhiteOneFactorPiecewiseConstantParameters parametersBumped = HullWhiteOneFactorPiecewiseConstantParameters
          .of(MEAN_REVERSION, DoubleArray.copyOf(volBumped), VOLATILITY_TIME);
      double factorPlus = MODEL.futuresConvexityFactor(parametersBumped, tradeLastTime, fixStartTime, fixEndTime);
      volBumped[loops] -= 2 * shift;
      parametersBumped = HullWhiteOneFactorPiecewiseConstantParameters.of(
          MEAN_REVERSION, DoubleArray.copyOf(volBumped), VOLATILITY_TIME);
      double factorMinus = MODEL.futuresConvexityFactor(parametersBumped, tradeLastTime, fixStartTime, fixEndTime);
      sigmaBarExpected[loops] = (factorPlus - factorMinus) / (2 * shift);
      assertEquals(sigmaBarExpected[loops], sigmaBar[loops], TOLERANCE_RATE);
    }
  }

  /**
   * Test the payment delay convexity adjustment factor.
   */
  public void paymentDelayConvexityFactor() {
    double startExpiryTime = 1.00;
    double endExpiryTime = 3.00;
    double startFixingPeriod = 3.05;
    double endFixingPeriod = 3.55;
    double paymentTime = 3.45;
    double hwMeanReversion = 0.011;
    // Constant volatility
    double hwEta = 0.02;
    HullWhiteOneFactorPiecewiseConstantParameters parameters = HullWhiteOneFactorPiecewiseConstantParameters.of(
        hwMeanReversion, DoubleArray.of(hwEta), DoubleArray.of());
    double factor1 = (Math.exp(-hwMeanReversion * endFixingPeriod) - Math.exp(-hwMeanReversion * paymentTime)) *
        (Math.exp(-hwMeanReversion * endFixingPeriod) - Math.exp(-hwMeanReversion * startFixingPeriod));
    double num = 2 * Math.pow(hwMeanReversion, 3);
    double factor2 = hwEta * hwEta *
        (Math.exp(2 * hwMeanReversion * endExpiryTime) - Math.exp(2 * hwMeanReversion * startExpiryTime));
    double factorExpected = Math.exp(factor1 * factor2 / num);
    double factorComputed = MODEL.paymentDelayConvexityFactor(parameters, startExpiryTime, endExpiryTime,
        startFixingPeriod, endFixingPeriod, paymentTime);
    assertEquals(factorExpected, factorComputed, TOLERANCE_RATE);
    // Piecewise constant constant volatility
    double[] hwEtaP = new double[] {0.02, 0.021, 0.022, 0.023 };
    double[] hwTime = new double[] {0.5, 1.0, 2.0 };
    HullWhiteOneFactorPiecewiseConstantParameters parametersP = HullWhiteOneFactorPiecewiseConstantParameters.of(
        hwMeanReversion, DoubleArray.copyOf(hwEtaP), DoubleArray.copyOf(hwTime));
    double factorP2 = hwEtaP[2] * hwEtaP[2] *
        (Math.exp(2 * hwMeanReversion * hwTime[2]) - Math.exp(2 * hwMeanReversion * startExpiryTime));
    factorP2 += hwEtaP[3] * hwEtaP[3] *
        (Math.exp(2 * hwMeanReversion * endExpiryTime) - Math.exp(2 * hwMeanReversion * hwTime[2]));
    double factorPExpected = Math.exp(factor1 * factorP2 / num);
    double factorPComputed = MODEL.paymentDelayConvexityFactor(
        parametersP, startExpiryTime, endExpiryTime, startFixingPeriod, endFixingPeriod, paymentTime);
    assertEquals(factorPExpected, factorPComputed, TOLERANCE_RATE);
  }

  /**
   * Test the bond volatility (called alpha) vs a hard-coded value.
   */
  public void alpha() {
    double expiry1 = 0.25;
    double expiry2 = 2.25;
    double numeraire = 10.0;
    double maturity = 9.0;
    double alphaExpected = -0.015191631;
    double alpha = MODEL.alpha(MODEL_PARAMETERS, expiry1, expiry2, numeraire, maturity); //All data
    assertEquals(alphaExpected, alpha, TOLERANCE_ALPHA);
    alphaExpected = -0.015859116;
    alpha = MODEL.alpha(MODEL_PARAMETERS, 0.0, expiry2, numeraire, maturity);//From today
    assertEquals(alphaExpected, alpha, TOLERANCE_ALPHA);
    alphaExpected = 0.111299267;
    alpha = MODEL.alpha(MODEL_PARAMETERS, 0.0, expiry2, expiry2, maturity);// From today with expiry numeraire
    assertEquals(alphaExpected, alpha, TOLERANCE_ALPHA);
    alpha = MODEL.alpha(MODEL_PARAMETERS, 0.0, 0.0, numeraire, maturity); // From 0 to 0
    assertEquals(0.0d, alpha, TOLERANCE_ALPHA);
  }

  /**
   * Test the adjoint algorithmic differentiation version of alpha.
   */
  public void alphaDSigma() {
    double expiry1 = 0.25;
    double expiry2 = 2.25;
    double numeraire = 10.0;
    double maturity = 9.0;
    int nbVolatility = VOLATILITY.size();
    ValueDerivatives alphaDeriv = MODEL.alphaAdjoint(MODEL_PARAMETERS, expiry1, expiry2, numeraire, maturity);
    double alpha = alphaDeriv.getValue();
    double[] alphaDerivatives = alphaDeriv.getDerivatives().toArray();
    double alpha2 = MODEL.alpha(MODEL_PARAMETERS, expiry1, expiry2, numeraire, maturity);
    assertEquals(alpha2, alpha, 1.0E-10);
    double shiftVol = 1.0E-6;
    double[] volatilityBumped = new double[nbVolatility];
    System.arraycopy(VOLATILITY.toArray(), 0, volatilityBumped, 0, nbVolatility);
    double[] alphaBumpedPlus = new double[nbVolatility];
    double[] alphaBumpedMinus = new double[nbVolatility];
    HullWhiteOneFactorPiecewiseConstantParameters parametersBumped;
    for (int loopvol = 0; loopvol < nbVolatility; loopvol++) {
      volatilityBumped[loopvol] += shiftVol;
      parametersBumped = HullWhiteOneFactorPiecewiseConstantParameters.of(
          MEAN_REVERSION, DoubleArray.copyOf(volatilityBumped), VOLATILITY_TIME);
      alphaBumpedPlus[loopvol] = MODEL.alpha(parametersBumped, expiry1, expiry2, numeraire, maturity);
      volatilityBumped[loopvol] -= 2 * shiftVol;
      parametersBumped = HullWhiteOneFactorPiecewiseConstantParameters.of(
          MEAN_REVERSION, DoubleArray.copyOf(volatilityBumped), VOLATILITY_TIME);
      alphaBumpedMinus[loopvol] = MODEL.alpha(parametersBumped, expiry1, expiry2, numeraire, maturity);
      assertEquals((alphaBumpedPlus[loopvol] - alphaBumpedMinus[loopvol]) / (2 * shiftVol), alphaDerivatives[loopvol], 1.0E-9);
      volatilityBumped[loopvol] = VOLATILITY.get(loopvol);
    }
  }

  /**
   * Test the swaption exercise boundary.
   */
  public void kappa() {
    double[] cashFlowAmount = new double[] {-1.0, 0.05, 0.05, 0.05, 0.05, 1.05 };
    double notional = 100000000; // 100m
    double[] cashFlowTime = new double[] {10.0, 11.0, 12.0, 13.0, 14.00, 15.00 };
    double expiryTime = cashFlowTime[0] - 2.0 / 365.0;
    int nbCF = cashFlowAmount.length;
    double[] discountedCashFlow = new double[nbCF];
    double[] alpha = new double[nbCF];
    double rate = 0.04;
    for (int loopcf = 0; loopcf < nbCF; loopcf++) {
      discountedCashFlow[loopcf] = cashFlowAmount[loopcf] * Math.exp(-rate * cashFlowTime[loopcf]) * notional;
      alpha[loopcf] = MODEL.alpha(MODEL_PARAMETERS, 0.0, expiryTime, expiryTime, cashFlowTime[loopcf]);
    }
    double kappa = MODEL.kappa(DoubleArray.copyOf(discountedCashFlow), DoubleArray.copyOf(alpha));
    double swapValue = 0.0;
    for (int loopcf = 0; loopcf < nbCF; loopcf++) {
      swapValue += discountedCashFlow[loopcf] * Math.exp(-Math.pow(alpha[loopcf], 2.0) / 2.0 - alpha[loopcf] * kappa);
    }
    assertEquals(0.0, swapValue, 1.0E-1);
  }

  public void swapRate() {
    double shift = 1.0E-4;
    double x = 0.1;
    double numerator = 0.0;
    for (int loopcf = 0; loopcf < DCF_IBOR.size(); loopcf++) {
      numerator += DCF_IBOR.get(loopcf)
          * Math.exp(-ALPHA_IBOR.get(loopcf) * x - 0.5 * ALPHA_IBOR.get(loopcf) * ALPHA_IBOR.get(loopcf));
    }
    double denominator = 0.0;
    for (int loopcf = 0; loopcf < DCF_FIXED.size(); loopcf++) {
      denominator += DCF_FIXED.get(loopcf)
          * Math.exp(-ALPHA_FIXED.get(loopcf) * x - 0.5 * ALPHA_FIXED.get(loopcf) * ALPHA_FIXED.get(loopcf));
    }
    double swapRateExpected = -numerator / denominator;
    double swapRateComputed = MODEL.swapRate(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    assertEquals(swapRateExpected, swapRateComputed, TOLERANCE_RATE);
    double swapRatePlus = MODEL.swapRate(x + shift, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    double swapRateMinus = MODEL.swapRate(x - shift, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    double swapRateDx1Expected = (swapRatePlus - swapRateMinus) / (2 * shift);
    double swapRateDx1Computed = MODEL.swapRateDx1(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    assertEquals(swapRateDx1Expected, swapRateDx1Computed, TOLERANCE_RATE_DELTA);
    double swapRateDx2Expected = (swapRatePlus + swapRateMinus - 2 * swapRateComputed) / (shift * shift);
    double swapRateDx2Computed = MODEL.swapRateDx2(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    assertEquals(swapRateDx2Expected, swapRateDx2Computed, TOLERANCE_RATE_DELTA2);
  }

  public void swapRateDdcf() {
    double shift = 1.0E-8;
    double x = 0.0;
    ValueDerivatives computed = MODEL.swapRateDdcff1(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    double swapRateComputed = computed.getValue();
    double[] ddcffComputed = computed.getDerivatives().toArray();
    double swapRateExpected = MODEL.swapRate(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    assertEquals(swapRateComputed, swapRateExpected, TOLERANCE_RATE);
    double[] ddcffExpected = new double[DCF_FIXED.size()];
    for (int loopcf = 0; loopcf < DCF_FIXED.size(); loopcf++) {
      double[] dsf_bumped = DCF_FIXED.toArray();
      dsf_bumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRate(x, DoubleArray.copyOf(dsf_bumped), ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
      dsf_bumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRate(x, DoubleArray.copyOf(dsf_bumped), ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
      ddcffExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    assertTrue(DoubleArrayMath.fuzzyEquals(ddcffExpected, ddcffComputed, TOLERANCE_RATE_DELTA));
    double[] ddcfiExpected = new double[DCF_IBOR.size()];
    for (int loopcf = 0; loopcf < DCF_IBOR.size(); loopcf++) {
      double[] dsf_bumped = DCF_IBOR.toArray();
      dsf_bumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRate(x, DCF_FIXED, ALPHA_FIXED, DoubleArray.copyOf(dsf_bumped), ALPHA_IBOR);
      dsf_bumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRate(x, DCF_FIXED, ALPHA_FIXED, DoubleArray.copyOf(dsf_bumped), ALPHA_IBOR);
      ddcfiExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    double[] ddcfiComputed = MODEL.swapRateDdcfi1(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR)
        .getDerivatives().toArray();
    assertTrue(DoubleArrayMath.fuzzyEquals(ddcfiExpected, ddcfiComputed, TOLERANCE_RATE_DELTA));
  }

  public void swapRateDa() {
    double shift = 1.0E-8;
    double x = 0.0;
    ValueDerivatives computed = MODEL.swapRateDaf1(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    double swapRateComputed = computed.getValue();
    double[] dafComputed = computed.getDerivatives().toArray();
    double swapRateExpected = MODEL.swapRate(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    assertEquals(swapRateComputed, swapRateExpected, TOLERANCE_RATE);
    double[] dafExpected = new double[ALPHA_FIXED.size()];
    for (int loopcf = 0; loopcf < ALPHA_FIXED.size(); loopcf++) {
      double[] afBumped = ALPHA_FIXED.toArray();
      afBumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRate(x, DCF_FIXED, DoubleArray.copyOf(afBumped), DCF_IBOR, ALPHA_IBOR);
      afBumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRate(x, DCF_FIXED, DoubleArray.copyOf(afBumped), DCF_IBOR, ALPHA_IBOR);
      dafExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    assertTrue(DoubleArrayMath.fuzzyEquals(dafExpected, dafComputed, TOLERANCE_RATE_DELTA));
    double[] daiExpected = new double[DCF_IBOR.size()];
    for (int loopcf = 0; loopcf < DCF_IBOR.size(); loopcf++) {
      double[] aiBumped = ALPHA_IBOR.toArray();
      aiBumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRate(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, DoubleArray.copyOf(aiBumped));
      aiBumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRate(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, DoubleArray.copyOf(aiBumped));
      daiExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    double[] daiComputed = MODEL.swapRateDai1(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR).getDerivatives().toArray();
    assertTrue(DoubleArrayMath.fuzzyEquals(daiExpected, daiComputed, TOLERANCE_RATE_DELTA));
  }

  public void swapRateDx2Ddcf() {
    double shift = 1.0E-7;
    double x = 0.0;
    Pair<DoubleArray, DoubleArray> dx2ddcfComputed = MODEL.swapRateDx2Ddcf1(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    double[] dx2DdcffExpected = new double[DCF_FIXED.size()];
    for (int loopcf = 0; loopcf < DCF_FIXED.size(); loopcf++) {
      double[] dsf_bumped = DCF_FIXED.toArray();
      dsf_bumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRateDx2(x, DoubleArray.copyOf(dsf_bumped), ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
      dsf_bumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRateDx2(x, DoubleArray.copyOf(dsf_bumped), ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
      dx2DdcffExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    assertTrue(DoubleArrayMath.fuzzyEquals(dx2DdcffExpected, dx2ddcfComputed.getFirst().toArray(), TOLERANCE_RATE_DELTA2));
    double[] dx2DdcfiExpected = new double[DCF_IBOR.size()];
    for (int loopcf = 0; loopcf < DCF_IBOR.size(); loopcf++) {
      double[] dsf_bumped = DCF_IBOR.toArray();
      dsf_bumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRateDx2(x, DCF_FIXED, ALPHA_FIXED, DoubleArray.copyOf(dsf_bumped), ALPHA_IBOR);
      dsf_bumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRateDx2(x, DCF_FIXED, ALPHA_FIXED, DoubleArray.copyOf(dsf_bumped), ALPHA_IBOR);
      dx2DdcfiExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    assertTrue(DoubleArrayMath.fuzzyEquals(dx2DdcfiExpected, dx2ddcfComputed.getSecond().toArray(), TOLERANCE_RATE_DELTA2));
  }

  public void swapRateDx2Da() {
    double shift = 1.0E-7;
    double x = 0.0;
    Pair<DoubleArray, DoubleArray> dx2DaComputed = MODEL.swapRateDx2Da1(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, ALPHA_IBOR);
    double[] dx2DafExpected = new double[DCF_FIXED.size()];
    for (int loopcf = 0; loopcf < DCF_FIXED.size(); loopcf++) {
      double[] afBumped = ALPHA_FIXED.toArray();
      afBumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRateDx2(x, DCF_FIXED, DoubleArray.copyOf(afBumped), DCF_IBOR, ALPHA_IBOR);
      afBumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRateDx2(x, DCF_FIXED, DoubleArray.copyOf(afBumped), DCF_IBOR, ALPHA_IBOR);
      dx2DafExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    assertTrue(DoubleArrayMath.fuzzyEquals(dx2DafExpected, dx2DaComputed.getFirst().toArray(), TOLERANCE_RATE_DELTA2));
    double[] dx2DaiExpected = new double[DCF_IBOR.size()];
    for (int loopcf = 0; loopcf < DCF_IBOR.size(); loopcf++) {
      double[] aiBumped = ALPHA_IBOR.toArray();
      aiBumped[loopcf] += shift;
      double swapRatePlus = MODEL.swapRateDx2(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, DoubleArray.copyOf(aiBumped));
      aiBumped[loopcf] -= 2 * shift;
      double swapRateMinus = MODEL.swapRateDx2(x, DCF_FIXED, ALPHA_FIXED, DCF_IBOR, DoubleArray.copyOf(aiBumped));
      dx2DaiExpected[loopcf] = (swapRatePlus - swapRateMinus) / (2 * shift);
    }
    assertTrue(DoubleArrayMath.fuzzyEquals(dx2DaiExpected, dx2DaComputed.getSecond().toArray(), TOLERANCE_RATE_DELTA2));
  }

  //-------------------------------------------------------------------------
  // Here methods used for Bermudan swaption pricing and Monte-Carlo are test weakly by regression to 2.x.
  // Proper tests should be added when these pricing methodologies are available.
  public void test_beta() {
    double[] theta = new double[] {0.0, 0.9930234298974474, 1.5013698630136987, 1.9917808219178081, 2.5013698630136987,
      2.9972602739726026, 3.5013698630136987, 3.9972602739726026, 4.501220151208923, 4.998487910771765,
      5.495890410958904 };
    double[] expected = new double[] {0.010526360888642377, 0.008653752074472373, 0.008551601997542554,
      0.009479708049949437, 0.009409731278859806, 0.009534948404597303, 0.009504300650429525, 0.009629338816014276,
      0.009613195012744198, 0.010403528524805543 };
    for (int i = 0; i < theta.length - 1; ++i) {
      assertEquals(MODEL.beta(MODEL_PARAMETERS, theta[i], theta[i + 1]), expected[i], TOLERANCE_RATE);
    }
  }

  public void test_lambda() {
    DoubleArray cashFlow = DoubleArray.of(1.1342484780379178E8, 178826.75595605336, -1.1353458434950349E8);
    DoubleArray alphaSq = DoubleArray.of(0.0059638289722142215, 0.0069253776359785415, 0.007985436623619701);
    DoubleArray hwH = DoubleArray.of(5.357967757629822, 5.593630711441366, 5.828706853806842);
    double computed = MODEL.lambda(cashFlow, alphaSq, hwH);
    assertEquals(computed, -0.0034407112369635212, TOLERANCE_RATE);
    double value = 0.0;
    for (int loopcf = 0; loopcf < 3; loopcf++) {
      value += cashFlow.get(loopcf) * Math.exp(-0.5 * alphaSq.get(loopcf) - hwH.get(loopcf) * computed);
    }
    assertEquals(value, 0d, 1.0E-7);
  }

  public void test_volatilityMaturityPart() {
    double u = 5.001332435062505;
    DoubleMatrix v = DoubleMatrix.copyOf(new double[][] {{5.012261396811139, 5.515068493150685, 6.010958904109589,
      6.515068493150685, 7.010958904109589, 7.515068493150685, 8.01095890410959, 8.520458118122614, 9.017725877685455,
      9.515068493150684, 10.013698630136986 } });
    DoubleMatrix computed = MODEL.volatilityMaturityPart(MODEL_PARAMETERS, u, v);
    double[] expected = new double[] {0.010395243419747402, 0.48742124221025085, 0.9555417903726049,
      1.4290478001940943, 1.8925104710768026, 2.361305017379811, 2.8201561576361778, 3.289235677728508,
      3.7447552766260217, 4.198083407732067, 4.650327387669373 };
    assertTrue(DoubleArrayMath.fuzzyEquals(computed.row(0).toArray(), expected, TOLERANCE_RATE));
  }


  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(MODEL);
  }

  public void test_serialization() {
    assertSerialization(MODEL);
  }

  //-------------------------------------------------------------------------
  /**
   * Tests of performance. "enabled = false" for the standard testing.
   */
  @Test(enabled = false)
  public void performanceAlphaAdjoint() {
    double expiry1 = 0.25;
    double expiry2 = 2.25;
    double numeraire = 10.0;
    double maturity = 9.0;
    int nbVolatility = VOLATILITY.size();
    long startTime, endTime;
    int nbTest = 100000;
    double alpha = 0.0;
    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < nbTest; looptest++) {
      alpha = MODEL.alpha(MODEL_PARAMETERS, expiry1, expiry2, numeraire, maturity);
    }
    endTime = System.currentTimeMillis();
    System.out.println(nbTest + " alpha Hull-White: " + (endTime - startTime) + " ms");
    startTime = System.currentTimeMillis();
    for (int looptest = 0; looptest < nbTest; looptest++) {
      ValueDerivatives computed = MODEL.alphaAdjoint(MODEL_PARAMETERS, expiry1, expiry2, numeraire, maturity);
      alpha = computed.getValue();
    }
    endTime = System.currentTimeMillis();
    System.out.println(nbTest + " alpha Hull-White adjoint (value+" + nbVolatility + " derivatives): " +
        (endTime - startTime) + " ms");
    // Performance note: value: 31-Aug-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 75 ms for 1000000 swaptions.
    // Performance note: value+derivatives: 31-Aug-11: On Mac Pro 3.2 GHz Quad-Core Intel Xeon: 100 ms for 1000000 swaptions.
    System.out.println("Alpha: " + alpha);
  }

  /**
   * Test the payment delay convexity adjustment factor. Analysis of the size.
   * In normal test, should have (enabled=false)
   */
  @Test(enabled = false)
  public void paymentDelayConvexityFactorAnalysis() {

    double hwMeanReversion = 0.01;
    double rate = 0.02;

    double[] tenorTime = {0.25, 0.50 };
    int nbTenors = tenorTime.length;
    double[] lagPayTime = {1.0d / 365.0d, 2.0d / 365.0d, 7.0d / 365.0d };
    int nbLags = lagPayTime.length;
    double lagFixTime = 2.0d / 365.0d;
    int nbPeriods = 120;
    double startTimeFirst = 0.25;
    double startTimeStep = 0.25;
    double[] startTime = new double[nbPeriods];
    for (int loopp = 0; loopp < nbPeriods; loopp++) {
      startTime[loopp] = startTimeFirst + loopp * startTimeStep;
    }

    // Constant volatility
    double hwEta = 0.02;
    HullWhiteOneFactorPiecewiseConstantParameters parameters = HullWhiteOneFactorPiecewiseConstantParameters.of(
        hwMeanReversion, DoubleArray.of(hwEta), DoubleArray.of(0));

    double[][][] factor = new double[nbTenors][nbLags][nbPeriods];
    double[][][] adj = new double[nbTenors][nbLags][nbPeriods];
    for (int loopt = 0; loopt < nbTenors; loopt++) {
      for (int loopl = 0; loopl < nbLags; loopl++) {
        for (int loopp = 0; loopp < nbPeriods; loopp++) {
          factor[loopt][loopl][loopp] = MODEL.paymentDelayConvexityFactor(parameters, 0, startTime[loopp] - lagFixTime,
              startTime[loopp], startTime[loopp] + tenorTime[loopt],
              startTime[loopp] + tenorTime[loopt] - lagPayTime[loopl]);
          adj[loopt][loopl][loopp] = (1.0d / tenorTime[loopt] - rate) * (factor[loopt][loopl][loopp] - 1);
        }
      }
    }

    @SuppressWarnings("unused")
    int t = 0;
    t++;
  }

}
