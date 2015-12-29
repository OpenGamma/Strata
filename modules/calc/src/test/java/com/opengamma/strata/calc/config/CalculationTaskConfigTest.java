/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.CalculationTask;

/**
 * Test {@link CalculationTaskConfig}.
 */
@Test
public class CalculationTaskConfigTest {

  private static final CalculationTarget TARGET = new TestTarget();
  private static final Measure MEASURE = Measure.of("PV");
  private static final MarketDataMappings EMPTY_MAPPINGS = MarketDataMappings.empty();
  private static final ReportingRules EMPTY_REPORTING = ReportingRules.empty();

  public void test_of() {
    CalculationTaskConfig test = CalculationTaskConfig.of(
        TARGET, MEASURE, 1, 2, FunctionConfig.missing(), ImmutableMap.of(), EMPTY_MAPPINGS, EMPTY_REPORTING);
    assertEquals(test.getTarget(), TARGET);
    assertEquals(test.getRowIndex(), 1);
    assertEquals(test.getColumnIndex(), 2);
    assertEquals(test.getFunctionConfig(), FunctionConfig.missing());
    assertEquals(test.getFunctionArguments(), ImmutableMap.of());
    assertEquals(test.getMarketDataMappings(), EMPTY_MAPPINGS);
    assertEquals(test.getReportingRules(), EMPTY_REPORTING);
  }

  public void test_create() {
    CalculationTaskConfig test = CalculationTaskConfig.of(
        TARGET, MEASURE, 1, 2, FunctionConfig.missing(), ImmutableMap.of(), EMPTY_MAPPINGS, EMPTY_REPORTING);
    assertEquals(test.createFunction().getClass(), FunctionConfig.missing().createFunction().getClass());
    CalculationTask expectedTask =
        CalculationTask.of(TARGET, MEASURE, 1, 2, test.createFunction(), EMPTY_MAPPINGS, EMPTY_REPORTING);
    assertEquals(test.createTask().toString(), expectedTask.toString());
  }

  public void coverage() {
    CalculationTaskConfig test = CalculationTaskConfig.of(
        TARGET, MEASURE, 1, 2, FunctionConfig.missing(), ImmutableMap.of(), EMPTY_MAPPINGS, EMPTY_REPORTING);
    coverImmutableBean(test);
  }

  //-------------------------------------------------------------------------
  private static final class TestTarget implements CalculationTarget {
  }

}
