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
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link FxIndexObservation}.
 */
@Test
public class FxIndexObservationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING_DATE = date(2016, 2, 22);
  private static final LocalDate MATURITY_DATE = GBP_USD_WM.calculateMaturityFromFixing(FIXING_DATE, REF_DATA);

  public void test_of() {
    FxIndexObservation test = FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getFixingDate(), FIXING_DATE);
    assertEquals(test.getMaturityDate(), MATURITY_DATE);
    assertEquals(test.getCurrencyPair(), GBP_USD_WM.getCurrencyPair());
    assertEquals(test.toString(), "FxIndexObservation[GBP/USD-WM on 2016-02-22]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxIndexObservation test = FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA);
    coverImmutableBean(test);
    FxIndexObservation test2 = FxIndexObservation.of(EUR_GBP_ECB, FIXING_DATE.plusDays(1), REF_DATA);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxIndexObservation test = FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA);
    assertSerialization(test);
  }

}
