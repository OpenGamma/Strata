/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ColumnHeader}.
 */
public class ColumnHeaderTest {

  @Test
  public void test_of_NameMeasure() {
    ColumnHeader test = ColumnHeader.of(ColumnName.of("ParRate"), TestingMeasures.PAR_RATE);
    assertThat(test.getName()).isEqualTo(ColumnName.of("ParRate"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PAR_RATE);
    assertThat(test.getCurrency()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_NameMeasureCurrency() {
    ColumnHeader test = ColumnHeader.of(ColumnName.of("NPV"), TestingMeasures.PRESENT_VALUE, USD);
    assertThat(test.getName()).isEqualTo(ColumnName.of("NPV"));
    assertThat(test.getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
    assertThat(test.getCurrency()).isEqualTo(Optional.of(USD));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ColumnHeader test = ColumnHeader.of(ColumnName.of("NPV"), TestingMeasures.PRESENT_VALUE, USD);
    coverImmutableBean(test);
    ColumnHeader test2 = ColumnHeader.of(ColumnName.of("ParRate"), TestingMeasures.PAR_RATE);
    coverBeanEquals(test, test2);
  }

}
