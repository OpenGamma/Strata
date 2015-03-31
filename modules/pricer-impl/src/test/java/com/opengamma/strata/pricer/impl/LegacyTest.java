/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.ImmutableOvernightIndex;

/**
 * Test.
 */
@Test
public class LegacyTest {
  // NOTE: all imports are LEGACY ones, not new ones!

  private static final com.opengamma.strata.basics.index.IborIndex NEW_GBP_3M =
      com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
  private static final com.opengamma.strata.basics.index.IborIndex CHF_LIBOR_1W =
      com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_1W;
  private static final com.opengamma.strata.basics.index.OvernightIndex NEW_GBP_SONIA =
      com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
  private static final com.opengamma.strata.basics.index.OvernightIndex NEW_USD_NONE =
      ImmutableOvernightIndex.builder()
          .name("USD_NONE")
          .currency(com.opengamma.strata.basics.currency.Currency.USD)
          .fixingCalendar(HolidayCalendars.USNY)
          .dayCount(DayCounts.ACT_360)
          .effectiveDateOffset(2)
          .publicationDateOffset(0)
          .build();

  //-------------------------------------------------------------------------
  public void test_iborIndex() {
    IborIndex test = Legacy.iborIndex(NEW_GBP_3M);
    assertEquals(test.getCurrency(), Currency.GBP);
    assertEquals(test.getTenor(), Period.ofMonths(3));
  }

  public void test_iborIndex_bad() {
    assertThrowsIllegalArg(() -> Legacy.iborIndex(CHF_LIBOR_1W));
  }

  //-------------------------------------------------------------------------
  public void test_overnightIndex() {
    IndexON test = Legacy.overnightIndex(NEW_GBP_SONIA);
    assertEquals(test.getCurrency(), Currency.GBP);
    assertEquals(test.getName(), "SONIA");
  }

  public void test_overnightIndex_bad() {
    assertThrowsIllegalArg(() -> Legacy.overnightIndex(NEW_USD_NONE));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(Legacy.class);
    coverPrivateConstructor(LegacyIndices.class);
  }

}
