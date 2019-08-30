/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility.smile;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;

/**
 * Test {@link SsviVolatilityFunction}.
 */
public class SsviVolatilityFunctionTest {
  
  private static final double VOL_ATM = 0.20;
  private static final double RHO = -0.25;
  private static final double ETA = 0.50;
  private static final SsviFormulaData DATA = SsviFormulaData.of(VOL_ATM, RHO, ETA);
  private static final double TIME_EXP = 2.5;
  private static final double FORWARD = 0.05;
  private static final int N = 10;
  private static final double[] STRIKES = new double[N];
  static {
    for (int i = 0; i < N; i++) {
      STRIKES[i] = FORWARD - 0.03 + (i * 0.05 / N);
    }
  }
  private static final SsviVolatilityFunction SSVI_FUNCTION = SsviVolatilityFunction.DEFAULT;

  private static final double TOLERANCE_VOL = 1.0E-10;
  private static final double TOLERANCE_AD = 1.0E-6;

  @Test
  public void volatility() { // Function versus local implementation of formula
    double theta = VOL_ATM * VOL_ATM * TIME_EXP;
    double phi = ETA / Math.sqrt(theta);
    for (int i = 0; i < N; i++) {
      double k = Math.log(STRIKES[i] / FORWARD);
      double w = 0.5 * theta * (1.0d + RHO * phi * k + Math.sqrt(Math.pow(phi * k + RHO, 2) + (1.0d - RHO * RHO)));
      double sigmaExpected = Math.sqrt(w / TIME_EXP);
      double sigmaComputed = SSVI_FUNCTION.volatility(FORWARD, STRIKES[i], TIME_EXP, DATA);
      assertThat(sigmaExpected).isCloseTo(sigmaComputed, offset(TOLERANCE_VOL));
    }
  }

  @Test
  public void derivatives() { // AD v Finite Difference   
    ScalarFieldFirstOrderDifferentiator differentiator =
        new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, 1.0E-5);
    for (int i = 0; i < N; i++) {
      Function<DoubleArray, Double> function = new Function<DoubleArray, Double>() {
        @Override
        public Double apply(DoubleArray x) {
          SsviFormulaData data = SsviFormulaData.of(x.get(3), x.get(4), x.get(5));
          return SSVI_FUNCTION.volatility(x.get(0), x.get(1), x.get(2), data);
        }
      };
      Function<DoubleArray, DoubleArray> d = differentiator.differentiate(function);
      DoubleArray fd = d.apply(DoubleArray.of(FORWARD, STRIKES[i], TIME_EXP, VOL_ATM, RHO, ETA));
      ValueDerivatives ad = 
          SSVI_FUNCTION.volatilityAdjoint(FORWARD, STRIKES[i], TIME_EXP, DATA);
      for (int j = 0; j < 6; j++) {
        assertThat(fd.get(j)).isCloseTo(ad.getDerivatives().get(j), offset(TOLERANCE_AD));
      }
    }   
  }

  @Test
  public void test_small_time() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SSVI_FUNCTION.volatility(FORWARD, STRIKES[0], 0.0, DATA));
  }

  @Test
  public void coverage() {
    coverImmutableBean(SSVI_FUNCTION);
  }

  @Test
  public void test_serialization() {
    assertSerialization(SSVI_FUNCTION);
  }
  
}
