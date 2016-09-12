/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;

/**
 * Test.
 */
@Test
public class NotionalScheduleTest {

  private static final CurrencyAmount CA_GBP_1000 = CurrencyAmount.of(GBP, 1000d);

  //-------------------------------------------------------------------------
  public void test_of_CurrencyAmount() {
    NotionalSchedule test = NotionalSchedule.of(CA_GBP_1000);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmount(), ValueSchedule.of(1000d));
    assertEquals(test.getFxReset(), Optional.empty());
    assertEquals(test.isInitialExchange(), false);
    assertEquals(test.isIntermediateExchange(), false);
    assertEquals(test.isFinalExchange(), false);
  }

  public void test_of_CurrencyAndAmount() {
    NotionalSchedule test = NotionalSchedule.of(GBP, 1000d);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmount(), ValueSchedule.of(1000d));
    assertEquals(test.getFxReset(), Optional.empty());
    assertEquals(test.isInitialExchange(), false);
    assertEquals(test.isIntermediateExchange(), false);
    assertEquals(test.isFinalExchange(), false);
  }

  public void test_of_CurrencyAndValueSchedule() {
    ValueSchedule valueSchedule = ValueSchedule.of(1000d, ValueStep.of(1, ValueAdjustment.ofReplace(2000d)));
    NotionalSchedule test = NotionalSchedule.of(GBP, valueSchedule);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getAmount(), valueSchedule);
    assertEquals(test.getFxReset(), Optional.empty());
    assertEquals(test.isInitialExchange(), false);
    assertEquals(test.isIntermediateExchange(), false);
    assertEquals(test.isFinalExchange(), false);
  }

  public void test_builder_FxResetSetsFlags() {
    FxResetCalculation fxReset = FxResetCalculation.builder()
        .referenceCurrency(GBP)
        .index(GBP_USD_WM)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .build();
    NotionalSchedule test = NotionalSchedule.builder()
        .currency(USD)
        .amount(ValueSchedule.of(2000d))
        .fxReset(fxReset)
        .build();
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getAmount(), ValueSchedule.of(2000d));
    assertEquals(test.getFxReset(), Optional.of(fxReset));
    assertEquals(test.isInitialExchange(), true);
    assertEquals(test.isIntermediateExchange(), true);
    assertEquals(test.isFinalExchange(), true);
  }

  public void test_builder_invalidCurrencyFxReset() {
    assertThrowsIllegalArg(() -> NotionalSchedule.builder()
        .currency(USD)
        .amount(ValueSchedule.of(2000d))
        .fxReset(FxResetCalculation.builder()
            .referenceCurrency(USD)
            .index(GBP_USD_WM)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build());
    assertThrowsIllegalArg(() -> NotionalSchedule.builder()
        .currency(EUR)
        .amount(ValueSchedule.of(2000d))
        .fxReset(FxResetCalculation.builder()
            .referenceCurrency(USD)
            .index(GBP_USD_WM)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .build());
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> NotionalSchedule.of(null));
    assertThrowsIllegalArg(() -> NotionalSchedule.of(null, 1000d));
    assertThrowsIllegalArg(() -> NotionalSchedule.of(GBP, null));
    assertThrowsIllegalArg(() -> NotionalSchedule.of(null, ValueSchedule.of(1000d)));
    assertThrowsIllegalArg(() -> NotionalSchedule.of(null, null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    NotionalSchedule test = NotionalSchedule.of(GBP, 1000d);
    coverImmutableBean(test);
    NotionalSchedule test2 = NotionalSchedule.builder()
        .currency(USD)
        .amount(ValueSchedule.of(2000d))
        .fxReset(FxResetCalculation.builder()
            .referenceCurrency(GBP)
            .index(GBP_USD_WM)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
            .build())
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    NotionalSchedule test = NotionalSchedule.of(GBP, 1000d);
    assertSerialization(test);
  }

}
