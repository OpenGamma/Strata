/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link BlackOneTouchCashPriceFormulaRepository}.
 */
public class BlackOneTouchCashPriceFormulaRepositoryTest {
  private static final ZonedDateTime REFERENCE_DATE = TestHelper.dateUtc(2011, 7, 1);
  private static final ZonedDateTime EXPIRY_DATE = TestHelper.dateUtc(2015, 1, 2);
  private static final double EXPIRY_TIME =
      DayCounts.ACT_ACT_ISDA.relativeYearFraction(REFERENCE_DATE.toLocalDate(), EXPIRY_DATE.toLocalDate());
  private static final SimpleConstantContinuousBarrier BARRIER_DOWN_IN =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 90);
  private static final SimpleConstantContinuousBarrier BARRIER_DOWN_OUT =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, 90);
  private static final SimpleConstantContinuousBarrier BARRIER_UP_IN =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, 110);
  private static final SimpleConstantContinuousBarrier BARRIER_UP_OUT =
      SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, 110);
  private static final SimpleConstantContinuousBarrier[] BARRIERS =
      new SimpleConstantContinuousBarrier[] {BARRIER_UP_IN, BARRIER_UP_OUT, BARRIER_DOWN_IN, BARRIER_DOWN_OUT};
  private static final double SPOT = 105;
  private static final double RATE_DOM = 0.05; // Domestic rate
  private static final double RATE_FOR = 0.02; // Foreign rate
  private static final double COST_OF_CARRY = RATE_DOM - RATE_FOR; // Domestic - Foreign rate
  private static final double VOLATILITY = 0.20;
  private static final double DF_DOM = Math.exp(-RATE_DOM * EXPIRY_TIME);

  private static final double TOL = 1.0e-14;
  private static final double EPS_FD = 1.0e-6;
  private static final BlackOneTouchCashPriceFormulaRepository PRICER = new BlackOneTouchCashPriceFormulaRepository();

  /**
   * standard in-out parity holds if r=0.
   */
  @Test
  public void inOutParity() {
    double upIn = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, 0d, VOLATILITY, BARRIER_UP_IN);
    double upOut = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, 0d, VOLATILITY, BARRIER_UP_OUT);
    double downIn = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, 0d, VOLATILITY, BARRIER_DOWN_IN);
    double downOut = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, 0d, VOLATILITY, BARRIER_DOWN_OUT);
    assertRelative(upIn + upOut, 1d);
    assertRelative(downIn + downOut, 1d);
  }

  /**
   * Upper barrier level is very high.
   */
  @Test
  public void largeBarrierTest() {
    SimpleConstantContinuousBarrier in = SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_IN, 1.0e4);
    SimpleConstantContinuousBarrier out = SimpleConstantContinuousBarrier.of(BarrierType.UP, KnockType.KNOCK_OUT, 1.0e4);
    double upIn = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, in);
    double upOut = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, out);
    assertRelative(upIn, 0d);
    assertRelative(upOut, DF_DOM);
  }

  /**
   * Lower barrier level is very small.
   */
  @Test
  public void smallBarrierTest() {
    SimpleConstantContinuousBarrier in =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 0.1d);
    SimpleConstantContinuousBarrier out =
        SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_OUT, 0.1d);
    double dwIn = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, in);
    double dwOut = PRICER.price(SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, out);
    assertRelative(dwIn, 0d);
    assertRelative(dwOut, DF_DOM);
  }

  /**
   * Greeks against finite difference approximation.
   */
  @Test
  public void greekfdTest() {
    for (SimpleConstantContinuousBarrier barrier : BARRIERS) {
      ValueDerivatives computed = PRICER.priceAdjoint(
          SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, barrier);
      double spotUp = PRICER.price(
          SPOT + EPS_FD, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, barrier);
      double spotDw = PRICER.price(
          SPOT - EPS_FD, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, barrier);
      double rateUp = PRICER.price(
          SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM + EPS_FD, VOLATILITY, barrier);
      double rateDw = PRICER.price(
          SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM - EPS_FD, VOLATILITY, barrier);
      double costUp = PRICER.price(
          SPOT, EXPIRY_TIME, COST_OF_CARRY + EPS_FD, RATE_DOM, VOLATILITY, barrier);
      double costDw = PRICER.price(
          SPOT, EXPIRY_TIME, COST_OF_CARRY - EPS_FD, RATE_DOM, VOLATILITY, barrier);
      double volUp = PRICER.price(
          SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY + EPS_FD, barrier);
      double volDw = PRICER.price(
          SPOT, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY - EPS_FD, barrier);
      double timeUp = PRICER.price(
          SPOT, EXPIRY_TIME + EPS_FD, COST_OF_CARRY, RATE_DOM, VOLATILITY, barrier);
      double timeDw = PRICER.price(
          SPOT, EXPIRY_TIME - EPS_FD, COST_OF_CARRY, RATE_DOM, VOLATILITY, barrier);
      ValueDerivatives spotUp1 = PRICER.priceAdjoint(
          SPOT + EPS_FD, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, barrier);
      ValueDerivatives spotDw1 = PRICER.priceAdjoint(
          SPOT - EPS_FD, EXPIRY_TIME, COST_OF_CARRY, RATE_DOM, VOLATILITY, barrier);
      assertThat(computed.getDerivative(0)).isCloseTo(0.5 * (spotUp - spotDw) / EPS_FD, offset(EPS_FD));
      assertThat(computed.getDerivative(1)).isCloseTo(0.5 * (rateUp - rateDw) / EPS_FD, offset(EPS_FD));
      assertThat(computed.getDerivative(2)).isCloseTo(0.5 * (costUp - costDw) / EPS_FD, offset(EPS_FD));
      assertThat(computed.getDerivative(3)).isCloseTo(0.5 * (volUp - volDw) / EPS_FD, offset(EPS_FD));
      assertThat(computed.getDerivative(4)).isCloseTo(0.5 * (timeUp - timeDw) / EPS_FD, offset(EPS_FD));
      assertThat(computed.getDerivative(5)).isCloseTo(0.5 * (spotUp1.getDerivative(0) - spotDw1.getDerivative(0)) / EPS_FD, offset(EPS_FD));
    }
  }

  /**
   * smoothly connected to limiting cases.
   */
  @Test
  public void smallsigmaTTest() {
    for (SimpleConstantContinuousBarrier barrier : BARRIERS) {
      double volUp = 2.0e-3;
      double volDw = 1.0e-3;
      double time = 1.0e-2;
      double optUp = PRICER.price(SPOT, time, COST_OF_CARRY, RATE_DOM, volUp, barrier);
      double optDw = PRICER.price(SPOT, time, COST_OF_CARRY, RATE_DOM, volDw, barrier);
      assertRelative(optUp, optDw);
      ValueDerivatives optUpAdj = PRICER.priceAdjoint(SPOT, time, COST_OF_CARRY, RATE_DOM, volUp, barrier);
      ValueDerivatives optDwAdj = PRICER.priceAdjoint(SPOT, time, COST_OF_CARRY, RATE_DOM, volDw, barrier);
      assertRelative(optUpAdj.getValue(), optDwAdj.getValue());
      for (int i = 0; i < 6; ++i) {
        assertRelative(optUpAdj.getDerivative(i), optDwAdj.getDerivative(i));
      }
    }
  }

  /**
   * Barrier event has occured already.
   */
  @Test
  public void illegalBarrierLevelTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.price(BARRIER_UP_IN.getBarrierLevel() + 0.1, EXPIRY_TIME, COST_OF_CARRY,
            RATE_DOM, VOLATILITY, BARRIER_UP_IN));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.price(BARRIER_DOWN_OUT.getBarrierLevel() - 0.1, EXPIRY_TIME, COST_OF_CARRY,
            RATE_DOM, VOLATILITY, BARRIER_DOWN_OUT));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.priceAdjoint(BARRIER_UP_IN.getBarrierLevel() + 0.1, EXPIRY_TIME, COST_OF_CARRY,
            RATE_DOM, VOLATILITY, BARRIER_UP_IN));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PRICER.priceAdjoint(BARRIER_DOWN_OUT.getBarrierLevel() - 0.1, EXPIRY_TIME,
            COST_OF_CARRY, RATE_DOM, VOLATILITY, BARRIER_DOWN_OUT));
  }

  //-------------------------------------------------------------------------
  private void assertRelative(double val1, double val2) {
    assertThat(val1).isCloseTo(val2, offset(Math.max(Math.abs(val2), 1d) * TOL));
  }
}
