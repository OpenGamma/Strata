/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.regression;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;

import java.time.LocalDate;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.runner.CalculationTaskRunner;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

/**
 * Regression test for an example swap report.
 */
@Test
public class SwapReportRegressionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  /**
   * Tests the full set of results against a golden copy.
   */
  public void testResults() {
    List<Trade> trades = ImmutableList.of(createTrade1());

    List<Column> columns = ImmutableList.of(
        Column.of(Measures.LEG_INITIAL_NOTIONAL),
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.LEG_PRESENT_VALUE),
        Column.of(Measures.PV01_CALIBRATED_SUM),
        Column.of(Measures.ACCRUED_INTEREST));

    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    LocalDate valuationDate = LocalDate.of(2009, 7, 31);
    CalculationRules rules = CalculationRules.of(
        StandardComponents.calculationFunctions(),
        Currency.USD,
        marketDataBuilder.ratesLookup(valuationDate));

    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTasks tasks = CalculationTasks.of(rules, trades, columns);
    MarketDataRequirements reqs = tasks.requirements(REF_DATA);
    MarketData calibratedMarketData = marketDataFactory().create(reqs, MarketDataConfig.empty(), marketData, REF_DATA);
    CalculationTaskRunner runner = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    Results results = runner.calculate(tasks, calibratedMarketData, REF_DATA);

    ReportCalculationResults calculationResults = ReportCalculationResults.of(valuationDate, trades, columns, results);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("swap-report-regression-test-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);

    String expectedResults = ExampleData.loadExpectedResults("swap-report");

    TradeReportRegressionTestUtils.assertAsciiTableEquals(tradeReport.toAsciiTableString(), expectedResults);
  }

  private static Trade createTrade1() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 12_000_000);

    PeriodicSchedule accrual = PeriodicSchedule.builder()
        .startDate(LocalDate.of(2006, 2, 24))
        .endDate(LocalDate.of(2011, 2, 24))
        .frequency(Frequency.P3M)
        .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendarIds.USNY))
        .build();

    PaymentSchedule payment = PaymentSchedule.builder()
        .paymentFrequency(Frequency.P3M)
        .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, HolidayCalendarIds.USNY))
        .build();

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.05004, DayCounts.ACT_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(accrual)
        .paymentSchedule(payment)
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .product(Swap.builder()
            .legs(payLeg, receiveLeg)
            .build())
        .info(TradeInfo.builder()
            .id(StandardId.of("mn", "14248"))
            .counterparty(StandardId.of("mn", "Dealer A"))
            .settlementDate(LocalDate.of(2006, 2, 24))
            .build())
        .build();
  }

}
