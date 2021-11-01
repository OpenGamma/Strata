/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static com.opengamma.strata.product.swap.SwapLegType.IBOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test.
 */
public class RatePeriodSwapLegTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_2014_06_28 = date(2014, 6, 28);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_28 = date(2014, 9, 28);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final LocalDate DATE_2014_12_30 = date(2014, 12, 30);
  private static final LocalDate DATE_2014_01_02 = date(2014, 1, 2);
  private static final IborRateComputation GBPLIBOR3M_2014_06_28 =
      IborRateComputation.of(GBP_LIBOR_3M, DATE_2014_06_28, REF_DATA);
  private static final IborRateComputation GBPLIBOR3M_2014_09_28 =
      IborRateComputation.of(GBP_LIBOR_3M, DATE_2014_09_28, REF_DATA);
  private static final NotionalExchange NOTIONAL_EXCHANGE =
      NotionalExchange.of(CurrencyAmount.of(GBP, 2000d), DATE_2014_10_01);
  private static final NotionalExchange FEE =
      NotionalExchange.of(CurrencyAmount.of(GBP, 1000d), DATE_2014_06_30);
  private static final RateAccrualPeriod RAP1 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_06_30)
      .endDate(DATE_2014_09_30)
      .yearFraction(0.25d)
      .rateComputation(GBPLIBOR3M_2014_06_28)
      .build();
  private static final RateAccrualPeriod RAP2 = RateAccrualPeriod.builder()
      .startDate(DATE_2014_09_30)
      .endDate(DATE_2014_12_30)
      .yearFraction(0.25d)
      .rateComputation(GBPLIBOR3M_2014_09_28)
      .build();
  private static final RatePaymentPeriod RPP1 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(5000d)
      .build();
  private static final RatePaymentPeriod RPP1_FXRESET = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP1)
      .dayCount(ACT_365F)
      .currency(GBP)
      .fxReset(FxReset.of(FxIndexObservation.of(GBP_USD_WM, DATE_2014_06_28, REF_DATA), USD))
      .notional(8000d)
      .build();
  private static final RatePaymentPeriod RPP2 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_01_02)
      .accrualPeriods(RAP2)
      .dayCount(ACT_365F)
      .currency(GBP)
      .notional(6000d)
      .build();
  private static final RatePaymentPeriod RPP3 = RatePaymentPeriod.builder()
      .paymentDate(DATE_2014_10_01)
      .accrualPeriods(RAP1)
      .dayCount(ACT_365F)
      .currency(USD)
      .notional(6000d)
      .build();
  private static final BusinessDayAdjustment FOLLOWING_GBLO = BusinessDayAdjustment.of(FOLLOWING, GBLO);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .paymentBusinessDayAdjustment(FOLLOWING_GBLO)
        .build();
    assertThat(test.getType()).isEqualTo(IBOR);
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(DATE_2014_06_30));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(DATE_2014_09_30));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getPaymentPeriods()).containsExactly(RPP1);
    assertThat(test.getPaymentEvents()).containsExactly(NOTIONAL_EXCHANGE);
    assertThat(test.isInitialExchange()).isTrue();
    assertThat(test.isIntermediateExchange()).isTrue();
    assertThat(test.isFinalExchange()).isTrue();
    assertThat(test.getPaymentBusinessDayAdjustment()).isEqualTo(FOLLOWING_GBLO);
  }

  @Test
  public void test_builder_defaults() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .build();
    assertThat(test.getPayReceive()).isEqualTo(RECEIVE);
    assertThat(test.getStartDate()).isEqualTo(AdjustableDate.of(DATE_2014_06_30));
    assertThat(test.getEndDate()).isEqualTo(AdjustableDate.of(DATE_2014_09_30));
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getPaymentPeriods()).containsExactly(RPP1);
    assertThat(test.getPaymentEvents()).isEmpty();
    assertThat(test.isInitialExchange()).isFalse();
    assertThat(test.isIntermediateExchange()).isFalse();
    assertThat(test.isFinalExchange()).isFalse();
    assertThat(test.getPaymentBusinessDayAdjustment()).isEqualTo(BusinessDayAdjustment.NONE);
  }

  @Test
  public void test_builder_invalidMixedCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RatePeriodSwapLeg.builder()
            .type(IBOR)
            .payReceive(RECEIVE)
            .paymentPeriods(RPP3)
            .paymentEvents(NOTIONAL_EXCHANGE)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(GBP_LIBOR_3M);
    assertThat(test.allCurrencies()).containsOnly(GBP);
  }

  @Test
  public void test_collectIndices_fxReset() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(GBP_LIBOR_3M, GBP_USD_WM);
    assertThat(test.allCurrencies()).containsOnly(GBP, USD);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_replaceStartDate() {
    // test case
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1, RPP2)
        .paymentEvents(FEE, NOTIONAL_EXCHANGE)
        .build();
    // expected
    RatePeriodSwapLeg expected = test.toBuilder()
        .paymentPeriods(RPP2)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    // assertion
    assertThatIllegalArgumentException().isThrownBy(() -> test.replaceStartDate(DATE_2014_01_02));
    assertThat(test.replaceStartDate(DATE_2014_06_30)).isEqualTo(test);
    assertThat(test.replaceStartDate(DATE_2014_09_30)).isEqualTo(expected);
    assertThatIllegalArgumentException().isThrownBy(() -> test.replaceStartDate(DATE_2014_12_30));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_createNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(
            NotionalExchange.of(CurrencyAmount.of(GBP, -5000d), DATE_2014_06_30),
            NotionalExchange.of(CurrencyAmount.of(GBP, 5000d), DATE_2014_10_01))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_fxResetNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET, RPP2)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    FxResetNotionalExchange ne1a = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, -8000d), DATE_2014_06_30, FxIndexObservation.of(GBP_USD_WM, DATE_2014_06_28, REF_DATA));
    FxResetNotionalExchange ne1b = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 8000d), DATE_2014_10_01, FxIndexObservation.of(GBP_USD_WM, DATE_2014_06_28, REF_DATA));
    NotionalExchange ne2a = NotionalExchange.of(CurrencyAmount.of(GBP, -6000d), DATE_2014_10_01);
    NotionalExchange ne2b = NotionalExchange.of(CurrencyAmount.of(GBP, 6000d), DATE_2014_01_02);
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET, RPP2)
        .paymentEvents(ne1a, ne1b, ne2a, ne2b)
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_FxResetOmitIntermediateNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET)
        .initialExchange(true)
        .intermediateExchange(false)
        .finalExchange(true)
        .build();

    FxResetNotionalExchange initialExchange = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, -8000d), DATE_2014_06_30, FxIndexObservation.of(GBP_USD_WM, DATE_2014_06_28, REF_DATA));
    FxResetNotionalExchange finalExchange = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 8000d), DATE_2014_10_01, FxIndexObservation.of(GBP_USD_WM, DATE_2014_06_28, REF_DATA));

    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1_FXRESET)
        .paymentEvents(initialExchange, finalExchange)
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_FxResetOmitInitialNotionalExchange() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(PAY)
        .paymentPeriods(RPP1_FXRESET)
        .initialExchange(false)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();

    FxResetNotionalExchange finalExchange = FxResetNotionalExchange.of(
        CurrencyAmount.of(USD, 8000d), DATE_2014_10_01, FxIndexObservation.of(GBP_USD_WM, DATE_2014_06_28, REF_DATA));

    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(PAY)
        .paymentPeriods(RPP1_FXRESET)
        .paymentEvents(finalExchange)
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_createNotionalExchange_noInitial() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(false)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(CurrencyAmount.of(GBP, 5000d), DATE_2014_10_01))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_createNotionalExchange_initialOnly() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(true)
        .intermediateExchange(false)
        .finalExchange(false)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(CurrencyAmount.of(GBP, -5000d), DATE_2014_06_30))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_resolve_createNotionalExchange_finalOnly() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .initialExchange(false)
        .intermediateExchange(false)
        .finalExchange(true)
        .build();
    ResolvedSwapLeg expected = ResolvedSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NotionalExchange.of(CurrencyAmount.of(GBP, 5000d), DATE_2014_10_01))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .paymentBusinessDayAdjustment(FOLLOWING_GBLO)
        .initialExchange(true)
        .intermediateExchange(true)
        .finalExchange(true)
        .build();
    coverImmutableBean(test);
    RatePeriodSwapLeg test2 = RatePeriodSwapLeg.builder()
        .type(FIXED)
        .payReceive(PAY)
        .paymentPeriods(RPP2)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    RatePeriodSwapLeg test = RatePeriodSwapLeg.builder()
        .type(IBOR)
        .payReceive(RECEIVE)
        .paymentPeriods(RPP1)
        .paymentEvents(NOTIONAL_EXCHANGE)
        .build();
    assertSerialization(test);
  }

}
