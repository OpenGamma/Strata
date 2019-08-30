/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ColumnName}.
 */
public class ColumnNameTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_columnNameFromMeasure() {
    ColumnName test = ColumnName.of("Test");
    assertThat(test.getName()).isEqualTo("Test");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    ColumnName test = ColumnName.of("Test");
    assertSerialization(test);
    assertJodaConvert(ColumnName.class, test);
  }

}
