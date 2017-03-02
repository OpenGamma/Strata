/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.ReportingCurrency;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.MarketDataRequirementsBuilder;
import com.opengamma.strata.collect.Messages;

/**
 * The tasks that will be used to perform the calculations.
 * <p>
 * This captures the targets, columns and tasks that define the result grid.
 * Each task can be executed to produce the result. Applications will typically
 * use {@link CalculationRunner} or {@link CalculationTaskRunner} to execute the tasks.
 */
@BeanDefinition(style = "light")
public final class CalculationTasks implements ImmutableBean {

  /**
   * The targets that calculations will be performed on.
   * <p>
   * The result of the calculations will be a grid where each row is taken from this list.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final List<CalculationTarget> targets;
  /**
   * The columns that will be calculated.
   * <p>
   * The result of the calculations will be a grid where each column is taken from this list.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final List<Column> columns;
  /**
   * The tasks that perform the individual calculations.
   * <p>
   * The results can be visualized as a grid, with a row for each target and a column for each measure.
   * Each task can calculate the result for one or more cells in the grid.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final List<CalculationTask> tasks;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a set of targets, columns and rules.
   * <p>
   * The targets will typically be trades.
   * The columns represent the measures to calculate.
   * 
   * @param rules  the rules defining how the calculation is performed
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the columns that will be calculated
   * @return the calculation tasks
   */
  public static CalculationTasks of(
      CalculationRules rules,
      List<? extends CalculationTarget> targets,
      List<Column> columns) {

    // create columns that are a combination of the column overrides and the defaults
    // this is done once as it is the same for all targets
    List<Column> effectiveColumns =
        columns.stream()
            .map(column -> column.combineWithDefaults(rules.getReportingCurrency(), rules.getParameters()))
            .collect(toImmutableList());

    // loop around the targets, then the columns, to build the tasks
    ImmutableList.Builder<CalculationTask> taskBuilder = ImmutableList.builder();
    for (int rowIndex = 0; rowIndex < targets.size(); rowIndex++) {
      CalculationTarget target = targets.get(rowIndex);

      // find the applicable function
      CalculationFunction<?> fn = rules.getFunctions().getFunction(target);

      // create the tasks
      List<CalculationTask> targetTasks = createTargetTasks(target, rowIndex, fn, effectiveColumns);
      taskBuilder.addAll(targetTasks);
    }

    // calculation tasks holds the original user-specified columns, not the derived ones
    return new CalculationTasks(taskBuilder.build(), columns);
  }

  // creates the tasks for a single target
  private static List<CalculationTask> createTargetTasks(
      CalculationTarget target,
      int rowIndex,
      CalculationFunction<?> function,
      List<Column> columns) {

    // create the cells and group them
    ListMultimap<CalculationParameters, CalculationTaskCell> grouped = ArrayListMultimap.create();
    for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
      Column column = columns.get(colIndex);
      Measure measure = column.getMeasure();

      ReportingCurrency reportingCurrency = column.getReportingCurrency().orElse(ReportingCurrency.NATURAL);
      CalculationTaskCell cell = CalculationTaskCell.of(rowIndex, colIndex, measure, reportingCurrency);
      // group to find cells that can be shared, with same mappings and params (minus reporting currency)
      CalculationParameters params = column.getParameters().filter(target, measure);
      grouped.put(params, cell);
    }

    // build tasks
    ImmutableList.Builder<CalculationTask> taskBuilder = ImmutableList.builder();
    for (CalculationParameters params : grouped.keySet()) {
      taskBuilder.add(CalculationTask.of(target, function, params, grouped.get(params)));
    }
    return taskBuilder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a set of tasks and columns.
   * 
   * @param tasks  the tasks that perform the calculations
   * @param columns  the columns that define the calculations
   * @return the calculation tasks
   */
  public static CalculationTasks of(List<CalculationTask> tasks, List<Column> columns) {
    return new CalculationTasks(tasks, columns);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param tasks  the tasks that perform the calculations
   * @param columns  the columns that define the calculations
   */
  private CalculationTasks(List<CalculationTask> tasks, List<Column> columns) {
    this.columns = ImmutableList.copyOf(columns);
    this.tasks = ImmutableList.copyOf(tasks);

    // validate the number of tasks and number of columns tally
    long cellCount = tasks.stream()
        .flatMap(task -> task.getCells().stream())
        .count();
    int columnCount = columns.size();
    if (cellCount != 0) {
      if (columnCount == 0) {
        throw new IllegalArgumentException("There must be at least one column");
      }
      if (cellCount % columnCount != 0) {
        throw new IllegalArgumentException(
            Messages.format(
                "Number of cells ({}) must be exactly divisible by the number of columns ({})",
                cellCount,
                columnCount));
      }
    }

    // pull out the targets from the tasks
    int targetCount = (int) cellCount / columnCount;
    CalculationTarget[] targets = new CalculationTarget[targetCount];
    for (CalculationTask task : tasks) {
      int rowIdx = task.getRowIndex();
      if (targets[rowIdx] == null) {
        targets[rowIdx] = task.getTarget();
      } else if (targets[rowIdx] != task.getTarget()) {
        throw new IllegalArgumentException(Messages.format(
            "Tasks define two different targets for row {}: {} and {}", rowIdx, targets[rowIdx], task.getTarget()));
      }
    }
    this.targets = ImmutableList.copyOf(targets);  // missing targets will be caught here by null check
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the market data that is required to perform the calculations.
   * <p>
   * This can be used to pass into the market data system to obtain and calibrate data.
   *
   * @param refData  the reference data
   * @return the market data required for all calculations
   * @throws RuntimeException if unable to obtain the requirements
   */
  public MarketDataRequirements requirements(ReferenceData refData) {
    // use for loop not streams for shorter stack traces
    MarketDataRequirementsBuilder builder = MarketDataRequirements.builder();
    for (CalculationTask task : tasks) {
      builder.addRequirements(task.requirements(refData));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return Messages.format("CalculationTasks[grid={}x{}]", targets.size(), columns.size());
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code CalculationTasks}.
   */
  private static final MetaBean META_BEAN = LightMetaBean.of(CalculationTasks.class);

  /**
   * The meta-bean for {@code CalculationTasks}.
   * @return the meta-bean, not null
   */
  public static MetaBean meta() {
    return META_BEAN;
  }

  static {
    JodaBeanUtils.registerMetaBean(META_BEAN);
  }

  private CalculationTasks(
      List<CalculationTarget> targets,
      List<Column> columns,
      List<CalculationTask> tasks) {
    JodaBeanUtils.notEmpty(targets, "targets");
    JodaBeanUtils.notEmpty(columns, "columns");
    JodaBeanUtils.notEmpty(tasks, "tasks");
    this.targets = ImmutableList.copyOf(targets);
    this.columns = ImmutableList.copyOf(columns);
    this.tasks = ImmutableList.copyOf(tasks);
  }

  @Override
  public MetaBean metaBean() {
    return META_BEAN;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the targets that calculations will be performed on.
   * <p>
   * The result of the calculations will be a grid where each row is taken from this list.
   * @return the value of the property, not empty
   */
  public List<CalculationTarget> getTargets() {
    return targets;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the columns that will be calculated.
   * <p>
   * The result of the calculations will be a grid where each column is taken from this list.
   * @return the value of the property, not empty
   */
  public List<Column> getColumns() {
    return columns;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the tasks that perform the individual calculations.
   * <p>
   * The results can be visualized as a grid, with a row for each target and a column for each measure.
   * Each task can calculate the result for one or more cells in the grid.
   * @return the value of the property, not empty
   */
  public List<CalculationTask> getTasks() {
    return tasks;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      CalculationTasks other = (CalculationTasks) obj;
      return JodaBeanUtils.equal(targets, other.targets) &&
          JodaBeanUtils.equal(columns, other.columns) &&
          JodaBeanUtils.equal(tasks, other.tasks);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(targets);
    hash = hash * 31 + JodaBeanUtils.hashCode(columns);
    hash = hash * 31 + JodaBeanUtils.hashCode(tasks);
    return hash;
  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
