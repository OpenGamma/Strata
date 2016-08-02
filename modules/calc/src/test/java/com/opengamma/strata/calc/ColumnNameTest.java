/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ColumnName}.
 */
@Test
public class ColumnNameTest {

  //-------------------------------------------------------------------------
  public void test_builder_columnNameFromMeasure() {
    ColumnName test = ColumnName.of("Test");
    assertEquals(test.getName(), "Test");
  }

  //-------------------------------------------------------------------------
  public void test_serialization() {
    ColumnName test = ColumnName.of("Test");
    assertSerialization(test);
    assertJodaConvert(ColumnName.class, test);
  }

}
