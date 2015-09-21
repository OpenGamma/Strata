/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculation.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.marketdata.MarketEnvironment;
import com.opengamma.strata.examples.data.ExampleData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.MarketDataBuilder;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.fx.FxSingle;
import com.opengamma.strata.finance.fx.FxSwap;
import com.opengamma.strata.finance.fx.FxSwapTrade;
import com.opengamma.strata.finance.fx.FxSingleTrade;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price FX trades.
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class FxPricingExample {

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   * 
   * @param args  ignored
   */
  public static void main(String[] args) {
    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2(), createTrade3());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PV01),
        Column.of(Measure.BUCKETED_PV01));

    // use the built-in example market data
    MarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataRules(marketDataBuilder.rules())
        .build();

    // build a market data snapshot for the valuation date
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    MarketEnvironment snapshot = marketDataBuilder.buildSnapshot(valuationDate);

    // create the engine and calculate the results
    CalculationEngine engine = ExampleEngine.create();
    Results results = engine.calculate(trades, columns, rules, snapshot);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults = ReportCalculationResults.of(
        valuationDate,
        trades,
        columns,
        results);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("fx-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create an FX Forward trade
  private static Trade createTrade1() {
    FxSingle fx = FxSingle.of(CurrencyAmount.of(GBP, 10000), FxRate.of(GBP, USD, 1.62), LocalDate.of(2014, 9, 14));
    return FxSingleTrade.builder()
        .product(fx)
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .attributes(ImmutableMap.of("description", "GBP 10,000/USD @ 1.62 fwd"))
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 14))
            .build())
        .build();
  }

  // create an FX Forward trade
  private static Trade createTrade2() {
    FxSingle fx = FxSingle.of(CurrencyAmount.of(USD, 15000), FxRate.of(GBP, USD, 1.62), LocalDate.of(2014, 9, 14));
    return FxSingleTrade.builder()
        .product(fx)
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("example", "2"))
            .attributes(ImmutableMap.of("description", "USD 15,000/GBP @ 1.62 fwd"))
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 14))
            .build())
        .build();
  }

  // create an FX Forward trade
  private static Trade createTrade3() {
    FxSwap swap = FxSwap.ofForwardPoints(
        CurrencyAmount.of(GBP, 10000), USD, 1.62, 0.03, LocalDate.of(2014, 6, 14), LocalDate.of(2014, 9, 14));
    return FxSwapTrade.builder()
        .product(swap)
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("example", "3"))
            .attributes(ImmutableMap.of("description", "GBP 10,000/USD @ 1.62 swap"))
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 14))
            .build())
        .build();
  }

}
