/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
@Test
public class RatePaymentPeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_2014_03_30 = date(2014, 3, 30);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final IborRateComputation GBP_LIBOR_3M_2014_03_28 =
      IborRateComputation.of(GBP_LIBOR_3M, date(2014, 3, 28), REF_DATA);
  private static final IborRateComputation GBP_LIBOR_3M_2014_06_28 =
      IborRateComputation.of(GBP_LIBOR_3M, date(2014, 6, 28), REF_DATA);
  private static final FxReset FX_RESET_USD =
      FxReset.of(FxIndexObservation.of(GBP_USD_WM, date(2014, 3, 28), REF_DATA), USD);
  private static final RateAccrualPeriod RAP1 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_03_30)
      .endDate(DATE_2014_06_30)
      .yearFraction(0.25d)
      .rateComputation(GBP_LIBOR_3M_2014_03_28)
      .build();
  private static final RateAccrualPeriod RAP2 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_06_30)
      .endDate(DATE_2014_09_30)
      .yearFraction(0.25d)
      .rateComputation(GBP_LIBOR_3M_2014_06_28)
      .build();

  public void test_builder_oneAccrualPeriod() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_06_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getPaymentDate(), DATE_2014_10_01);
    assertEquals(test.getAccrualPeriods(), ImmutableList.of(RAP2));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getFxReset(), Optional.empty());
    assertEquals(test.getNotional(), 1000d, 0d);
    assertEquals(test.getNotionalAmount(), CurrencyAmount.of(GBP, 1000d));
    assertEquals(test.getCompoundingMethod(), CompoundingMethod.STRAIGHT);
    assertEquals(test.isCompoundingApplicable(), false);
  }

  public void test_builder_twoAccrualPeriods() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP1, RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_03_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getPaymentDate(), DATE_2014_10_01);
    assertEquals(test.getAccrualPeriods(), ImmutableList.of(RAP1, RAP2));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getFxReset(), Optional.empty());
    assertEquals(test.getNotional(), 1000d, 0d);
    assertEquals(test.getCompoundingMethod(), CompoundingMethod.STRAIGHT);
    assertEquals(test.isCompoundingApplicable(), true);
  }

  public void test_builder_twoAccrualPeriods_compoundingDefaultedToNone_fxReset() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP1, RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .fxReset(FX_RESET_USD)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.NONE)
        .build();
    assertEquals(test.getStartDate(), DATE_2014_03_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getPaymentDate(), DATE_2014_10_01);
    assertEquals(test.getAccrualPeriods(), ImmutableList.of(RAP1, RAP2));
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getFxReset(), Optional.of(FX_RESET_USD));
    assertEquals(test.getNotional(), 1000d, 0d);
    assertEquals(test.getNotionalAmount(), CurrencyAmount.of(USD, 1000d));
    assertEquals(test.isCompoundingApplicable(), false);
  }

  public void test_builder_badFxReset() {
    assertThrowsIllegalArg(() -> RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP1, RAP2)
        .dayCount(ACT_365F)
        .currency(USD)
        .fxReset(FX_RESET_USD)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.NONE)
        .build());
    assertThrowsIllegalArg(() -> RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP1, RAP2)
        .dayCount(ACT_365F)
        .currency(EUR)
        .fxReset(FX_RESET_USD)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.NONE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_adjustPaymentDate() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    RatePaymentPeriod expected = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01.plusDays(2))
        .accrualPeriods(RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0))), test);
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2))), expected);
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices_simple() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M));
  }

  public void test_collectIndices_fxReset() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(1000d)
        .fxReset(FX_RESET_USD)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_3M, GBP_USD_WM));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP1, RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .fxReset(FX_RESET_USD)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    coverImmutableBean(test);
    RatePaymentPeriod test2 = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_09_30)
        .accrualPeriods(RAP1)
        .dayCount(ACT_360)
        .currency(USD)
        .notional(2000d)
        .compoundingMethod(CompoundingMethod.NONE)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RatePaymentPeriod test = RatePaymentPeriod.builder()
        .paymentDate(DATE_2014_10_01)
        .accrualPeriods(RAP1, RAP2)
        .dayCount(ACT_365F)
        .currency(GBP)
        .notional(1000d)
        .compoundingMethod(CompoundingMethod.STRAIGHT)
        .build();
    assertSerialization(test);
  }

}
