/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableMap;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirementsBuilder;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.collect.result.Result;

/**
 * A single task that will be used to perform a calculation.
 * <p>
 * This presents a uniform interface to the engine so all functions can be treated equally during execution.
 * Without this class the engine would need to keep track of which functions to use for each input.
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
   * The mappings to select market data.
   */
  @PropertyDefinition(validate = "notNull")
  private final MarketDataMappings marketDataMappings;
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
   * @param marketDataMappings  the mappings that specify the market data that should be used in the calculation
   * @param cells  the cells to be calculated by this task
   * @return the task
   */
  public static CalculationTask of(
      CalculationTarget target,
      CalculationFunction<? extends CalculationTarget> function,
      MarketDataMappings marketDataMappings,
      CalculationTaskCell... cells) {

    return of(target, function, marketDataMappings, CalculationParameters.empty(), ImmutableList.copyOf(cells));
  }

  /**
   * Obtains an instance that will calculate the specified cells.
   * <p>
   * The cells must all be for the same row index and none of the column indices must overlap.
   *
   * @param target  the target for which the value will be calculated
   * @param function  the function that performs the calculation
   * @param marketDataMappings  the mappings that specify the market data that should be used in the calculation
   * @param parameters  the additional parameters
   * @param cells  the cells to be calculated by this task
   * @return the task
   */
  public static CalculationTask of(
      CalculationTarget target,
      CalculationFunction<? extends CalculationTarget> function,
      MarketDataMappings marketDataMappings,
      CalculationParameters parameters,
      List<CalculationTaskCell> cells) {

    @SuppressWarnings("unchecked")
    CalculationFunction<CalculationTarget> functionCast = (CalculationFunction<CalculationTarget>) function;
    return new CalculationTask(target, functionCast, marketDataMappings, parameters, cells);
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

    // convert function requirements to market data requirements
    MarketDataRequirementsBuilder requirementsBuilder = MarketDataRequirements.builder();
    functionRequirements.getTimeSeriesRequirements().stream()
        .map(marketDataMappings::getIdForObservableKey)
        .forEach(requirementsBuilder::addTimeSeries);
    for (MarketDataKey<?> key : functionRequirements.getSingleValueRequirements()) {
      requirementsBuilder.addValues(marketDataMappings.getIdForKey(key));
    }

    // add requirements for the FX rates needed to convert the output values into the reporting currency
    for (CalculationTaskCell cell : cells) {
      if (cell.getMeasure().isCurrencyConvertible()) {
        Currency reportingCurrency = cell.reportingCurrency(this, refData);
        List<MarketDataId<FxRate>> fxRateIds = functionRequirements.getOutputCurrencies().stream()
            .filter(outputCurrency -> !outputCurrency.equals(reportingCurrency))
            .map(outputCurrency -> CurrencyPair.of(outputCurrency, reportingCurrency))
            .map(FxRateKey::of)
            .map(marketDataMappings::getIdForKey)
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
  public CalculationResults execute(CalculationEnvironment marketData, ReferenceData refData) {
    // use the mappings to filter the complete market data to the subset needed here
    CalculationMarketData selectedMarketData = DefaultCalculationMarketData.of(marketData, marketDataMappings);

    // calculate the results
    Map<Measure, Result<?>> results = calculate(selectedMarketData, refData);

    // convert the results, using a normal loop for better stack traces
    ImmutableList.Builder<CalculationResult> resultBuilder = ImmutableList.builder();
    for (CalculationTaskCell cell : cells) {
      resultBuilder.add(cell.createResult(this, target, results, selectedMarketData, refData));
    }

    // return the result
    return CalculationResults.of(target, resultBuilder.build());
  }

  // calculates the result
  private Map<Measure, Result<?>> calculate(CalculationMarketData marketData, ReferenceData refData) {
    try {
      return function.calculate(target, getMeasures(), parameters, marketData, refData);

    } catch (RuntimeException ex) {
      // return a failure for each requested measure with details of the problem
      Result<?> failure = Result.failure(
          ex, "Function '{}' threw an exception: {}", function.getClass().getSimpleName(), ex.getMessage());
      return getMeasures().stream()
          .collect(toImmutableMap(m -> m, m -> failure));
    }
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
      MarketDataMappings marketDataMappings,
      CalculationParameters parameters,
      List<CalculationTaskCell> cells) {
    JodaBeanUtils.notNull(target, "target");
    JodaBeanUtils.notNull(function, "function");
    JodaBeanUtils.notNull(marketDataMappings, "marketDataMappings");
    JodaBeanUtils.notNull(parameters, "parameters");
    JodaBeanUtils.notEmpty(cells, "cells");
    this.target = target;
    this.function = function;
    this.marketDataMappings = marketDataMappings;
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
   * Gets the mappings to select market data.
   * @return the value of the property, not null
   */
  public MarketDataMappings getMarketDataMappings() {
    return marketDataMappings;
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
          JodaBeanUtils.equal(marketDataMappings, other.marketDataMappings) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(marketDataMappings);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    hash = hash * 31 + JodaBeanUtils.hashCode(cells);
    return hash;
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
