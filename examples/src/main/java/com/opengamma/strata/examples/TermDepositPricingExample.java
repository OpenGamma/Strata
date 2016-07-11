/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeAttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.deposit.TermDeposit;
import com.opengamma.strata.product.deposit.TermDepositTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price a Term Deposit.
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class TermDepositPricingExample {

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   * 
   * @param args  ignored
   */
  public static void main(String[] args) {
    // setup calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      calculate(runner);
    }
  }

  // obtains the data and calculates the grid of results
  private static void calculate(CalculationRunner runner) {
    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.PV01_CALIBRATED_SUM),
        Column.of(Measures.PAR_RATE),
        Column.of(Measures.PAR_SPREAD),
        Column.of(Measures.PV01_CALIBRATED_BUCKETED));

    // use the built-in example market data
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions, marketDataBuilder.ratesLookup(valuationDate));

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    Results results = runner.calculate(rules, trades, columns, marketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, functions, refData);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("term-deposit-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create a TermDeposit trade
  private static Trade createTrade1() {
    TermDeposit td = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .startDate(LocalDate.of(2014, 9, 12))
        .endDate(LocalDate.of(2014, 12, 12))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO))
        .currency(Currency.USD)
        .notional(10_000_000)
        .dayCount(DayCounts.THIRTY_360_ISDA)
        .rate(0.003)
        .build();

    return TermDepositTrade.builder()
        .product(td)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "Deposit 10M at 3%")
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 12, 16))
            .build())
        .build();
  }

  // create a TermDeposit trade
  private static Trade createTrade2() {
    TermDeposit td = TermDeposit.builder()
        .buySell(BuySell.BUY)
        .startDate(LocalDate.of(2014, 12, 12))
        .endDate(LocalDate.of(2015, 12, 12))
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendarIds.GBLO))
        .currency(Currency.USD)
        .notional(5_000_000)
        .dayCount(DayCounts.THIRTY_360_ISDA)
        .rate(0.0038)
        .build();

    return TermDepositTrade.builder()
        .product(td)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "2"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "Deposit 5M at 3.8%")
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2015, 12, 16))
            .build())
        .build();
  }

}
