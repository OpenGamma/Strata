/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.function.StandardComponents.marketDataFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.Trade;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.function.marketdata.curve.CurveParallelShifts;
import com.opengamma.strata.function.marketdata.scenario.curve.AnyCurveFilter;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.product.TradeAttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Example to illustrate using the scenario framework to apply shifts to calibrated curves.
 * <p>
 * Two scenarios are run:
 * <ul>
 *   <li>A base scenario with no perturbations applied to the market data</li>
 *   <li>A scenario with a 1 basis point shift applied to all curves</li>
 * </ul>
 * Present value and PV01 are calculated for a single swap. The present value from the second scenario
 * is compared to the sum of the present value and PV01 from the base scenario.
 * <p>
 * This makes use of the example engine and the example market data environment.
 */
public class CurveScenarioExample {

  private static final double ONE_BP = 1e-4;

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
    // the trade that will have measures calculated
    List<Trade> trades = ImmutableList.of(createVanillaFixedVsLibor3mSwap());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.PV01));

    // use the built-in example market data
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataRules(marketDataBuilder.rules())
        .reportingCurrency(ReportingCurrency.of(Currency.USD))
        .build();

    // mappings that select which market data to apply perturbations to
    // this applies the perturbations above to all curves
    PerturbationMapping<Curve> mapping = PerturbationMapping.of(
        Curve.class,
        AnyCurveFilter.INSTANCE,
        // no shift for the base scenario, 1bp absolute shift to calibrated curves (zeros)
        CurveParallelShifts.absolute(0, ONE_BP));

    // create a scenario definition containing the single mapping above
    // this creates two scenarios - one for each perturbation in the mapping
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping);

    // build a market data snapshot for the valuation date
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    MarketEnvironment marketSnapshot = marketDataBuilder.buildSnapshot(valuationDate);

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, refData);
    MarketEnvironment enhancedMarketData = marketDataFactory()
        .buildMarketData(reqs, MarketDataConfig.empty(), marketSnapshot, refData, scenarioDefinition);
    Results results = runner.calculateMultipleScenarios(rules, trades, columns, enhancedMarketData, refData);

    // TODO Replace the results processing below with a report once the reporting framework supports scenarios

    // The results are lists of currency amounts containing one value for each scenario
    ScenarioResult<?> pvList = (ScenarioResult<?>) results.get(0, 0).getValue();
    ScenarioResult<?> pv01List = (ScenarioResult<?>) results.get(0, 1).getValue();

    double pvBase = ((CurrencyAmount) pvList.get(0)).getAmount();
    double pvShifted = ((CurrencyAmount) pvList.get(1)).getAmount();
    double pv01Base = ((CurrencyAmount) pv01List.get(0)).getAmount();
    NumberFormat numberFormat = new DecimalFormat("###,##0.00", new DecimalFormatSymbols(Locale.ENGLISH));

    System.out.println("                         PV (base) = " + numberFormat.format(pvBase));
    System.out.println("             PV (1 bp curve shift) = " + numberFormat.format(pvShifted));
    System.out.println("PV01 (algorithmic differentiation) = " + numberFormat.format(pv01Base));
    System.out.println("          PV01 (finite difference) = " + numberFormat.format(pvShifted - pvBase));
  }

  //-----------------------------------------------------------------------
  // create a vanilla fixed vs libor 3m swap
  private static Trade createVanillaFixedVsLibor3mSwap() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 100_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendarIds.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(FixedRateCalculation.of(0.015, DayCounts.THIRTY_U_360))
        .build();

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2021, 9, 12))
            .frequency(Frequency.P3M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendarIds.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_3M))
        .build();

    return SwapTrade.builder()
        .product(Swap.of(payLeg, receiveLeg))
        .info(TradeInfo.builder()
            .addAttribute(TradeAttributeType.DESCRIPTION, "Fixed vs Libor 3m")
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }
}
