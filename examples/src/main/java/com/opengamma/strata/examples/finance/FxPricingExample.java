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
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.examples.data.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.product.TradeAttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.fx.FxSingleTrade;
import com.opengamma.strata.product.fx.FxSwap;
import com.opengamma.strata.product.fx.FxSwapTrade;
import com.opengamma.strata.product.payment.BulletPayment;
import com.opengamma.strata.product.payment.BulletPaymentTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price FX trades.
 * <p>
 * This makes use of the example engine and the example market data environment.
 * A bullet payment trade is also included.
 */
public class FxPricingExample {

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
    List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2(), createTrade3(), createTrade4());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.PV01),
        Column.of(Measures.BUCKETED_PV01));

    // use the built-in example market data
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataRules(marketDataBuilder.rules())
        .build();

    // build a market data snapshot for the valuation date
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    MarketEnvironment marketSnapshot = marketDataBuilder.buildSnapshot(valuationDate);

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    Results results = runner.calculateSingleScenario(rules, trades, columns, marketSnapshot, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, refData);

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
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "GBP 10,000/USD @ 1.62 fwd")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(LocalDate.of(2014, 9, 15))
            .build())
        .build();
  }

  // create an FX Forward trade
  private static Trade createTrade2() {
    FxSingle fx = FxSingle.of(CurrencyAmount.of(USD, 15000), FxRate.of(GBP, USD, 1.62), LocalDate.of(2014, 9, 14));
    return FxSingleTrade.builder()
        .product(fx)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "2"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "USD 15,000/GBP @ 1.62 fwd")
            .counterparty(StandardId.of("example", "BigBankB"))
            .settlementDate(LocalDate.of(2014, 9, 15))
            .build())
        .build();
  }

  // create an FX Swap trade
  private static Trade createTrade3() {
    FxSwap swap = FxSwap.ofForwardPoints(
        CurrencyAmount.of(GBP, 10000), FxRate.of(GBP, USD, 1.62), 0.03, LocalDate.of(2014, 6, 14), LocalDate.of(2014, 9, 14));
    return FxSwapTrade.builder()
        .product(swap)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "3"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "GBP 10,000/USD @ 1.62 swap")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(LocalDate.of(2014, 9, 15))
            .build())
        .build();
  }

  // create a Bullet Payment trade
  private static Trade createTrade4() {
    BulletPayment bp = BulletPayment.builder()
        .payReceive(PayReceive.PAY)
        .value(CurrencyAmount.of(GBP, 20_000))
        .date(AdjustableDate.of(LocalDate.of(2014, 9, 16)))
        .build();
    return BulletPaymentTrade.builder()
        .product(bp)
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "4"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "Bullet payment GBP 20,000")
            .counterparty(StandardId.of("example", "BigBankC"))
            .settlementDate(LocalDate.of(2014, 9, 16))
            .build())
        .build();
  }

}
