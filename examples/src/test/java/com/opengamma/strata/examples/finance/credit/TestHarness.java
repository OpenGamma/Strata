/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.examples.finance.credit.api.Calculator;
import com.opengamma.strata.examples.finance.credit.api.TradeSource;
import org.testng.Assert;

import java.time.LocalDate;
import java.util.Arrays;

public class TestHarness {

  private static final double epsilon = 10e-9;
  private static final TradeSource tradeSource = ExampleTradeSource.of();
  private static final Calculator calculator = ExampleCalculator.of();

  public static ScalarMeasureOnADay pvShouldBe(double expected) {
    return new ScalarMeasureOnADay(Measure.PRESENT_VALUE, expected);
  }

  public static ScalarMeasureOnADay ir01ParallelParShouldBe(double expected) {
    return new ScalarMeasureOnADay(Measure.IR01_PARALLEL_PAR, expected);
  }

  public static ScalarMeasureOnADay cs01ParallelParShouldBe(double expected) {
    return new ScalarMeasureOnADay(Measure.CS01_PARALLEL_PAR, expected);
  }

  public static class ScalarMeasureOnADay {

    private final Measure measure;
    private final double expected;

    private ScalarMeasureOnADay(Measure measure, double expected) {
      this.measure = measure;
      this.expected = expected;
    }

    public void on(LocalDate valuationDate) {
      double pv = calculator.calculateScalarValue(valuationDate, tradeSource, measure);
      Assert.assertEquals(pv, expected, epsilon);
    }
  }

  public static class VectorMeasureOnADay {

    private final Measure measure;
    private final double[] expected;

    private VectorMeasureOnADay(Measure measure, double[] expected) {
      this.expected = expected;
      this.measure = measure;
    }

    public void on(LocalDate valuationDate) {
      double[] pv = calculator.calculateVectorValue(valuationDate, tradeSource, measure);
      Assert.assertEquals(Arrays.asList(pv), Arrays.asList(expected));
    }
  }

}
