/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

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

  @Test
  public void testNullDelta() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SmileDeltaParameters.of(TIME_TO_EXPIRY, ATM, null, RISK_REVERSAL, STRANGLE));
  }

  @Test
  public void testRRLength() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SmileDeltaParameters.of(TIME_TO_EXPIRY, ATM, DELTA, DoubleArray.filled(3), STRANGLE));
  }

  @Test
  public void testStrangleLength() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SmileDeltaParameters.of(TIME_TO_EXPIRY, ATM, DELTA, RISK_REVERSAL, DoubleArray.filled(3)));
  }

  /**
   * Tests the constructor directly from volatilities (not RR and S).
   */
  @Test
  public void constructorVolatility() {
    DoubleArray volatility = SMILE.getVolatility();
    SmileDeltaParameters smileFromVolatility =
        SmileDeltaParameters.of(TIME_TO_EXPIRY, DELTA, volatility, PARAMETER_METADATA);
    assertThat(smileFromVolatility).as("Smile by delta: constructor").isEqualTo(SMILE);
  }

  /**
   * Tests the getters.
   */
  @Test
  public void getter() {
    assertThat(SMILE.getExpiry()).as("Smile by delta: time to expiry").isEqualTo(TIME_TO_EXPIRY);
    assertThat(SMILE.getDelta()).as("Smile by delta: delta").isEqualTo(DELTA);
    SmileDeltaParameters smile2 = SmileDeltaParameters.of(TIME_TO_EXPIRY, DELTA, SMILE.getVolatility());
    assertThat(smile2.getVolatility()).as("Smile by delta: volatility").isEqualTo(SMILE.getVolatility());
  }

  /**
   * Tests the volatility computations.
   */
  @Test
  public void volatility() {
    DoubleArray volatility = SMILE.getVolatility();
    int nbDelta = DELTA.size();
    assertThat(volatility.get(nbDelta)).as("Volatility: ATM").isEqualTo(ATM);
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      assertThat(volatility.get(2 * nbDelta - loopdelta) - volatility.get(loopdelta))
          .as("Volatility: Risk Reversal " + loopdelta)
          .isCloseTo(RISK_REVERSAL.get(loopdelta), offset(1e-8));
      assertThat((volatility.get(2 * nbDelta - loopdelta) + volatility.get(loopdelta)) / 2 - volatility.get(nbDelta))
          .as("Volatility: Strangle " + loopdelta)
          .isCloseTo(STRANGLE.get(loopdelta), offset(1e-8));
    }
  }

  /**
   * Tests the strikes computations.
   */
  @Test
  public void strike() {
    double[] strike = SMILE.strike(FORWARD).toArrayUnsafe();
    DoubleArray volatility = SMILE.getVolatility();
    int nbDelta = DELTA.size();
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      ValueDerivatives dPut = BlackFormulaRepository.priceAdjoint(
          FORWARD, strike[loopdelta], TIME_TO_EXPIRY, volatility.get(loopdelta), false);
      assertThat(-DELTA.get(loopdelta)).as("Strike: Put " + loopdelta).isCloseTo(dPut.getDerivative(0), offset(1e-8));
      ValueDerivatives dCall = BlackFormulaRepository.priceAdjoint(
          FORWARD, strike[2 * nbDelta - loopdelta], TIME_TO_EXPIRY, volatility.get(2 * nbDelta - loopdelta), true);
      assertThat(DELTA.get(loopdelta)).as("Strike: Call " + loopdelta).isCloseTo(dCall.getDerivative(0), offset(1e-8));
    }
    ValueDerivatives dPut = BlackFormulaRepository.priceAdjoint(
        FORWARD, strike[nbDelta], TIME_TO_EXPIRY, volatility.get(nbDelta), false);
    ValueDerivatives dCall = BlackFormulaRepository.priceAdjoint(
        FORWARD, strike[nbDelta], TIME_TO_EXPIRY, volatility.get(nbDelta), true);
    assertThat(0.0).as("Strike: ATM").isCloseTo(dCall.getDerivative(0) + dPut.getDerivative(0), offset(1e-8));
  }

}
