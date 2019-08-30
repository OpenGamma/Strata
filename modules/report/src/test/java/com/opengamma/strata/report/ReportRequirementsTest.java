/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.measure.Measures;

/**
 * Test {@link ReportRequirements}.
 */
public class ReportRequirementsTest {

  private static final Column COLUMN = Column.of(Measures.PRESENT_VALUE);
  private static final Column COLUMN2 = Column.of(Measures.PAR_RATE);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ReportRequirements test = sut();
    assertThat(test.getTradeMeasureRequirements()).containsExactly(COLUMN);
  }

  @Test
  public void test_of_array() {
    ReportRequirements test = ReportRequirements.of(COLUMN, COLUMN2);
    assertThat(test.getTradeMeasureRequirements()).containsExactly(COLUMN, COLUMN2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  //-------------------------------------------------------------------------
  static ReportRequirements sut() {
    return ReportRequirements.of(ImmutableList.of(COLUMN));
  }

  static ReportRequirements sut2() {
    return ReportRequirements.of(ImmutableList.of(COLUMN2));
  }

}
