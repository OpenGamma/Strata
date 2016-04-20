/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Builder used to create calculation tasks.
 * <p>
 * The {@link CalculationRunner} produces a grid of results, with a row for each target
 * and a column for each measure. The targets and columns that define the grid of results
 * are passed in using an instance of {@code CalculationTasks}.
 * This builder is used to produce the tasks for a single target.
 * <p>
 * The result of the builder consists of a number of tasks, each containing one or more task cells.
 * Each task cell corresponds to a single column in the row.
 */
public final class TaskBuilder {

  /**
   * The target for which the value will be calculated.
   * This is typically a trade.
   */
  private final CalculationTarget target;
  /**
   * The row index of the target in the results grid.
   */
  private final int rowIndex;
  /**
   * The function to use.
   */
  private final CalculationFunction<?> function;
  /**
   * The columns to be calculated.
   */
  private final List<TaskColumn> columns;
  /**
   * The columns to be calculated separately.
   */
  private final boolean[] separate;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   *
   * @param target  the target for which the value will be calculated
   * @param rowIndex  the row index of the value in the results grid
   * @param function  the function that performs the calculation
   * @param columns  the columns to calculate
   * @return the builder
   */
  static TaskBuilder of(
      CalculationTarget target,
      int rowIndex,
      CalculationFunction<?> function,
      List<TaskColumn> columns) {

    return new TaskBuilder(target, rowIndex, function, columns);
  }

  /**
   * Creates the builder.
   *
   * @param target  the target for which the value will be calculated
   * @param rowIndex  the row index of the value in the results grid
   * @param function  the function that performs the calculation
   * @param columns  the columns to calculate
   */
  private TaskBuilder(
      CalculationTarget target,
      int rowIndex,
      CalculationFunction<?> function,
      List<TaskColumn> columns) {

    this.target = ArgChecker.notNull(target, "target");
    this.rowIndex = ArgChecker.notNegative(rowIndex, "rowIndex");
    this.function = ArgChecker.notNull(function, "function");
    this.columns = ArgChecker.notNull(columns, "columns");
    this.separate = new boolean[columns.size()];
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the target.
   * 
   * @return the calculation target
   */
  public CalculationTarget getTarget() {
    return target;
  }

  /**
   * Gets the row index.
   * 
   * @return the row index
   */
  public int getRowIndex() {
    return rowIndex;
  }

  /**
   * Gets the function.
   * 
   * @return the calculation function
   */
  public CalculationFunction<?> getFunction() {
    return function;
  }

  /**
   * Gets the columns.
   * 
   * @return the calculation columns
   */
  public List<TaskColumn> getColumns() {
    return columns;
  }

  //-------------------------------------------------------------------------
  /**
   * Causes the specified column to be executed as a separate task.
   * <p>
   * This might be used if a measure is significantly slower to calculate than other measures.
   * By executing separately, the calculation of the slow measure will not block calculation
   * of other measures.
   * 
   * @param column  the column to execute separately
   * @return this builder
   */
  public TaskBuilder executeSeparately(TaskColumn column) {
    separate[column.getColumnIndex()] = true;
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Builds the tasks based on the state of the builder.
   * 
   * @return the tasks
   */
  public List<CalculationTask> build() {
    ImmutableList.Builder<CalculationTask> taskBuilder = ImmutableList.builder();

    // create the cells and group them
    ListMultimap<GroupingKey, CalculationTaskCell> grouped = ArrayListMultimap.create();
    for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
      TaskColumn column = columns.get(colIndex);
      CalculationTaskCell cell =
          CalculationTaskCell.of(rowIndex, colIndex, column.getMeasure(), column.getReportingCurrency());
      if (separate[colIndex]) {
        taskBuilder.add(CalculationTask.of(
            target, function, column.getMarketDataMappings(), column.getParameters(), ImmutableList.of(cell)));
      } else {
        grouped.put(new GroupingKey(column.getMarketDataMappings(), column.getParameters()), cell);
      }
    }

    // build tasks
    for (GroupingKey key : grouped.keySet()) {
      taskBuilder.add(CalculationTask.of(target, function, key.mappings, key.parameters, grouped.get(key)));
    }
    return taskBuilder.build();
  }

  // key used for grouping
  static final class GroupingKey {
    final MarketDataMappings mappings;
    final CalculationParameters parameters;

    GroupingKey(MarketDataMappings mappings, CalculationParameters parameters) {
      this.mappings = mappings;
      this.parameters = parameters;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof GroupingKey) {
        GroupingKey other = (GroupingKey) obj;
        // mappings matched by ==, parameters by equals()
        return mappings == other.mappings &&
            parameters.equals(other.parameters);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return mappings.hashCode() ^ parameters.hashCode();
    }
  }

}
