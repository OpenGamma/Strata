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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test.
 */
public class FxResetCalculationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, EUTA);
  private static final DaysAdjustment MINUS_THREE_DAYS = DaysAdjustment.ofBusinessDays(-3, EUTA);
  private static final LocalDate DATE_2014_03_31 = date(2014, 3, 31);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);

  @Test
  public void test_builder() {
    FxResetCalculation test = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .fixingRelativeTo(FxResetFixingRelativeTo.PERIOD_START)
        .build();
    assertThat(test.getIndex()).isEqualTo(EUR_GBP_ECB);
    assertThat(test.getReferenceCurrency()).isEqualTo(GBP);
    assertThat(test.getFixingDateOffset()).isEqualTo(MINUS_TWO_DAYS);
    assertThat(test.getFixingRelativeTo()).isEqualTo(FxResetFixingRelativeTo.PERIOD_START);
  }

  @Test
  public void test_builder_defaults() {
    FxResetCalculation test = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .build();
    assertThat(test.getIndex()).isEqualTo(EUR_GBP_ECB);
    assertThat(test.getReferenceCurrency()).isEqualTo(GBP);
    assertThat(test.getFixingDateOffset()).isEqualTo(EUR_GBP_ECB.getFixingDateOffset());
    assertThat(test.getFixingRelativeTo()).isEqualTo(FxResetFixingRelativeTo.PERIOD_START);
  }

  @Test
  public void test_invalidCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxResetCalculation.builder()
            .index(EUR_USD_ECB)
            .referenceCurrency(GBP)
            .fixingDateOffset(MINUS_TWO_DAYS)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve_beforeStart_weekend() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    Optional<FxReset> test = base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertThat(test).isEqualTo(Optional.of(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, date(2014, 3, 27), REF_DATA), GBP)));
  }

  @Test
  public void test_resolve_beforeEnd_weekend() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .fixingRelativeTo(FxResetFixingRelativeTo.PERIOD_END)
        .build();
    Optional<FxReset> test = base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertThat(test).isEqualTo(Optional.of(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, date(2014, 6, 26), REF_DATA), GBP)));
  }

  @Test
  public void test_resolve_beforeStart_threeDays() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_THREE_DAYS)
        .build();
    Optional<FxReset> test = base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertThat(test).isEqualTo(Optional.of(FxReset.of(FxIndexObservation.of(EUR_GBP_ECB, date(2014, 3, 26), REF_DATA), GBP)));
  }

  @Test
  public void test_resolve_initial_notional_override() {
    FxResetCalculation base = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .initialNotionalValue(100000d)
        .build();
    Optional<FxReset> fxResetFirstPeriod =
        base.resolve(REF_DATA).apply(0, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertThat(fxResetFirstPeriod.isPresent()).isFalse();

    Optional<FxReset> fxResetSecondPeriod =
        base.resolve(REF_DATA).apply(1, SchedulePeriod.of(DATE_2014_03_31, DATE_2014_06_30));
    assertThat(fxResetSecondPeriod.isPresent()).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_serialization() {
    FxResetCalculation test = FxResetCalculation.builder()
        .index(EUR_GBP_ECB)
        .referenceCurrency(GBP)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    assertSerialization(test);
  }

}
