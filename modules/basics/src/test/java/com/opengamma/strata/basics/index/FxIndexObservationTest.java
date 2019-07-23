/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link FxIndexObservation}.
 */
public class FxIndexObservationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = date(2016, 2, 22);
  private static final LocalDate MATURITY_DATE = GBP_USD_WM.calculateMaturityFromFixing(FIXING_DATE, REF_DATA);

  @Test
  public void test_of() {
    FxIndexObservation test = FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_DATE);
    assertThat(test.getMaturityDate()).isEqualTo(MATURITY_DATE);
    assertThat(test.getCurrencyPair()).isEqualTo(GBP_USD_WM.getCurrencyPair());
    assertThat(test.toString()).isEqualTo("FxIndexObservation[GBP/USD-WM on 2016-02-22]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxIndexObservation test = FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA);
    coverImmutableBean(test);
    FxIndexObservation test2 = FxIndexObservation.of(EUR_GBP_ECB, FIXING_DATE.plusDays(1), REF_DATA);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FxIndexObservation test = FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA);
    assertSerialization(test);
  }

}
