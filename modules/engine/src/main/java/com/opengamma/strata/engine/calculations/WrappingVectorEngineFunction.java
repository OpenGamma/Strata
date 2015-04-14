/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

/**
 * Vector engine function that wraps a {@link ScalarEngineFunction}.
 * <p>
 * This function loops over a set of scenario data calling the scalar function for each scenario.
 * <p>
 * If there is one scenario in the market data the return value of the execute method is the return value
 * of the execute method of the scalar function.
 * <p>
 * If there are multiple scenarios the execute method returns a list containing the results of
 * calling the execute method on the scalar function for each scenario.
 *
 * @param <T>  the type of target handled by this function
 */
public class WrappingVectorEngineFunction<T extends CalculationTarget, R>
    implements VectorEngineFunction<T, Object> {

  /** The function that calculates the results, one scenario at a time. */
  private final ScalarEngineFunction<T, R> delegate;

  /**
   * @param delegate  the function that calculates the results, one scenario at a time
   */
  public WrappingVectorEngineFunction(ScalarEngineFunction<T, R> delegate) {
    this.delegate = delegate;
  }

  @Override
  public CalculationRequirements requirements(T target) {
    return delegate.requirements(target);
  }

  @Override
  public Object execute(T input, CalculationMarketData marketData, ReportingRules reportingRules) {
    List<R> results =
        IntStream.range(0, marketData.getScenarioCount())
            .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
            .map(data -> delegate.execute(input, data, reportingRules))
            .collect(toImmutableList());

    return (marketData.getScenarioCount() == 1) ?
        results.get(0) :
        results;
  }
}
