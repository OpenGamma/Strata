package com.opengamma.strata.measure.fxopt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.fxopt.FxCollar;
import com.opengamma.strata.product.fxopt.FxCollarTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxCollarTrade;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FxCollarTradeCalculationFunction implements CalculationFunction<FxCollarTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, FxCollarMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, FxCollarMeasureCalculations.DEFAULT::pv01RatesCalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, FxCollarMeasureCalculations.DEFAULT::pv01RatesCalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, FxCollarMeasureCalculations.DEFAULT::pv01RatesMarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, FxCollarMeasureCalculations.DEFAULT::pv01RatesMarketQuoteBucketed)
          .put(Measures.CURRENCY_EXPOSURE, FxCollarMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, FxCollarMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.VEGA_MARKET_QUOTE_BUCKETED, FxCollarMeasureCalculations.DEFAULT::vegaMarketQuoteBucketed)
          .put(Measures.RESOLVED_TARGET, (rt, smd, m) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public FxCollarTradeCalculationFunction() {
  }

  @Override
  public Class<FxCollarTrade> targetType() {
    return FxCollarTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(FxCollarTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(FxCollarTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrencyPair().getBase();
  }

  @Override
  public FunctionRequirements requirements(
      FxCollarTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    FxCollar product = trade.getProduct();
    CurrencyPair currencyPair = product.getCurrencyPair();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(
        ImmutableSet.of(currencyPair.getBase(), currencyPair.getCounter()));
    FxOptionMarketDataLookup optionLookup = parameters.getParameter(FxOptionMarketDataLookup.class);
    FunctionRequirements optionReqs = optionLookup.requirements(currencyPair);
    return ratesReqs.combinedWith(optionReqs);
  }

  @Override
  public Map<Measure, Result<?>> calculate(
      FxCollarTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // expand the trade once for all measures and all scenarios
    ResolvedFxCollarTrade resolved = trade.resolve(refData);
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData ratesMarketData = ratesLookup.marketDataView(scenarioMarketData);
    FxOptionMarketDataLookup optionLookup = parameters.getParameter(FxOptionMarketDataLookup.class);
    FxOptionScenarioMarketData optionMarketData = optionLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, ratesMarketData, optionMarketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedFxCollarTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for FxCollarTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, ratesMarketData, optionMarketData));
  }

  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedFxCollarTrade trade,
        RatesScenarioMarketData ratesMarketData,
        FxOptionScenarioMarketData optionMarketData);
  }
}
