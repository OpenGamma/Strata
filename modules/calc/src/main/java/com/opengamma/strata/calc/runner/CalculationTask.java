/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirementsBuilder;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.FxRateId;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.data.ObservableSource;
import com.opengamma.strata.data.scenario.ScenarioFxRateProvider;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * A single task that will be used to perform a calculation.
 * <p>
 * This is a single unit of execution in the calculation runner.
 * It consists of a {@link CalculationFunction} and the appropriate inputs,
 * including a single {@link CalculationTarget}. When invoked, it will
 * calculate a result for one or more columns in the grid of results.
 */
@BeanDefinition(style = "light")
public final class CalculationTask implements ImmutableBean {

  /**
   * The target for which the value will be calculated.
   * This is typically a trade.
   */
  @PropertyDefinition(validate = "notNull")
  private final CalculationTarget target;
  /**
   * The function that will calculate the value.
   */
  @PropertyDefinition(validate = "notNull")
  private final CalculationFunction<CalculationTarget> function;
  /**
   * The additional parameters.
   */
  @PropertyDefinition(validate = "notNull")
  private final CalculationParameters parameters;
  /**
   * The cells to be calculated.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final List<CalculationTaskCell> cells;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that will calculate the specified cells.
   * <p>
   * The cells must all be for the same row index and none of the column indices must overlap.
   * The result will contain no calculation parameters.
   *
   * @param target  the target for which the value will be calculated
   * @param function  the function that performs the calculation
   * @param cells  the cells to be calculated by this task
   * @return the task
   */
  public static CalculationTask of(
      CalculationTarget target,
      CalculationFunction<? extends CalculationTarget> function,
      CalculationTaskCell... cells) {

    return of(target, function, CalculationParameters.empty(), ImmutableList.copyOf(cells));
  }

  /**
   * Obtains an instance that will calculate the specified cells.
   * <p>
   * The cells must all be for the same row index and none of the column indices must overlap.
   *
   * @param target  the target for which the value will be calculated
   * @param function  the function that performs the calculation
   * @param parameters  the additional parameters
   * @param cells  the cells to be calculated by this task
   * @return the task
   */
  public static CalculationTask of(
      CalculationTarget target,
      CalculationFunction<? extends CalculationTarget> function,
      CalculationParameters parameters,
      List<CalculationTaskCell> cells) {

    @SuppressWarnings("unchecked")
    CalculationFunction<CalculationTarget> functionCast = (CalculationFunction<CalculationTarget>) function;
    return new CalculationTask(target, functionCast, parameters, cells);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the index of the row in the grid of results.
   * 
   * @return the row index
   */
  public int getRowIndex() {
    return cells.get(0).getRowIndex();
  }

  /**
   * Gets the set of measures that will be calculated by this task.
   * 
   * @return the measures
   */
  public Set<Measure> getMeasures() {
    return cells.stream().map(c -> c.getMeasure()).collect(toImmutableSet());
  }

  //-------------------------------------------------------------------------
  /**
   * Returns requirements specifying the market data the function needs to perform its calculations.
   * 
   * @param refData  the reference data
   * @return requirements specifying the market data the function needs to perform its calculations
   */
  @SuppressWarnings("unchecked")
  public MarketDataRequirements requirements(ReferenceData refData) {
    // determine market data requirements of the function
    FunctionRequirements functionRequirements = function.requirements(target, getMeasures(), parameters, refData);
    ObservableSource obsSource = functionRequirements.getObservableSource();

    // convert function requirements to market data requirements
    MarketDataRequirementsBuilder requirementsBuilder = MarketDataRequirements.builder();
    for (ObservableId id : functionRequirements.getTimeSeriesRequirements()) {
      requirementsBuilder.addTimeSeries(id.withObservableSource(obsSource));
    }
    for (MarketDataId<?> id : functionRequirements.getValueRequirements()) {
      if (id instanceof ObservableId) {
        requirementsBuilder.addValues(((ObservableId) id).withObservableSource(obsSource));
      } else {
        requirementsBuilder.addValues(id);
      }
    }

    // add requirements for the FX rates needed to convert the output values into the reporting currency
    for (CalculationTaskCell cell : cells) {
      if (cell.getMeasure().isCurrencyConvertible() && !cell.getReportingCurrency().isNone()) {
        Currency reportingCurrency = cell.reportingCurrency(this, refData);
        List<MarketDataId<FxRate>> fxRateIds = functionRequirements.getOutputCurrencies().stream()
            .filter(outputCurrency -> !outputCurrency.equals(reportingCurrency))
            .map(outputCurrency -> CurrencyPair.of(outputCurrency, reportingCurrency))
            .map(pair -> FxRateId.of(pair, obsSource))
            .collect(toImmutableList());
        requirementsBuilder.addValues(fxRateIds);
      }
    }
    return requirementsBuilder.build();
  }

  /**
   * Determines the natural currency of the target.
   * <p>
   * This is only called for measures that are currency convertible.
   * 
   * @param refData  the reference data
   * @return the natural currency
   */
  public Currency naturalCurrency(ReferenceData refData) {
    return function.naturalCurrency(target, refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Executes the task, performing calculations for the target using multiple sets of market data.
   * <p>
   * This invokes the function with the correct set of market data.
   *
   * @param marketData  the market data used in the calculation
   * @param refData  the reference data
   * @return results of the calculation, one for every scenario in the market data
   */
  @SuppressWarnings("unchecked")
  public CalculationResults execute(ScenarioMarketData marketData, ReferenceData refData) {
    // calculate the results
    Map<Measure, Result<?>> results = calculate(marketData, refData);

    // convert the results, using a normal loop for better stack traces
    ScenarioFxRateProvider fxProvider = ScenarioFxRateProvider.of(marketData);
    ImmutableList.Builder<CalculationResult> resultBuilder = ImmutableList.builder();
    for (CalculationTaskCell cell : cells) {
      resultBuilder.add(cell.createResult(this, target, results, fxProvider, refData));
    }

    // return the result
    return CalculationResults.of(target, resultBuilder.build());
  }

  // calculates the result
  private Map<Measure, Result<?>> calculate(ScenarioMarketData marketData, ReferenceData refData) {
    try {
      Set<Measure> requestedMeasures = getMeasures();
      Set<Measure> supportedMeasures = function.supportedMeasures();
      Set<Measure> measures = Sets.intersection(requestedMeasures, supportedMeasures);
      Map<Measure, Result<?>> map = ImmutableMap.of();
      if (!measures.isEmpty()) {
        map = function.calculate(target, measures, parameters, marketData, refData);
      }
      // check if result does not contain all requested measures
      if (!map.keySet().containsAll(requestedMeasures)) {
        return handleMissing(requestedMeasures, supportedMeasures, map);
      }
      return map;

    } catch (RuntimeException ex) {
      return handleFailure(ex);
    }
  }

  // populate the result with failures
  private Map<Measure, Result<?>> handleMissing(
      Set<Measure> requestedMeasures,
      Set<Measure> supportedMeasures,
      Map<Measure, Result<?>> calculatedResults) {

    // need to add missing measures
    Map<Measure, Result<?>> updated = new HashMap<>(calculatedResults);
    String fnName = function.getClass().getSimpleName();
    for (Measure requestedMeasure : requestedMeasures) {
      if (!calculatedResults.containsKey(requestedMeasure)) {
        if (supportedMeasures.contains(requestedMeasure)) {
          String msg = function.identifier(target)
              .map(v -> "for ID '" + v + "'")
              .orElse("for target '" + target.toString() + "'");
          updated.put(requestedMeasure, Result.failure(
              FailureReason.CALCULATION_FAILED,
              "Function '{}' did not return requested measure '{}' {}",
              fnName,
              requestedMeasure,
              msg));
        } else {
          updated.put(requestedMeasure, Result.failure(
              FailureReason.UNSUPPORTED,
              "Measure '{}' is not supported by function '{}'",
              requestedMeasure,
              fnName));
        }
      }
    }
    return updated;
  }

  // handle the failure, extracted to aid inlining
  private Map<Measure, Result<?>> handleFailure(RuntimeException ex) {
    Result<?> failure;
    String fnName = function.getClass().getSimpleName();
    String exMsg = ex.getMessage();
    Optional<String> id = function.identifier(target);
    String msg = id.map(v -> " for ID '" + v + "': " + exMsg).orElse(": " + exMsg + ": for target '" + target.toString() + "'");
    if (ex instanceof MarketDataNotFoundException) {
      failure = Result.failure(
          FailureReason.MISSING_DATA,
          ex,
          "Missing market data when invoking function '{}'{}",
          fnName,
          msg);

    } else if (ex instanceof ReferenceDataNotFoundException) {
      failure = Result.failure(
          FailureReason.MISSING_DATA,
          ex,
          "Missing reference data when invoking function '{}'{}",
          fnName,
          msg);

    } else if (ex instanceof UnsupportedOperationException) {
      failure = Result.failure(
          FailureReason.UNSUPPORTED,
          ex,
          "Unsupported operation when invoking function '{}'{}",
          fnName,
          msg);

    } else {
      failure = Result.failure(
          FailureReason.CALCULATION_FAILED,
          ex,
          "Error when invoking function '{}'{}",
          fnName,
          msg);
    }
    return getMeasures().stream().collect(toImmutableMap(m -> m, m -> failure));
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "CalculationTask" + cells;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationTask}.
   */
  private static MetaBean META_BEAN = LightMetaBean.of(CalculationTask.class);

  /**
   * The meta-bean for {@code CalculationTask}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  private CalculationTask(
      CalculationTarget target,
      CalculationFunction<CalculationTarget> function,
      CalculationParameters parameters,
      List<CalculationTaskCell> cells) {
    JodaBeanUtils.notNull(target, "target");
    JodaBeanUtils.notNull(function, "function");
    JodaBeanUtils.notNull(parameters, "parameters");
    JodaBeanUtils.notEmpty(cells, "cells");
    this.target = target;
    this.function = function;
    this.parameters = parameters;
    this.cells = ImmutableList.copyOf(cells);
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the target for which the value will be calculated.
   * This is typically a trade.
   * @return the value of the property, not null
   */
  public CalculationTarget getTarget() {
    return target;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the function that will calculate the value.
   * @return the value of the property, not null
   */
  public CalculationFunction<CalculationTarget> getFunction() {
    return function;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional parameters.
   * @return the value of the property, not null
   */
  public CalculationParameters getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the cells to be calculated.
   * @return the value of the property, not empty
   */
  public List<CalculationTaskCell> getCells() {
    return cells;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationTask other = (CalculationTask) obj;
      return JodaBeanUtils.equal(target, other.target) &&
          JodaBeanUtils.equal(function, other.function) &&
          JodaBeanUtils.equal(parameters, other.parameters) &&
          JodaBeanUtils.equal(cells, other.cells);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(target);
    hash = hash * 31 + JodaBeanUtils.hashCode(function);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    hash = hash * 31 + JodaBeanUtils.hashCode(cells);
    return hash;
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
