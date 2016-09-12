/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
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
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeAttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.IborFutureTrade;
import com.opengamma.strata.product.index.type.IborFutureConventions;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price an Ibor Future (STIR).
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class StirFuturePricingExample {

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
    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createTrade1(refData), createTrade2(refData));

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.PV01_CALIBRATED_SUM),
        Column.of(Measures.PAR_SPREAD),
        Column.of(Measures.PV01_CALIBRATED_BUCKETED));

    // use the built-in example market data
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions, marketDataBuilder.ratesLookup(valuationDate));

    // calculate the results
    Results results = runner.calculate(rules, trades, columns, marketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, functions, refData);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("stir-future-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create a trade
  private static Trade createTrade1(ReferenceData refData) {
    SecurityId secId = SecurityId.of("OG-Future", "Ibor-USD-LIBOR-3M-Mar15");
    IborFutureTrade trade = IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM.createTrade(
        LocalDate.of(2014, 9, 12), secId, Period.ofMonths(1), 2, 5, 1_000_000, 0.9998, refData);
    return trade.toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "Mar15 IMM Ibor Future")
            .counterparty(StandardId.of("example", "A"))
            .tradeDate(LocalDate.of(2014, 9, 12))
            .settlementDate(LocalDate.of(2014, 9, 14))
            .build())
        .quantity(20)
        .price(0.9997)
        .build();
  }

  // create a trade
  private static Trade createTrade2(ReferenceData refData) {
    SecurityId secId = SecurityId.of("OG-Future", "Ibor-USD-LIBOR-3M-Jun15");
    IborFutureTrade trade = IborFutureConventions.USD_LIBOR_3M_QUARTERLY_IMM.createTrade(
        LocalDate.of(2014, 9, 12), secId, Period.ofMonths(1), 3, 10, 1_000_000, 0.9996, refData);
    return trade.toBuilder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "Jun15 IMM Ibor Future")
            .counterparty(StandardId.of("example", "A"))
            .tradeDate(LocalDate.of(2014, 9, 12))
            .settlementDate(LocalDate.of(2014, 9, 14))
            .build())
        .quantity(20)
        .price(0.9997)
        .build();
  }

}
