/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.calc.ReportingCurrency;
import com.opengamma.strata.calc.TestingMeasures;

/**
 * Test {@link CalculationTaskCell}.
 */
public class CalculationTaskCellTest {

  @Test
  public void of() {
    CalculationTaskCell test = CalculationTaskCell.of(1, 2, TestingMeasures.PRESENT_VALUE, ReportingCurrency.of(USD));
    assertThat(test.getRowIndex()).isEqualTo(1);
    assertThat(test.getColumnIndex()).isEqualTo(2);
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getReportingCurrency()).isEqualTo(ReportingCurrency.of(USD));
    assertThat(test.toString()).isEqualTo("CalculationTaskCell[(1, 2), measure=PresentValue, currency=Specific:USD]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CalculationTaskCell test = CalculationTaskCell.of(1, 2, TestingMeasures.PRESENT_VALUE, ReportingCurrency.of(USD));
    coverImmutableBean(test);
    CalculationTaskCell test2 = CalculationTaskCell.of(1, 2, TestingMeasures.PAR_RATE, ReportingCurrency.NATURAL);
    coverBeanEquals(test, test2);
    assertThat(CalculationTaskCell.meta()).isNotNull();
  }

}
