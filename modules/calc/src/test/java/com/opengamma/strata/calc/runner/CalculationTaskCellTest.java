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

import com.opengamma.strata.calc.config.ReportingCurrency;

/**
 * Test {@link CalculationTaskCell}.
 */
@Test
public class CalculationTaskCellTest {

  public void of() {
    CalculationTaskCell test = CalculationTaskCell.of(1, 2, PRESENT_VALUE, ReportingCurrency.of(USD));
    assertEquals(test.getRowIndex(), 1);
    assertEquals(test.getColumnIndex(), 2);
    assertEquals(test.getMeasure(), PRESENT_VALUE);
    assertEquals(test.getReportingCurrency(), ReportingCurrency.of(USD));
    assertEquals(test.toString(), "CalculationTaskCell[(1, 2), measure=PresentValue, currency=Specific:USD]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationTaskCell test = CalculationTaskCell.of(1, 2, PRESENT_VALUE, ReportingCurrency.of(USD));
    coverImmutableBean(test);
    CalculationTaskCell test2 = CalculationTaskCell.of(1, 2, PAR_RATE, ReportingCurrency.NATURAL);
    coverBeanEquals(test, test2);
    assertNotNull(CalculationTaskCell.meta());
  }

}
