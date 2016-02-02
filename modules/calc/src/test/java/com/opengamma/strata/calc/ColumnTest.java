/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.calc.config.Measures;

@Test
public class ColumnTest {

  /**
   * Tests that the column name is taken from the measure name if no name is supplied for the column.
   */
  public void columnNameTakenFromMeasure() {
    Column column = Column.builder().measure(Measures.PRESENT_VALUE).build();
    assertThat(column.getName().toString()).isEqualTo(Measures.PRESENT_VALUE.getName());
  }
}
