/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import java.util.List;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.pricing.PricingRules;
import com.opengamma.strata.marketdata.BaseMarketData;
import com.opengamma.strata.marketdata.ScenarioMarketData;

/**
 * Runs a set of calculations over a portfolio and returns the results.
 * <p>
 * TODO Example code in the Javadoc showing how to use this
 */
public interface CalculationRunner {

  /**
   * Creates configuration for a set of calculations.
   *
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated, including the measure and
   *   any column-specific overrides
   * @param pricingRules  rules which define how the values are calculated
   * @param marketDataRules  rules which define the market data used in the calculations
   * @param reportingRules  rules which define how results should be reported
   * @return configuration for calculating values for a set of measures for a set of targets
   */
  public abstract CalculationTasksConfig createCalculationConfig(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      PricingRules pricingRules,
      MarketDataRules marketDataRules,
      ReportingRules reportingRules);

  /**
   * Creates a set of calculations for calculating a set of measures for a set of targets.
   *
   * @param config  configuration for the tasks the perform the calculations
   * @return a set of calculations for calculating a set of measures for a set of targets
   */
  public abstract CalculationTasks createCalculationTasks(CalculationTasksConfig config);

  /**
   * Performs a set of calculations for a single scenario.
   *
   * @param tasks  configuration defining the calculations
   * @param marketData  market data to be used in the calculations
   * @return the calculation results
   */
  public abstract Results calculate(CalculationTasks tasks, BaseMarketData marketData);

  /**
   * Performs a set of calculations for multiple scenarios, each with a different set of market data.
   *
   * @param tasks  tasks that perform the calculations
   * @param marketData  the market data used in the calculations
   * @return the results of running the calculations in the view for every item in the portfolio and every scenario
   */
  public abstract Results calculate(CalculationTasks tasks, ScenarioMarketData marketData);

  /**
   * Asynchronously performs a set of calculations for a single scenario, invoking a listener as
   * each calculation completes.
   * <p>
   * This method requires the listener to assemble the results, but it can be much more memory efficient when
   * calculating aggregate results. If the individual results are discarded after they are incorporated into
   * the aggregate they can be garbage collected.
   *
   * @param tasks  tasks that perform the calculations
   * @param marketData  market data to be used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateAsync(
      CalculationTasks tasks,
      BaseMarketData marketData,
      CalculationListener listener);

  /**
   * Asynchronously performs a set of calculations for multiple scenarios, each with a different set of market data.
   * A listener is invoked when each calculation completes.
   * <p>
   * This method requires the listener to assemble the results, but it can be much more memory efficient when
   * calculating aggregate results. If the individual results are discarded after they are incorporated into
   * the aggregate they can be garbage collected.
   *
   * @param tasks  tasks that perform the calculations
   * @param marketData  the market data used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateAsync(
      CalculationTasks tasks,
      ScenarioMarketData marketData,
      CalculationListener listener);
}
