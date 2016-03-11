/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.examples.data.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.future.GenericFuture;
import com.opengamma.strata.product.future.GenericFutureOption;
import com.opengamma.strata.product.future.GenericFutureOptionTrade;
import com.opengamma.strata.product.future.GenericFutureTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price generic Futures.
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class GenericFuturePricingExample {

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
    List<Trade> trades = ImmutableList.of(createFutureTrade1(), createFutureTrade2(), createOptionTrade1());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE));

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

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("future-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create a GenericFuture trade
  private static Trade createFutureTrade1() {
    GenericFuture product = GenericFuture.builder()
        .productId(StandardId.of("Eurex", "FGBL"))
        .expiryMonth(YearMonth.of(2014, 3))
        .expiryDate(LocalDate.of(2014, 3, 13))
        .tickSize(0.01)
        .tickValue(CurrencyAmount.of(EUR, 10))
        .build();

    return GenericFutureTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(product)
            .standardId(StandardId.of("OG-Future", "Eurex-FGBL-Mar14"))
            .build()))
        .tradeInfo(TradeInfo.builder()
            .attributes(ImmutableMap.of("description", "Euro-Bund Mar14"))
            .counterparty(StandardId.of("mn", "Dealer G"))
            .settlementDate(LocalDate.of(2013, 12, 15))
            .build())
        .quantity(20)
        .initialPrice(99.550)
        .build();
  }

  // create a GenericFuture trade
  private static Trade createFutureTrade2() {
    GenericFuture product = GenericFuture.builder()
        .productId(StandardId.of("CME", "ED"))
        .expiryMonth(YearMonth.of(2014, 3))
        .expiryDate(LocalDate.of(2014, 3, 10))
        .tickSize(0.005)
        .tickValue(CurrencyAmount.of(USD, 12.5))
        .build();

    return GenericFutureTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(product)
            .standardId(StandardId.of("OG-Future", "CME-ED-Mar14"))
            .build()))
        .tradeInfo(TradeInfo.builder()
            .attributes(ImmutableMap.of("description", "EuroDollar Mar14"))
            .counterparty(StandardId.of("mn", "Dealer G"))
            .settlementDate(LocalDate.of(2013, 12, 18))
            .build())
        .quantity(50)
        .initialPrice(99.550)
        .build();
  }

  // create a GenericFutureOption trade
  private static Trade createOptionTrade1() {
    GenericFuture future = GenericFuture.builder()
        .productId(StandardId.of("Eurex", "FGBL"))
        .expiryMonth(YearMonth.of(2014, 3))
        .expiryDate(LocalDate.of(2014, 3, 13))
        .tickSize(0.01)
        .tickValue(CurrencyAmount.of(EUR, 10))
        .build();

    GenericFutureOption product = GenericFutureOption.builder()
        .productId(StandardId.of("Eurex", "OGBL"))
        .expiryMonth(YearMonth.of(2014, 3))
        .expiryDate(LocalDate.of(2014, 3, 10))
        .tickSize(0.01)
        .tickValue(CurrencyAmount.of(EUR, 10))
        .underlyingLink(SecurityLink.resolved(UnitSecurity.builder(future)
            .standardId(StandardId.of("OG-Future", "Eurex-FGBL-Mar14"))
            .build()))
        .build();

    return GenericFutureOptionTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(product)
            .standardId(StandardId.of("OG-FutOpt", "Eurex-OGBL-Mar14-C150"))
            .build()))
        .tradeInfo(TradeInfo.builder()
            .attributes(ImmutableMap.of("description", "Call on Euro-Bund Mar14"))
            .counterparty(StandardId.of("mn", "Dealer G"))
            .settlementDate(LocalDate.of(2013, 1, 15))
            .build())
        .quantity(20)
        .initialPrice(1.6)
        .build();
  }

}
