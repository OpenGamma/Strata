/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;

/**
 * Test {@link SmileDeltaParameters}.
 */
@Test
public class SmileDeltaParametersTest {

  private static final double TIME_TO_EXPIRY = 2.0;
  private static final double FORWARD = 1.40;
  private static final double ATM = 0.185;
  private static final DoubleArray DELTA = DoubleArray.of(0.10, 0.25);
  private static final DoubleArray RISK_REVERSAL = DoubleArray.of(-0.0130, -0.0050);
  private static final DoubleArray STRANGLE = DoubleArray.of(0.0300, 0.0100);

  private static final SmileDeltaParameters SMILE = SmileDeltaParameters.of(
      TIME_TO_EXPIRY, ATM, DELTA, RISK_REVERSAL, STRANGLE);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDelta() {
    SmileDeltaParameters.of(TIME_TO_EXPIRY, ATM, null, RISK_REVERSAL, STRANGLE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRRLength() {
    SmileDeltaParameters.of(TIME_TO_EXPIRY, ATM, DELTA, DoubleArray.filled(3), STRANGLE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testStrangleLength() {
    SmileDeltaParameters.of(TIME_TO_EXPIRY, ATM, DELTA, RISK_REVERSAL, DoubleArray.filled(3));
  }

  /**
   * Tests the constructor directly from volatilities (not RR and S).
   */
  public void constructorVolatility() {
    DoubleArray volatility = SMILE.getVolatility();
    SmileDeltaParameters smileFromVolatility = SmileDeltaParameters.of(TIME_TO_EXPIRY, DELTA, volatility);
    assertEquals("Smile by delta: constructor", SMILE, smileFromVolatility);
  }

  /**
   * Tests the getters.
   */
  public void getter() {
    assertEquals("Smile by delta: time to expiry", TIME_TO_EXPIRY, SMILE.getExpiry());
    assertEquals("Smile by delta: delta", DELTA, SMILE.getDelta());
    SmileDeltaParameters smile2 = SmileDeltaParameters.of(TIME_TO_EXPIRY, DELTA, SMILE.getVolatility());
    assertEquals("Smile by delta: volatility", SMILE.getVolatility(), smile2.getVolatility());
  }

  /**
   * Tests the volatility computations.
   */
  public void volatility() {
    DoubleArray volatility = SMILE.getVolatility();
    int nbDelta = DELTA.size();
    assertEquals("Volatility: ATM", ATM, volatility.get(nbDelta));
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      assertEquals(
          "Volatility: Risk Reversal " + loopdelta,
          RISK_REVERSAL.get(loopdelta),
          volatility.get(2 * nbDelta - loopdelta) - volatility.get(loopdelta), 1e-8);
      assertEquals(
          "Volatility: Strangle " + loopdelta,
          STRANGLE.get(loopdelta),
          (volatility.get(2 * nbDelta - loopdelta) + volatility.get(loopdelta)) / 2 - volatility.get(nbDelta), 1e-8);
    }
  }

  /**
   * Tests the strikes computations.
   */
  public void strike() {
    double[] strike = SMILE.strike(FORWARD).toArrayUnsafe();
    DoubleArray volatility = SMILE.getVolatility();
    int nbDelta = DELTA.size();
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      ValueDerivatives dPut = BlackFormulaRepository.priceAdjoint(
          FORWARD, strike[loopdelta], TIME_TO_EXPIRY, volatility.get(loopdelta), false);
      assertEquals("Strike: Put " + loopdelta, dPut.getDerivative(0), -DELTA.get(loopdelta), 1e-8);
      ValueDerivatives dCall = BlackFormulaRepository.priceAdjoint(
          FORWARD, strike[2 * nbDelta - loopdelta], TIME_TO_EXPIRY, volatility.get(2 * nbDelta - loopdelta), true);
      assertEquals("Strike: Call " + loopdelta, dCall.getDerivative(0), DELTA.get(loopdelta), 1e-8);
    }
    ValueDerivatives dPut = BlackFormulaRepository.priceAdjoint(
        FORWARD, strike[nbDelta], TIME_TO_EXPIRY, volatility.get(nbDelta), false);
    ValueDerivatives dCall = BlackFormulaRepository.priceAdjoint(
        FORWARD, strike[nbDelta], TIME_TO_EXPIRY, volatility.get(nbDelta), true);
    assertEquals("Strike: ATM", dCall.getDerivative(0) + dPut.getDerivative(0), 0.0, 1e-8);
  }

}
