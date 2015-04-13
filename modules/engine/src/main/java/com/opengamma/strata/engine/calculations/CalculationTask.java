/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.MarketDataRequirementsBuilder;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.key.MarketDataKey;
import com.opengamma.strata.marketdata.key.ObservableKey;

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
  private final VectorEngineFunction<CalculationTarget, ?> function;

  /** The mappings to select market data. */
  private final MarketDataMappings marketDataMappings;

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
      VectorEngineFunction<? extends CalculationTarget, ?> function,
      MarketDataMappings marketDataMappings,
      ReportingRules reportingRules) {

    this.rowIndex = ArgChecker.notNegative(rowIndex, "rowIndex");
    this.columnIndex = ArgChecker.notNegative(columnIndex, "columnIndex");
    this.target = ArgChecker.notNull(target, "target");
    this.marketDataMappings = ArgChecker.notNull(marketDataMappings, "marketDataMappings");
    this.reportingRules = ArgChecker.notNull(reportingRules, "reportingRules");
    // TODO check the target types are compatible
    this.function = (VectorEngineFunction<CalculationTarget, ?>) ArgChecker.notNull(function, "function");
  }

  /**
   * Returns requirements specifying the market data the function needs to perform its calculations.
   *
   * @return requirements specifying the market data the function needs to perform its calculations
   */
  public MarketDataRequirements requirements() {
    CalculationRequirements calculationRequirements = function.requirements(target);
    MarketDataRequirementsBuilder requirementsBuilder = MarketDataRequirements.builder();

    calculationRequirements.getTimeSeriesRequirements().stream()
        .map(marketDataMappings::getIdForObservableKey)
        .forEach(requirementsBuilder::timeSeries);

    for (MarketDataKey<?> key : calculationRequirements.getSingleValueRequirements()) {
      MarketDataId<?> id;

      if (key instanceof ObservableKey) {
        id = marketDataMappings.getIdForObservableKey((ObservableKey) key);
      } else {
        id = marketDataMappings.getIdForKey(key);
      }
      requirementsBuilder.values(id);
    }
    return requirementsBuilder.build();
  }

  /**
   * Performs calculations for the target using multiple sets of market data.
   *
   * @param marketData  the market data used in the calculation
   * @return results of the calculation, one for every scenario in the market data
   */
  public CalculationResult execute(ScenarioMarketData marketData) {
    DefaultCalculationMarketData calculationData = new DefaultCalculationMarketData(marketData, marketDataMappings);
    Result<?> result = Result.of(() -> function.execute(target, calculationData, reportingRules));
    return CalculationResult.of(target, rowIndex, columnIndex, result);
  }
}
