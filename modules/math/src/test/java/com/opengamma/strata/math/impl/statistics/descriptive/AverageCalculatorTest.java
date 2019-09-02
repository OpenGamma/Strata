/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.MathException;

/**
 * Test.
 */
public class AverageCalculatorTest {
  private static final double[] DATA = {1., 1., 3., 2.5, 5.7, 3.7, 5.7, 5.7, -4., 9.};
  private static final Function<double[], Double> MEAN = new MeanCalculator();
  private static final Function<double[], Double> MEDIAN = new MedianCalculator();
  private static final Function<double[], Double> MODE = new ModeCalculator();
  private static final double EPS = 1e-15;

  @Test
  public void testNull() {
    assertNull(MEAN);
    assertNull(MEDIAN);
    assertNull(MODE);
  }

  @Test
  public void testEmpty() {
    assertEmpty(MEAN);
    assertEmpty(MEDIAN);
    assertEmpty(MODE);
  }

  @Test
  public void testSingleValue() {
    double value = 3.;
    double[] x = {value};
    assertThat(value).isCloseTo(MEAN.apply(x), offset(EPS));
    assertThat(value).isCloseTo(MEDIAN.apply(x), offset(EPS));
    assertThat(value).isCloseTo(MODE.apply(x), offset(EPS));
  }

  @Test
  public void testMean() {
    assertThat(MEAN.apply(DATA)).isCloseTo(3.33, offset(EPS));
  }

  @Test
  public void testMedian() {
    assertThat(MEDIAN.apply(DATA)).isCloseTo(3.35, offset(EPS));
    double[] x = Arrays.copyOf(DATA, DATA.length - 1);
    assertThat(MEDIAN.apply(x)).isCloseTo(3, offset(EPS));
  }

  @Test
  public void testMode() {
    double[] x = {1., 2., 3., 4., 5., 6., 7., 8., 9., 10.};
    assertThatExceptionOfType(MathException.class)
        .isThrownBy(() -> MODE.apply(x));
    assertThat(MODE.apply(DATA)).isCloseTo(5.7, offset(EPS));
  }

  private void assertNull(Function<double[], Double> calculator) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> calculator.apply(null));
  }

  private void assertEmpty(Function<double[], Double> calculator) {
    double[] x = new double[0];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> calculator.apply(x));
  }

}
