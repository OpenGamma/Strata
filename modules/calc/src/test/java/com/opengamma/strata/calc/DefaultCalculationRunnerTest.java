/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

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
    Column column1 = Column.of(TestingMeasures.PRESENT_VALUE);
    Column column2 = Column.of(TestingMeasures.BUCKETED_PV01);
    ImmutableList<Column> columns = ImmutableList.of(column1, column2);
    CalculationRules rules = CalculationRules.of(CalculationFunctions.empty());
    MarketData md = MarketData.empty(date(2016, 6, 30));
    ScenarioMarketData smd = ScenarioMarketData.empty();

    // use of try-with-resources checks class is AutoCloseable
    try (CalculationRunner test = CalculationRunner.of(MoreExecutors.newDirectExecutorService())) {
      assertThat(test.calculate(rules, targets, columns, md, REF_DATA).get(0, 0).isFailure()).isTrue();
      assertThat(test.calculateMultiScenario(rules, targets, columns, smd, REF_DATA).get(0, 0).isFailure()).isTrue();
    }
  }

  //-------------------------------------------------------------------------
  private static class TestTarget implements CalculationTarget {
  }

}
