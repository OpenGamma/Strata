/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.config.ReportingCurrency;
import com.opengamma.strata.calc.runner.CalculationParameters;

/**
 * Test {@link Column}.
 */
@Test
public class ColumnTest {

  public void test_builder_columnNameFromMeasure() {
    Column test = Column.builder().measure(Measures.PRESENT_VALUE).build();
    assertEquals(test.getName(), ColumnName.of(Measures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_builder_columnNameSpecified() {
    Column test = Column.builder().measure(Measures.PRESENT_VALUE).name(ColumnName.of("NPV")).build();
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_Measure() {
    Column test = Column.of(Measures.PRESENT_VALUE);
    assertEquals(test.getName(), ColumnName.of(Measures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MeasureCalculationParameters() {
    Column test = Column.of(Measures.PRESENT_VALUE, ReportingCurrency.NATURAL);
    assertEquals(test.getName(), ColumnName.of(Measures.PRESENT_VALUE.getName()));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.NATURAL));
  }

  public void test_of_MeasureString() {
    Column test = Column.of(Measures.PRESENT_VALUE, "NPV");
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getParameters(), CalculationParameters.empty());
  }

  public void test_of_MeasureStringCalculationParameters() {
    Column test = Column.of(Measures.PRESENT_VALUE, "NPV", ReportingCurrency.NATURAL);
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), Measures.PRESENT_VALUE);
    assertEquals(test.getParameters(), CalculationParameters.of(ReportingCurrency.NATURAL));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Column test = Column.of(Measures.PRESENT_VALUE, "NPV", ReportingCurrency.NATURAL);
    coverImmutableBean(test);
    Column test2 = Column.of(Measures.CASH_FLOWS);
    coverBeanEquals(test, test2);
  }

}
