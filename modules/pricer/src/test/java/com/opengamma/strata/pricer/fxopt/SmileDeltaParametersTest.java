/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.pricer.common.GenericVolatilitySurfaceYearFractionParameterMetadata;
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
  private static final ImmutableList<ParameterMetadata> PARAMETER_METADATA = ImmutableList.of(
      GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, DeltaStrike.of(0.9d)),
      GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, DeltaStrike.of(0.75d)),
      GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, DeltaStrike.of(0.5d)),
      GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, DeltaStrike.of(0.25d)),
      GenericVolatilitySurfaceYearFractionParameterMetadata.of(TIME_TO_EXPIRY, DeltaStrike.of(0.1d)));

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
    SmileDeltaParameters smileFromVolatility =
        SmileDeltaParameters.of(TIME_TO_EXPIRY, DELTA, volatility, PARAMETER_METADATA);
    assertEquals(smileFromVolatility, SMILE, "Smile by delta: constructor");
  }

  /**
   * Tests the getters.
   */
  public void getter() {
    assertEquals(SMILE.getExpiry(), TIME_TO_EXPIRY, "Smile by delta: time to expiry");
    assertEquals(SMILE.getDelta(), DELTA, "Smile by delta: delta");
    SmileDeltaParameters smile2 = SmileDeltaParameters.of(TIME_TO_EXPIRY, DELTA, SMILE.getVolatility());
    assertEquals(smile2.getVolatility(), SMILE.getVolatility(), "Smile by delta: volatility");
  }

  /**
   * Tests the volatility computations.
   */
  public void volatility() {
    DoubleArray volatility = SMILE.getVolatility();
    int nbDelta = DELTA.size();
    assertEquals(volatility.get(nbDelta), ATM, "Volatility: ATM");
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      assertEquals(
          volatility.get(2 * nbDelta - loopdelta) - volatility.get(loopdelta),
          RISK_REVERSAL.get(loopdelta),
          1e-8,
          "Volatility: Risk Reversal " + loopdelta);
      assertEquals(
          (volatility.get(2 * nbDelta - loopdelta) + volatility.get(loopdelta)) / 2 - volatility.get(nbDelta),
          STRANGLE.get(loopdelta),
          1e-8,
          "Volatility: Strangle " + loopdelta);
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
      assertEquals(-DELTA.get(loopdelta), dPut.getDerivative(0), 1e-8, "Strike: Put " + loopdelta);
      ValueDerivatives dCall = BlackFormulaRepository.priceAdjoint(
          FORWARD, strike[2 * nbDelta - loopdelta], TIME_TO_EXPIRY, volatility.get(2 * nbDelta - loopdelta), true);
      assertEquals(DELTA.get(loopdelta), dCall.getDerivative(0), 1e-8, "Strike: Call " + loopdelta);
    }
    ValueDerivatives dPut = BlackFormulaRepository.priceAdjoint(
        FORWARD, strike[nbDelta], TIME_TO_EXPIRY, volatility.get(nbDelta), false);
    ValueDerivatives dCall = BlackFormulaRepository.priceAdjoint(
        FORWARD, strike[nbDelta], TIME_TO_EXPIRY, volatility.get(nbDelta), true);
    assertEquals(0.0, dCall.getDerivative(0) + dPut.getDerivative(0), 1e-8, "Strike: ATM");
  }

}
