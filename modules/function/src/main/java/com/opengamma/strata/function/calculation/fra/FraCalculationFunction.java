/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.calculation.fra;

import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.FunctionUtils;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.market.key.DiscountCurveKey;
import com.opengamma.strata.market.key.IborIndexCurveKey;
import com.opengamma.strata.market.key.IndexRateKey;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;
import com.opengamma.strata.product.fra.ResolvedFraTrade;

/**
 * Perform calculations on a single {@code FraTrade} for each of a set of scenarios.
 * <p>
 * This uses the standard discounting calculation method.
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PAR_RATE Par rate}
 *   <li>{@linkplain Measures#PAR_SPREAD Par spread}
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PRESENT_VALUE_MULTI_CCY Present value with no currency conversion}
 *   <li>{@linkplain Measures#EXPLAIN_PRESENT_VALUE Explain present value}
 *   <li>{@linkplain Measures#CASH_FLOWS Cash flows}
 *   <li>{@linkplain Measures#PV01 PV01}
 *   <li>{@linkplain Measures#BUCKETED_PV01 Bucketed PV01}
 *   <li>{@linkplain Measures#BUCKETED_GAMMA_PV01 Bucketed Gamma PV01}
 * </ul>
 */
public class FraCalculationFunction
    implements CalculationFunction<FraTrade> {

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.PAR_RATE, FraMeasureCalculations::parRate)
          .put(Measures.PAR_SPREAD, FraMeasureCalculations::parSpread)
          .put(Measures.PRESENT_VALUE, FraMeasureCalculations::presentValue)
          .put(Measures.EXPLAIN_PRESENT_VALUE, FraMeasureCalculations::explainPresentValue)
          .put(Measures.CASH_FLOWS, FraMeasureCalculations::cashFlows)
          .put(Measures.PV01, FraMeasureCalculations::pv01)
          .put(Measures.BUCKETED_PV01, FraMeasureCalculations::bucketedPv01)
          .put(Measures.BUCKETED_GAMMA_PV01, FraMeasureCalculations::bucketedGammaPv01)
          .build();

  private static final ImmutableSet<Measure> MEASURES = ImmutableSet.<Measure>builder()
      .addAll(CALCULATORS.keySet())
      .add(Measures.PRESENT_VALUE_MULTI_CCY)
      .build();

  /**
   * Creates an instance.
   */
  public FraCalculationFunction() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<FraTrade> targetType() {
    return FraTrade.class;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Currency naturalCurrency(FraTrade trade, ReferenceData refData) {
    return trade.getProduct().getCurrency();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(FraTrade trade, Set<Measure> measures, ReferenceData refData) {
    Fra product = trade.getProduct();

    // Create a set of all indices referenced by the FRA
    Set<IborIndex> indices = new HashSet<>();

    // The main index is always present
    indices.add(product.getIndex());

    // The index used for linear interpolation is optional
    product.getIndexInterpolated().ifPresent(indices::add);

    // Create a key identifying the rate of each index referenced by the FRA
    Set<ObservableKey> indexRateKeys = indices.stream()
        .map(IndexRateKey::of)
        .collect(toImmutableSet());

    // Create a key identifying the forward curve of each index referenced by the FRA
    Set<MarketDataKey<?>> indexCurveKeys = indices.stream()
        .map(IborIndexCurveKey::of)
        .collect(toImmutableSet());

    // Create a key identifying the discount factors for the FRA currency
    Set<DiscountCurveKey> discountFactorsKeys = ImmutableSet.of(DiscountCurveKey.of(product.getCurrency()));

    return FunctionRequirements.builder()
        .singleValueRequirements(Sets.union(indexCurveKeys, discountFactorsKeys))
        .timeSeriesRequirements(indexRateKeys)
        .outputCurrencies(product.getCurrency())
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      FraTrade trade,
      Set<Measure> measures,
      CalculationMarketData scenarioMarketData,
      ReferenceData refData) {

    // resolve the trade once for all measures and all scenarios
    ResolvedFraTrade resolved = trade.resolve(refData);

    // loop around measures, calculating all scenarios for one measure
    Map<Measure, Result<?>> results = new HashMap<>();
    for (Measure measure : measures) {
      results.put(measure, calculate(measure, resolved, scenarioMarketData));
    }
    // The calculated value is the same for these two measures but they are handled differently WRT FX conversion
    FunctionUtils.duplicateResult(Measures.PRESENT_VALUE, Measures.PRESENT_VALUE_MULTI_CCY, results);
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedFraTrade trade,
      CalculationMarketData scenarioMarketData) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unsupported measure: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, scenarioMarketData));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract ScenarioResult<?> calculate(
        ResolvedFraTrade trade,
        CalculationMarketData marketData);
  }

}
