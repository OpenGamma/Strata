/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.option.BlackBarrierPriceFormulaRepository;
import com.opengamma.strata.pricer.impl.option.BlackOneTouchCashPriceFormulaRepository;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link ConstantContinuousSingleBarrierKnockoutFunction}.
 */
@Test
public class ConstantContinuousSingleBarrierKnockoutFunctionTest {

  private static final double STRIKE = 130d;
  private static final double TIME_TO_EXPIRY = 0.257;
  private static final int NUM = 35;
  private static final double BARRIER = 140d;
  private static final double REBATE_AMOUNT = 5d;
  private static final DoubleArray REBATE = DoubleArray.of(NUM + 1, i -> REBATE_AMOUNT);

  public void test_of() {
    ConstantContinuousSingleBarrierKnockoutFunction test = ConstantContinuousSingleBarrierKnockoutFunction.of(
        STRIKE, TIME_TO_EXPIRY, PutCall.PUT, NUM, BarrierType.UP, BARRIER, REBATE);
    assertEquals(test.getSign(), -1d);
    assertEquals(test.getStrike(), STRIKE);
    assertEquals(test.getTimeToExpiry(), TIME_TO_EXPIRY);
    assertEquals(test.getNumberOfSteps(), NUM);
    assertEquals(test.getBarrierLevel(), BARRIER);
    assertEquals(test.getBarrierLevel(23), BARRIER);
    assertEquals(test.getBarrierType(), BarrierType.UP);
    assertEquals(test.getRebate(), REBATE);
    assertEquals(test.getRebate(14), REBATE_AMOUNT);
  }

  public void test_optionPrice_up() {
    double tol = 1.0e-12;
    ConstantContinuousSingleBarrierKnockoutFunction test = ConstantContinuousSingleBarrierKnockoutFunction.of(
        STRIKE, TIME_TO_EXPIRY, PutCall.PUT, NUM, BarrierType.UP, BARRIER, REBATE);
    double spot = 130d;
    double u = 1.05;
    double d = 0.98;
    double m = Math.sqrt(u * d);
    double up = 0.29;
    double dp = 0.25;
    double mp = 1d - up - dp;
    // test getPayoffAtExpiryTrinomial
    DoubleArray computedPayoff = test.getPayoffAtExpiryTrinomial(spot, d, m);
    int expectedSize = 2 * NUM + 1;
    assertEquals(computedPayoff.size(), expectedSize);
    double[] price = new double[expectedSize];
    for (int i = 0; i < expectedSize; ++i) {
      price[i] = spot * Math.pow(u, 0.5 * i) * Math.pow(d, NUM - 0.5 * i);
    }
    for (int i = 0; i < expectedSize; ++i) {
      double expectedPayoff = price[i] < BARRIER ? Math.max(STRIKE - price[i], 0d) : REBATE_AMOUNT;
      if (i != expectedSize - 1 && price[i] < BARRIER && price[i + 1] > BARRIER) {
        expectedPayoff = 0.5 * ((BARRIER - price[i]) * expectedPayoff + (price[i + 1] - BARRIER) * REBATE_AMOUNT) /
            (price[i + 1] - price[i]) + 0.5 * expectedPayoff;
      }
      assertEquals(computedPayoff.get(i), expectedPayoff, tol);
    }
    // test getNextOptionValues
    double df = 0.92;
    int n = 2;
    DoubleArray values = DoubleArray.of(1.4, 0.9, 0.1, 0.05, 0.0, 0.0, 0.0);
    DoubleArray computedNextValues = test.getNextOptionValues(df, up, mp, dp, values, spot, d, m, n);
    double tmp = df * 0.05 * dp;
    DoubleArray expectedNextValues = DoubleArray.of(
        df * (1.4 * dp + 0.9 * mp + 0.1 * up),
        df * (0.9 * dp + 0.1 * mp + 0.05 * up),
        df * (0.1 * dp + 0.05 * mp),
        0.5 * ((BARRIER / spot - u * m) * tmp + (u * u - BARRIER / spot) * REBATE_AMOUNT) / (u * u - u * m)
            + 0.5 * tmp,
        REBATE_AMOUNT);
    assertTrue(DoubleArrayMath.fuzzyEquals(computedNextValues.toArray(), expectedNextValues.toArray(), tol));
  }

  public void test_optionPrice_down() {
    double tol = 1.0e-12;
    double barrier = 97d;
    ConstantContinuousSingleBarrierKnockoutFunction test = ConstantContinuousSingleBarrierKnockoutFunction.of(
        STRIKE, TIME_TO_EXPIRY, PutCall.CALL, NUM, BarrierType.DOWN, barrier, REBATE);
    double spot = 100d;
    double u = 1.05;
    double d = 0.98;
    double m = Math.sqrt(u * d);
    double up = 0.29;
    double dp = 0.25;
    double mp = 1d - up - dp;
    // test getPayoffAtExpiryTrinomial
    DoubleArray computedPayoff = test.getPayoffAtExpiryTrinomial(spot, d, m);
    int expectedSize = 2 * NUM + 1;
    assertEquals(computedPayoff.size(), expectedSize);
    double[] price = new double[expectedSize];
    for (int i = 0; i < expectedSize; ++i) {
      price[i] = spot * Math.pow(u, 0.5 * i) * Math.pow(d, NUM - 0.5 * i);
    }
    for (int i = 0; i < expectedSize; ++i) {
      double expectedPayoff = price[i] > barrier ? Math.max(price[i] - STRIKE, 0d) : REBATE_AMOUNT;
      if (i != 0 && price[i - 1] < barrier && price[i] > barrier) {
        expectedPayoff = 0.5 * (expectedPayoff * (price[i] - barrier) + REBATE_AMOUNT * (barrier - price[i - 1])) /
                (price[i] - price[i - 1]) + 0.5 * expectedPayoff;
      }
      assertEquals(computedPayoff.get(i), expectedPayoff, tol);
    }
    // test getNextOptionValues
    double df = 0.92;
    int n = 2;
    DoubleArray values = DoubleArray.of(1.4, 0.9, 0.1, 0.05, 0.0, 0.0, 0.0);
    DoubleArray computedNextValues = test.getNextOptionValues(df, up, mp, dp, values, spot, d, m, n);
    double tmp = df * (0.9 * dp + 0.1 * mp + 0.05 * up);
    DoubleArray expectedNextValues = DoubleArray.of(
        REBATE_AMOUNT,
        0.5 * (tmp * (m * d - barrier / spot) + REBATE_AMOUNT * (barrier / spot - d * d)) / (m * d - d * d)
            + 0.5 * tmp,
        df * (0.1 * dp + 0.05 * mp),
        df * 0.05 * dp,
        0.0);
    assertTrue(DoubleArrayMath.fuzzyEquals(computedNextValues.toArray(), expectedNextValues.toArray(), tol));
  }

  private static final TrinomialTree TRINOMIAL_TREE = new TrinomialTree();
  private static final double SPOT = 105.;
  private static final double[] STRIKES = new double[] {81d, 97d, 105d, 105.1, 114d, 128d };
  private static final double TIME = 1.25;
  private static final double[] INTERESTS = new double[] {-0.01, 0.0, 0.05 };
  private static final double[] VOLS = new double[] {0.05, 0.1, 0.25 };
  private static final double[] DIVIDENDS = new double[] {0.0, 0.02 };

  private static final BlackBarrierPriceFormulaRepository BARRIER_PRICER = new BlackBarrierPriceFormulaRepository();
  private static final BlackOneTouchCashPriceFormulaRepository REBATE_PRICER = new BlackOneTouchCashPriceFormulaRepository();

  public void test_trinomialTree_up() {
    int nSteps = 133;
    LatticeSpecification lattice = new CoxRossRubinsteinLatticeSpecification();
    DoubleArray rebate = DoubleArray.of(nSteps + 1, i -> REBATE_AMOUNT);
    double barrierLevel = 135d;
    double tol = 1.0e-2;
    for (boolean isCall : new boolean[] {true, false }) {
      for (double strike : STRIKES) {
        for (double interest : INTERESTS) {
          for (double vol : VOLS) {
            for (double dividend : DIVIDENDS) {
              OptionFunction function = ConstantContinuousSingleBarrierKnockoutFunction.of(
                  strike, TIME, PutCall.ofPut(!isCall), nSteps, BarrierType.UP, barrierLevel, rebate);
              SimpleConstantContinuousBarrier barrier =
                  SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, barrierLevel);
              double exact = REBATE_AMOUNT *
                      REBATE_PRICER.price(SPOT, TIME, interest - dividend, interest, vol, barrier.inverseKnockType())
                  +BARRIER_PRICER.price(SPOT, strike, TIME, interest - dividend, interest, vol, isCall, barrier);
              double computed = TRINOMIAL_TREE.optionPrice(function, lattice, SPOT, vol, interest, dividend);
              assertEquals(computed, exact, Math.max(exact, 1d) * tol);
            }
          }
        }
      }
    }
  }

  public void test_trinomialTree_down() {
    int nSteps = 133;
    LatticeSpecification lattice = new CoxRossRubinsteinLatticeSpecification();
    DoubleArray rebate = DoubleArray.of(nSteps + 1, i -> REBATE_AMOUNT);
    double barrierLevel = 76d;
    double tol = 1.0e-2;
    for (boolean isCall : new boolean[] {true, false }) {
      for (double strike : STRIKES) {
        for (double interest : INTERESTS) {
          for (double vol : VOLS) {
            for (double dividend : DIVIDENDS) {
              OptionFunction function = ConstantContinuousSingleBarrierKnockoutFunction.of(
                  strike, TIME, PutCall.ofPut(!isCall), nSteps, BarrierType.DOWN, barrierLevel, rebate);
              SimpleConstantContinuousBarrier barrier =
                  SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, barrierLevel);
              double exact = REBATE_AMOUNT *
                  REBATE_PRICER.price(SPOT, TIME, interest - dividend, interest, vol, barrier.inverseKnockType())
                  + BARRIER_PRICER.price(SPOT, strike, TIME, interest - dividend, interest, vol, isCall, barrier);
              double computed = TRINOMIAL_TREE.optionPrice(function, lattice, SPOT, vol, interest, dividend);
              assertEquals(computed, exact, Math.max(exact, 1d) * tol);
            }
          }
        }
      }
    }
  }

}
