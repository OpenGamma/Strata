/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import java.util.List;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.calculation.Results;
import com.opengamma.strata.engine.marketdata.MarketEnvironment;
import com.opengamma.strata.engine.marketdata.scenario.ScenarioDefinition;

/**
 * The calculation engine is the main entry point for performing calculations.
 * <p>
 * The engine calculates values of measures for a set of calculation targets, often trades.
 * <p>
 * The output of a calculation is a grid of results with a row for each target and a column for each measure.
 * <p>
 * In order to perform calculations, a user must provide the following information:
 * <ul>
 *   <li>The targets for which the calculations should be performed, often trades</li>
 *   <li>The measures which should be calculated</li>
 *   <li>Rules defining how the calculations should be performed and what market data should be used</li>
 *   <li>The market data</li>
 * </ul>
 * Providing the market data is optional. The engine will attempt to provide any data that is required by
 * the calculations but not provided by the user.
 */
public interface CalculationEngine {

  /**
   * Calculates values of measures for a set of targets.
   * <p>
   * The output of the calculations is a grid of results with a row for each target and a column for each measure.
   * <p>
   * The calculation rules specify how the calculations should be performed. This includes the model and
   * model parameters, the market data that should be used, and the reporting currency of the results.
   * <p>
   * The column arguments specify what measure the column contains and also specifies any rule overrides
   * that apply to the column.
   *
   * @param targets  the targets for which values of the measures will be calculated, often trades
   * @param columns  the configuration for the columns that will be calculated, including the measure and
   *   any column-specific overrides
   * @param calculationRules  the rules defining how the calculations are performed, what market data
   *   should be used for each calculation and how the results should be reported
   * @param marketEnvironment  the market data used in the calculations. If the calculations require data that is
   *   not provided in this set, the engine will attempts to provide the missing data
   * @return the results of the calculations
   */
  public abstract Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      MarketEnvironment marketEnvironment);

  /**
   * Calculates values of measures for a set of targets over multiple scenarios.
   * Multiple values are calculated for each measure, one for each scenario.
   * <p>
   * The output of the calculations is a grid of results with a row for each target and a column for each measure.
   * Each cell contains the results for all scenarios, normally as a vector or array, or possibly an aggregate
   * value.
   * <p>
   * The calculation rules specify how the calculations should be performed. This includes the model and
   * model parameters, the market data that should be used, and the reporting currency of the results.
   * <p>
   * The column arguments specify what measure the column contains and also specifies any rule overrides
   * that apply to the column.
   * <p>
   * The scenario data is derived by applying the scenario definition to the base market data to produce
   * a set of market data for each scenario.
   *
   * @param targets  the targets for which values of the measures will be calculated, often trades
   * @param columns  the configuration for the columns that will be calculated, including the measure and
   *   any column-specific overrides
   * @param calculationRules  the rules defining how the calculations are performed, what market data
   *   should be used for each calculation and how the results should be reported
   * @param marketEnvironment  the market data used in the calculations. If the calculations require data that is
   *   not provided in this set, the engine will attempts to provide the missing data
   * @param scenarioDefinition  defines how the market data for each scenario is derived from the base data
   *
   * @return the results of the calculations
   */
  public abstract Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      MarketEnvironment marketEnvironment,
      ScenarioDefinition scenarioDefinition);
}
