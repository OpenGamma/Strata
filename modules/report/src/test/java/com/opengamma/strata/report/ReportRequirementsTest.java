/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.measure.Measures;

/**
 * Test {@link ReportRequirements}.
 */
@Test
public class ReportRequirementsTest {

  private static final Column COLUMN = Column.of(Measures.PRESENT_VALUE);
  private static final Column COLUMN2 = Column.of(Measures.PAR_RATE);

  //-------------------------------------------------------------------------
  public void test_of() {
    ReportRequirements test = sut();
    assertEquals(test.getTradeMeasureRequirements(), ImmutableList.of(COLUMN));
  }

  public void test_of_array() {
    ReportRequirements test = ReportRequirements.of(COLUMN, COLUMN2);
    assertEquals(test.getTradeMeasureRequirements(), ImmutableList.of(COLUMN, COLUMN2));
  }

  //-------------------------------------------------------------------------
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
