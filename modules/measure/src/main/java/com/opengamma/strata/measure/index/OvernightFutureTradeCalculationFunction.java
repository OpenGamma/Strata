/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FieldName;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.SecuritizedProductPortfolioItem;
import com.opengamma.strata.product.index.OvernightFuture;
import com.opengamma.strata.product.index.OvernightFuturePosition;
import com.opengamma.strata.product.index.OvernightFutureTrade;
import com.opengamma.strata.product.index.ResolvedOvernightFutureTrade;

/**
 * Perform calculations on a single {@code OvernightFutureTrade} for each of a set of scenarios.
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
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * 
 * <h4>Price</h4>
 * The price of an Overnight rate future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Overnight rate futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 * 
 * @param <T> the trade or position type
 */
public class OvernightFutureTradeCalculationFunction<T extends SecuritizedProductPortfolioItem<OvernightFuture> & Resolvable<ResolvedOvernightFutureTrade>>
    implements CalculationFunction<T> {

  /**
   * The trade instance
   */
  public static final OvernightFutureTradeCalculationFunction<OvernightFutureTrade> TRADE =
      new OvernightFutureTradeCalculationFunction<>(OvernightFutureTrade.class);
  /**
   * The position instance
   */
  public static final OvernightFutureTradeCalculationFunction<OvernightFuturePosition> POSITION =
      new OvernightFutureTradeCalculationFunction<>(OvernightFuturePosition.class);

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PRESENT_VALUE, OvernightFutureMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, OvernightFutureMeasureCalculations.DEFAULT::pv01CalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED, OvernightFutureMeasureCalculations.DEFAULT::pv01CalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM, OvernightFutureMeasureCalculations.DEFAULT::pv01MarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED,
              OvernightFutureMeasureCalculations.DEFAULT::pv01MarketQuoteBucketed)
          .put(Measures.UNIT_PRICE, OvernightFutureMeasureCalculations.DEFAULT::unitPrice)
          .put(Measures.PAR_SPREAD, OvernightFutureMeasureCalculations.DEFAULT::parSpread)
          .put(Measures.RESOLVED_TARGET, (rt, smd) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  /**
   * The trade or position type.
   */
  private final Class<T> targetType;

  /**
   * Creates an instance.
   * 
   * @param targetType  the trade or position type
   */
  public OvernightFutureTradeCalculationFunction(Class<T> targetType) {
    this.targetType = ArgChecker.notNull(targetType, "targetType");
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<T> targetType() {
    return targetType;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(T target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(T target, ReferenceData refData) {
    return target.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    OvernightFuture product = target.getProduct();
    QuoteId quoteId = QuoteId.of(target.getProduct().getSecurityId().getStandardId(), FieldName.SETTLEMENT_PRICE);
    OvernightIndex index = product.getIndex();

    // use lookup to build requirements
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = ratesLookup.requirements(ImmutableSet.of(), ImmutableSet.of(index));
    ImmutableSet<MarketDataId<?>> valueReqs = ImmutableSet.<MarketDataId<?>>builder()
        .add(quoteId)
        .addAll(ratesReqs.getValueRequirements())
        .build();
    return ratesReqs.toBuilder().valueRequirements(valueReqs).build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedOvernightFutureTrade resolved = target.resolve(refData);

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
      ResolvedOvernightFutureTrade resolved,
      RatesScenarioMarketData marketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.UNSUPPORTED, "Unsupported measure for OvernightFutureTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(resolved, marketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedOvernightFutureTrade resolved,
        RatesScenarioMarketData marketData);
  }

}
