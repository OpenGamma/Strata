/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
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
 * This function loops over a set of scenario data, calls the scalar function for each scenario and assembles
 * the results into a list.
 *
 * @param <T>  the type of target handled by this function
 * @param <R>  the return type of this function
 */
public class WrappingVectorEngineFunction<T extends CalculationTarget, R>
    implements VectorEngineFunction<T, List<R>> {

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
  public List<R> execute(T input, CalculationMarketData marketData, ReportingRules reportingRules) {
    return IntStream.range(0, marketData.getScenarioCount())
        .mapToObj(index -> new DefaultSingleCalculationMarketData(marketData, index))
        .map(data -> delegate.execute(input, data, reportingRules))
        .collect(toImmutableList());
  }
}
