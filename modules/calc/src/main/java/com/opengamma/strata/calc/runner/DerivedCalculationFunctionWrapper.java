/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * A {@link CalculationFunction} implementation which wraps a {@link DerivedCalculationFunction}.
 * <p>
 * A derived calculation function calculates a measure using the measures calculated by another function.
 * This functions takes care of calling the delegate function and passing the results to the derived function.
 * <p>
 * Most of the logic is concerned with bookkeeping - packing and unpacking maps of measures and results before
 * passing them on or returning them.
 */
class DerivedCalculationFunctionWrapper<T extends CalculationTarget, R> implements CalculationFunction<T> {

  /**
   * The derive calculation function which calculates one measure.
   * <p>
   * The inputs to the measure can include measures calculated by the delegate calculation function.
   */
  private final DerivedCalculationFunction<T, R> calculationFunction;

  /**
   * A calculation function whose results can be used by the derived calculation function.
   */
  private final CalculationFunction<T> delegate;

  /**
   * The measures supported by this function; the union of the measures supported by the delegate function and
   * the derived function.
   */
  private final Set<Measure> supportedMeasures;

  /**
   * True if the delegate function supports all measures required by the calculation function.
   * If this is true the calculation function can be invoked.
   * If it is false the only measures which can be calculated are the measures supported by the delegate.
   */
  private final boolean requiredMeasuresSupported;

  /**
   * Creates a new function which invokes the delegate function, passes the result to the derived function
   * and returns the combined results.
   *
   * @param calculationFunction  a function which calculates one measure using the measure values calculated by the other function
   * @param delegate  a function which calculates multiple measures
   */
  DerivedCalculationFunctionWrapper(
      DerivedCalculationFunction<T, R> calculationFunction,
      CalculationFunction<T> delegate) {

    this.calculationFunction = calculationFunction;
    this.delegate = delegate;

    Set<Measure> delegateMeasures = delegate.supportedMeasures();
    this.requiredMeasuresSupported = delegateMeasures.containsAll(calculationFunction.requiredMeasures());
    this.supportedMeasures = requiredMeasuresSupported ?
        ImmutableSet.<Measure>builder().addAll(delegateMeasures).add(calculationFunction.measure()).build() :
        delegateMeasures;
  }

  @Override
  public Class<T> targetType() {
    return calculationFunction.targetType();
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return supportedMeasures;
  }

  @Override
  public Currency naturalCurrency(T target, ReferenceData refData) {
    return delegate.naturalCurrency(target, refData);
  }

  @Override
  public FunctionRequirements requirements(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    FunctionRequirements delegateRequirements = delegate.requirements(target, measures, parameters, refData);
    FunctionRequirements functionRequirements = calculationFunction.requirements(target, parameters, refData);
    return delegateRequirements.combinedWith(functionRequirements);
  }

  @Override
  public Map<Measure, Result<?>> calculate(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    // Does the caller want the extra measure?
    boolean measureRequested = measures.contains(calculationFunction.measure());

    // We need to add the measures required to calculate the measure
    // But we don't need to do that if the delegate can't provide them or if the user hasn't asked for the measure
    Set<Measure> requiredMeasures = requiredMeasuresSupported && measureRequested ?
        Sets.union(measures, calculationFunction.requiredMeasures()) :
        measures;

    Map<Measure, Result<?>> delegateResults = delegate.calculate(target, requiredMeasures, parameters, marketData, refData);

    // The user didn't ask for the extra measure so just return the measures calculated by the delegate
    if (!measureRequested) {
      return delegateResults;
    }
    // Calculate the extra measure provided by calculationFunction
    Result<?> result = calculateMeasure(target, delegateResults, parameters, marketData, refData);

    // The results containing only the requested measures and not including extra measures that were inserted above
    // Also filter out any results for calculationFunction.measure(). There will be failures from functions below
    // that don't support that measure.
    Map<Measure, Result<?>> requestedResults = MapStream.of(delegateResults)
        .filterKeys(measures::contains)
        .filterKeys(measure -> !measure.equals(calculationFunction.measure()))
        .toMap();

    return ImmutableMap.<Measure, Result<?>>builder()
        .put(calculationFunction.measure(), result)
        .putAll(requestedResults)
        .build();
  }

  private Result<?> calculateMeasure(
      T target,
      Map<Measure, Result<?>> delegateResults,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    if (!requiredMeasuresSupported) {
      // Can't calculate the measure if the delegate can't calculate its inputs
      return Result.failure(
          FailureReason.NOT_APPLICABLE,
          "The delegate function cannot calculate the required measures. Required measures: {}, " +
              "supported measures: {}, delegate {}",
          calculationFunction.requiredMeasures(),
          delegate.supportedMeasures(),
          delegate);
    }
    if (!delegateResults.keySet().containsAll(calculationFunction.requiredMeasures())) {
      // There's a bug in the delegate function - it claims to support the required measures but didn't return
      // a result for all of them.
      return Result.failure(
          FailureReason.CALCULATION_FAILED,
          "Delegate did not return the expected measures. Required {}, actual {}, delegate {}",
          calculationFunction.requiredMeasures(),
          delegateResults.keySet(),
          delegate);
    }
    // Check whether all the required measures were successfully calculated
    List<Result<?>> failures = MapStream.of(delegateResults)
        .filterKeys(calculationFunction.requiredMeasures()::contains)
        .map(entry -> entry.getValue())
        .filter(result -> result.isFailure())
        .collect(toList());

    if (!failures.isEmpty()) {
      return Result.failure(failures);
    }
    // Unwrap the results before passing them to the function
    Map<Measure, ?> resultValues = MapStream.of(delegateResults)
        .filterKeys(calculationFunction.requiredMeasures()::contains)
        .mapValues(result -> result.getValue())
        .toMap();
    return Result.of(() -> calculationFunction.calculate(target, resultValues, parameters, marketData, refData));
  }
}
