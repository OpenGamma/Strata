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

@Test
public class CalculationTaskConfigTest {

  private static final CalculationTarget TARGET = new TestTarget();

  public void test_of() {
    CalculationTaskConfig test = CalculationTaskConfig.of(
        TARGET, 1, 2, FunctionConfig.missing(), ImmutableMap.of(), MarketDataMappings.empty(), ReportingRules.empty());
    assertEquals(test.getTarget(), TARGET);
    assertEquals(test.getRowIndex(), 1);
    assertEquals(test.getColumnIndex(), 2);
    assertEquals(test.getFunctionConfig(), FunctionConfig.missing());
    assertEquals(test.getFunctionArguments(), ImmutableMap.of());
    assertEquals(test.getMarketDataMappings(), MarketDataMappings.empty());
    assertEquals(test.getReportingRules(), ReportingRules.empty());
  }

  public void coverage() {
    CalculationTaskConfig test = CalculationTaskConfig.of(
        TARGET, 1, 2, FunctionConfig.missing(), ImmutableMap.of(), MarketDataMappings.empty(), ReportingRules.empty());
    coverImmutableBean(test);
  }

  //-------------------------------------------------------------------------
  private static final class TestTarget implements CalculationTarget {
  }

}
