/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.examples.data.ExampleData;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.MarketDataBuilder;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitRedCode;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.credit.RestructuringClause;
import com.opengamma.strata.finance.credit.SeniorityLevel;
import com.opengamma.strata.finance.credit.type.CdsConventions;
import com.opengamma.strata.function.OpenGammaPricingRules;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * Example to illustrate using the engine to price a credit default swap.
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class CdsPricingExample {

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   *
   * @param args  ignored
   */
  public static void main(String[] args) {
    // the trades that will have measures calculated
    List<Trade> trades = createCdsTrades();

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PAR_RATE),
        Column.of(Measure.RECOVERY01),
        Column.of(Measure.JUMP_TO_DEFAULT),
        Column.of(Measure.IR01_PARALLEL_PAR),
        Column.of(Measure.IR01_PARALLEL_ZERO),
        Column.of(Measure.CS01_PARALLEL_PAR),
        Column.of(Measure.CS01_PARALLEL_HAZARD),
        Column.of(Measure.IR01_BUCKETED_PAR),
        Column.of(Measure.IR01_BUCKETED_ZERO),
        Column.of(Measure.CS01_BUCKETED_PAR),
        Column.of(Measure.CS01_BUCKETED_HAZARD)
    );

    // use the built-in example market data
    MarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(OpenGammaPricingRules.standard())
        .marketDataRules(marketDataBuilder.rules())
        .build();

    // build a market data snapshot for the valuation date
    LocalDate valuationDate = LocalDate.of(2014, 10, 16);
    BaseMarketData snapshot = marketDataBuilder.buildSnapshot(valuationDate);

    // create the engine and calculate the results
    CalculationEngine engine = ExampleEngine.create();
    Results results = engine.calculate(trades, columns, rules, snapshot);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults = ReportCalculationResults.of(
        valuationDate,
        trades,
        columns,
        results);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("cds-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeCsv(System.out);
  }

  //-----------------------------------------------------------------------  
  // create CDS trades
  private static List<Trade> createCdsTrades() {
    return ImmutableList.of(
        createCompany01Cds(),
        createCompany02Cds(),
        createIndex0001());
  }

  //-----------------------------------------------------------------------  
  // create a single name CDS with 100 bps coupon
  private static Trade createCompany01Cds() {
    return CdsConventions.NORTH_AMERICAN_USD
        .toSingleNameTrade(
            LocalDate.of(2014, 9, 22),
            LocalDate.of(2019, 12, 20),
            BuySell.BUY,
            100_000_000d,
            0.0100,
            MarkitRedCode.id("COMP01"),
            SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
            RestructuringClause.NO_RESTRUCTURING_2014,
            3_694_117.72d,
            LocalDate.of(2014, 10, 21));
  }

  // create a single name CDS with 500 bps coupon
  private static Trade createCompany02Cds() {
    return CdsConventions.NORTH_AMERICAN_USD
        .toSingleNameTrade(
            LocalDate.of(2014, 9, 22),
            LocalDate.of(2019, 12, 20),
            BuySell.BUY,
            100_000_000d,
            0.0500,
            MarkitRedCode.id("COMP02"),
            SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
            RestructuringClause.NO_RESTRUCTURING_2014,
            -1_370_582.00d,
            LocalDate.of(2014, 10, 21));
  }

  // create a index CDS on with 500 bps coupon
  private static Trade createIndex0001() {
    return CdsConventions.NORTH_AMERICAN_USD
        .toIndexTrade(
            LocalDate.of(2014, 3, 20),
            LocalDate.of(2019, 6, 20),
            BuySell.BUY,
            100_000_000d,
            0.0500,
            MarkitRedCode.id("INDEX0001"),
            22,
            4,
            2_000_000d,
            LocalDate.of(2014, 10, 21));
  }

}
