/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.Perturbation;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.CalculationRules;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculation.Results;
import com.opengamma.strata.engine.calculation.function.result.ScenarioResult;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.MarketEnvironment;
import com.opengamma.strata.engine.marketdata.scenario.PerturbationMapping;
import com.opengamma.strata.engine.marketdata.scenario.ScenarioDefinition;
import com.opengamma.strata.examples.engine.ExampleEngine;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.MarketDataBuilder;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;
import com.opengamma.strata.finance.rate.swap.SwapTrade;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.function.marketdata.scenario.curve.AnyCurveFilter;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.perturb.CurveParallelShift;

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
    // the trade that will have measures calculated
    List<Trade> trades = ImmutableList.of(createVanillaFixedVsLibor3mSwap());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measure.PRESENT_VALUE),
        Column.of(Measure.PV01));

    // use the built-in example market data
    MarketDataBuilder marketDataBuilder = ExampleMarketData.builder();

    // the complete set of rules for calculating measures
    CalculationRules rules = CalculationRules.builder()
        .pricingRules(StandardComponents.pricingRules())
        .marketDataRules(marketDataBuilder.rules())
        .reportingRules(ReportingRules.fixedCurrency(Currency.USD))
        .build();

    // two perturbations that can be applied to curves
    List<Perturbation<Curve>> curvePerturbations = ImmutableList.of(
        Perturbation.none(),                  // no shift for the base scenario
        CurveParallelShift.absolute(ONE_BP)); // 1bp absolute shift to calibrated curves (zeros)

    // mappings that select which market data to apply perturbations to
    // this applies the perturbations above to all curves
    PerturbationMapping<Curve> mapping = PerturbationMapping.of(
        Curve.class,
        AnyCurveFilter.INSTANCE,
        curvePerturbations);

    // create a scenario definition containing the single mapping above
    // this creates two scenarios - one for each perturbation in the mapping
    ScenarioDefinition scenarioDefinition = ScenarioDefinition.ofMappings(mapping);

    // build a market data snapshot for the valuation date
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    MarketEnvironment snapshot = marketDataBuilder.buildSnapshot(valuationDate);

    // create the engine and calculate the results
    CalculationEngine engine = ExampleEngine.create();
    Results results = engine.calculate(trades, columns, rules, snapshot, scenarioDefinition);

    // TODO Replace the results processing below with a report once the reporting framework supports scenarios

    // The results are lists of currency amounts containing one value for each scenario
    ScenarioResult<?> pvList = (ScenarioResult<?>) results.get(0, 0).getValue();
    ScenarioResult<?> pv01List = (ScenarioResult<?>) results.get(0, 1).getValue();

    double pvBase = ((CurrencyAmount) pvList.get(0)).getAmount();
    double pvShifted = ((CurrencyAmount) pvList.get(1)).getAmount();
    double pv01Base = ((CurrencyAmount) pv01List.get(0)).getAmount();
    NumberFormat numberFormat = new DecimalFormat("###,##0.00");

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
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
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
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.USNY))
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
        .tradeInfo(TradeInfo.builder()
            .attributes(ImmutableMap.of("description", "Fixed vs Libor 3m"))
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2014, 9, 12))
            .build())
        .build();
  }
}
