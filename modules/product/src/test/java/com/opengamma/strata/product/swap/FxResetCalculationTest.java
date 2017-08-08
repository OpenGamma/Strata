/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.EUR_USD_ECB;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test.
 */
@Test
public class FxResetCalculationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, EUTA);
  private static final DaysAdjustment MINUS_THREE_DAYS = DaysAdjustment.ofBusinessDays(-3, EUTA);
  private static final LocalDate DATE_2014_03_31 = date(2014, 3, 31);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);

  public void test_builder() {
    FxResetCalculation test = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .fixingRelativeTo(FxResetFixingRelativeTo.PERIOD_START)
        .build();
    assertEquals(test.getIndex(), EUR_GBP_ECB);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getFixingDateOffset(), MINUS_TWO_DAYS);
    assertEquals(test.getFixingRelativeTo(), FxResetFixingRelativeTo.PERIOD_START);
  }

  public void test_builder_defaults() {
    FxResetCalculation test = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .build();
    assertEquals(test.getIndex(), EUR_GBP_ECB);
    assertEquals(test.getReferenceCurrency(), GBP);
    assertEquals(test.getFixingDateOffset(), EUR_GBP_ECB.getFixingDateOffset());
    assertEquals(test.getFixingRelativeTo(), FxResetFixingRelativeTo.PERIOD_START);
  }

  public void test_invalidCurrency() {
    assertThrowsIllegalArg(() -> FxResetCalculation.builder()
        .index(EUR_USD_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve_beforeStart_weekend() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    Optional<FxReset> test = base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertEquals(test, Optional.of(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, date(2014, 3, 27), REF_DATA), GBP)));
  }

  public void test_resolve_beforeEnd_weekend() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .fixingRelativeTo(FxResetFixingRelativeTo.PERIOD_END)
        .build();
    Optional<FxReset> test = base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertEquals(test, Optional.of(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, date(2014, 6, 26), REF_DATA), GBP)));
  }

  public void test_resolve_beforeStart_threeDays() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_THREE_DAYS)
        .build();
    Optional<FxReset> test = base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertEquals(test, Optional.of(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, date(2014, 3, 26), REF_DATA), GBP)));
  }

  public void test_resolve_initial_notional_override() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .initialNotionalValue(100000d)
        .build();
    Optional<FxReset> fxResetFirstPeriod =
        base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertFalse(fxResetFirstPeriod.isPresent());

    Optional<FxReset> fxResetSecondPeriod =
        base.resolve(REF_DATA).apply(1, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertTrue(fxResetSecondPeriod.isPresent());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxResetCalculation test = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    coverImmutableBean(test);
    FxResetCalculation test2 = FxResetCalculation.builder()
        .index(EUR_USD_ECB)
        .referenceCurrency(Currency.EUR)
        .fixingDateOffset(MINUS_THREE_DAYS)
        .fixingRelativeTo(FxResetFixingRelativeTo.PERIOD_END)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FxResetCalculation test = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    assertSerialization(test);
  }

}
