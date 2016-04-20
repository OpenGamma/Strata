/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.calc.config.Measures.PAR_RATE;
import static com.opengamma.strata.calc.config.Measures.PRESENT_VALUE;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;

/**
 * Test {@link TaskColumn}.
 */
@Test
public class TaskColumnTest {

  private static final TestTarget TARGET = new TestTarget();
  private static final TestTarget TARGET2 = new TestTarget();
  private static final ReportingCurrency REP_CCY = ReportingCurrency.of(USD);

  public void of() {
    Column column = Column.of(PRESENT_VALUE, REP_CCY);
    TaskColumn test = TaskColumn.of(TARGET, column, 1);
    assertEquals(test.getColumnIndex(), 1);
    assertEquals(test.getMeasure(), PRESENT_VALUE);
    assertEquals(test.getParameters(), CalculationParameters.empty());
    assertEquals(test.getMarketDataMappings(), NoMatchingRuleMappings.INSTANCE);
    assertEquals(test.getReportingCurrency(), REP_CCY);
    assertEquals(test.toString(), "TaskColumn[index=1, measure=PresentValue, currency=Specific:USD]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Column column = Column.of(PRESENT_VALUE, REP_CCY);
    TaskColumn test = TaskColumn.of(TARGET, column, 1);
    coverImmutableBean(test);
    Column column2 = Column.of(PAR_RATE, REP_CCY);
    TaskColumn test2 = TaskColumn.of(TARGET2, column2, 2);
    coverBeanEquals(test, test2);
    assertNotNull(TaskColumn.meta());
  }

}
