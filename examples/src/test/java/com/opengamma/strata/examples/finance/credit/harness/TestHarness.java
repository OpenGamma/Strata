/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance.credit.harness;

import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.Assert;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitRedCode;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.credit.CreditMeasures;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.IndexReferenceInformation;
import com.opengamma.strata.product.credit.RestructuringClause;
import com.opengamma.strata.product.credit.SeniorityLevel;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;
import com.opengamma.strata.product.credit.type.CdsConventions;

public class TestHarness {

  private static final double epsilon = 10e-9;
  private static final Calculator calculator = TestCalculator.of();

  public static class TradeFactory {
    private final TradeSource tradeSource;

    private TradeFactory(TradeSource tradeSource) {
      this.tradeSource = tradeSource;
    }

    public static TradeFactory withCompany01() {
      final LocalDate cashSettleDate = LocalDate.of(2014, 10, 21);
      final double feeAmount = 3_694_117.73D;
      final BuySell buySell = BuySell.BUY;
      return withCompany01(buySell, feeAmount, cashSettleDate);
    }

    public static TradeFactory withCompany01(BuySell buySell, double feeAmount, LocalDate cashSettleDate) {
      TradeSource tradeSource = () -> CdsConventions.USD_NORTH_AMERICAN
          .toTrade(
              LocalDate.of(2014, 9, 22),
              LocalDate.of(2019, 12, 20),
              buySell,
              100_000_000D,
              0.0100,
              SingleNameReferenceInformation.of(
                  MarkitRedCode.id("COMP01"),
                  SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
                  Currency.USD,
                  RestructuringClause.NO_RESTRUCTURING_2014),
              feeAmount,
              cashSettleDate);
      return new TradeFactory(tradeSource);
    }

    public static TradeFactory withCompany02() {
      TradeSource tradeSource = () -> CdsConventions.USD_NORTH_AMERICAN
          .toTrade(
              LocalDate.of(2014, 9, 22),
              LocalDate.of(2019, 12, 20),
              BuySell.BUY,
              100_000_000D,
              0.0500,
              SingleNameReferenceInformation.of(
                  MarkitRedCode.id("COMP02"),
                  SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
                  Currency.USD,
                  RestructuringClause.NO_RESTRUCTURING_2014),
              -1_370_582.00D,
              LocalDate.of(2014, 10, 21));
      return new TradeFactory(tradeSource);
    }

    public static TradeFactory withIndex0001() {
      TradeSource tradeSource = () -> CdsConventions.USD_NORTH_AMERICAN
          .toTrade(
              LocalDate.of(2014, 3, 20),
              LocalDate.of(2019, 6, 20),
              BuySell.BUY,
              100_000_000D,
              0.0500,
              IndexReferenceInformation.of(MarkitRedCode.id("INDEX0001"), 22, 4),
              2_000_000D,
              LocalDate.of(2014, 10, 21));
      return new TradeFactory(tradeSource);
    }

    public Trade getTrade() {
      return tradeSource.apply();
    }

    public ScalarMeasureOnADay pvShouldBe(double expected) {
      return new ScalarMeasureOnADay(Measures.PRESENT_VALUE, expected, tradeSource);
    }

    public ScalarMeasureOnADay parRateShouldBe(double expected) {
      return new ScalarMeasureOnADay(Measures.PAR_RATE, expected, tradeSource);
    }

    public ScalarMeasureOnADay jumpToDefaultShouldBe(double expected) {
      return new ScalarMeasureOnADay(CreditMeasures.JUMP_TO_DEFAULT, expected, tradeSource);
    }

    public ScalarMeasureOnADay recovery01ShouldBe(double expected) {
      return new ScalarMeasureOnADay(CreditMeasures.RECOVERY01, expected, tradeSource);
    }

    public ScalarMeasureOnADay ir01ParallelParShouldBe(double expected) {
      return new ScalarMeasureOnADay(CreditMeasures.IR01_PARALLEL_PAR, expected, tradeSource);
    }

    public VectorMeasureOnADay ir01BucketedParShouldBe(double... expected) {
      return new VectorMeasureOnADay(CreditMeasures.IR01_BUCKETED_PAR, expected, tradeSource);
    }

    public ScalarMeasureOnADay cs01ParallelParShouldBe(double expected) {
      return new ScalarMeasureOnADay(CreditMeasures.CS01_PARALLEL_PAR, expected, tradeSource);
    }

    public VectorMeasureOnADay cs01BucketedParShouldBe(double... expected) {
      return new VectorMeasureOnADay(CreditMeasures.CS01_BUCKETED_PAR, expected, tradeSource);
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
    private final DoubleArray expected;
    private final TradeSource tradeSource;

    private VectorMeasureOnADay(Measure measure, double[] expected, TradeSource tradeSource) {
      this.expected = DoubleArray.copyOf(expected);
      this.measure = measure;
      this.tradeSource = tradeSource;
    }

    public void on(LocalDate valuationDate) {
      DoubleArray values = calculator.calculateVectorValue(valuationDate, tradeSource, measure);
      assertTrue(values.equalWithTolerance(expected, epsilon));
    }
  }

}
