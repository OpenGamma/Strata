/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.Tenor;
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
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.dsf.Dsf;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price a Deliverable Swap Future (DSF).
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class DsfPricingExample {

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

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("dsf-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create a trade
  private static Trade createTrade1(ReferenceData refData) {
    Swap swap = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M.createTrade(
        LocalDate.of(2015, 3, 18), Tenor.TENOR_5Y, BuySell.SELL, 1, 0.02, refData).getProduct();

    Dsf product = Dsf.builder()
        .securityId(SecurityId.of("OG-Future", "CME-F1U-Mar15"))
        .lastTradeDate(LocalDate.of(2015, 3, 16))
        .deliveryDate(LocalDate.of(2015, 3, 18))
        .notional(100_000)
        .underlyingSwap(swap)
        .build();

    return DsfTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "CME-5Y-DSF Mar15")
            .counterparty(StandardId.of("mn", "Dealer G"))
            .tradeDate(LocalDate.of(2015, 3, 18))
            .settlementDate(LocalDate.of(2015, 3, 18))
            .build())
        .product(product)
        .quantity(20)
        .price(1.0075)
        .build();
  }

  // create a trade
  private static Trade createTrade2(ReferenceData refData) {
    Swap swap = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M.createTrade(
        LocalDate.of(2015, 6, 17), Tenor.TENOR_5Y, BuySell.SELL, 1, 0.02, refData).getProduct();

    Dsf product = Dsf.builder()
        .securityId(SecurityId.of("OG-Future", "CME-F1U-Jun15"))
        .lastTradeDate(LocalDate.of(2015, 6, 15))
        .deliveryDate(LocalDate.of(2015, 6, 17))
        .notional(100_000)
        .underlyingSwap(swap)
        .build();

    return DsfTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "2"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "CME-5Y-DSF Jun15")
            .counterparty(StandardId.of("mn", "Dealer G"))
            .tradeDate(LocalDate.of(2015, 6, 17))
            .settlementDate(LocalDate.of(2015, 6, 17))
            .build())
        .product(product)
        .quantity(20)
        .price(1.0085)
        .build();
  }

}
