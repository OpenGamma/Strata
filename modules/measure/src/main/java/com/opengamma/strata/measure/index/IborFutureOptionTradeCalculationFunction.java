/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.index.IborFutureOption;
import com.opengamma.strata.product.index.IborFutureOptionTrade;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Perform calculations on a single {@code IborFutureOptionTrade} for each of a set of scenarios.
 * <p>
 * This uses Normal pricing.
 * An instance of {@link RatesMarketDataLookup} and {@link IborFutureOptionMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed}
 *   <li>{@linkplain Measures#UNIT_PRICE Unit price}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * 
 * <h4>Price</h4>
 * The price of an Ibor future option is based on the price of the underlying future, the volatility
 * and the time to expiry. The price of the at-the-money option tends to zero as expiry approaches.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor future options in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, an option price of 0.2 is related to a futures price of 99.32 that implies an
 * interest rate of 0.68%. Strata represents the price of the future as 0.9932 and thus
 * represents the price of the option as 0.002.
 */
public class IborFutureOptionTradeCalculationFunction
    implements CalculationFunction<IborFutureOptionTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, IborFutureOptionMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, IborFutureOptionMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, IborFutureOptionMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, IborFutureOptionMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, IborFutureOptionMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
          .put(Measures.UNIT_PRICE, IborFutureOptionMeasureCalculations.DEFAULT::unitPrice)
          .put(Measures.RESOLVED_TARGET, (rt, smd, m) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public IborFutureOptionTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<IborFutureOptionTrade> targetType() {
    return IborFutureOptionTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(IborFutureOptionTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(IborFutureOptionTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      IborFutureOptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    IborFutureOption option = trade.getProduct();
    QuoteId optionQuoteId = QuoteId.of(option.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
    Currency currency = option.getCurrency();
    IborIndex index = option.getIndex();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(currency, index);
    IborFutureOptionMarketDataLookup optionLookup = parameters.getParameter(IborFutureOptionMarketDataLookup.class);
    FunctionRequirements optionReqs = optionLookup.requirements(index);
    ImmutableSet<MarketDataId<?>> valueReqs = ImmutableSet.<MarketDataId<?>>builder()
        .add(optionQuoteId)
        .addAll(ratesReqs.getValueRequirements())
        .addAll(optionReqs.getValueRequirements())
        .build();
    return ratesReqs.toBuilder().valueRequirements(valueReqs).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      IborFutureOptionTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedIborFutureOptionTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData ratesMarketData = ratesLookup.marketDataView(scenarioMarketData);
    IborFutureOptionMarketDataLookup optionLookup = parameters.getParameter(IborFutureOptionMarketDataLookup.class);
    IborFutureOptionScenarioMarketData optionMarketData = optionLookup.marketDataView(scenarioMarketData);

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
      ResolvedIborFutureOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      IborFutureOptionScenarioMarketData optionMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for IborFutureOptionTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, ratesMarketData, optionMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedIborFutureOptionTrade trade,
        RatesScenarioMarketData ratesMarketData,
        IborFutureOptionScenarioMarketData optionMarketData);
  }

}
