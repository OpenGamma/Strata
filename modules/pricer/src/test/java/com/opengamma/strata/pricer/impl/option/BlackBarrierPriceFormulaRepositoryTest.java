/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link BlackBarrierPriceFormulaRepository}.
 */
@Test
public class BlackBarrierPriceFormulaRepositoryTest {
  private static final ZonedDateTime REFERENCE_DATE = TestHelper.dateUtc(2011, 7, 1);
  private static final ZonedDateTime EXPIRY_DATE = TestHelper.dateUtc(2015, 1, 2);
  private static final double EXPIRY_TIME =
      DayCounts.ACT_ACT_ISDA.relativeYearFraction(REFERENCE_DATE.toLocalDate(), EXPIRY_DATE.toLocalDate());
  private static final double STRIKE_MID = 100;
  private static final double STRIKE_HIGH = 120;
  private static final double STRIKE_LOW = 85;
  private static final double[] STRIKES = new double[] {STRIKE_HIGH, STRIKE_MID, STRIKE_LOW };
  private static final SimpleConstantContinuousBarrier BARRIER_DOWN_IN =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 90);
  private static final SimpleConstantContinuousBarrier BARRIER_DOWN_OUT =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, 90);
  private static final SimpleConstantContinuousBarrier BARRIER_UP_IN =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, 110);
  private static final SimpleConstantContinuousBarrier BARRIER_UP_OUT =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, 110);
  private static final double REBATE = 2;
  private static final double SPOT = 105;
  private static final double RATE_DOM = 0.05; // Domestic rate
  private static final double RATE_FOR = 0.02; // Foreign rate
  private static final double COST_OF_CARRY = RATE_DOM - RATE_FOR; // Domestic - Foreign rate
  private static final double VOLATILITY = 0.20;
  private static final BlackBarrierPriceFormulaRepository BARRIER_PRICER = new BlackBarrierPriceFormulaRepository();

  private static final double DF_FOR = Math.exp(-RATE_FOR * EXPIRY_TIME); // 'Base Ccy
  private static final double DF_DOM = Math.exp(-RATE_DOM * EXPIRY_TIME); // 'Quote Ccy
  private static final double FWD_FX = SPOT * DF_FOR / DF_DOM;

  private static final double TOL = 1.0e-14;
  private static final double EPS_FD = 1.0e-6;

  /**
   * Check "in + out = vanilla" is satisfied.
   */
  public void inOutParity() {
    for (double strike : STRIKES) {
    // call
      double callVanilla = BlackFormulaRepository.price(FWD_FX, strike, EXPIRY_TIME, VOLATILITY, true) * DF_DOM;
    double callUpIn = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, BARRIER_UP_IN);
    double callUpOut = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, BARRIER_UP_OUT);
    double callDownIn = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, BARRIER_DOWN_IN);
    double callDownOut = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, BARRIER_DOWN_OUT);
    assertRelative(callUpIn + callUpOut, callVanilla);
    assertRelative(callDownIn + callDownOut, callVanilla);
    // put
    double putVanilla = BlackFormulaRepository.price(FWD_FX, strike, EXPIRY_TIME, VOLATILITY, false) * DF_DOM;
    double putUpIn = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, BARRIER_UP_IN);
    double putUpOut = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, BARRIER_UP_OUT);
    double putDownIn = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, BARRIER_DOWN_IN);
    double putDownOut = BARRIER_PRICER.price(
          SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, BARRIER_DOWN_OUT);
    assertRelative(putUpIn + putUpOut, putVanilla);
    assertRelative(putDownIn + putDownOut, putVanilla);
    }
  }

  /**
   * Upper barrier level is very high: knock-in is close to 0, knock-out is close to vanilla.
   */
  public void largeBarrierTest() {
    SimpleConstantContinuousBarrier upIn = SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, 1.0e4);
    SimpleConstantContinuousBarrier upOut = SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, 1.0e4);
    for (double strike : STRIKES) {
    // call
      double callVanilla = BlackFormulaRepository.price(FWD_FX, strike, EXPIRY_TIME, VOLATILITY, true) * DF_DOM;
      double callUpIn = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, upIn);
      double callUpOut = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, upOut);
      assertRelative(callUpIn, 0d);
      assertRelative(callUpOut, callVanilla);
    // put
      double putVanilla = BlackFormulaRepository.price(FWD_FX, strike, EXPIRY_TIME, VOLATILITY, false) * DF_DOM;
      double putUpIn = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, upIn);
      double putUpOut = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, upOut);
      assertRelative(putUpIn, 0d);
      assertRelative(putUpOut, putVanilla);
    }
  }

  /**
   * Lower barrier level is very small: knock-in is close to 0, knock-out is close to vanilla.
   */
  public void smallBarrierTest() {
    SimpleConstantContinuousBarrier dwIn =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 0.1d);
    SimpleConstantContinuousBarrier dwOut =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, 0.1d);
    for (double strike : STRIKES) {
    // call
      double callVanilla = BlackFormulaRepository.price(FWD_FX, strike, EXPIRY_TIME, VOLATILITY, true) * DF_DOM;
      double callDwIn = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, dwIn);
      double callDwOut = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, dwOut);
      assertRelative(callDwIn, 0d);
      assertRelative(callDwOut, callVanilla);
    // put
      double putVanilla = BlackFormulaRepository.price(FWD_FX, strike, EXPIRY_TIME, VOLATILITY, false) * DF_DOM;
      double putDwIn = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, dwIn);
      double putDwOut = BARRIER_PRICER.price(SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, dwOut);
      assertRelative(putDwIn, 0d);
      assertRelative(putDwOut, putVanilla);
    }
  }

  /**
   * Greeks against finite difference approximation.
   */
  public void greekfdTest() {
    for (double strike : STRIKES) {
      // call
      testDerivatives(strike, true, BARRIER_UP_IN);
      testDerivatives(strike, true, BARRIER_UP_OUT);
      testDerivatives(strike, true, BARRIER_DOWN_IN);
      testDerivatives(strike, true, BARRIER_DOWN_OUT);
      // put
      testDerivatives(strike, false, BARRIER_UP_IN);
      testDerivatives(strike, false, BARRIER_UP_OUT);
      testDerivatives(strike, false, BARRIER_DOWN_IN);
      testDerivatives(strike, false, BARRIER_DOWN_OUT);
    }
  }

  /**
   * smoothly connected to limiting cases.
   */
  public void smallsigmaTTest() {
    for (double strike : STRIKES) {
      // call
      testSmallValues(strike, true, BARRIER_UP_IN);
      testSmallValues(strike, true, BARRIER_UP_OUT);
      testSmallValues(strike, true, BARRIER_DOWN_IN);
      testSmallValues(strike, true, BARRIER_DOWN_OUT);
      // put
      testSmallValues(strike, false, BARRIER_UP_IN);
      testSmallValues(strike, false, BARRIER_UP_OUT);
      testSmallValues(strike, false, BARRIER_DOWN_IN);
      testSmallValues(strike, false, BARRIER_DOWN_OUT);
    }
  }

  /**
   * Barrier event has occured already.
   */
  public void illegalBarrierLevelTest() {
    assertThrowsIllegalArg(() -> BARRIER_PRICER.price(BARRIER_UP_IN.getBarrierLevel() + 0.1, STRIKE_MID,
        EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, BARRIER_UP_IN));
    assertThrowsIllegalArg(() -> BARRIER_PRICER.price(BARRIER_DOWN_OUT.getBarrierLevel() - 0.1, STRIKE_MID,
        EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, true, BARRIER_DOWN_OUT));
    assertThrowsIllegalArg(() -> BARRIER_PRICER.priceAdjoint(BARRIER_UP_IN.getBarrierLevel() + 0.1, STRIKE_MID,
        EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, BARRIER_UP_IN));
    assertThrowsIllegalArg(() -> BARRIER_PRICER.priceAdjoint(BARRIER_DOWN_OUT.getBarrierLevel() - 0.1, STRIKE_MID,
        EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, false, BARRIER_DOWN_OUT));
  }

  /**
   * Regression to 2.x, including rebate.
   */
  public void adjointPriceRegression() {
    BlackOneTouchCashPriceFormulaRepository rebate = new BlackOneTouchCashPriceFormulaRepository();
    double[] priceDIExp = new double[] {6.625939880275156, 8.17524397035564, 3.51889794875554, 16.046696834562567,
      10.70082805329517, 4.016261046580751 };
    double[] priceDOExp = new double[] {16.801234633074746, 1.2809481492685348, 11.695029389570358, 1.9796398042263066,
      21.122005303422565, 1.2480461457697478 };
    double[] priceUIExp = new double[] {21.738904060619003, 5.660922675994705, 13.534230659666587, 12.751249399664466,
      30.003917380997216, 2.454685902906281 };
    double[] priceUOExp = new double[] {1.8022701280119453, 3.909269118910516, 1.7936963539403596, 5.389086914405454,
      1.9329156510015661, 2.9236209647252656 };
    double[][] derivativesDIExp = new double[][] {
      {-0.256723218835973, -0.21326378136855229, -23.23617273082793, 53.887600866294676, 58.707263782832555 },
      {-0.22502757956883834, 0.3356609584177749, -28.669348717959508, -89.71793740640288, 57.79705127007245 },
      {-0.13240455064001755, -0.10777900727966121, -12.34024486138928, 37.506678277403935, 41.63892946136302 },
      {-0.42819609658445323, 0.441145732506666, -56.273347803397485, -151.04122279937647, 78.46755307304 },
      {-0.38528948548712183, -0.33466444779640325, -37.52619152936393, 62.57455743118484, 68.36140924884158 },
      {-0.10797845731130963, 0.21426029198992397, -14.08442230033797, -47.32420873845068, 39.147069642753685 } };
    double[][] derivativesDOExp = new double[][] {
      {0.925317598744783, -0.2806575880039709, -55.697543854725964, 194.462195344832, 3.192368381065041 },
      {-0.03864414399539151, 0.009587256919136517, -1.270237829323396, -5.21052475720073, 4.102580893825152 },
      {0.6324628371075294, -0.22479677856150546, -37.79085149394349, 148.7848961295844, 31.79584488974962 },
      {-0.004011720421074989, 0.06544806636160204, -3.7204441809560977, -5.9454611683655045, -5.032778721927358 },
      {1.1693201681318741, -0.29024484492310754, -70.84983552060324, 228.28109929421754, -24.681781274058867 },
      {-0.04025696351697804, 0.0, -1.1548554608892951, -5.098392910877228, 4.53255833202904 } };
    double[][] derivativesUIExp = new double[][] {
      {0.6472001227436213, -0.49131423321491496, -76.23506081532145, 247.30828672024398, 60.930906232993976 },
      {0.15101969748879138, 0.2734357730161942, -19.852002808967725, -65.3919684893132, 53.213862714176926 },
      {0.4769152573039112, -0.33257578584116665, -47.46250751883076, 185.24241099218733, 72.3408333224538 },
      {0.28724757364329634, 0.43217422038994247, -44.716710223480845, -110.92464376467034, 67.97645289437169 },
      {0.7893004079366213, -0.6080809040345517, -105.21921711692173, 290.19622455207696, 44.461552265540746 },
      {0.06323542648613031, 0.15666910219655739, -8.608213577315155, -34.903930997004814, 34.230011428672505 } };
    double[][] derivativesUOExp = new double[][] {
      {0.03976906121488867, -0.0026071361576082536, -0.590128468837802, 1.9384002530437727, 0.40226173936432547 },
      {-0.3963166170033215, 0.07181244232071722, -7.979056436920486, -28.639602912129345, 8.119305258181384 },
      {0.041517833213300284, 0.0, -0.5600615351073366, 1.946054176962064, 0.5274768371195269 },
      {-0.7010805865991248, 0.07441957847832553, -13.168554459478084, -45.16514944091054, 4.891857265201665 },
      {0.013105078757830808, -0.016828388684959006, -1.0482826316507563, 1.5563229354864467, -1.3483884822973111 },
      {-0.19309604326471816, 0.05759118979336658, -4.522536882517435, -16.621779890162028, 8.88315235457093 } };

    EuropeanVanillaOption[] options = new EuropeanVanillaOption[] {
      EuropeanVanillaOption.of(STRIKE_MID, EXPIRY_TIME, PutCall.CALL),
      EuropeanVanillaOption.of(STRIKE_MID, EXPIRY_TIME, PutCall.PUT),
      EuropeanVanillaOption.of(STRIKE_HIGH, EXPIRY_TIME, PutCall.CALL),
      EuropeanVanillaOption.of(STRIKE_HIGH, EXPIRY_TIME, PutCall.PUT),
      EuropeanVanillaOption.of(STRIKE_LOW, EXPIRY_TIME, PutCall.CALL),
      EuropeanVanillaOption.of(STRIKE_LOW, EXPIRY_TIME, PutCall.PUT) };
    int n = options.length;

    for (int j = 0; j < n; ++j) {
      // down-in
      double priceDINew = BARRIER_PRICER.price(SPOT, options[j].getStrike(), EXPIRY_TIME, COST_OF_CARRY, RATE_DOM,
          VOLATILITY, options[j].isCall(), BARRIER_DOWN_IN);
      ValueDerivatives priceDIAdjointNew = BARRIER_PRICER.priceAdjoint(SPOT, options[j].getStrike(), EXPIRY_TIME,
          COST_OF_CARRY, RATE_DOM, VOLATILITY, options[j].isCall(), BARRIER_DOWN_IN);
      double priceDIRb = rebate.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_DOWN_OUT);
      ValueDerivatives priceDIAdjointRb =
          rebate.priceAdjoint(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_DOWN_OUT);
      assertRelative(priceDIExp[j], priceDINew + priceDIRb * REBATE);
      assertRelative(priceDIExp[j], priceDIAdjointNew.getValue() + priceDIAdjointRb.getValue() * REBATE);
      // down-out
      double priceDONew = BARRIER_PRICER.price(SPOT, options[j].getStrike(), EXPIRY_TIME, COST_OF_CARRY, RATE_DOM,
          VOLATILITY, options[j].isCall(), BARRIER_DOWN_OUT);
      ValueDerivatives priceDOAdjointNew = BARRIER_PRICER.priceAdjoint(SPOT, options[j].getStrike(), EXPIRY_TIME,
          COST_OF_CARRY, RATE_DOM, VOLATILITY, options[j].isCall(), BARRIER_DOWN_OUT);
      double priceDORb =
          rebate.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_DOWN_IN);
      ValueDerivatives priceDOAdjointRb = rebate.priceAdjoint(
          SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_DOWN_IN);
      assertRelative(priceDOExp[j], priceDONew + priceDORb * REBATE);
      assertRelative(priceDOExp[j], priceDOAdjointNew.getValue() + priceDOAdjointRb.getValue() * REBATE);
      // up-in
      double priceUINew = BARRIER_PRICER.price(SPOT, options[j].getStrike(), EXPIRY_TIME, COST_OF_CARRY, RATE_DOM,
          VOLATILITY, options[j].isCall(), BARRIER_UP_IN);
      ValueDerivatives priceUIAdjointNew = BARRIER_PRICER.priceAdjoint(SPOT, options[j].getStrike(), EXPIRY_TIME,
          COST_OF_CARRY, RATE_DOM, VOLATILITY, options[j].isCall(), BARRIER_UP_IN);
      double priceUIRb = rebate.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_UP_OUT);
      ValueDerivatives priceUIAdjointRb =
          rebate.priceAdjoint(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_UP_OUT);
      assertRelative(priceUIExp[j], priceUINew + priceUIRb * REBATE);
      assertRelative(priceUIExp[j], priceUIAdjointNew.getValue() + priceUIAdjointRb.getValue() * REBATE);
      // up-out
      double priceUONew = BARRIER_PRICER.price(SPOT, options[j].getStrike(), EXPIRY_TIME, COST_OF_CARRY, RATE_DOM,
          VOLATILITY, options[j].isCall(), BARRIER_UP_OUT);
      ValueDerivatives priceUOAdjointNew = BARRIER_PRICER.priceAdjoint(SPOT, options[j].getStrike(), EXPIRY_TIME,
          COST_OF_CARRY, RATE_DOM, VOLATILITY, options[j].isCall(), BARRIER_UP_OUT);
      double priceUORb = rebate.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_UP_IN);
      ValueDerivatives priceUOAdjointRb =
          rebate.priceAdjoint(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_UP_IN);
      assertRelative(priceUOExp[j], priceUONew + priceUORb * REBATE);
      assertRelative(priceUOExp[j], priceUOAdjointNew.getValue() + priceUOAdjointRb.getValue() * REBATE);
      // derivatives
      for (int i = 0; i < 5; ++i) {
        int k = i == 0 ? i : i - 1;
        double rebateDI = i == 1 ? 0d : priceDIAdjointRb.getDerivative(k);
        double rebateDO = i == 1 ? 0d : priceDOAdjointRb.getDerivative(k);
        double rebateUI = i == 1 ? 0d : priceUIAdjointRb.getDerivative(k);
        double rebateUO = i == 1 ? 0d : priceUOAdjointRb.getDerivative(k);
        assertRelative(derivativesDIExp[j][i], priceDIAdjointNew.getDerivative(i) + REBATE * rebateDI);
        assertRelative(derivativesDOExp[j][i], priceDOAdjointNew.getDerivative(i) + REBATE * rebateDO);
        assertRelative(derivativesUIExp[j][i], priceUIAdjointNew.getDerivative(i) + REBATE * rebateUI);
        assertRelative(derivativesUOExp[j][i], priceUOAdjointNew.getDerivative(i) + REBATE * rebateUO);
      }
    }
  }

  //-------------------------------------------------------------------------
  private void assertRelative(double val1, double val2) {
    assertEquals(val1, val2, Math.max(Math.abs(val2), 1d) * TOL);
  }

  private void testDerivatives(double strike, boolean isCall, SimpleConstantContinuousBarrier barrier) {
    ValueDerivatives computed = BARRIER_PRICER.priceAdjoint(
        SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    double spotUp = BARRIER_PRICER.price(
        SPOT + EPS_FD, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    double spotDw = BARRIER_PRICER.price(
        SPOT - EPS_FD, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    double strikeUp = BARRIER_PRICER.price(
        SPOT, strike + EPS_FD, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    double strikeDw = BARRIER_PRICER.price(
        SPOT, strike - EPS_FD, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    double rateUp = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM + EPS_FD, VOLATILITY, isCall, barrier);
    double rateDw = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM - EPS_FD, VOLATILITY, isCall, barrier);
    double costUp = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME, COST_OF_CARRY + EPS_FD, RATE_DOM, VOLATILITY, isCall, barrier);
    double costDw = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME, COST_OF_CARRY - EPS_FD, RATE_DOM, VOLATILITY, isCall, barrier);
    double volUp = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY + EPS_FD, isCall, barrier);
    double volDw = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY - EPS_FD, isCall, barrier);
    double timeUp = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME + EPS_FD, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    double timeDw = BARRIER_PRICER.price(
        SPOT, strike, EXPIRY_TIME - EPS_FD, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    ValueDerivatives spotUp1 = BARRIER_PRICER.priceAdjoint(
        SPOT + EPS_FD, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    ValueDerivatives spotDw1 = BARRIER_PRICER.priceAdjoint(
        SPOT - EPS_FD, strike, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, isCall, barrier);
    assertEquals(computed.getDerivative(0), 0.5 * (spotUp - spotDw) / EPS_FD, EPS_FD);
    assertEquals(computed.getDerivative(1), 0.5 * (strikeUp - strikeDw) / EPS_FD, EPS_FD);
    assertEquals(computed.getDerivative(2), 0.5 * (rateUp - rateDw) / EPS_FD, EPS_FD);
    assertEquals(computed.getDerivative(3), 0.5 * (costUp - costDw) / EPS_FD, EPS_FD);
    assertEquals(computed.getDerivative(4), 0.5 * (volUp - volDw) / EPS_FD, EPS_FD);
    assertEquals(computed.getDerivative(5), 0.5 * (timeUp - timeDw) / EPS_FD, EPS_FD);
    assertEquals(computed.getDerivative(6), 0.5 * (spotUp1.getDerivative(0) - spotDw1.getDerivative(0)) / EPS_FD, EPS_FD);
  }

  private void testSmallValues(double strike, boolean isCall, SimpleConstantContinuousBarrier barrier) {
    // small parameters
    double volUp = 2.0e-3;
    double volDw = 1.0e-3;
    double time = 1.0e-2;
    // price
    double optUp = BARRIER_PRICER.price(SPOT, strike, time, COST_OF_CARRY, RATE_DOM, volUp, isCall, barrier);
    double optDw = BARRIER_PRICER.price(SPOT, strike, time, COST_OF_CARRY, RATE_DOM, volDw, isCall, barrier);
    assertRelative(optUp, optDw);
    // price adjoint
    ValueDerivatives optUpAdj =
        BARRIER_PRICER.priceAdjoint(SPOT, strike, time, COST_OF_CARRY, RATE_DOM, volUp, isCall, barrier);
    ValueDerivatives optDwAdj =
        BARRIER_PRICER.priceAdjoint(SPOT, strike, time, COST_OF_CARRY, RATE_DOM, volDw, isCall, barrier);
    assertRelative(optUpAdj.getValue(), optDwAdj.getValue());
    for (int i = 0; i < 6; ++i) {
      assertRelative(optUpAdj.getDerivative(i), optDwAdj.getDerivative(i));
    }
  }

}
