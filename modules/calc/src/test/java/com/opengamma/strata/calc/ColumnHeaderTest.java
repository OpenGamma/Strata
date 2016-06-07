/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

/**
 * Test {@link ColumnHeader}.
 */
@Test
public class ColumnHeaderTest {

  public void test_of_NameMeasure() {
    ColumnHeader test = ColumnHeader.of(ColumnName.of("ParRate"), TestingMeasures.PAR_RATE);
    assertEquals(test.getName(), ColumnName.of("ParRate"));
    assertEquals(test.getMeasure(), TestingMeasures.PAR_RATE);
    assertEquals(test.getCurrency(), Optional.empty());
  }

  public void test_of_NameMeasureCurrency() {
    ColumnHeader test = ColumnHeader.of(ColumnName.of("NPV"), TestingMeasures.PRESENT_VALUE, USD);
    assertEquals(test.getName(), ColumnName.of("NPV"));
    assertEquals(test.getMeasure(), TestingMeasures.PRESENT_VALUE);
    assertEquals(test.getCurrency(), Optional.of(USD));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ColumnHeader test = ColumnHeader.of(ColumnName.of("NPV"), TestingMeasures.PRESENT_VALUE, USD);
    coverImmutableBean(test);
    ColumnHeader test2 = ColumnHeader.of(ColumnName.of("ParRate"), TestingMeasures.PAR_RATE);
    coverBeanEquals(test, test2);
  }

}
