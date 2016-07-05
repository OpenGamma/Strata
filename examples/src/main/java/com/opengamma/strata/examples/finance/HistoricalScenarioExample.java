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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
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
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.loader.csv.RatesCurvesCsvLoader;
import com.opengamma.strata.market.ShiftType;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurvePointShifts;
import com.opengamma.strata.market.curve.CurvePointShiftsBuilder;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleInterpolationQuantileMethod;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.rate.RatesMarketData;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
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
 * Example to illustrate using the calculation API to run a set of historical scenarios on a single swap.
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

  // loads the trade and market data, and performs the calculations
  private static void calculate(CalculationRunner runner) {
    // the trade to price
    List<Trade> trades = ImmutableList.of(createTrade());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE));

    // build the set of market data for the base scenario on the valuation date
    // this is the snapshot which will be perturbed in the scenarios
    RatesMarketData baseMarketData = buildBaseMarketData();

    // build the historical scenarios
    ScenarioDefinition scenarios = buildScenarios(baseMarketData.getMarketData());

    // use the standard rules defining how to calculate the measures we are requesting
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions, baseMarketData.getLookup());

    // use the built-in reference data, which includes some holiday calendars
    ReferenceData refData = ReferenceData.standard();

    // now combine the base market data with the scenario definition to create the full set of scenario market data
    MarketDataRequirements reqs = MarketDataRequirements.of(rules, trades, columns, refData);
    ScenarioMarketData scenarioMarketData =
        marketDataFactory().createMultiScenario(reqs, MarketDataConfig.empty(), baseMarketData, refData, scenarios);

    // calculate the results
    Results results = runner.calculateMultiScenario(rules, trades, columns, scenarioMarketData, refData);

    // the results contain the one measure requested (Present Value) for each scenario
    // the first scenario is the base
    ScenarioArray<?> pvVector = (ScenarioArray<?>) results.get(0, 0).getValue();
    outputCurrencyValues("PVs", pvVector);

    // transform the present values into P&Ls, sorted from greatest loss to greatest profit
    CurrencyValuesArray pnlVector = getSortedPnls(pvVector);
    outputCurrencyValues("Scenario PnLs", pnlVector);

    // use a built-in utility to calculate VaR
    // since the P&Ls are sorted starting with the greatest loss, the 95% greatest loss occurs at the 5% position
    double var95 = SampleInterpolationQuantileMethod.DEFAULT.quantileFromSorted(0.05, pnlVector.getValues());
    System.out.println(Messages.format("95% VaR: {}", var95));
  }

  //-------------------------------------------------------------------------
  // builds the set of market data representing the base scenario
  private static RatesMarketData buildBaseMarketData() {

    // initialise the market data builder for the valuation date
    LocalDate valuationDate = LocalDate.of(2015, 4, 23);
    ImmutableMarketDataBuilder baseMarketDataBuilder = ImmutableMarketData.builder(valuationDate);

    ResourceLocator curveGroupsResource = ResourceLocator.ofClasspath(MARKET_DATA_RESOURCE_ROOT + "/curves/groups.csv");
    ResourceLocator curveSettingsResource = ResourceLocator.ofClasspath(MARKET_DATA_RESOURCE_ROOT + "/curves/settings.csv");
    ResourceLocator curvesResource = ResourceLocator.ofClasspath(MARKET_DATA_RESOURCE_ROOT + "/curves/2015-04-23.csv");
    List<ResourceLocator> curvesResources = ImmutableList.of(curvesResource);

    List<CurveGroup> baseCurveGroups = RatesCurvesCsvLoader.load(valuationDate, curveGroupsResource, curveSettingsResource, curvesResources);
    CurveGroup baseCurveGroup = Iterables.getOnlyElement(baseCurveGroups);

    // build a single market data snapshot for the valuation date
    MarketData baseMarketData = baseMarketDataBuilder.build();

    return RatesMarketDataLookup.of(baseCurveGroup).marketDataView(baseMarketData);
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
