/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirementsBuilder;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.CurrencyConvertible;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * A single task that will be used to perform a calculation.
 * <p>
 * This presents a uniform interface to the engine so all functions can be treated equally during execution.
 * Without this class the engine would need to keep track of which functions to use for each input.
 */
public final class CalculationTask {

  /**
   * The target for which the value will be calculated.
   * This is typically a trade.
   */
  private final CalculationTarget target;
  /**
   * The measure to be calculated.
   */
  private final Measure measure;
  /**
   * The row index of the value in the results grid.
   */
  private final int rowIndex;
  /**
   * The column index of the value in the results grid.
   */
  private final int columnIndex;
  /**
   * The function that will calculate the value.
   */
  private final CalculationFunction<CalculationTarget> function;
  /**
   * The mappings to select market data.
   */
  private final MarketDataMappings marketDataMappings;
  /**
   * The reporting currency.
   */
  private final ReportingCurrency reportingCurrency;

  //-------------------------------------------------------------------------
  /**
   * Obtains configuration for a task that will calculate a value for a target.
   * <p>
   * This specifies the configuration of a single target, including the rules and cell index.
   *
   * @param target  the target for which the value will be calculated
   * @param measure  the measure being calculated
   * @param rowIndex  the row index of the value in the results grid
   * @param columnIndex  the column index of the value in the results grid
   * @param function  the function that performs the calculation
   * @param marketDataMappings  the mappings that specify the market data that should be used in the calculation
   * @param reportingCurrency  the reporting currency
   * @return the configuration for a task that will calculate the value of a measure for a target
   */
  public static CalculationTask of(
      CalculationTarget target,
      Measure measure,
      int rowIndex,
      int columnIndex,
      CalculationFunction<? extends CalculationTarget> function,
      MarketDataMappings marketDataMappings,
      ReportingCurrency reportingCurrency) {

    return new CalculationTask(
        target,
        measure,
        rowIndex,
        columnIndex,
        function,
        marketDataMappings,
        reportingCurrency);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a task, based on the target, the location of the result in the results grid, the function,
   * mappings and reporting currency.
   *
   * @param target  the target for which the value will be calculated
   * @param measure  the measure being calculated
   * @param rowIndex  the row index of the value in the results grid
   * @param columnIndex  the column index of the value in the results grid
   * @param function  the function that performs the calculation
   * @param marketDataMappings  the mappings that specify the market data that should be used in the calculation
   * @param reportingCurrency  the reporting currency
   */
  @SuppressWarnings("unchecked")
  private CalculationTask(
      CalculationTarget target,
      Measure measure,
      int rowIndex,
      int columnIndex,
      CalculationFunction<? extends CalculationTarget> function,
      MarketDataMappings marketDataMappings,
      ReportingCurrency reportingCurrency) {

    this.target = ArgChecker.notNull(target, "target");
    this.measure = ArgChecker.notNull(measure, "measure");
    this.rowIndex = ArgChecker.notNegative(rowIndex, "rowIndex");
    this.columnIndex = ArgChecker.notNegative(columnIndex, "columnIndex");
    this.marketDataMappings = ArgChecker.notNull(marketDataMappings, "marketDataMappings");
    this.reportingCurrency = ArgChecker.notNull(reportingCurrency, "reportingCurrency");
    // TODO check the target types are compatible
    this.function = (CalculationFunction<CalculationTarget>) ArgChecker.notNull(function, "function");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the target.
   * 
   * @return the target.
   */
  public CalculationTarget getTarget() {
    return target;
  }

  /**
   * Gets the measure.
   * 
   * @return the measure.
   */
  public Measure getMeasure() {
    return measure;
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
    ImmutableSet<Measure> measures = ImmutableSet.of(getMeasure());
    FunctionRequirements functionRequirements = function.requirements(target, measures, refData);

    MarketDataRequirementsBuilder requirementsBuilder = MarketDataRequirements.builder();

    functionRequirements.getTimeSeriesRequirements().stream()
        .map(marketDataMappings::getIdForObservableKey)
        .forEach(requirementsBuilder::addTimeSeries);

    for (MarketDataKey<?> key : functionRequirements.getSingleValueRequirements()) {
      requirementsBuilder.addValues(marketDataMappings.getIdForKey(key));
    }
    if (measure.isCurrencyConvertible()) {
      Currency reportingCurrency = reportingCurrency(refData);
      // Add requirements for the FX rates needed to convert the output values into the reporting currency
      List<MarketDataId<FxRate>> fxRateIds = functionRequirements.getOutputCurrencies().stream()
          .filter(outputCurrency -> !outputCurrency.equals(reportingCurrency))
          .map(outputCurrency -> CurrencyPair.of(outputCurrency, reportingCurrency))
          .map(FxRateKey::of)
          .map(marketDataMappings::getIdForKey)
          .collect(toImmutableList());
      requirementsBuilder.addValues(fxRateIds);
    }
    return requirementsBuilder.build();
  }

  // determines the reporting currency
  private Currency reportingCurrency(ReferenceData refData) {
    if (reportingCurrency.isSpecific()) {
      return reportingCurrency.getCurrency();
    }
    // this should never throw an exception, because it is only called if the measure is currency-convertible
    return function.naturalCurrency(target, refData);
  }

  /**
   * Executes the task, performing calculations for the target using multiple sets of market data.
   * <p>
   * This invokes the function with the correct set of market data.
   *
   * @param scenarioData  the market data used in the calculation
   * @param refData  the reference data
   * @return results of the calculation, one for every scenario in the market data
   */
  @SuppressWarnings("unchecked")
  public CalculationResults execute(CalculationEnvironment scenarioData, ReferenceData refData) {
    CalculationMarketData calculationData = DefaultCalculationMarketData.of(scenarioData, marketDataMappings);
    Result<?> result = Result.wrap(() -> calculate(calculationData, refData));
    Result<?> converted = convertToReportingCurrency(result, calculationData, refData);
    CalculationResult calcResult = CalculationResult.of(rowIndex, columnIndex, converted);
    return CalculationResults.of(target, ImmutableList.of(calcResult));
  }

  /**
   * Calculates the result for the specified market data.
   * 
   * @param marketData  the market data
   * @param refData  the reference data
   * @return the result
   */
  private Result<?> calculate(CalculationMarketData marketData, ReferenceData refData) {
    ImmutableSet<Measure> measures = ImmutableSet.of(getMeasure());
    Map<Measure, Result<?>> map = function.calculate(target, measures, marketData, refData);
    if (!map.containsKey(getMeasure())) {
      return Result.failure(
          FailureReason.CALCULATION_FAILED,
          "Function '{}' did not return requested measure '{}'",
          function.getClass().getName(),
          getMeasure());
    }
    return map.get(getMeasure());
  }

  // converts the value, if appropriate
  private Result<?> convertToReportingCurrency(
      Result<?> result,
      CalculationMarketData marketData,
      ReferenceData refData) {

    // the result is only converted if it is a success and both the measure and value are convertible
    if (result.isSuccess() && measure.isCurrencyConvertible() && result.getValue() instanceof CurrencyConvertible) {
      CurrencyConvertible<?> convertible = (CurrencyConvertible<?>) result.getValue();
      return performCurrencyConversion(convertible, marketData, refData);
    }
    return result;
  }

  // converts the value
  private Result<?> performCurrencyConversion(
      CurrencyConvertible<?> value,
      CalculationMarketData marketData,
      ReferenceData refData) {

    Currency currency = reportingCurrency(refData);
    try {
      return Result.success(value.convertedTo(currency, marketData));
    } catch (RuntimeException ex) {
      return Result.failure(FailureReason.ERROR, ex, "Failed to convert value {} to currency {}", value, currency);
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return Messages.format("CalculationTask[cell=({}, {}), measure={}]", rowIndex, columnIndex, measure);
  }

}
