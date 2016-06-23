/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.MarketDataFilter;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.PerturbationMapping;
import com.opengamma.strata.calc.marketdata.ScenarioDefinition;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurvePointShifts;
import com.opengamma.strata.market.curve.CurvePointShiftsBuilder;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeAttributeType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Example to illustrate using the engine to run a set of historical scenarios on a single swap
 * to produce a P&L series. This P&L series could then be used to calculate historical VaR.
 * <p>
 * In this example we are provided with market data containing:
 * <li>a complete snapshot to value the swap on the valuation date (curves only as the swap is forward-starting)
 * <li>a series of historical curves for every date leading up to the valuation date
 * <p>
 * The differences between the zero rates in consecutive historical curves (dates d-1 and d)
 * are used to generate a scenario, later attributed to date d, containing these relative curve
 * shifts. The swap is then valued on the valuation date, applying each scenario to the base
 * snapshot from the valuation date, to produce a PV series. A P&L series is then generated from
 * this.
 * <p>
 * Instead of generating the perturbations on-the-fly from real data as in this example, the
 * scenario could be pre-generated and stored, or generated in any other way.
 */
public class HistoricalScenarioExample {

  private static final String MARKET_DATA_RESOURCE_ROOT = "example-historicalscenario-marketdata";

  public static void main(String[] args) {
    // setup calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      calculate(runner);
    }
  }

  // obtains the data and calculates the grid of results
  private static void calculate(CalculationRunner runner) {
    // the trades for which to calculate a P&L series
    List<Trade> trades = ImmutableList.of(createTrade());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE));

    // use the built-in example historical scenario market data
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketDataBuilder.ofResource(MARKET_DATA_RESOURCE_ROOT);

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions, marketDataBuilder.ratesLookup(LocalDate.of(2015, 4, 23)));

    // load the historical calibrated curves from which we will build our scenarios
    // these curves are provided in the example data environment
    SortedMap<LocalDate, CurveGroup> historicalCurves = marketDataBuilder.loadAllRatesCurves();

    // sorted list of dates for the available series of curves
    // the entries in the P&L vector we produce will correspond to these dates
    List<LocalDate> scenarioDates = new ArrayList<>(historicalCurves.keySet());

    // build the historical scenarios
    ScenarioDefinition historicalScenarios = buildHistoricalScenarios(historicalCurves, scenarioDates);

    // build a market data snapshot for the valuation date
    // this is the base snapshot which will be perturbed by the scenarios
    LocalDate valuationDate = LocalDate.of(2015, 4, 23);
    MarketData marketData = marketDataBuilder.buildSnapshot(valuationDate);

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, refData);
    ScenarioMarketData scenarioMarketData =
        marketDataFactory().createMultiScenario(reqs, MarketDataConfig.empty(), marketData, refData, historicalScenarios);
    Results results = runner.calculateMultiScenario(rules, trades, columns, scenarioMarketData, refData);

    // the results contain the one measure requested (Present Value) for each scenario
    ScenarioArray<?> scenarioValuations = (ScenarioArray<?>) results.get(0, 0).getValue();
    outputPnl(scenarioDates, scenarioValuations);
  }

  private static ScenarioDefinition buildHistoricalScenarios(
      Map<LocalDate, CurveGroup> historicalCurves,
      List<LocalDate> scenarioDates) {

    // extract the curves to perturb
    List<Curve> usdDiscountCurves = scenarioDates.stream()
        .map(date -> historicalCurves.get(date))
        .map(group -> group.findDiscountCurve(Currency.USD).get())
        .collect(toImmutableList());

    List<Curve> libor3mCurves = scenarioDates.stream()
        .map(date -> historicalCurves.get(date))
        .map(group -> group.findForwardCurve(IborIndices.USD_LIBOR_3M).get())
        .collect(toImmutableList());

    List<Curve> libor6mCurves = scenarioDates.stream()
        .map(date -> historicalCurves.get(date))
        .map(group -> group.findForwardCurve(IborIndices.USD_LIBOR_6M).get())
        .collect(toImmutableList());

    // create mappings which will cause the point shift perturbations generated above
    // to be applied to the correct curves
    PerturbationMapping<Curve> discountCurveMappings = PerturbationMapping.of(
        Curve.class,
        MarketDataFilter.ofName(CurveName.of("USD-Disc")),
        buildShifts(usdDiscountCurves));

    PerturbationMapping<Curve> libor3mMappings = PerturbationMapping.of(
        Curve.class,
        MarketDataFilter.ofName(CurveName.of("USD-3ML")),
        buildShifts(libor3mCurves));

    PerturbationMapping<Curve> libor6mMappings = PerturbationMapping.of(
        Curve.class,
        MarketDataFilter.ofName(CurveName.of("USD-6ML")),
        buildShifts(libor6mCurves));

    // create a scenario definition from these mappings
    return ScenarioDefinition.ofMappings(
        discountCurveMappings,
        libor3mMappings,
        libor6mMappings);
  }

  private static CurvePointShifts buildShifts(List<Curve> historicalCurves) {
    CurvePointShiftsBuilder builder = CurvePointShifts.builder(ShiftType.ABSOLUTE);

    for (int scenarioIndex = 1; scenarioIndex < historicalCurves.size(); scenarioIndex++) {
      Curve previousCurve = historicalCurves.get(scenarioIndex - 1);
      Curve curve = historicalCurves.get(scenarioIndex);

      // obtain the curve node metadata - this is used to identify a node to apply a perturbation to
      List<ParameterMetadata> curveNodeMetadata = curve.getMetadata().getParameterMetadata().get();

      // build up the shifts to apply to each node
      // these are calculated as the actual change in the zero rate at that node between the two scenario dates
      for (int curveNodeIdx = 0; curveNodeIdx < curve.getParameterCount(); curveNodeIdx++) {
        double zeroRate = curve.getParameter(curveNodeIdx);
        double previousZeroRate = previousCurve.getParameter(curveNodeIdx);
        double shift = (zeroRate - previousZeroRate);
        builder.addShift(scenarioIndex, curveNodeMetadata.get(curveNodeIdx).getIdentifier(), shift);
      }
    }
    return builder.build();
  }

  private static void outputPnl(List<LocalDate> scenarioDates, ScenarioArray<?> scenarioValuations) {
    NumberFormat numberFormat = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
    double basePv = ((CurrencyAmount) scenarioValuations.get(0)).getAmount();
    System.out.println("Base PV (USD): " + numberFormat.format(basePv));
    System.out.println();
    System.out.println("P&L series (USD):");
    for (int i = 1; i < scenarioValuations.getScenarioCount(); i++) {
      double scenarioPv = ((CurrencyAmount) scenarioValuations.get(i)).getAmount();
      double pnl = scenarioPv - basePv;
      LocalDate scenarioDate = scenarioDates.get(i);
      System.out.println(Messages.format("{} = {}", scenarioDate, numberFormat.format(pnl)));
    }
  }

  //-------------------------------------------------------------------------
  // create a libor 3m vs libor 6m swap
  private static Trade createTrade() {
    NotionalSchedule notional = NotionalSchedule.of(Currency.USD, 1_000_000);

    SwapLeg payLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2015, 9, 11))
            .endDate(LocalDate.of(2021, 9, 11))
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

    SwapLeg receiveLeg = RateCalculationSwapLeg.builder()
        .payReceive(PayReceive.RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2015, 9, 11))
            .endDate(LocalDate.of(2021, 9, 11))
            .frequency(Frequency.P6M)
            .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendarIds.USNY))
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(notional)
        .calculation(IborRateCalculation.of(IborIndices.USD_LIBOR_6M))
        .build();

    return SwapTrade.builder()
        .product(Swap.of(payLeg, receiveLeg))
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(TradeAttributeType.DESCRIPTION, "Libor 3m vs Libor 6m")
            .counterparty(StandardId.of("example", "A"))
            .settlementDate(LocalDate.of(2015, 9, 11))
            .build())
        .build();
  }

}
