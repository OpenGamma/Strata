/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;

import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataId;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
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
import com.opengamma.strata.product.GenericSecurity;
import com.opengamma.strata.product.GenericSecurityTrade;
import com.opengamma.strata.product.SecurityAttributeType;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityInfo;
import com.opengamma.strata.product.SecurityTrade;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeAttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Example to illustrate using the engine to price generic securities.
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class GenericSecurityPricingExample {

  private static final SecurityAttributeType<String> EXCHANGE_TYPE = SecurityAttributeType.of("exchange");
  private static final SecurityAttributeType<String> PRODUCT_FAMILY_TYPE = SecurityAttributeType.of("productFamily");
  private static final SecurityAttributeType<LocalDate> EXPIRY_TYPE = SecurityAttributeType.of("expiryDate");
  private static final SecurityId FGBL_MAR14_ID = SecurityId.of("OG-Future", "Eurex-FGBL-Mar14");
  private static final SecurityId OGBL_MAR14_C150_ID = SecurityId.of("OG-FutOpt", "Eurex-OGBL-Mar14-C150");
  private static final SecurityId ED_MAR14_ID = SecurityId.of("OG-Future", "CME-ED-Mar14");
  private static final GenericSecurity FGBL_MAR14 = GenericSecurity.of(
      SecurityInfo.of(FGBL_MAR14_ID, 0.01, CurrencyAmount.of(EUR, 10))
          .withAttribute(EXCHANGE_TYPE, "Eurex")
          .withAttribute(PRODUCT_FAMILY_TYPE, "FGBL")
          .withAttribute(EXPIRY_TYPE, LocalDate.of(2014, 3, 13)));
  private static final GenericSecurity OGBL_MAR14_C150 = GenericSecurity.of(
      SecurityInfo.of(OGBL_MAR14_C150_ID, 0.01, CurrencyAmount.of(EUR, 10))
          .withAttribute(EXCHANGE_TYPE, "Eurex")
          .withAttribute(PRODUCT_FAMILY_TYPE, "OGBL")
          .withAttribute(EXPIRY_TYPE, LocalDate.of(2014, 3, 10)));
  private static final GenericSecurity ED_MAR14 = GenericSecurity.of(
      SecurityInfo.of(ED_MAR14_ID, 0.005, CurrencyAmount.of(USD, 12.5))
          .withAttribute(EXCHANGE_TYPE, "CME")
          .withAttribute(PRODUCT_FAMILY_TYPE, "ED")
          .withAttribute(EXPIRY_TYPE, LocalDate.of(2014, 3, 10)));

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
    List<Trade> trades = ImmutableList.of(
        createFutureTrade1(), createFutureTrade2(), createOptionTrade1(), createOptionTrade2());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE));

    // use the built-in example market data
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions);

    // the reference data, such as holidays and securities
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.<ReferenceDataId<?>, Object>of(
        FGBL_MAR14_ID, FGBL_MAR14, OGBL_MAR14_C150_ID, OGBL_MAR14_C150, ED_MAR14_ID, ED_MAR14));

    // calculate the results
    Results results = runner.calculate(rules, trades, columns, marketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, functions, refData);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("security-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------  
  // create a futures trade where the security is looked up in reference data
  private static Trade createFutureTrade1() {
    TradeInfo tradeInfo = TradeInfo.builder()
        .addAttribute(TradeAttributeType.DESCRIPTION, "20 x Euro-Bund Mar14")
        .counterparty(StandardId.of("mn", "Dealer G"))
        .settlementDate(LocalDate.of(2013, 12, 15))
        .build();
    return SecurityTrade.of(tradeInfo, FGBL_MAR14_ID, 20, 99.550);
  }

  // create a futures trade that embeds details of the security
  private static Trade createFutureTrade2() {
    TradeInfo tradeInfo = TradeInfo.builder()
        .addAttribute(TradeAttributeType.DESCRIPTION, "8 x EuroDollar Mar14")
        .counterparty(StandardId.of("mn", "Dealer G"))
        .settlementDate(LocalDate.of(2013, 12, 18))
        .build();
    return GenericSecurityTrade.of(tradeInfo, ED_MAR14, 8, 99.550);
  }

  // create an options trade where the security is looked up in reference data
  private static Trade createOptionTrade1() {
    TradeInfo tradeInfo = TradeInfo.builder()
        .addAttribute(TradeAttributeType.DESCRIPTION, "20 x Call on Euro-Bund Mar14")
        .counterparty(StandardId.of("mn", "Dealer G"))
        .settlementDate(LocalDate.of(2013, 1, 15))
        .build();
    return SecurityTrade.of(tradeInfo, OGBL_MAR14_C150_ID, 20, 1.6);
  }

  // create an options trade that embeds details of the security
  private static Trade createOptionTrade2() {
    TradeInfo tradeInfo = TradeInfo.builder()
        .addAttribute(TradeAttributeType.DESCRIPTION, "15 x Call on Euro-Bund Mar14")
        .counterparty(StandardId.of("mn", "Dealer G"))
        .settlementDate(LocalDate.of(2013, 1, 15))
        .build();
    return GenericSecurityTrade.of(tradeInfo, OGBL_MAR14_C150, 15, 1.62);
  }

}
