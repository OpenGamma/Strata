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
import com.opengamma.strata.basics.currency.CurrencyAmount;
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
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.UnitSecurity;
import com.opengamma.strata.finance.future.GenericFuture;
import com.opengamma.strata.finance.future.GenericFutureOption;
import com.opengamma.strata.finance.future.GenericFutureOptionTrade;
import com.opengamma.strata.finance.future.GenericFutureTrade;
import com.opengamma.strata.function.StandardComponents;
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
    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createFutureTrade1(), createFutureTrade2(), createOptionTrade1());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE));

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

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("future-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create a GenericFuture trade
  private static Trade createFutureTrade1() {
    GenericFuture product = GenericFuture.builder()
        .productId(StandardId.of("Eurex", "FGBL"))
        .expirationMonth(YearMonth.of(2014, 3))
        .expirationDate(LocalDate.of(2014, 3, 13))
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
        .expirationMonth(YearMonth.of(2014, 3))
        .expirationDate(LocalDate.of(2014, 3, 10))
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
        .expirationMonth(YearMonth.of(2014, 3))
        .expirationDate(LocalDate.of(2014, 3, 13))
        .tickSize(0.01)
        .tickValue(CurrencyAmount.of(EUR, 10))
        .build();

    GenericFutureOption product = GenericFutureOption.builder()
        .productId(StandardId.of("Eurex", "OGBL"))
        .expirationMonth(YearMonth.of(2014, 3))
        .expirationDate(LocalDate.of(2014, 3, 10))
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
