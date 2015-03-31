/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;

/**
 * The functions for performing a set of calculations and the market data required by the calculations.
 */
public class CalculationTasks {

  private final List<CalculationTask> calculationTasks;
  private final List<Measure> measures;
  private final MarketDataRequirements marketDataRequirements;

  /**
   * @param calculationTasks  the tasks that perform the calculations
   * @param measures  the measures calculated by the tasks
   */
  public CalculationTasks(List<CalculationTask> calculationTasks, List<Measure> measures) {
    this.calculationTasks = ImmutableList.copyOf(calculationTasks);
    this.measures = ImmutableList.copyOf(measures);
    List<MarketDataRequirements> reqs = calculationTasks.stream().map(CalculationTask::requirements).collect(toList());
    marketDataRequirements = MarketDataRequirements.combine(reqs);
  }

  /**
   * Returns IDs for the market data required for all calculations.
   *
   * @return IDs for the market data required for all calculations
   */
  public MarketDataRequirements getMarketDataRequirements() {
    return marketDataRequirements;
  }

  /**
   * Returns the objects that perform the individual calculations.
   * <p>
   * The results can be visualized as a grid with a column for each measure and a row for each target.
   * There is one calculation for each cell in the grid. The calculations in the list are arranged in row order.
   * For example, if a grid has 5 columns, the calculations 0-4 in the list are the first row, calculations 5-9 are
   * the second row and so on.
   *
   * @return the objects that perform the calculations
   */
  public List<CalculationTask> getTasks() {
    return calculationTasks;
  }

  /**
   * Returns the measures calculated by these calculations.
   * <p>
   * These can be thought of as the columns in the grid of results containing the calculations, where
   * there is a row for each input target.
   *
   * @return the measures calculated by these calculations
   */
  public List<Measure> getMeasures() {
    return measures;
  }
}
