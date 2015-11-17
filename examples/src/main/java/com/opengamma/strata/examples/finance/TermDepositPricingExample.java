/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.calc.CalculationEngine;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.examples.data.ExampleData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.rate.deposit.TermDeposit;
import com.opengamma.strata.product.rate.deposit.TermDepositTrade;
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
    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PV01),
        Column.of(Measure.PAR_RATE),
        Column.of(Measure.PAR_SPREAD),
        Column.of(Measure.BUCKETED_PV01));

    // use the built-in example market data
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

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

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("termdeposit-report-template");
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
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendars.GBLO))
        .currency(Currency.USD)
        .notional(10_000_000)
        .dayCount(DayCounts.THIRTY_360_ISDA)
        .rate(0.003)
        .build();

    return TermDepositTrade.builder()
        .product(td)
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .attributes(ImmutableMap.of("description", "Deposit 10M at 3%"))
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
        .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, HolidayCalendars.GBLO))
        .currency(Currency.USD)
        .notional(5_000_000)
        .dayCount(DayCounts.THIRTY_360_ISDA)
        .rate(0.0038)
        .build();

    return TermDepositTrade.builder()
        .product(td)
        .tradeInfo(TradeInfo.builder()
            .id(StandardId.of("example", "2"))
            .attributes(ImmutableMap.of("description", "Deposit 5M at 3.8%"))
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2015, 12, 16))
            .build())
        .build();
  }

}
