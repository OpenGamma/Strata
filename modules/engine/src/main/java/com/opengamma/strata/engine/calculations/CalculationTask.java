/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.DefaultCalculationMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.key.MarketDataKey;

/**
 * Wraps an input and a function that calculates a value for the input.
 * <p>
 * This presents a uniform interface to the engine so all functions can be treated equally during execution.
 * Without this class the engine would need to keep track of which functions to use for each input.
 */
public class CalculationTask {

  /**
   * The target, such as a trade.
   */
  private final CalculationTarget target;
  /**
   * The function to invoke.
   */
  private final VectorEngineFunction<CalculationTarget, ?> function;
  /**
   * The mappings to select market data.
   */
  private final MarketDataMappings marketDataMappings;
  /**
   * The rules for reporting the output.
   */
  private final ReportingRules reportingRules;

  /**
   * Creates a task, based on the target, function, mappings and reporting rules.
   * 
   * @param target  the target for which the calculation is performed
   * @param function  the function that performs the calculation
   * @param marketDataMappings  specifies the market data used in the calculation
   * @param reportingRules  the currency in which monetary values should be returned
   */
  @SuppressWarnings("unchecked")
  public CalculationTask(
      CalculationTarget target,
      VectorEngineFunction<? extends CalculationTarget, ?> function,
      MarketDataMappings marketDataMappings,
      ReportingRules reportingRules) {

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
    ImmutableSet.Builder<ObservableId> timeSeriesReqsBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<MarketDataId<?>> singleValueReqsBuilder = ImmutableSet.builder();

    calculationRequirements.getTimeSeriesRequirements().stream()
        .forEach(key -> timeSeriesReqsBuilder.add(marketDataMappings.getIdForObservableKey(key)));

    // This should be possible using streams but I can't persuade the type inference to handle it
    for (MarketDataKey<?> key : calculationRequirements.getSingleValueRequirements()) {
      MarketDataId<?> id = marketDataMappings.getIdForKey(key);
      singleValueReqsBuilder.add(id);
    }
    return MarketDataRequirements.builder()
        .timeSeriesRequirements(timeSeriesReqsBuilder.build())
        .singleValueRequirements(singleValueReqsBuilder.build())
        .build();
  }

  /**
   * Performs calculations for the target using multiple sets of market data.
   *
   * @param marketData  the market data used in the calculation
   * @return results of the calculation, one for every scenario in the market data
   */
  public Result<?> execute(ScenarioMarketData marketData) {
    try {
      DefaultCalculationMarketData calculationData = new DefaultCalculationMarketData(marketData, marketDataMappings);
      return Result.success(function.execute(target, calculationData, reportingRules));
    } catch (Exception e) {
      return Result.failure(e);
    }
  }
}
