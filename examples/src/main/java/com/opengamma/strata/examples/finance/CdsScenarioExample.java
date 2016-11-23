/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.finance;

import static com.opengamma.strata.measure.StandardComponents.marketDataFactory;
import static java.util.stream.Collectors.toList;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
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
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.MarketDataBox;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.data.scenario.ScenarioPerturbation;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitRedCode;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitSingleNameCreditCurveDataParser;
import com.opengamma.strata.examples.marketdata.credit.markit.MarkitYieldCurveDataParser;
import com.opengamma.strata.math.impl.statistics.descriptive.SampleInterpolationQuantileMethod;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.pricer.credit.IsdaCreditCurveInputs;
import com.opengamma.strata.pricer.credit.IsdaSingleNameCreditCurveInputsId;
import com.opengamma.strata.pricer.credit.IsdaYieldCurveInputs;
import com.opengamma.strata.pricer.credit.IsdaYieldCurveInputsId;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.CdsTrade;
import com.opengamma.strata.product.credit.RestructuringClause;
import com.opengamma.strata.product.credit.SeniorityLevel;
import com.opengamma.strata.product.credit.SingleNameReferenceInformation;
import com.opengamma.strata.product.credit.type.CdsConventions;

/**
 * Example to illustrate using the calculation API to run a set of scenarios on a single name CDS.
 * <p>
 * In this example we load the interest rate curve and the credit curve required to price the CDS using the built-in
 * curve file loaders.
 * <p>
 * We then define scenarios in which each point on the credit curve is randomly perturbed, illustrating a very simple
 * Monte Carlo approach. It would be equally possible to use other sources of data, such as historical credit spreads,
 * to generate the perturbations instead.
 * <p>
 * The calculation API is used to obtain the present value under each scenario, producing a vector of results.
 * We then transform this into a vector of P&Ls, which we use to calculate VaR.
 */
public class CdsScenarioExample {

  private static final String MARKET_DATA_RESOURCE_ROOT = "example-marketdata";

  public static void main(String[] args) {
    // set up calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      calculate(runner);
    }
  }

  // loads the trade and market data, and performs the calculations
  private static void calculate(CalculationRunner runner) {
    // the trade to price
    CdsTrade cds = createSingleNameCds();
    List<Trade> trades = ImmutableList.of(cds);

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE));

    // build the set of market data for the base scenario on the valuation date
    // this is the snapshot which will be perturbed in the scenarios
    MarketData baseMarketData = buildBaseMarketData();

    // build scenarios containing credit curve shifts
    ScenarioDefinition scenarios = buildScenarios(baseMarketData);

    // use the standard rules defining how to calculate the measures we are requesting
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    CalculationRules rules = CalculationRules.of(functions);

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
    CurrencyScenarioArray pvVector = (CurrencyScenarioArray) results.get(0, 0).getValue();
    outputCurrencyValues("PVs", pvVector);

    // transform the present values into P&Ls, sorted from greatest loss to greatest profit
    CurrencyScenarioArray pnlVector = getSortedPnls(pvVector);
    outputCurrencyValues("Scenario PnLs", pnlVector);

    // use a built-in utility to calculate VaR
    // since the P&Ls are sorted starting with the greatest loss, the 95% greatest loss occurs at the 5% position
    double var95 = SampleInterpolationQuantileMethod.DEFAULT.quantileFromSorted(0.05, pnlVector.getAmounts().getValues());
    System.out.println(Messages.format("95% VaR: {}", var95));
  }

  //-------------------------------------------------------------------------
  // builds the set of market data representing the base scenario
  private static MarketData buildBaseMarketData() {

    // initialise the market data builder for the valuation date
    LocalDate valuationDate = LocalDate.of(2014, 10, 16);
    ImmutableMarketDataBuilder baseMarketDataBuilder = ImmutableMarketData.builder(valuationDate);

    // add yield curves
    loadBaseYieldCurves(baseMarketDataBuilder);

    // add credit curves
    loadBaseSingleNameCreditCurves(baseMarketDataBuilder);

    // build a single market data snapshot for the valuation date
    return baseMarketDataBuilder.build();
  }

  // loads the base yield curves from a fixed resource
  private static void loadBaseYieldCurves(ImmutableMarketDataBuilder builder) {
    ResourceLocator resource = ResourceLocator.ofClasspath(MARKET_DATA_RESOURCE_ROOT + "/credit/2014-10-16/cds.yieldCurves.csv");
    CharSource inputSource = resource.getCharSource();

    // use the built-in markit yield curve parser
    Map<IsdaYieldCurveInputsId, IsdaYieldCurveInputs> yieldCurves = MarkitYieldCurveDataParser.parse(inputSource);

    builder.addValueMap(yieldCurves);
  }

  // loads the base single name credit curves from a fixed resource
  private static void loadBaseSingleNameCreditCurves(ImmutableMarketDataBuilder builder) {
    ResourceLocator curvesResource =
        ResourceLocator.ofClasspath(MARKET_DATA_RESOURCE_ROOT + "/credit/2014-10-16/singleName.creditCurves.csv");
    CharSource curvesInputSource = curvesResource.getCharSource();

    ResourceLocator staticDataResource =
        ResourceLocator.ofClasspath(MARKET_DATA_RESOURCE_ROOT + "/credit/2014-10-16/singleName.staticData.csv");
    CharSource staticDataInputSource = staticDataResource.getCharSource();

    // use the built-in markit credit curve parser
    MarkitSingleNameCreditCurveDataParser.parse(builder, curvesInputSource, staticDataInputSource);
  }

  //-----------------------------------------------------------------------
  // create a single name CDS with 100 bps coupon
  private static CdsTrade createSingleNameCds() {
    return CdsConventions.USD_NORTH_AMERICAN
        .toTrade(
            LocalDate.of(2014, 9, 22),
            LocalDate.of(2019, 12, 20),
            BuySell.BUY,
            100_000_000d,
            0.0100,
            SingleNameReferenceInformation.of(
                MarkitRedCode.id("COMP01"),
                SeniorityLevel.SENIOR_UNSECURED_FOREIGN,
                Currency.USD,
                RestructuringClause.NO_RESTRUCTURING_2014),
            3_694_117.72d,
            LocalDate.of(2014, 10, 21));
  }

  //-----------------------------------------------------------------------
  // build the scenarios to use to perturb the base market data
  private static ScenarioDefinition buildScenarios(MarketData baseMarketData) {

    // build perturbations for each single name credit curve in the base market data set
    // here we are only interested in one credit curve, but this shows how we might deal with a larger set
    // each perturbation mapping represents the shifts to apply to one item of market data (here, a curve) in all scenarios
    List<PerturbationMapping<?>> perturbations = baseMarketData.getIds().stream()
        .filter(id -> id instanceof IsdaSingleNameCreditCurveInputsId)
        .map(IsdaSingleNameCreditCurveInputsId.class::cast)
        .map(id -> PerturbationMapping.of(
            IsdaCreditCurveInputs.class,
            IsdaCreditCurveFilter.of(id),
            buildScenarioShifts(baseMarketData.getValue(id))))
        .collect(toList());

    // we could add perturbations to other pieces of market data by continuing to add to the perturbations list
    // each perturbation mapping must contain shifts for the same number of scenarios

    // together the perturbations to the items of market data define the complete set of scenarios
    return ScenarioDefinition.ofMappings(perturbations);
  }

  // build the shifts to apply to a given curve in each scenario
  private static IsdaCreditCurveShifts buildScenarioShifts(IsdaCreditCurveInputs baseCurve) {

    // create a shift matrix - one row per scenario, each column representing a nodal point on the curve
    int scenarioCount = 100;
    int curveNodes = baseCurve.getNumberOfPoints();
    double[][] relativeShifts = new double[scenarioCount + 1][curveNodes];

    // include a base scenario with no shifts (i.e. multiply by 1)
    for (int nodeIndex = 0; nodeIndex < curveNodes; nodeIndex++) {
      relativeShifts[0][nodeIndex] = 1;
    }

    // for the remaining scenarios, create random relative shifts between 0.95 and 1.05 for illustration
    // here we could instead calculate the shifts from historical data, perhaps applying a weighting such as EWMA
    Random r = new Random();
    for (int scenarioIndex = 1; scenarioIndex <= scenarioCount; scenarioIndex++) {
      for (int nodeIndex = 0; nodeIndex < curveNodes; nodeIndex++) {
        relativeShifts[scenarioIndex][nodeIndex] = 1 + ((r.nextDouble() - 0.5) * 0.1);
      }
    }

    // return the shifts object which will be applied to the base market data
    DoubleMatrix shiftMatrix = DoubleMatrix.copyOf(relativeShifts);
    return new IsdaCreditCurveShifts(shiftMatrix);
  }

  //-------------------------------------------------------------------------
  private static CurrencyScenarioArray getSortedPnls(CurrencyScenarioArray pvVector) {
    double[] scenarioPnls = new double[pvVector.getScenarioCount() - 1];

    // the base PV was calculated in the first scenario where no shifts were applied
    double basePv = pvVector.get(0).getAmount();

    // for the remaining scenarios, work out the scenario P&L by subtracting the base PV
    for (int i = 1; i < pvVector.getScenarioCount(); i++) {
      double scenarioPv = pvVector.get(i).getAmount();
      double pnl = scenarioPv - basePv;
      scenarioPnls[i - 1] = pnl;
    }

    // sort the P&Ls, so we have the highest loss to the highest profit
    Arrays.sort(scenarioPnls);

    return CurrencyScenarioArray.of(pvVector.getCurrency(), DoubleArray.ofUnsafe(scenarioPnls));
  }

  //-------------------------------------------------------------------------
  private static void outputCurrencyValues(String title, CurrencyScenarioArray currencyValues) {
    NumberFormat numberFormat = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.ENGLISH));
    System.out.println(Messages.format("{} ({}):", title, currencyValues.getCurrency()));
    for (int i = 0; i < currencyValues.getScenarioCount(); i++) {
      double scenarioValue = currencyValues.get(i).getAmount();
      System.out.println(numberFormat.format(scenarioValue));
    }
    System.out.println();
  }

  //-------------------------------------------------------------------------
  // implements shifts to a credit curve across all scenarios
  // this custom implementation of ScenarioPerturbation is necessary for CDS where non-standard market data types are used
  // for other asset clases, the built-in CurvePointShifts may be used
  private static class IsdaCreditCurveShifts implements ScenarioPerturbation<IsdaCreditCurveInputs> {

    private final DoubleMatrix shifts;

    public IsdaCreditCurveShifts(DoubleMatrix shifts) {
      this.shifts = shifts;
    }

    @Override
    public MarketDataBox<IsdaCreditCurveInputs> applyTo(MarketDataBox<IsdaCreditCurveInputs> marketData, ReferenceData refData) {
      return marketData.mapWithIndex(shifts.rowCount(), (curve, scenarioIndex) -> applyShifts(scenarioIndex, curve));
    }

    @Override
    public int getScenarioCount() {
      return shifts.rowCount();
    }

    private IsdaCreditCurveInputs applyShifts(int scenarioIndex, IsdaCreditCurveInputs curve) {
      // return the manipulated curve
      return IsdaCreditCurveInputs.of(
          curve.getName(),
          curve.getCreditCurvePoints(),
          curve.getEndDatePoints(),
          getShiftedParRates(curve.getParRates(), shifts.rowArray(scenarioIndex)),
          curve.getCdsConvention(),
          curve.getScalingFactor());
    }

    private double[] getShiftedParRates(double[] parRates, double[] shifts) {
      double[] shiftedParRates = new double[parRates.length];
      for (int i = 0; i < shiftedParRates.length; i++) {
        // multiply the par rates from the base curve by the relative shifts generated for this scenario
        shiftedParRates[i] = parRates[i] * shifts[i];
      }
      return shiftedParRates;
    }

  }

  // implements a filter which selects the credit curve to perturb based on matching a given ID
  // this custom implementation of MarketDataFilter is necessary for CDS where non-standard market data types are used
  // for other asset classes, the built-in CurveNameFilter or AllCurvesFilter may be used
  private static class IsdaCreditCurveFilter
      implements MarketDataFilter<IsdaCreditCurveInputs, IsdaSingleNameCreditCurveInputsId> {

    private final IsdaSingleNameCreditCurveInputsId id;

    public static IsdaCreditCurveFilter of(IsdaSingleNameCreditCurveInputsId id) {
      return new IsdaCreditCurveFilter(id);
    }

    private IsdaCreditCurveFilter(IsdaSingleNameCreditCurveInputsId id) {
      this.id = id;
    }

    @Override
    public Class<?> getMarketDataIdType() {
      return IsdaSingleNameCreditCurveInputsId.class;
    }

    @Override
    public boolean matches(
        IsdaSingleNameCreditCurveInputsId marketDataId,
        MarketDataBox<IsdaCreditCurveInputs> marketData,
        ReferenceData refData) {

      return this.id.equals(marketDataId);
    }

  }

}
