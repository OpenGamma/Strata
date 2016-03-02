/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.pricing.PricingRules;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;

/**
 * Test {@link CalculationRunner} and {@link DefaultCalculationRunner}.
 */
@Test
public class DefaultCalculationRunnerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final TestTarget TARGET = new TestTarget();

  //-------------------------------------------------------------------------
  public void test_of() {
    try (CalculationRunner test = CalculationRunner.ofMultiThreaded()) {
      assertThat(test.getTaskRunner()).isNotNull();
    }
  }

  //-------------------------------------------------------------------------
  public void calculate() {
    ImmutableList<CalculationTarget> targets = ImmutableList.of(TARGET);
    Column column1 = Column.of(Measures.PRESENT_VALUE);
    Column column2 = Column.of(Measures.BUCKETED_PV01);
    ImmutableList<Column> columns = ImmutableList.of(column1, column2);
    CalculationRules rules = CalculationRules.of(PricingRules.empty(), MarketDataRules.empty());
    CalculationEnvironment marketData = CalculationEnvironment.empty();

    // use of try-with-resources checks class is AutoCloseable
    try (CalculationRunner test = CalculationRunner.of(MoreExecutors.newDirectExecutorService())) {
      assertThat(test.calculateSingleScenario(rules, targets, columns, marketData, REF_DATA).get(0, 0).isFailure()).isTrue();
      assertThat(test.calculateMultipleScenarios(rules, targets, columns, marketData, REF_DATA).get(0, 0).isFailure()).isTrue();
    }
  }

  //-------------------------------------------------------------------------
  private static class TestTarget implements CalculationTarget {
  }

}
