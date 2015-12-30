/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.collect.Messages;

/**
 * The functions for performing a set of calculations and the market data required by the calculations.
 */
public class CalculationTasks {

  /**
   * The columns.
   */
  private final List<Column> columns;
  /**
   * The calculation tasks.
   * These are arranged in row order.
   */
  private final List<CalculationTask> calculationTasks;
  /**
   * The market data requirements.
   */
  private final MarketDataRequirements requirements;

  /**
   * @param calculationTasks  the tasks that perform the calculations
   * @param columns  the columns that define the calculations
   */
  public CalculationTasks(List<CalculationTask> calculationTasks, List<Column> columns) {
    this.calculationTasks = ImmutableList.copyOf(calculationTasks);
    this.columns = ImmutableList.copyOf(columns);
    List<MarketDataRequirements> reqs = calculationTasks.stream().map(CalculationTask::requirements).collect(toList());
    requirements = MarketDataRequirements.combine(reqs);

    // Validate the number of tasks and number of columns tally
    if (calculationTasks.size() != 0) {
      if (columns.size() == 0) {
        throw new IllegalArgumentException("There must be at least one column");
      }
      if (calculationTasks.size() % columns.size() != 0) {
        throw new IllegalArgumentException(
            Messages.format(
                "Number of tasks ({}) must be exactly divisible by the number of columns ({})",
                calculationTasks.size(),
                columns.size()));
      }
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the columns that will be calculated.
   * <p>
   * The result of the calculations will be a grid where each column is taken from this list.
   *
   * @return the columns forming the grid of calculations
   */
  public List<Column> getColumns() {
    return columns;
  }

  /**
   * Gets the tasks that perform the individual calculations.
   * <p>
   * The results can be visualized as a grid of columns with a row for each target.
   * There is one calculation for each cell in the grid. The calculations in the list are arranged in row order.
   * For example, if a grid has 5 columns, the calculations 0-4 in the list are the first row, calculations 5-9 are
   * the second row and so on.
   *
   * @return the tasks that perform the calculations
   */
  public List<CalculationTask> getTasks() {
    return calculationTasks;
  }

  /**
   * Gets the market data that is required to perform the calculations.
   *
   * @return the market data required for all calculations
   */
  public MarketDataRequirements getRequirements() {
    return requirements;
  }

}
