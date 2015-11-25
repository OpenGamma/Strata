/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.calc.config.ReportingRules;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirementsBuilder;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.calc.runner.function.CurrencyConvertible;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Wraps an input and a function that calculates a value for the input.
 * <p>
 * This presents a uniform interface to the engine so all functions can be treated equally during execution.
 * Without this class the engine would need to keep track of which functions to use for each input.
 */
public class CalculationTask {

  /** The target, such as a trade. */
  private final CalculationTarget target;

  /** The row index of the value in the results grid. */
  private final int rowIndex;

  /** The column index of the value in the results grid. */
  private final int columnIndex;

  /** The function that performs the calculations. */
  private final CalculationSingleFunction<CalculationTarget, ?> function;

  /** The mappings to select market data. */
  private final MarketDataMappings marketDataMappings;

  // These aren't used at the moment but will be required when we add support for functions that perform
  // their own currency conversion
  /** The rules for reporting the output. */
  private final ReportingRules reportingRules;

  /**
   * Creates a task, based on the target, the location of the result in the results grid, the function,
   * mappings and reporting rules.
   *
   * @param target  the target for which the calculation is performed
   * @param rowIndex  the row index of the value in the results grid
   * @param columnIndex  the column index of the value in the results grid
   * @param function  the function that performs the calculation
   * @param marketDataMappings  specifies the market data used in the calculation
   * @param reportingRules  the currency in which monetary values should be returned
   */
  @SuppressWarnings("unchecked")
  public CalculationTask(
      CalculationTarget target,
      int rowIndex,
      int columnIndex,
      CalculationSingleFunction<? extends CalculationTarget, ?> function,
      MarketDataMappings marketDataMappings,
      ReportingRules reportingRules) {

    this.rowIndex = ArgChecker.notNegative(rowIndex, "rowIndex");
    this.columnIndex = ArgChecker.notNegative(columnIndex, "columnIndex");
    this.target = ArgChecker.notNull(target, "target");
    this.marketDataMappings = ArgChecker.notNull(marketDataMappings, "marketDataMappings");
    this.reportingRules = ArgChecker.notNull(reportingRules, "reportingRules");
    // TODO check the target types are compatible
    this.function = (CalculationSingleFunction<CalculationTarget, ?>) ArgChecker.notNull(function, "function");
  }

  /**
   * Returns requirements specifying the market data the function needs to perform its calculations.
   *
   * @return requirements specifying the market data the function needs to perform its calculations
   */
  public MarketDataRequirements requirements() {
    FunctionRequirements functionRequirements = function.requirements(target);
    MarketDataRequirementsBuilder requirementsBuilder = MarketDataRequirements.builder();

    functionRequirements.getTimeSeriesRequirements().stream()
        .map(marketDataMappings::getIdForObservableKey)
        .forEach(requirementsBuilder::addTimeSeries);

    for (MarketDataKey<?> key : functionRequirements.getSingleValueRequirements()) {
      requirementsBuilder.addValues(marketDataMappings.getIdForKey(key));
    }
    Optional<Currency> optionalReportingCurrency =
        reportingCurrency(reportingRules.reportingCurrency(target), function.defaultReportingCurrency(target));

    if (optionalReportingCurrency.isPresent()) {
      Currency reportingCurrency = optionalReportingCurrency.get();

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

  /**
   * Returns an optional containing the first currency from the arguments or empty if both arguments are empty.
   */
  private static Optional<Currency> reportingCurrency(Optional<Currency> ccy1, Optional<Currency> ccy2) {
    if (ccy1.isPresent()) {
      return ccy1;
    }
    if (ccy2.isPresent()) {
      return ccy2;
    }
    return Optional.empty();
  }

  /**
   * Performs calculations for the target using multiple sets of market data.
   *
   * @param scenarioData  the market data used in the calculation
   * @return results of the calculation, one for every scenario in the market data
   */
  public CalculationResult execute(CalculationEnvironment scenarioData) {
    CalculationMarketData calculationData = new DefaultCalculationMarketData(scenarioData, marketDataMappings);
    Result<?> result;

    try {
      Object value = function.execute(target, calculationData);
      result = value instanceof Result ?
          (Result<?>) value :
          Result.success(value);
    } catch (RuntimeException e) {
      result = Result.failure(e);
    }
    return CalculationResult.of(target, rowIndex, columnIndex, convertToReportingCurrency(result, calculationData));
  }

  /**
   * Converts the value in a result to the reporting currency.
   * <p>
   * If the result is a failure or does not contain an value that can be converted it is returned unchanged.
   * <p>
   * The reporting rules are used to determine the reporting currency for the target. If the rules do
   * not specify a reporting currency for the target the input result is returned.
   * <p>
   * If the rules specify a reporting currency but the conversion cannot be performed a failure is
   * returned with details of the problem.
   *
   * @param result  the result of a calculation
   * @param marketData  market data containing FX rates needed to perform currency conversion
   * @return a result containing the value from the input result, converted to the reporting currency if possible
   */
  private Result<?> convertToReportingCurrency(Result<?> result, CalculationMarketData marketData) {
    if (!result.isSuccess()) {
      return result;
    }
    Object value = result.getValue();

    if (!(value instanceof CurrencyConvertible)) {
      return result;
    }
    Optional<Currency> optionalReportingCurrency =
        reportingCurrency(reportingRules.reportingCurrency(target), function.defaultReportingCurrency(target));

    if (!optionalReportingCurrency.isPresent()) {
      return Result.failure(FailureReason.MISSING_DATA, "No reporting currency available for convert value {}", value);
    }
    Currency reportingCurrency = optionalReportingCurrency.get();
    CurrencyConvertible<?> convertible = (CurrencyConvertible<?>) value;

    try {
      Object convertedValue = convertible.convertedTo(reportingCurrency, marketData);
      return Result.success(convertedValue);
    } catch (RuntimeException e) {
      return Result.failure(FailureReason.ERROR, e, "Failed to convert value {} to currency {}", value, reportingCurrency);
    }
  }
}
