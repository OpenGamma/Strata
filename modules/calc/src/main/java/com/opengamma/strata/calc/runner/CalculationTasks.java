/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingRules;
import com.opengamma.strata.calc.config.pricing.ConfiguredFunctionGroup;
import com.opengamma.strata.calc.config.pricing.FunctionGroup;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.collect.Messages;

/**
 * The tasks that will be used to perform the calculations.
 */
public final class CalculationTasks {

  /**
   * The targets.
   */
  private final List<CalculationTarget> targets;
  /**
   * The columns.
   */
  private final List<Column> columns;
  /**
   * The calculation tasks.
   * <p>
   * The tasks in the list are arranged in row order.
   * For example, if a grid has 5 columns, the calculations 0-4 in the list are the first row,
   * calculations 5-9 are the second row and so on.
   */
  private final List<CalculationTask> calculationTasks;
  /**
   * The market data requirements.
   */
  private volatile MarketDataRequirements requirements;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from a set of targets, columns and rules.
   * 
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the columns that will be calculated
   * @param calculationRules  the rules defining how the calculation is performed
   * @return the calculation tasks
   */
  public static CalculationTasks of(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules) {

    // Create columns with rules that are a combination of the column overrides and the defaults
    List<Column> effectiveColumns =
        columns.stream()
            .map(column -> column.withDefaultRules(calculationRules))
            .collect(toImmutableList());

    // create the task configuration
    ImmutableList.Builder<CalculationTask> configBuilder = ImmutableList.builder();
    for (int i = 0; i < targets.size(); i++) {
      for (int j = 0; j < columns.size(); j++) {
        // TODO For each target, build a map of function group to set of measures.
        // Then request function config from the group for all measures at once
        configBuilder.add(createTask(i, j, targets.get(i), effectiveColumns.get(j)));
      }
    }

    // calculation tasks holds the original user-specified columns, not the derived ones
    return CalculationTasks.of(configBuilder.build(), columns);
  }

  // TODO This needs to handle a whole set of columns and return a list of config
  // TODO Need to group the columns by configured function group, market data mappings and reporting rules.
  //   Columns are only eligible to be calculated by the same fn if all the rules are the same
  //   Need a compound key? ConfigKey[ConfiguredFunctionGroup, MarketDataMappings]. ConfigGroup?
  //   What's the value? Column? Measure? Index? Some combination of the 3?
  // TODO Does this need to return an object containing different types of task config? CalculationTasksConfig?
  /**
   * Creates configuration for calculating the value of a single measure for a target.
   *
   * @param rowIndex  the row index of the value in the results grid
   * @param columnIndex  the column index of the value in the results grid
   * @param target  the target for which the measure will be calculated
   * @param column  the column for which the value is calculated
   * @return configuration for calculating the value for the target
   */
  private static CalculationTask createTask(
      int rowIndex,
      int columnIndex,
      CalculationTarget target,
      Column column) {

    Measure measure = column.getMeasure(target);
    Optional<ConfiguredFunctionGroup> functionGroup = column.getPricingRules().functionGroup(target, measure);

    // Use the mappings from the market data rules, else create a set of mappings that cause a failure to
    // be returned in the market data with an error message saying the rules didn't match the target
    MarketDataMappings marketDataMappings =
        column.getMarketDataRules().mappings(target)
            .orElse(NoMatchingRuleMappings.INSTANCE);

    ReportingRules reportingRules = column.getReportingRules();

    FunctionConfig<?> functionConfig = functionGroup
        .map(group -> functionConfig(group, target, column))
        .orElse(FunctionConfig.missing());

    Map<String, Object> functionArguments = functionGroup
        .map(ConfiguredFunctionGroup::getArguments)
        .orElse(ImmutableMap.of());

    return CalculationTask.of(
        target,
        measure,
        rowIndex,
        columnIndex,
        functionConfig.createFunction(functionArguments),
        marketDataMappings,
        reportingRules);
  }

  /**
   * Returns configuration for calculating a value.
   *
   * @param configuredGroup  the function group providing the function to calculate the value
   * @param target  the target of the calculation
   * @param column  the column containing the value. This defines the measure that is calculated
   * @return configuration for calculating the value
   */
  private static <T extends CalculationTarget> FunctionConfig<T> functionConfig(
      ConfiguredFunctionGroup configuredGroup,
      CalculationTarget target,
      Column column) {

    @SuppressWarnings("unchecked")
    FunctionGroup<T> functionGroup = (FunctionGroup<T>) configuredGroup.getFunctionGroup();
    Measure measure = column.getMeasure(target);
    return functionGroup.functionConfig(target, measure).orElse(FunctionConfig.missing());
  }

  /**
   * Obtains an instance from a set of tasks and columns.
   * 
   * @param calculationTasks  the tasks that perform the calculations
   * @param columns  the columns that define the calculations
   * @return the calculation tasks
   */
  public static CalculationTasks of(List<CalculationTask> calculationTasks, List<Column> columns) {
    return new CalculationTasks(calculationTasks, columns);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param calculationTasks  the tasks that perform the calculations
   * @param columns  the columns that define the calculations
   */
  private CalculationTasks(List<CalculationTask> calculationTasks, List<Column> columns) {
    this.columns = ImmutableList.copyOf(columns);
    this.calculationTasks = ImmutableList.copyOf(calculationTasks);

    // Validate the number of tasks and number of columns tally
    int taskCount = calculationTasks.size();
    int columnCount = columns.size();
    if (taskCount != 0) {
      if (columnCount == 0) {
        throw new IllegalArgumentException("There must be at least one column");
      }
      if (taskCount % columnCount != 0) {
        throw new IllegalArgumentException(
            Messages.format(
                "Number of tasks ({}) must be exactly divisible by the number of columns ({})",
                taskCount,
                columnCount));
      }
    }
    // pull out the targets from the tasks
    int targetCount = taskCount / columnCount;
    this.targets = IntStream.range(0, targetCount)
        .mapToObj(i -> calculationTasks.get(i * columnCount).getTarget())
        .collect(toImmutableList());
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the targets that will be calculated.
   * <p>
   * The result of the calculations will be a grid where each row is taken from this list.
   *
   * @return the targets forming the grid of calculations
   */
  public List<CalculationTarget> getTargets() {
    return targets;
  }

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
   * <p>
   * This can be used to feed into the market data system to obtain and calibrate data.
   *
   * @return the market data required for all calculations
   * @throws RuntimeException if unable to obtain the requirements
   */
  public MarketDataRequirements getRequirements() {
    MarketDataRequirements reqs = requirements;
    if (reqs == null) {
      List<MarketDataRequirements> result = calculationTasks.stream()
          .map(CalculationTask::requirements)
          .collect(toList());
      reqs = requirements = MarketDataRequirements.combine(result);

    }
    return reqs;
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return Messages.format("CalculationTasks[grid={}x{}]", targets.size(), columns.size());
  }

}
