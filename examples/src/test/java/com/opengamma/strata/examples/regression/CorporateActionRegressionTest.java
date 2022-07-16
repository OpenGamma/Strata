

/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.regression;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.examples.marketdata.CorporateActionExampleData;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardCorporateActionComponents;
import com.opengamma.strata.product.*;
import com.opengamma.strata.product.corporateaction.*;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.opengamma.strata.basics.currency.Currency.GBP;

/**
 * Example to illustrate using the calculation API to price a swap.
 * <p>
 * This makes use of the example market data environment.
 */
public class CorporateActionRegressionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   *
   * @param args  ignored
   */
  @Test
  public void testResults() {

    // the trades that will have measures calculated
    List<SecuritizedProductPosition> corporateActions = createCorporateActions();

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.GROSS_VALUE));

    // use the built-in example market data
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardCorporateActionComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions);

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    Results results = CalculationRunner.ofMultiThreaded().calculate(rules, corporateActions, columns, marketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, corporateActions, columns, results, functions, refData);

    TradeReportTemplate reportTemplate = CorporateActionExampleData.loadCorporateActionReportTemplate("corporateaction-report-regression-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);

    System.out.println(tradeReport.toAsciiTableString());
    String expectedResults = ExampleData.loadExpectedResults("corporateaction-report");

    //TradeReportRegressionTestUtils.assertAsciiTableEquals(tradeReport.toAsciiTableString(), expectedResults);
  }

  //-----------------------------------------------------------------------
  // create swap trades
  private static List<SecuritizedProductPosition> createCorporateActions() {
    return ImmutableList.of(
        createCashDividend());
  }

  //-----------------------------------------------------------------------

  private static SecuritizedProductPosition createCashDividend() {

    CorporateActionInfo info = CorporateActionInfo.builder()
        .corpRefProvider(StandardId.of("OG-Test", "ABC"))
        .corpRefOfficial(StandardId.of("OG-Test", "CBA"))
        .addAttribute(AttributeType.DESCRIPTION, "Cash Dividend")
        .eventType(CorporateActionEventType.of("Cash Dividend"))
        .build();

    AnnouncementCorporateAction corporateAction =
        SingleCashPaymentConventions.CASH_DIVIDEND_MANDATORY.toAnnouncementCorporateAction(
            info,
            SecurityId.of(StandardId.of("US0378331005", "APPLE")),
            1D,
            LocalDate.of(2022, 07, 16),
            CurrencyAmount.of(GBP, 10)
        );


    SecuritizedProductPosition corporateActionPosition = AnnouncementCorporateActionPosition.ofNet(
        PositionInfo.of(StandardId.of("Account", "123")),
        corporateAction, 100d);

    return corporateActionPosition;
  }



}

