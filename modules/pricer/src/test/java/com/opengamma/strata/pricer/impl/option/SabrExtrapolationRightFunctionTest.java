/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.VolatilityFunctionProvider;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link SabrExtrapolationRightFunction}.
 */
@Test
public class SabrExtrapolationRightFunctionTest {

  private static final double NU = 0.50;
  private static final double RHO = -0.25;
  private static final double BETA = 0.50;
  private static final double ALPHA = 0.05;
  private static final double FORWARD = 0.05;
  private static final SabrFormulaData SABR_DATA = SabrFormulaData.of(ALPHA, BETA, RHO, NU);
  private static final double CUT_OFF_STRIKE = 0.10; // Set low for the test
  private static final double MU = 4.0;
  private static final double TIME_TO_EXPIRY = 2.0;
  private static final SabrExtrapolationRightFunction SABR_EXTRAPOLATION =
      SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, SABR_DATA, CUT_OFF_STRIKE, MU);
  private static final SabrHaganVolatilityFunctionProvider SABR_FUNCTION = SabrHaganVolatilityFunctionProvider.DEFAULT;
  private static final double TOLERANCE_PRICE = 1.0E-10;

  /**
   * Tests getter.
   */
  public void getter() {
    SabrExtrapolationRightFunction func = SabrExtrapolationRightFunction.of(
        FORWARD, SABR_DATA, CUT_OFF_STRIKE, TIME_TO_EXPIRY, MU, SabrHaganVolatilityFunctionProvider.DEFAULT);
    assertEquals(func.getCutOffStrike(), CUT_OFF_STRIKE);
    assertEquals(func.getMu(), MU);
    assertEquals(func.getSabrData(), SABR_DATA);
    assertEquals(func.getTimeToExpiry(), TIME_TO_EXPIRY);
  }

  /**
   * Tests the price for options in SABR model with extrapolation.
   */
  public void price() {
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    double volatilityIn = SABR_FUNCTION.volatility(FORWARD, strikeIn, TIME_TO_EXPIRY, SABR_DATA);
    double priceExpectedIn = BlackFormulaRepository.price(FORWARD, strikeIn, TIME_TO_EXPIRY, volatilityIn, true);
    double priceIn = SABR_EXTRAPOLATION.price(strikeIn, PutCall.CALL);
    assertEquals(priceExpectedIn, priceIn, TOLERANCE_PRICE);
    double volatilityAt = SABR_FUNCTION.volatility(FORWARD, strikeAt, TIME_TO_EXPIRY, SABR_DATA);
    double priceExpectedAt = BlackFormulaRepository.price(FORWARD, strikeAt, TIME_TO_EXPIRY, volatilityAt, true);
    double priceAt = SABR_EXTRAPOLATION.price(strikeAt, PutCall.CALL);
    assertEquals(priceExpectedAt, priceAt, TOLERANCE_PRICE);
    double priceOut = SABR_EXTRAPOLATION.price(strikeOut, PutCall.CALL);
    double priceExpectedOut = 5.427104E-5; // From previous run
    assertEquals(priceExpectedOut, priceOut, TOLERANCE_PRICE);
  }

  /**
   * Tests the price for options in SABR model with extrapolation.
   */
  public void priceCloseToExpiry() {
    double[] timeToExpiry = {1.0 / 365, 0.0}; // One day and on expiry day.
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    for (int loopexp = 0; loopexp < timeToExpiry.length; loopexp++) {
      SabrExtrapolationRightFunction sabrExtra =
          SabrExtrapolationRightFunction.of(FORWARD, timeToExpiry[loopexp], SABR_DATA, CUT_OFF_STRIKE, MU);
      double volatilityIn = SABR_FUNCTION.volatility(FORWARD, strikeIn, timeToExpiry[loopexp], SABR_DATA);
      double priceExpectedIn = BlackFormulaRepository.price(FORWARD, strikeIn, timeToExpiry[loopexp], volatilityIn, true);
      double priceIn = sabrExtra.price(strikeIn, PutCall.CALL);
      assertEquals(priceExpectedIn, priceIn, TOLERANCE_PRICE);
      double volatilityAt = SABR_FUNCTION.volatility(FORWARD, strikeAt, timeToExpiry[loopexp], SABR_DATA);
      double priceExpectedAt = BlackFormulaRepository.price(FORWARD, strikeAt, timeToExpiry[loopexp], volatilityAt, true);
      double priceAt = sabrExtra.price(strikeAt, PutCall.CALL);
      assertEquals(priceExpectedAt, priceAt, TOLERANCE_PRICE);
      double priceOut = sabrExtra.price(strikeOut, PutCall.CALL);
      double priceExpectedOut = 0.0; // From previous run
      assertEquals(priceExpectedOut, priceOut, TOLERANCE_PRICE);
    }
  }

  /**
   * Tests the price derivative with respect to forward for options in SABR model with extrapolation.
   */
  public void priceDerivativeForwardCall() {
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    EuropeanVanillaOption optionIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.CALL);
    double shiftF = 0.000001;
    SabrFormulaData sabrDataFP = SabrFormulaData.of(ALPHA, BETA, RHO, NU);
    SabrExtrapolationRightFunction sabrExtrapolationFP =
        SabrExtrapolationRightFunction.of(FORWARD + shiftF, TIME_TO_EXPIRY, sabrDataFP, CUT_OFF_STRIKE, MU);
    // Below cut-off strike
    double priceIn = SABR_EXTRAPOLATION.price(optionIn.getStrike(), optionIn.getPutCall());
    double priceInFP = sabrExtrapolationFP.price(optionIn.getStrike(), optionIn.getPutCall());
    double priceInDF = SABR_EXTRAPOLATION.priceDerivativeForward(optionIn.getStrike(), optionIn.getPutCall());
    double priceInDFExpected = (priceInFP - priceIn) / shiftF;
    assertEquals(priceInDFExpected, priceInDF, 1E-5);
    // At cut-off strike
    double priceAt = SABR_EXTRAPOLATION.price(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtFP = sabrExtrapolationFP.price(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtDF = SABR_EXTRAPOLATION.priceDerivativeForward(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtDFExpected = (priceAtFP - priceAt) / shiftF;
    assertEquals(priceAtDFExpected, priceAtDF, 1E-6);
    // Above cut-off strike
    double[] abc = SABR_EXTRAPOLATION.getParameter();
    double[] abcDF = SABR_EXTRAPOLATION.getParameterDerivativeForward();
    double[] abcFP = sabrExtrapolationFP.getParameter();
    double[] abcDFExpected = new double[3];
    for (int loopparam = 0; loopparam < 3; loopparam++) {
      abcDFExpected[loopparam] = (abcFP[loopparam] - abc[loopparam]) / shiftF;
      assertEquals(1.0, abcDFExpected[loopparam] / abcDF[loopparam], 5E-2);
    }
    double priceOut = SABR_EXTRAPOLATION.price(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutFP = sabrExtrapolationFP.price(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutDF = SABR_EXTRAPOLATION.priceDerivativeForward(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutDFExpected = (priceOutFP - priceOut) / shiftF;
    assertEquals(priceOutDFExpected, priceOutDF, 1E-5);
  }

  /**
   * Tests the price derivative with respect to forward for options in SABR model with extrapolation.
   */
  public void priceDerivativeForwardPut() {
    SabrExtrapolationRightFunction func = SabrExtrapolationRightFunction.of(
        FORWARD, SABR_DATA, CUT_OFF_STRIKE, TIME_TO_EXPIRY, MU, SabrHaganVolatilityFunctionProvider.DEFAULT);
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    EuropeanVanillaOption optionIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.PUT);
    double shiftF = 0.000001;
    SabrFormulaData sabrDataFP = SabrFormulaData.of(ALPHA, BETA, RHO, NU);
    SabrExtrapolationRightFunction sabrExtrapolationFP =
        SabrExtrapolationRightFunction.of(FORWARD + shiftF, TIME_TO_EXPIRY, sabrDataFP, CUT_OFF_STRIKE, MU);
    // Below cut-off strike
    double priceIn = func.price(optionIn.getStrike(), optionIn.getPutCall());
    double priceInFP = sabrExtrapolationFP.price(optionIn.getStrike(), optionIn.getPutCall());
    double priceInDF = func.priceDerivativeForward(optionIn.getStrike(), optionIn.getPutCall());
    double priceInDFExpected = (priceInFP - priceIn) / shiftF;
    assertEquals(priceInDFExpected, priceInDF, 1E-5);
    // At cut-off strike
    double priceAt = func.price(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtFP = sabrExtrapolationFP.price(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtDF = func.priceDerivativeForward(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtDFExpected = (priceAtFP - priceAt) / shiftF;
    assertEquals(priceAtDFExpected, priceAtDF, 1E-6);
    // Above cut-off strike
    double priceOut = func.price(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutFP = sabrExtrapolationFP.price(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutDF = func.priceDerivativeForward(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutDFExpected = (priceOutFP - priceOut) / shiftF;
    assertEquals(priceOutDFExpected, priceOutDF, 1E-5);
    double[] abc = func.getParameter();
    double[] abcDF = func.getParameterDerivativeForward();
    double[] abcFP = sabrExtrapolationFP.getParameter();
    double[] abcDFExpected = new double[3];
    for (int loopparam = 0; loopparam < 3; loopparam++) {
      abcDFExpected[loopparam] = (abcFP[loopparam] - abc[loopparam]) / shiftF;
      assertEquals(1.0, abcDFExpected[loopparam] / abcDF[loopparam], 5E-2);
    }
  }

  /**
   * Tests the price derivative with respect to forward for options in SABR model with extrapolation.
   */
  public void priceDerivativeStrikeCall() {
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    double shiftK = 0.000001;
    EuropeanVanillaOption optionIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionInKP = EuropeanVanillaOption.of(strikeIn + shiftK, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionAtKP = EuropeanVanillaOption.of(strikeAt + shiftK, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionOutKP = EuropeanVanillaOption.of(strikeOut + shiftK, TIME_TO_EXPIRY, PutCall.CALL);
    // Below cut-off strike
    double priceIn = SABR_EXTRAPOLATION.price(optionIn.getStrike(), optionIn.getPutCall());
    double priceInKP = SABR_EXTRAPOLATION.price(optionInKP.getStrike(), optionInKP.getPutCall());
    double priceInDK = SABR_EXTRAPOLATION.priceDerivativeStrike(optionIn.getStrike(), optionIn.getPutCall());
    double priceInDFExpected = (priceInKP - priceIn) / shiftK;
    assertEquals(priceInDFExpected, priceInDK, 1E-5);
    // At cut-off strike
    double priceAt = SABR_EXTRAPOLATION.price(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtKP = SABR_EXTRAPOLATION.price(optionAtKP.getStrike(), optionAtKP.getPutCall());
    double priceAtDK = SABR_EXTRAPOLATION.priceDerivativeStrike(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtDFExpected = (priceAtKP - priceAt) / shiftK;
    assertEquals(priceAtDFExpected, priceAtDK, 1E-5);
    // At cut-off strike
    double priceOut = SABR_EXTRAPOLATION.price(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutKP = SABR_EXTRAPOLATION.price(optionOutKP.getStrike(), optionOutKP.getPutCall());
    double priceOutDK = SABR_EXTRAPOLATION.priceDerivativeStrike(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutDFExpected = (priceOutKP - priceOut) / shiftK;
    assertEquals(priceOutDFExpected, priceOutDK, 1E-5);
  }

  /**
   * Tests the price derivative with respect to forward for options in SABR model with extrapolation.
   */
  public void priceDerivativeStrikePut() {
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    double shiftK = 0.000001;
    EuropeanVanillaOption optionIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionInKP = EuropeanVanillaOption.of(strikeIn + shiftK, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionAtKP = EuropeanVanillaOption.of(strikeAt + shiftK, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionOutKP = EuropeanVanillaOption.of(strikeOut + shiftK, TIME_TO_EXPIRY, PutCall.PUT);
    // Below cut-off strike
    double priceIn = SABR_EXTRAPOLATION.price(optionIn.getStrike(), optionIn.getPutCall());
    double priceInKP = SABR_EXTRAPOLATION.price(optionInKP.getStrike(), optionInKP.getPutCall());
    double priceInDK = SABR_EXTRAPOLATION.priceDerivativeStrike(optionIn.getStrike(), optionIn.getPutCall());
    double priceInDFExpected = (priceInKP - priceIn) / shiftK;
    assertEquals(priceInDFExpected, priceInDK, 1E-5);
    // At cut-off strike
    double priceAt = SABR_EXTRAPOLATION.price(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtKP = SABR_EXTRAPOLATION.price(optionAtKP.getStrike(), optionAtKP.getPutCall());
    double priceAtDK = SABR_EXTRAPOLATION.priceDerivativeStrike(optionAt.getStrike(), optionAt.getPutCall());
    double priceAtDFExpected = (priceAtKP - priceAt) / shiftK;
    assertEquals(priceAtDFExpected, priceAtDK, 1E-5);
    // At cut-off strike
    double priceOut = SABR_EXTRAPOLATION.price(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutKP = SABR_EXTRAPOLATION.price(optionOutKP.getStrike(), optionOutKP.getPutCall());
    double priceOutDK = SABR_EXTRAPOLATION.priceDerivativeStrike(optionOut.getStrike(), optionOut.getPutCall());
    double priceOutDFExpected = (priceOutKP - priceOut) / shiftK;
    assertEquals(priceOutDFExpected, priceOutDK, 1E-5);
  }

  /**
   * Tests the price derivative with respect to forward for options in SABR model with extrapolation.
   */
  public void priceDerivativeSabrCall() {
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    EuropeanVanillaOption optionIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption optionOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.CALL);
    double shift = 0.000001;
    SabrFormulaData sabrDataAP = SabrFormulaData.of(ALPHA + shift, BETA, RHO, NU);
    SabrFormulaData sabrDataBP = SabrFormulaData.of(ALPHA, BETA + shift, RHO, NU);
    SabrFormulaData sabrDataRP = SabrFormulaData.of(ALPHA, BETA, RHO + shift, NU);
    SabrFormulaData sabrDataNP = SabrFormulaData.of(ALPHA, BETA, RHO, NU + shift);
    SabrExtrapolationRightFunction sabrExtrapolationAP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataAP, CUT_OFF_STRIKE, MU);
    SabrExtrapolationRightFunction sabrExtrapolationBP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataBP, CUT_OFF_STRIKE, MU);
    SabrExtrapolationRightFunction sabrExtrapolationRP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataRP, CUT_OFF_STRIKE, MU);
    SabrExtrapolationRightFunction sabrExtrapolationNP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataNP, CUT_OFF_STRIKE, MU);
    // Below cut-off strike
    double priceInExpected = SABR_EXTRAPOLATION.price(optionIn.getStrike(), optionIn.getPutCall());
    double[] priceInPP = new double[4];
    priceInPP[0] = sabrExtrapolationAP.price(optionIn.getStrike(), optionIn.getPutCall());
    priceInPP[1] = sabrExtrapolationBP.price(optionIn.getStrike(), optionIn.getPutCall());
    priceInPP[2] = sabrExtrapolationRP.price(optionIn.getStrike(), optionIn.getPutCall());
    priceInPP[3] = sabrExtrapolationNP.price(optionIn.getStrike(), optionIn.getPutCall());
    ValueDerivatives resIn = SABR_EXTRAPOLATION.priceAdjointSabr(optionIn.getStrike(), optionIn.getPutCall());
    double priceIn = resIn.getValue();
    double[] priceInDsabr = resIn.getDerivatives().toArray();
    assertEquals(priceInExpected, priceIn, TOLERANCE_PRICE);
    double[] priceInDsabrExpected = new double[4];
    for (int loopparam = 0; loopparam < 3; loopparam++) {
      priceInDsabrExpected[loopparam] = (priceInPP[loopparam] - priceIn) / shift;
      assertEquals(priceInDsabrExpected[loopparam], priceInDsabr[loopparam], 1E-5);
    }
    // At cut-off strike
    double priceAtExpected = SABR_EXTRAPOLATION.price(optionAt.getStrike(), optionAt.getPutCall());
    double[] priceAtPP = new double[4];
    priceAtPP[0] = sabrExtrapolationAP.price(optionAt.getStrike(), optionAt.getPutCall());
    priceAtPP[1] = sabrExtrapolationBP.price(optionAt.getStrike(), optionAt.getPutCall());
    priceAtPP[2] = sabrExtrapolationRP.price(optionAt.getStrike(), optionAt.getPutCall());
    priceAtPP[3] = sabrExtrapolationNP.price(optionAt.getStrike(), optionAt.getPutCall());
    ValueDerivatives resAt = SABR_EXTRAPOLATION.priceAdjointSabr(optionAt.getStrike(), optionAt.getPutCall());
    double priceAt = resAt.getValue();
    double[] priceAtDsabr = resAt.getDerivatives().toArray();
    assertEquals(priceAtExpected, priceAt, TOLERANCE_PRICE);
    double[] priceAtDsabrExpected = new double[4];
    for (int loopparam = 0; loopparam < 3; loopparam++) {
      priceAtDsabrExpected[loopparam] = (priceAtPP[loopparam] - priceAt) / shift;
      assertEquals(priceAtDsabrExpected[loopparam], priceAtDsabr[loopparam], 1E-5);
    }
    // Above cut-off strike
    double[] abc = SABR_EXTRAPOLATION.getParameter();
    double[][] abcDP = SABR_EXTRAPOLATION.getParameterDerivativeSabr();
    double[][] abcPP = new double[4][3];
    abcPP[0] = sabrExtrapolationAP.getParameter();
    abcPP[1] = sabrExtrapolationBP.getParameter();
    abcPP[2] = sabrExtrapolationRP.getParameter();
    abcPP[3] = sabrExtrapolationNP.getParameter();
    double[][] abcDPExpected = new double[4][3];
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      for (int loopabc = 0; loopabc < 3; loopabc++) {
        abcDPExpected[loopparam][loopabc] = (abcPP[loopparam][loopabc] - abc[loopabc]) / shift;
        assertEquals(1.0, abcDPExpected[loopparam][loopabc] / abcDP[loopparam][loopabc], 5.0E-2);
      }
    }
    double priceOutExpected = SABR_EXTRAPOLATION.price(optionOut.getStrike(), optionOut.getPutCall());
    double[] priceOutPP = new double[4];
    priceOutPP[0] = sabrExtrapolationAP.price(optionOut.getStrike(), optionOut.getPutCall());
    priceOutPP[1] = sabrExtrapolationBP.price(optionOut.getStrike(), optionOut.getPutCall());
    priceOutPP[2] = sabrExtrapolationRP.price(optionOut.getStrike(), optionOut.getPutCall());
    priceOutPP[3] = sabrExtrapolationNP.price(optionOut.getStrike(), optionOut.getPutCall());
    ValueDerivatives resOut = SABR_EXTRAPOLATION.priceAdjointSabr(optionOut.getStrike(), optionOut.getPutCall());
    double priceOut = resOut.getValue();
    double[] priceOutDsabr = resOut.getDerivatives().toArray();
    assertEquals(priceOutExpected, priceOut, TOLERANCE_PRICE);
    double[] priceOutDsabrExpected = new double[4];
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      priceOutDsabrExpected[loopparam] = (priceOutPP[loopparam] - priceOut) / shift;
      assertEquals(1.0, priceOutDsabrExpected[loopparam] / priceOutDsabr[loopparam], 4.0E-4);
    }
  }

  /**
   * Tests the price derivative with respect to forward for options in SABR model with extrapolation.
   */
  public void priceDerivativeSabrPut() {
    SabrExtrapolationRightFunction func = SabrExtrapolationRightFunction.of(
        FORWARD, SABR_DATA, CUT_OFF_STRIKE, TIME_TO_EXPIRY, MU, SabrHaganVolatilityFunctionProvider.DEFAULT);
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    EuropeanVanillaOption optionIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption optionOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.PUT);
    double shift = 0.000001;
    SabrFormulaData sabrDataAP = SabrFormulaData.of(ALPHA + shift, BETA, RHO, NU);
    SabrFormulaData sabrDataBP = SabrFormulaData.of(ALPHA, BETA + shift, RHO, NU);
    SabrFormulaData sabrDataRP = SabrFormulaData.of(ALPHA, BETA, RHO + shift, NU);
    SabrFormulaData sabrDataNP = SabrFormulaData.of(ALPHA, BETA, RHO, NU + shift);
    SabrExtrapolationRightFunction sabrExtrapolationAP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataAP, CUT_OFF_STRIKE, MU);
    SabrExtrapolationRightFunction sabrExtrapolationBP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataBP, CUT_OFF_STRIKE, MU);
    SabrExtrapolationRightFunction sabrExtrapolationRP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataRP, CUT_OFF_STRIKE, MU);
    SabrExtrapolationRightFunction sabrExtrapolationNP =
        SabrExtrapolationRightFunction.of(FORWARD, TIME_TO_EXPIRY, sabrDataNP, CUT_OFF_STRIKE, MU);
    // Below cut-off strike
    double priceInExpected = func.price(optionIn.getStrike(), optionIn.getPutCall());
    double[] priceInPP = new double[4];
    priceInPP[0] = sabrExtrapolationAP.price(optionIn.getStrike(), optionIn.getPutCall());
    priceInPP[1] = sabrExtrapolationBP.price(optionIn.getStrike(), optionIn.getPutCall());
    priceInPP[2] = sabrExtrapolationRP.price(optionIn.getStrike(), optionIn.getPutCall());
    priceInPP[3] = sabrExtrapolationNP.price(optionIn.getStrike(), optionIn.getPutCall());
    ValueDerivatives resIn = func.priceAdjointSabr(optionIn.getStrike(), optionIn.getPutCall());
    double priceIn = resIn.getValue();
    double[] priceInDsabr = resIn.getDerivatives().toArray();
    assertEquals(priceInExpected, priceIn, TOLERANCE_PRICE);
    double[] priceInDsabrExpected = new double[4];
    for (int loopparam = 0; loopparam < 3; loopparam++) {
      priceInDsabrExpected[loopparam] = (priceInPP[loopparam] - priceIn) / shift;
      assertEquals(priceInDsabrExpected[loopparam], priceInDsabr[loopparam], 1E-5);
    }
    // At cut-off strike
    double priceAtExpected = func.price(optionAt.getStrike(), optionAt.getPutCall());
    double[] priceAtPP = new double[4];
    priceAtPP[0] = sabrExtrapolationAP.price(optionAt.getStrike(), optionAt.getPutCall());
    priceAtPP[1] = sabrExtrapolationBP.price(optionAt.getStrike(), optionAt.getPutCall());
    priceAtPP[2] = sabrExtrapolationRP.price(optionAt.getStrike(), optionAt.getPutCall());
    priceAtPP[3] = sabrExtrapolationNP.price(optionAt.getStrike(), optionAt.getPutCall());
    ValueDerivatives resAt = func.priceAdjointSabr(optionAt.getStrike(), optionAt.getPutCall());
    double priceAt = resAt.getValue();
    double[] priceAtDsabr = resAt.getDerivatives().toArray();
    assertEquals(priceAtExpected, priceAt, TOLERANCE_PRICE);
    double[] priceAtDsabrExpected = new double[4];
    for (int loopparam = 0; loopparam < 3; loopparam++) {
      priceAtDsabrExpected[loopparam] = (priceAtPP[loopparam] - priceAt) / shift;
      assertEquals(priceAtDsabrExpected[loopparam], priceAtDsabr[loopparam], 1E-5);
    }
    // Above cut-off strike
    double priceOutExpected = func.price(optionOut.getStrike(), optionOut.getPutCall());
    double[] priceOutPP = new double[4];
    priceOutPP[0] = sabrExtrapolationAP.price(optionOut.getStrike(), optionOut.getPutCall());
    priceOutPP[1] = sabrExtrapolationBP.price(optionOut.getStrike(), optionOut.getPutCall());
    priceOutPP[2] = sabrExtrapolationRP.price(optionOut.getStrike(), optionOut.getPutCall());
    priceOutPP[3] = sabrExtrapolationNP.price(optionOut.getStrike(), optionOut.getPutCall());
    ValueDerivatives resOut = func.priceAdjointSabr(optionOut.getStrike(), optionOut.getPutCall());
    double priceOut = resOut.getValue();
    double[] priceOutDsabr = resOut.getDerivatives().toArray();
    assertEquals(priceOutExpected, priceOut, TOLERANCE_PRICE);
    double[] abc = func.getParameter();
    double[][] abcDP = func.getParameterDerivativeSabr();
    double[][] abcPP = new double[4][3];
    abcPP[0] = sabrExtrapolationAP.getParameter();
    abcPP[1] = sabrExtrapolationBP.getParameter();
    abcPP[2] = sabrExtrapolationRP.getParameter();
    abcPP[3] = sabrExtrapolationNP.getParameter();
    double[][] abcDPExpected = new double[4][3];
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      for (int loopabc = 0; loopabc < 3; loopabc++) {
        abcDPExpected[loopparam][loopabc] = (abcPP[loopparam][loopabc] - abc[loopabc]) / shift;
        assertEquals(1.0, abcDPExpected[loopparam][loopabc] / abcDP[loopparam][loopabc], 5.0E-2);
      }
    }
    double[] priceOutDsabrExpected = new double[4];
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      priceOutDsabrExpected[loopparam] = (priceOutPP[loopparam] - priceOut) / shift;
      assertEquals(1.0, priceOutDsabrExpected[loopparam] / priceOutDsabr[loopparam], 4.0E-4);
    }
  }

  /**
   * Tests the price derivative with respect to forward for options in SABR model with extrapolation. Other data.
   */
  public void priceDerivativeSABR2() {
    double alpha = 0.06;
    double beta = 0.5;
    double rho = 0.0;
    double nu = 0.3;
    double cutOff = 0.10;
    double mu = 2.5;
    double strike = 0.15;
    double t = 2.366105247;
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, t, PutCall.CALL);
    SabrFormulaData sabrData = SabrFormulaData.of(alpha, beta, rho, nu);
    double forward = 0.0404500579038675;
    SabrExtrapolationRightFunction sabrExtrapolation =
        SabrExtrapolationRightFunction.of(forward, t, sabrData, cutOff, mu);
    double shift = 0.000001;
    SabrFormulaData sabrDataAP = SabrFormulaData.of(alpha + shift, beta, rho, nu);
    SabrFormulaData sabrDataBP = SabrFormulaData.of(alpha, beta + shift, rho, nu);
    SabrFormulaData sabrDataRP = SabrFormulaData.of(alpha, beta, rho + shift, nu);
    SabrFormulaData sabrDataNP = SabrFormulaData.of(alpha, beta, rho, nu + shift);
    SabrExtrapolationRightFunction sabrExtrapolationAP =
        SabrExtrapolationRightFunction.of(forward, t, sabrDataAP, cutOff, mu);
    SabrExtrapolationRightFunction sabrExtrapolationBP =
        SabrExtrapolationRightFunction.of(forward, t, sabrDataBP, cutOff, mu);
    SabrExtrapolationRightFunction sabrExtrapolationRP =
        SabrExtrapolationRightFunction.of(forward, t, sabrDataRP, cutOff, mu);
    SabrExtrapolationRightFunction sabrExtrapolationNP =
        SabrExtrapolationRightFunction.of(forward, t, sabrDataNP, cutOff, mu);
    // Above cut-off strike
    double[] abc = sabrExtrapolation.getParameter();
    double[][] abcDP = sabrExtrapolation.getParameterDerivativeSabr();
    double[][] abcPP = new double[4][3];
    abcPP[0] = sabrExtrapolationAP.getParameter();
    abcPP[1] = sabrExtrapolationBP.getParameter();
    abcPP[2] = sabrExtrapolationRP.getParameter();
    abcPP[3] = sabrExtrapolationNP.getParameter();
    double[][] abcDPExpected = new double[4][3];
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      for (int loopabc = 0; loopabc < 3; loopabc++) {
        abcDPExpected[loopparam][loopabc] = (abcPP[loopparam][loopabc] - abc[loopabc]) / shift;
        assertEquals(1.0, abcDPExpected[loopparam][loopabc] / abcDP[loopparam][loopabc], 5.0E-2);
      }
    }
    double priceOutExpected = sabrExtrapolation.price(option.getStrike(), option.getPutCall());
    double[] priceOutPP = new double[4];
    priceOutPP[0] = sabrExtrapolationAP.price(option.getStrike(), option.getPutCall());
    priceOutPP[1] = sabrExtrapolationBP.price(option.getStrike(), option.getPutCall());
    priceOutPP[2] = sabrExtrapolationRP.price(option.getStrike(), option.getPutCall());
    priceOutPP[3] = sabrExtrapolationNP.price(option.getStrike(), option.getPutCall());
    ValueDerivatives resOut = sabrExtrapolation.priceAdjointSabr(option.getStrike(), option.getPutCall());
    double priceOut = resOut.getValue();
    double[] priceOutDsabr = resOut.getDerivatives().toArray();
    assertEquals(priceOutExpected, priceOut, 1E-5);
    double[] priceOutDsabrExpected = new double[4];
    for (int loopparam = 0; loopparam < 4; loopparam++) {
      priceOutDsabrExpected[loopparam] = (priceOutPP[loopparam] - priceOut) / shift;
      assertEquals(1.0, priceOutDsabrExpected[loopparam] / priceOutDsabr[loopparam], 4.0E-4);
    }
  }

  /**
   * Tests the price put/call parity for options in SABR model with extrapolation.
   */
  public void pricePutCallParity() {
    double strikeIn = 0.08;
    double strikeAt = CUT_OFF_STRIKE;
    double strikeOut = 0.12;
    EuropeanVanillaOption callIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption putIn = EuropeanVanillaOption.of(strikeIn, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption callAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption putAt = EuropeanVanillaOption.of(strikeAt, TIME_TO_EXPIRY, PutCall.PUT);
    EuropeanVanillaOption callOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.CALL);
    EuropeanVanillaOption putOut = EuropeanVanillaOption.of(strikeOut, TIME_TO_EXPIRY, PutCall.PUT);
    double priceCallIn = SABR_EXTRAPOLATION.price(callIn.getStrike(), callIn.getPutCall());
    double pricePutIn = SABR_EXTRAPOLATION.price(putIn.getStrike(), putIn.getPutCall());
    assertEquals(FORWARD - strikeIn, priceCallIn - pricePutIn, TOLERANCE_PRICE);
    double priceCallAt = SABR_EXTRAPOLATION.price(callAt.getStrike(), callAt.getPutCall());
    double pricePutAt = SABR_EXTRAPOLATION.price(putAt.getStrike(), putAt.getPutCall());
    assertEquals(FORWARD - strikeAt, priceCallAt - pricePutAt, TOLERANCE_PRICE);
    double priceCallOut = SABR_EXTRAPOLATION.price(callOut.getStrike(), callOut.getPutCall());
    double pricePutOut = SABR_EXTRAPOLATION.price(putOut.getStrike(), putOut.getPutCall());
    assertEquals(FORWARD - strikeOut, priceCallOut - pricePutOut, TOLERANCE_PRICE);
  }

  /**
   * Tests that the smile and its derivatives are smooth enough in SABR model with extrapolation.
   */
  public void smileSmooth() {
    int nbPts = 100;
    double rangeStrike = 0.02;
    double[] price = new double[nbPts + 1];
    double[] strike = new double[nbPts + 1];
    for (int looppts = 0; looppts <= nbPts; looppts++) {
      strike[looppts] = CUT_OFF_STRIKE - rangeStrike + looppts * 2.0 * rangeStrike / nbPts;
      EuropeanVanillaOption option = EuropeanVanillaOption.of(strike[looppts], TIME_TO_EXPIRY, PutCall.CALL);
      price[looppts] = SABR_EXTRAPOLATION.price(option.getStrike(), option.getPutCall());
    }
    double[] priceD = new double[nbPts];
    double[] priceD2 = new double[nbPts];
    for (int looppts = 1; looppts < nbPts; looppts++) {
      priceD[looppts] = (price[looppts + 1] - price[looppts - 1]) / (strike[looppts + 1] - strike[looppts - 1]);
      priceD2[looppts] = (price[looppts + 1] + price[looppts - 1] - 2 * price[looppts]) /
          ((strike[looppts + 1] - strike[looppts]) * (strike[looppts + 1] - strike[looppts]));
    }
    for (int looppts = 2; looppts < nbPts; looppts++) {
      assertEquals(priceD[looppts - 1], priceD[looppts], 1.5E-3);
      assertEquals(priceD2[looppts - 1], priceD2[looppts], 1.5E-1);
    }
  }

  /**
   * Tests that the smile and its derivatives are smooth enough in SABR model with extrapolation 
   * for different time to maturity (in particular close to maturity).
   */
  public void smileSmoothMaturity() {
    int nbPts = 100;
    double[] timeToExpiry = new double[] {2.0, 1.0, 0.50, 0.25, 1.0d / 12.0d, 1.0d / 52.0d, 1.0d / 365d};
    int nbTTM = timeToExpiry.length;
    double rangeStrike = 0.02;
    double[] strike = new double[nbPts + 1];
    for (int looppts = 0; looppts <= nbPts; looppts++) {
      strike[looppts] = CUT_OFF_STRIKE - rangeStrike + looppts * 2.0 * rangeStrike / nbPts;
    }
    SabrExtrapolationRightFunction[] sabrExtrapolation = new SabrExtrapolationRightFunction[nbTTM];
    for (int loopmat = 0; loopmat < nbTTM; loopmat++) {
      sabrExtrapolation[loopmat] = SabrExtrapolationRightFunction.of(FORWARD, timeToExpiry[loopmat], SABR_DATA,
          CUT_OFF_STRIKE, MU);
    }
    double[][] price = new double[nbTTM][nbPts + 1];
    for (int loopmat = 0; loopmat < nbTTM; loopmat++) {
      for (int looppts = 0; looppts <= nbPts; looppts++) {
        EuropeanVanillaOption option = EuropeanVanillaOption.of(strike[looppts], timeToExpiry[loopmat], PutCall.CALL);
        price[loopmat][looppts] = sabrExtrapolation[loopmat].price(option.getStrike(), option.getPutCall());
      }
    }
    double[][] priceD = new double[nbTTM][nbPts - 1];
    double[][] priceD2 = new double[nbTTM][nbPts - 1];
    for (int loopmat = 0; loopmat < nbTTM; loopmat++) {
      for (int looppts = 1; looppts < nbPts; looppts++) {
        priceD[loopmat][looppts - 1] = (price[loopmat][looppts + 1] - price[loopmat][looppts - 1]) /
            (strike[looppts + 1] - strike[looppts - 1]);
        priceD2[loopmat][looppts - 1] = (price[loopmat][looppts + 1] + price[loopmat][looppts - 1] - 2 * price[loopmat][looppts])
            / ((strike[looppts + 1] - strike[looppts]) * (strike[looppts + 1] - strike[looppts]));
      }
    }
    double epsDensity = 1.0E-20; // Conditions are not checked when the density is very small.
    for (int loopmat = 0; loopmat < nbTTM; loopmat++) {
      for (int looppts = 1; looppts < nbPts - 1; looppts++) {
        assertTrue(((priceD[loopmat][looppts] / priceD[loopmat][looppts - 1] < 1) && (priceD[loopmat][looppts] /
            priceD[loopmat][looppts - 1] > 0.50)) || Math.abs(priceD2[loopmat][looppts]) < epsDensity);
        assertTrue(priceD2[loopmat][looppts] > 0 || Math.abs(priceD2[loopmat][looppts]) < epsDensity);
        assertTrue((priceD2[loopmat][looppts] / priceD2[loopmat][looppts - 1] < 1 && priceD2[loopmat][looppts] /
            priceD2[loopmat][looppts - 1] > 0.50) || Math.abs(priceD2[loopmat][looppts]) < epsDensity);
      }
    }
  }

  private static final double EPS = 1.0e-6;
  private static final SabrHaganVolatilityFunctionProvider FUNC_HAGAN = SabrHaganVolatilityFunctionProvider.DEFAULT;
  @SuppressWarnings("unchecked")
  private static final VolatilityFunctionProvider<SabrFormulaData>[] FUNCTIONS =
      new VolatilityFunctionProvider[] {FUNC_HAGAN}; // other volatility functions to be added

  /**
   * Testing C2 continuity.
   */
  @Test
  public void smoothnessTest() {
    for (VolatilityFunctionProvider<SabrFormulaData> func : FUNCTIONS) {
      SabrExtrapolationRightFunction extrapolation =
          SabrExtrapolationRightFunction.of(FORWARD, SABR_DATA, CUT_OFF_STRIKE, TIME_TO_EXPIRY, MU, func);
      for (PutCall isCall : new PutCall[] {PutCall.CALL, PutCall.PUT}) {
        double priceBase = extrapolation.price(CUT_OFF_STRIKE, isCall);
        double priceUp = extrapolation.price(CUT_OFF_STRIKE + EPS, isCall);
        double priceDw = extrapolation.price(CUT_OFF_STRIKE - EPS, isCall);
        assertEquals(priceBase, priceUp, EPS);
        assertEquals(priceBase, priceDw, EPS);
        double priceUpUp = extrapolation.price(CUT_OFF_STRIKE + 2.0 * EPS, isCall);
        double priceDwDw = extrapolation.price(CUT_OFF_STRIKE - 2.0 * EPS, isCall);
        double firstUp = (-0.5 * priceUpUp + 2.0 * priceUp - 1.5 * priceBase) / EPS;
        double firstDw = (-2.0 * priceDw + 0.5 * priceDwDw + 1.5 * priceBase) / EPS;
        assertEquals(firstDw, firstUp, EPS);
        // The second derivative values are poorly connected due to finite difference approximation 
        double firstUpUp = 0.5 * (priceUpUp - priceBase) / EPS;
        double firstDwDw = 0.5 * (priceBase - priceDwDw) / EPS;
        double secondUp = (firstUpUp - firstUp) / EPS;
        double secondDw = (firstDw - firstDwDw) / EPS;
        double secondRef = 0.5 * (firstUpUp - firstDwDw) / EPS;
        assertEquals(secondRef, secondUp, secondRef * 0.15);
        assertEquals(secondRef, secondDw, secondRef * 0.15);
      }
    }
  }

  /**
   * Test small forward.
   */
  @Test
  public void smallForwardTest() {
    double smallForward = 0.1e-6;
    double smallCutoff = 0.9e-6;
    for (VolatilityFunctionProvider<SabrFormulaData> func : FUNCTIONS) {
      SabrExtrapolationRightFunction right =
          SabrExtrapolationRightFunction.of(smallForward, SABR_DATA, smallCutoff, TIME_TO_EXPIRY, MU, func);
      for (PutCall isCall : new PutCall[] {PutCall.CALL, PutCall.PUT}) {
        double priceBase = right.price(smallCutoff, isCall);
        double priceUp = right.price(smallCutoff + EPS * 0.1, isCall);
        double priceDw = right.price(smallCutoff - EPS * 0.1, isCall);
        assertEquals(priceBase, priceUp, EPS * 10.0);
        assertEquals(priceBase, priceDw, EPS * 10.0);
      }
    }
  }

  /**
   * Extrapolator is not calibrated in this case, then the gap may be produced at the cutoff.
   */
  @Test
  public void smallExpiryTest() {
    double smallExpiry = 0.5e-6;
    for (VolatilityFunctionProvider<SabrFormulaData> func : FUNCTIONS) {
      SabrExtrapolationRightFunction right =
          SabrExtrapolationRightFunction.of(FORWARD * 0.01, SABR_DATA, CUT_OFF_STRIKE, smallExpiry, MU, func);
      for (PutCall isCall : new PutCall[] {PutCall.CALL, PutCall.PUT}) {
        double priceBase = right.price(CUT_OFF_STRIKE, isCall);
        double priceUp = right.price(CUT_OFF_STRIKE + EPS * 0.1, isCall);
        double priceDw = right.price(CUT_OFF_STRIKE - EPS * 0.1, isCall);
        assertEquals(priceBase, priceUp, EPS);
        assertEquals(priceBase, priceDw, EPS);
        assertEquals(right.getParameter()[0], -1.0E4, 1.e-12);
        assertEquals(right.getParameter()[1], 0.0, 1.e-12);
        assertEquals(right.getParameter()[2], 0.0, 1.e-12);
      }
    }
  }

}
