/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.Resolvable;
import com.opengamma.strata.engine.calculations.CalculationRunner;
import com.opengamma.strata.engine.calculations.CalculationTasks;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataResult;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;

/**
 * Default implementation of {@link CalculationEngine} that delegates to a {@link CalculationRunner}
 * and a {@link MarketDataFactory}.
 */
public final class DefaultCalculationEngine implements CalculationEngine {

  /** The calculation runner that performs the calculations. */
  private final CalculationRunner calculationRunner;

  /** The factory that builds any market data not supplied by the caller. */
  private final MarketDataFactory marketDataFactory;

  /**
   * Resolves any links in the calculation targets to reference the linked objects.
   * <p>
   * For example, trades in fungible securities use a link to refer to the security instead of
   * embedding the security details in the trade. In order for the security to be accessible
   * to the calculation logic, the link must be resolved.
   */
  private final LinkResolver linkResolver;

  /**
   * @param calculationRunner  the calculation runner that performs the calculations
   * @param marketDataFactory  the factory that builds any market data not supplied by the caller
   * @param linkResolver  resolves links in the calculation targets to reference the linked objects
   */
  public DefaultCalculationEngine(
      CalculationRunner calculationRunner,
      MarketDataFactory marketDataFactory,
      LinkResolver linkResolver) {

    this.calculationRunner = ArgChecker.notNull(calculationRunner, "calculationRunner");
    this.marketDataFactory = ArgChecker.notNull(marketDataFactory, "marketDataFactory");
    this.linkResolver = ArgChecker.notNull(linkResolver, "linkResolver");
  }

  @Override
  public Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      BaseMarketData marketData) {

    CalculationTasksConfig config =
        calculationRunner.createCalculationConfig(
            resolveTargetLinks(targets),
            columns,
            calculationRules.getPricingRules(),
            calculationRules.getMarketDataRules(),
            calculationRules.getReportingRules());
    
    CalculationTasks tasks = calculationRunner.createCalculationTasks(config);

    MarketDataResult marketDataResult =
        marketDataFactory.buildBaseMarketData(
            tasks.getMarketDataRequirements(),
            marketData);

    return calculationRunner.calculate(tasks, marketDataResult.getMarketData());
  }

  @Override
  public Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      BaseMarketData baseMarketData,
      ScenarioDefinition scenarioDefinition) {

    CalculationTasksConfig config =
        calculationRunner.createCalculationConfig(
            resolveTargetLinks(targets),
            columns,
            calculationRules.getPricingRules(),
            calculationRules.getMarketDataRules(),
            calculationRules.getReportingRules());

    CalculationTasks tasks = calculationRunner.createCalculationTasks(config);

    MarketDataResult marketDataResult =
        marketDataFactory.buildBaseMarketData(
            tasks.getMarketDataRequirements(),
            baseMarketData);

    ScenarioMarketData scenarioMarketData =
        marketDataFactory.buildScenarioMarketData(
            marketDataResult.getMarketData(),
            scenarioDefinition);

    return calculationRunner.calculate(tasks, scenarioMarketData);
  }

  /**
   * Returns calculation targets with any links resolved to reference the linked objects.
   *
   * @param targets  the calculation targets
   * @return the targets with any links resolved to reference the linked objects
   */
  private List<CalculationTarget> resolveTargetLinks(List<? extends CalculationTarget> targets) {
    return targets.stream().map(this::resolveTargetLinks).collect(toImmutableList());
  }

  /**
   * Returns a calculation target with any links resolved to reference the linked objects.
   *
   * @param target  a calculation target
   * @return the target with any links resolved to reference the linked objects
   */
  private CalculationTarget resolveTargetLinks(CalculationTarget target) {
    if (target instanceof Resolvable) {
      Object resolvedTarget = ((Resolvable) target).resolveLinks(linkResolver);
      return (CalculationTarget) resolvedTarget;
    } else {
      return target;
    }
  }
}
