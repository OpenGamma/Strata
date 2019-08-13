/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

/**
 * Test {@link PriceIndexObservation}.
 */
public class PriceIndexObservationTest {

  private static final YearMonth FIXING_MONTH = YearMonth.of(2016, 2);

  @Test
  public void test_of() {
    PriceIndexObservation test = PriceIndexObservation.of(GB_HICP, FIXING_MONTH);
    assertThat(test.getIndex()).isEqualTo(GB_HICP);
    assertThat(test.getFixingMonth()).isEqualTo(FIXING_MONTH);
    assertThat(test.getCurrency()).isEqualTo(GB_HICP.getCurrency());
    assertThat(test.toString()).isEqualTo("PriceIndexObservation[GB-HICP on 2016-02]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    PriceIndexObservation test = PriceIndexObservation.of(GB_HICP, FIXING_MONTH);
    coverImmutableBean(test);
    PriceIndexObservation test2 = PriceIndexObservation.of(CH_CPI, FIXING_MONTH.plusMonths(1));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    PriceIndexObservation test = PriceIndexObservation.of(GB_HICP, FIXING_MONTH);
    assertSerialization(test);
  }

}
