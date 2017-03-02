/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.report.ReportCalculationResults;

/**
 * Test {@link ValuePathEvaluator}.
 */
@Test
public class ValuePathEvaluatorTest {

  public void measurePath() {
    ReportCalculationResults reportResults = reportResults();

    List<Result<?>> currencyResults = ValuePathEvaluator.evaluate("Measures.PresentValue.Currency", reportResults);
    List<Result<?>> expectedCurrencies = ImmutableList.of(
        Result.success(Currency.CAD),
        Result.success(Currency.AUD),
        Result.success(Currency.CHF));
    assertThat(currencyResults).isEqualTo(expectedCurrencies);

    // Amount returns the CurrencyAmount which is slightly unexpected
    // It's required in order to be able to format the amount to the correct number of decimal places
    List<Result<?>> amountResults = ValuePathEvaluator.evaluate("Measures.PresentValue.Amount", reportResults);
    List<Result<?>> expectedAmounts = ImmutableList.of(
        Result.success(CurrencyAmount.of(Currency.CAD, 2d)),
        Result.success(CurrencyAmount.of(Currency.AUD, 3d)),
        Result.success(CurrencyAmount.of(Currency.CHF, 4d)));
    assertThat(amountResults).isEqualTo(expectedAmounts);
  }

  public void measurePath_failure_noDot() {
    ReportCalculationResults reportResults = reportResults();

    List<Result<?>> results = ValuePathEvaluator.evaluate("Measures", reportResults);
    Result<?> result = results.get(0);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getFailure().getMessage()).contains("PresentValue");
    assertThat(result.getFailure().getMessage()).contains("ParRate");
  }

  public void measurePath_failure_noMeasureName() {
    ReportCalculationResults reportResults = reportResults();

    List<Result<?>> results = ValuePathEvaluator.evaluate("Measures.", reportResults);
    Result<?> result = results.get(0);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getFailure().getMessage()).contains("PresentValue");
    assertThat(result.getFailure().getMessage()).contains("ParRate");
  }

  public void measurePath_failure_unknownMeasure() {
    ReportCalculationResults reportResults = reportResults();

    List<Result<?>> results = ValuePathEvaluator.evaluate("Measures.Wibble", reportResults);
    Result<?> result = results.get(0);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getFailure().getMessage()).contains("Wibble");
    assertThat(result.getFailure().getMessage()).contains("PresentValue");
    assertThat(result.getFailure().getMessage()).contains("ParRate");
  }

  public void measurePath_failure_nonQueriedMeasure() {
    ReportCalculationResults reportResults = reportResults();

    List<Result<?>> results = ValuePathEvaluator.evaluate("Measures.ParSpread", reportResults);
    Result<?> result = results.get(0);
    assertThat(result.isFailure()).isTrue();
    assertThat(result.getFailure().getMessage()).contains("PresentValue");
    assertThat(result.getFailure().getMessage()).contains("ParRate");
  }

  public void tradePath() {
    ReportCalculationResults reportResults = reportResults();

    List<Result<?>> counterpartyResults = ValuePathEvaluator.evaluate("Trade.Counterparty.Value", reportResults);
    List<Result<?>> expectedCounterparties = ImmutableList.of(
        Result.success("cpty1"),
        Result.success("cpty2"),
        Result.success("cpty3"));
    assertThat(counterpartyResults).isEqualTo(expectedCounterparties);
    List<Result<?>> counterpartyResults2 = ValuePathEvaluator.evaluate("Target.Counterparty.Value", reportResults);
    assertThat(counterpartyResults2).isEqualTo(expectedCounterparties);
  }

  public void productPath() {
    ReportCalculationResults reportResults = reportResults();

    List<Result<?>> counterpartyResults = ValuePathEvaluator.evaluate("Trade.Product.Notional", reportResults);
    List<Result<?>> expectedCounterparties = ImmutableList.of(
        Result.success(1_000_000d),
        Result.success(10_000_000d),
        Result.success(100_000_000d));
    assertThat(counterpartyResults).isEqualTo(expectedCounterparties);
  }

  //--------------------------------------------------------------------------------------------------

  private static ReportCalculationResults reportResults() {
    Measure measure = Measure.of("PresentValue");
    Column column = Column.of(measure);
    List<Column> columns = ImmutableList.of(column);
    List<? extends Result<?>> resultValues = ImmutableList.of(
        Result.success(CurrencyAmount.of(Currency.CAD, 2d)),
        Result.success(CurrencyAmount.of(Currency.AUD, 3d)),
        Result.success(CurrencyAmount.of(Currency.CHF, 4d)));
    List<Trade> trades = ImmutableList.of(
        trade("cpty1", 1_000_000),
        trade("cpty2", 10_000_000),
        trade("cpty3", 100_000_000));
    Results results = Results.of(ImmutableList.of(column.toHeader()), resultValues);
    return ReportCalculationResults.of(LocalDate.now(ZoneOffset.UTC), trades, columns, results);
  }

  private static Trade trade(String counterparty, double notional) {
    TradeInfo tradeInfo = TradeInfo.builder()
        .counterparty(StandardId.of("cpty", counterparty))
        .build();
    Fra fra = Fra.builder()
        .buySell(BUY)
        .notional(notional)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .paymentDate(AdjustableDate.of(date(2015, 8, 7)))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    return FraTrade.builder()
        .info(tradeInfo)
        .product(fra)
        .build();
  }
}
