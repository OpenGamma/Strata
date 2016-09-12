/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
   * The derived calculation function which calculates one measure.
   * <p>
   * The inputs to the measure can include measures calculated by the delegate calculation function.
   */
  private final DerivedCalculationFunction<T, R> derivedFunction;

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
   * @param derivedFunction  a function which calculates one measure using the measure values calculated by the other function
   * @param delegate  a function which calculates multiple measures
   */
  DerivedCalculationFunctionWrapper(
      DerivedCalculationFunction<T, R> derivedFunction,
      CalculationFunction<T> delegate) {

    this.derivedFunction = derivedFunction;
    this.delegate = delegate;

    Set<Measure> delegateMeasures = delegate.supportedMeasures();
    this.requiredMeasuresSupported = delegateMeasures.containsAll(derivedFunction.requiredMeasures());
    this.supportedMeasures = requiredMeasuresSupported ?
        ImmutableSet.<Measure>builder().addAll(delegateMeasures).add(derivedFunction.measure()).build() :
        delegateMeasures;
  }

  @Override
  public Class<T> targetType() {
    return derivedFunction.targetType();
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return supportedMeasures;
  }

  @Override
  public Optional<String> identifier(T target) {
    return delegate.identifier(target);
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
    FunctionRequirements functionRequirements = derivedFunction.requirements(target, parameters, refData);
    return delegateRequirements.combinedWith(functionRequirements);
  }

  @Override
  public Map<Measure, Result<?>> calculate(
      T target,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    // The caller didn't ask for the derived measure so just return the measures calculated by the delegate
    if (!measures.contains(derivedFunction.measure())) {
      return delegate.calculate(target, measures, parameters, marketData, refData);
    }
    // Add the measures required to calculate the derived measure to the measures requested by the caller
    Set<Measure> requiredMeasures = Sets.union(measures, derivedFunction.requiredMeasures());
    Map<Measure, Result<?>> delegateResults = delegate.calculate(target, requiredMeasures, parameters, marketData, refData);

    // Calculate the derived measure
    Result<?> result = calculateMeasure(target, delegateResults, parameters, marketData, refData);

    // The results containing only the requested measures and not including extra measures that were inserted above
    // Also filter out any results for calculationFunction.measure(). There will be failures from functions below
    // that don't support that measure.
    Map<Measure, Result<?>> requestedResults = MapStream.of(delegateResults)
        .filterKeys(measures::contains)
        .filterKeys(measure -> !measure.equals(derivedFunction.measure()))
        .toMap();

    return ImmutableMap.<Measure, Result<?>>builder()
        .put(derivedFunction.measure(), result)
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
          derivedFunction.requiredMeasures(),
          delegate.supportedMeasures(),
          delegate);
    }
    if (!delegateResults.keySet().containsAll(derivedFunction.requiredMeasures())) {
      // There's a bug in the delegate function - it claims to support the required measures but didn't return
      // a result for all of them.
      return Result.failure(
          FailureReason.CALCULATION_FAILED,
          "Delegate did not return the expected measures. Required {}, actual {}, delegate {}",
          derivedFunction.requiredMeasures(),
          delegateResults.keySet(),
          delegate);
    }
    // Check whether all the required measures were successfully calculated
    List<Result<?>> failures = MapStream.of(delegateResults)
        .filterKeys(derivedFunction.requiredMeasures()::contains)
        .map(entry -> entry.getValue())
        .filter(result -> result.isFailure())
        .collect(toList());

    if (!failures.isEmpty()) {
      return Result.failure(failures);
    }
    // Unwrap the results before passing them to the function
    Map<Measure, Object> resultValues = MapStream.of(delegateResults)
        .filterKeys(derivedFunction.requiredMeasures()::contains)
        .mapValues(result -> (Object) result.getValue()) // This compiler needs this cast. Which seems odd.
        .toMap();
    return Result.of(() -> derivedFunction.calculate(target, resultValues, parameters, marketData, refData));
  }
}
