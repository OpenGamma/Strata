/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import java.util.List;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.PricingRules;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;

/**
 * Runs a set of calculations over a portfolio and returns the results.
 */
public interface CalculationEngine {

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
   * @param calculationTasks  configuration defining the calculations
   * @param marketData  market data to be used in the calculations
   * @return the calculation results
   */
  public abstract Results calculate(CalculationTasks calculationTasks, BaseMarketData marketData);

  /**
   * Performs a set of calculations for multiple scenarios, each with a different set of market data.
   *
   * @param calculationTasks  configuration defining the calculations
   * @param marketData  the market data used in the calculations
   * @return the results of running the calculations in the view for every item in the portfolio and every scenario
   */
  public abstract Results calculate(CalculationTasks calculationTasks, ScenarioMarketData marketData);
}
