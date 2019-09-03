/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link IborFixingDeposit}.
 */
public class IborFixingDepositTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BuySell SELL = BuySell.SELL;
  private static final LocalDate START_DATE = LocalDate.of(2015, 1, 19);
  private static final LocalDate END_DATE = LocalDate.of(2015, 7, 19);
  private static final double NOTIONAL = 100000000d;
  private static final double RATE = 0.0250;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment DAY_ADJ = DaysAdjustment.ofBusinessDays(1, GBLO);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_full() {
    IborFixingDeposit test = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .currency(GBP)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .fixingDateOffset(DAY_ADJ)
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_6M)
        .fixedRate(RATE)
        .build();
    assertThat(test.getBusinessDayAdjustment().get()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getBuySell()).isEqualTo(SELL);
    assertThat(test.getFixingDateOffset()).isEqualTo(DAY_ADJ);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getStartDate()).isEqualTo(START_DATE);
    assertThat(test.getEndDate()).isEqualTo(END_DATE);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_6M);
    assertThat(test.getFixedRate()).isEqualTo(RATE);
    assertThat(test.isCrossCurrency()).isFalse();
    assertThat(test.allPaymentCurrencies()).containsOnly(GBP);
    assertThat(test.allCurrencies()).containsOnly(GBP);
  }

  @Test
  public void test_builder_minimum() {
    IborFixingDeposit test = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .fixedRate(RATE)
        .build();
    assertThat(test.getBusinessDayAdjustment().get()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getBuySell()).isEqualTo(SELL);
    assertThat(test.getFixingDateOffset()).isEqualTo(GBP_LIBOR_6M.getFixingDateOffset());
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getStartDate()).isEqualTo(START_DATE);
    assertThat(test.getEndDate()).isEqualTo(END_DATE);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_6M);
    assertThat(test.getFixedRate()).isEqualTo(RATE);
  }

  @Test
  public void test_builder_wrongDates() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborFixingDeposit.builder()
            .buySell(SELL)
            .notional(NOTIONAL)
            .startDate(LocalDate.of(2015, 9, 19))
            .endDate(END_DATE)
            .businessDayAdjustment(BDA_MOD_FOLLOW)
            .index(GBP_LIBOR_6M)
            .fixedRate(RATE)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    IborFixingDeposit base = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .fixedRate(RATE)
        .build();
    ResolvedIborFixingDeposit test = base.resolve(REF_DATA);
    LocalDate expectedEndDate = BDA_MOD_FOLLOW.adjust(END_DATE, REF_DATA);
    double expectedYearFraction = ACT_365F.yearFraction(START_DATE, expectedEndDate);
    IborRateComputation expectedObservation = IborRateComputation.of(
        GBP_LIBOR_6M, GBP_LIBOR_6M.getFixingDateOffset().adjust(START_DATE, REF_DATA), REF_DATA);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getStartDate()).isEqualTo(START_DATE);
    assertThat(test.getEndDate()).isEqualTo(expectedEndDate);
    assertThat(test.getFloatingRate()).isEqualTo(expectedObservation);
    assertThat(test.getNotional()).isEqualTo(-NOTIONAL);
    assertThat(test.getFixedRate()).isEqualTo(RATE);
    assertThat(test.getYearFraction()).isEqualTo(expectedYearFraction);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborFixingDeposit test1 = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .fixedRate(RATE)
        .build();
    coverImmutableBean(test1);
    IborFixingDeposit test2 = IborFixingDeposit.builder()
        .buySell(BuySell.BUY)
        .notional(NOTIONAL)
        .startDate(LocalDate.of(2015, 1, 19))
        .endDate(LocalDate.of(2015, 4, 19))
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_3M)
        .fixedRate(0.015)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    IborFixingDeposit test = IborFixingDeposit.builder()
        .buySell(SELL)
        .notional(NOTIONAL)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .index(GBP_LIBOR_6M)
        .fixedRate(RATE)
        .build();
    assertSerialization(test);
  }

}
