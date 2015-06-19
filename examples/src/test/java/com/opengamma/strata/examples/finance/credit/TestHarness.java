/*
 * *
 *  * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *  *
 *  * Please see distribution for license.
 *
 *
 */

package com.opengamma.strata.examples.finance.credit;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.examples.finance.credit.api.Calculator;
import com.opengamma.strata.examples.finance.credit.api.TradeSource;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.markit.MarkitRedCode;
import com.opengamma.strata.finance.credit.type.StandardCdsConventions;
import com.opengamma.strata.finance.credit.type.StandardCdsTemplate;
import org.testng.Assert;

import java.time.LocalDate;
import java.util.Arrays;

public class TestHarness {

  private static final double epsilon = 10e-9;
  private static final Calculator calculator = ExampleCalculator.of();

  public static class TradeFactory {
    private final TradeSource tradeSource;

    private TradeFactory(BuySell buySell, double feeAmount, LocalDate cashSettleDate) {
      tradeSource = () -> ImmutableList.of(
          StandardCdsTemplate
              .of(StandardCdsConventions.northAmericanUsd())
              .toSingleNameTrade(
                  StandardId.of("tradeid", "62726762"),
                  LocalDate.of(2014, 9, 22),
                  LocalDate.of(2019, 12, 20),
                  buySell,
                  100_000_000D,
                  0.0100,
                  MarkitRedCode.id("AH98A7"),
                  SeniorityLevel.SeniorUnsecuredForeign,
                  RestructuringClause.NoRestructuring2014,
                  feeAmount,
                  cashSettleDate
              )
      );
    }

    public static TradeFactory withTrade(BuySell buySell, double feeAmount, LocalDate cashSettleDate) {
      return new TradeFactory(buySell, feeAmount, cashSettleDate);
    }

    public ScalarMeasureOnADay pvShouldBe(double expected) {
      return new ScalarMeasureOnADay(Measure.PRESENT_VALUE, expected, tradeSource);
    }

    public ScalarMeasureOnADay ir01ParallelParShouldBe(double expected) {
      return new ScalarMeasureOnADay(Measure.IR01_PARALLEL_PAR, expected, tradeSource);
    }

    public VectorMeasureOnADay ir01BucketedParShouldBe(double... expected) {
      return new VectorMeasureOnADay(Measure.IR01_BUCKETED_PAR, expected, tradeSource);
    }

    public ScalarMeasureOnADay cs01ParallelParShouldBe(double expected) {
      return new ScalarMeasureOnADay(Measure.CS01_PARALLEL_PAR, expected, tradeSource);
    }

    public VectorMeasureOnADay cs01BucketedParShouldBe(double... expected) {
      return new VectorMeasureOnADay(Measure.CS01_BUCKETED_PAR, expected, tradeSource);
    }

  }

  public static class ScalarMeasureOnADay {

    private final Measure measure;
    private final double expected;
    private final TradeSource tradeSource;

    private ScalarMeasureOnADay(Measure measure, double expected, TradeSource tradeSource) {
      this.measure = measure;
      this.expected = expected;
      this.tradeSource = tradeSource;
    }

    public void on(LocalDate valuationDate) {
      double value = calculator.calculateScalarValue(valuationDate, tradeSource, measure);
      Assert.assertEquals(value, expected, epsilon);
    }
  }

  public static class VectorMeasureOnADay {

    private final Measure measure;
    private final double[] expected;
    private final TradeSource tradeSource;

    private VectorMeasureOnADay(Measure measure, double[] expected, TradeSource tradeSource) {
      this.expected = expected;
      this.measure = measure;
      this.tradeSource = tradeSource;
    }

    public void on(LocalDate valuationDate) {
      double[] values = calculator.calculateVectorValue(valuationDate, tradeSource, measure);
//      for (double value : values) {
//        System.out.println(value + ",");
//      }
      Assert.assertEquals(Arrays.asList(values), Arrays.asList(expected));
    }
  }

}
