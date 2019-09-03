/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;

/**
 * Test.
 */
public class NotionalScheduleTest {

  private static final CurrencyAmount CA_GBP_1000 = CurrencyAmount.of(GBP, 1000d);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_CurrencyAmount() {
    NotionalSchedule test = NotionalSchedule.of(CA_GBP_1000);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(ValueSchedule.of(1000d));
    assertThat(test.getFxReset()).isEqualTo(Optional.empty());
    assertThat(test.isInitialExchange()).isFalse();
    assertThat(test.isIntermediateExchange()).isFalse();
    assertThat(test.isFinalExchange()).isFalse();
  }

  @Test
  public void test_of_CurrencyAndAmount() {
    NotionalSchedule test = NotionalSchedule.of(GBP, 1000d);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(ValueSchedule.of(1000d));
    assertThat(test.getFxReset()).isEqualTo(Optional.empty());
    assertThat(test.isInitialExchange()).isFalse();
    assertThat(test.isIntermediateExchange()).isFalse();
    assertThat(test.isFinalExchange()).isFalse();
  }

  @Test
  public void test_of_CurrencyAndValueSchedule() {
    ValueSchedule valueSchedule = ValueSchedule.of(1000d, ValueStep.of(1, ValueAdjustment.ofReplace(2000d)));
    NotionalSchedule test = NotionalSchedule.of(GBP, valueSchedule);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getAmount()).isEqualTo(valueSchedule);
    assertThat(test.getFxReset()).isEqualTo(Optional.empty());
    assertThat(test.isInitialExchange()).isFalse();
    assertThat(test.isIntermediateExchange()).isFalse();
    assertThat(test.isFinalExchange()).isFalse();
  }

  @Test
  public void test_builder_FxResetSetsFlags() {
    FxResetCalculation fxReset = FxResetCalculation.builder()
        .referenceCurrency(GBP)
        .index(GBP_USD_WM)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .build();
    NotionalSchedule test = NotionalSchedule.builder()
        .currency(USD)
        .amount(ValueSchedule.of(2000d))
        .intermediateExchange(true)
        .finalExchange(true)
        .fxReset(fxReset)
        .build();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getAmount()).isEqualTo(ValueSchedule.of(2000d));
    assertThat(test.getFxReset()).isEqualTo(Optional.of(fxReset));
    assertThat(test.isInitialExchange()).isFalse();
    assertThat(test.isIntermediateExchange()).isTrue();
    assertThat(test.isFinalExchange()).isTrue();
  }

  @Test
  public void test_builder_invalidCurrencyFxReset() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalSchedule.builder()
            .currency(USD)
            .amount(ValueSchedule.of(2000d))
            .fxReset(FxResetCalculation.builder()
                .referenceCurrency(USD)
                .index(GBP_USD_WM)
                .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
                .build())
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalSchedule.builder()
            .currency(EUR)
            .amount(ValueSchedule.of(2000d))
            .fxReset(FxResetCalculation.builder()
                .referenceCurrency(USD)
                .index(GBP_USD_WM)
                .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
                .build())
            .build());
  }

  @Test
  public void test_of_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalSchedule.of(null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalSchedule.of(null, 1000d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalSchedule.of(GBP, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalSchedule.of(null, ValueSchedule.of(1000d)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> NotionalSchedule.of(null, null));
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_serialization() {
    NotionalSchedule test = NotionalSchedule.of(GBP, 1000d);
    assertSerialization(test);
  }

}
