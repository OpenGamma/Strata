/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.dsf;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
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
import com.opengamma.strata.product.dsf.Dsf;
import com.opengamma.strata.product.dsf.DsfTrade;
import com.opengamma.strata.product.dsf.ResolvedDsfTrade;

/**
 * Perform calculations on a single {@code DsfTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * An instance of {@link RatesMarketDataLookup} must be specified.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed}
 *   <li>{@linkplain Measures#UNIT_PRICE Unit price}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * <p>
 * The "natural" currency is the currency of the swap leg that is received.
 * 
 * <h4>Price</h4>
 * The price of a DSF is based on the present value (NPV) of the underlying swap on the delivery date.
 * For example, a price of 100.182 represents a present value of $100,182.00, if the notional is $100,000.
 * This price can also be viewed as a percentage present value - {@code (100 + percentPv)}, or 0.182% in this example.
 * <p>
 * Strata uses <i>decimal prices</i> for DSFs in the trade model, pricers and market data.
 * The decimal price is based on the decimal multiplier equivalent to the implied percentage.
 * Thus the market price of 100.182 is represented in Strata by 1.00182.
 */
public class DsfTradeCalculationFunction
    implements CalculationFunction<DsfTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, DsfMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, DsfMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, DsfMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, DsfMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED, DsfMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
          .put(Measures.UNIT_PRICE, DsfMeasureCalculations.DEFAULT::unitPrice)
          .put(Measures.CURRENCY_EXPOSURE, DsfMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.RESOLVED_TARGET, (rt, smd) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * Creates an instance.
   */
  public DsfTradeCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<DsfTrade> targetType() {
    return DsfTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(DsfTrade target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(DsfTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      DsfTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    Dsf product = trade.getProduct();
    QuoteId quoteId = QuoteId.of(trade.getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
    ImmutableSet<Index> indices = product.getUnderlyingSwap().allIndices();
    ImmutableSet<Currency> currencies = ImmutableSet.of(product.getCurrency());

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(currencies, indices);
    ImmutableSet<MarketDataId<?>> valueReqs = ImmutableSet.<MarketDataId<?>>builder()
        .add(quoteId)
        .addAll(ratesReqs.getValueRequirements())
        .build();
    return ratesReqs.toBuilder().valueRequirements(valueReqs).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      DsfTrade trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedDsfTrade resolved = trade.resolve(refData);

    // use lookup to query market data
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData marketData = ratesLookup.marketDataView(scenarioMarketData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, marketData));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedDsfTrade trade,
      RatesScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for DsfTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedDsfTrade trade,
        RatesScenarioMarketData marketData);
  }

}
