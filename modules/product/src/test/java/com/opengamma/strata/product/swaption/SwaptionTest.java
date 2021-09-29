/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedInflationSwapConventions;
import com.opengamma.strata.product.swap.type.IborIborSwapConventions;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConventions;

/**
 * Test {@link Swaption}.
 */
public class SwaptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate EXPIRY_06_10 = LocalDate.of(2014, 6, 10);  // Tue
  private static final LocalDate EXPIRY_06_11 = LocalDate.of(2014, 6, 11);  // Wed
  private static final LocalDate START_06_12 = LocalDate.of(2014, 6, 12);  // Thu
  private static final LocalDate START_06_16 = LocalDate.of(2014, 6, 16);  // Mon
  private static final LocalDate EXPIRY_07_10 = LocalDate.of(2014, 7, 10); // Thu
  private static final LocalDate START_07_14 = LocalDate.of(2014, 7, 14); // Mon
  private static final LocalDate EXPIRY_08_10 = LocalDate.of(2014, 8, 10); // Sun
  private static final LocalDate EXPIRY_08_11_ADJ = LocalDate.of(2014, 8, 11); // Mon
  private static final LocalDate START_08_13 = LocalDate.of(2014, 8, 13); // Wed
  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final Swap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(EXPIRY_06_10, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE, REF_DATA).getProduct();
  private static final Swap SWAP2 = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(START_06_12, Tenor.TENOR_10Y, BuySell.BUY, 1d, FIXED_RATE, REF_DATA).getProduct();
  private static final BusinessDayAdjustment ADJUSTMENT =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, GBLO.combinedWith(USNY));
  private static final DaysAdjustment OFFSET = DaysAdjustment.ofBusinessDays(2, GBLO);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(11, 0);
  private static final LocalTime EXPIRY_TIME2 = LocalTime.of(14, 0);
  private static final ZoneId ZONE = ZoneId.of("Z");
  private static final ZoneId ZONE2 = ZoneId.of("GMT");
  private static final AdjustableDate ADJUSTABLE_EXPIRY_DATE = AdjustableDate.of(EXPIRY_06_10, ADJUSTMENT);
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE =
      CashSwaptionSettlement.of(SWAP.getStartDate().getUnadjusted(), CashSwaptionSettlementMethod.PAR_YIELD);
  private static final Swap SWAP_INFL = FixedInflationSwapConventions.USD_FIXED_ZC_US_CPI
      .createTrade(START_06_12, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE, REF_DATA).getProduct();
  private static final Swap SWAP_BASIS = IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M
      .createTrade(START_06_12, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE, REF_DATA).getProduct();
  private static final Swap SWAP_XCCY = XCcyIborIborSwapConventions.EUR_EURIBOR_3M_USD_LIBOR_3M
      .createTrade(START_06_12, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, NOTIONAL * 1.1, FIXED_RATE, REF_DATA)
      .getProduct();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    Swaption test = sut();
    assertThat(test.getExpiryDate()).isEqualTo(ADJUSTABLE_EXPIRY_DATE);
    assertThat(test.getExpiryTime()).isEqualTo(EXPIRY_TIME);
    assertThat(test.getExpiryZone()).isEqualTo(ZONE);
    assertThat(test.getExpiry()).isEqualTo(EXPIRY_06_10.atTime(EXPIRY_TIME).atZone(ZONE));
    assertThat(test.getLongShort()).isEqualTo(LONG);
    assertThat(test.getSwaptionSettlement()).isEqualTo(PHYSICAL_SETTLE);
    assertThat(test.getUnderlying()).isEqualTo(SWAP);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getIndex()).isEqualTo(IborIndices.USD_LIBOR_3M);
    assertThat(test.isCrossCurrency()).isFalse();
    assertThat(test.allPaymentCurrencies()).containsOnly(USD);
    assertThat(test.allCurrencies()).containsOnly(USD);
  }

  @Test
  public void test_builder_invalidSwapInflation() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Swaption.builder()
            .expiryDate(ADJUSTABLE_EXPIRY_DATE)
            .expiryTime(EXPIRY_TIME)
            .expiryZone(ZONE)
            .longShort(LONG)
            .swaptionSettlement(PHYSICAL_SETTLE)
            .underlying(SWAP_INFL)
            .build());
  }

  @Test
  public void test_builder_invalidSwapBasis() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Swaption.builder()
            .expiryDate(ADJUSTABLE_EXPIRY_DATE)
            .expiryTime(EXPIRY_TIME)
            .expiryZone(ZONE)
            .longShort(LONG)
            .swaptionSettlement(PHYSICAL_SETTLE)
            .underlying(SWAP_BASIS)
            .build());
  }

  @Test
  public void test_builder_invalidSwapXCcy() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Swaption.builder()
            .expiryDate(ADJUSTABLE_EXPIRY_DATE)
            .expiryTime(EXPIRY_TIME)
            .expiryZone(ZONE)
            .longShort(LONG)
            .swaptionSettlement(PHYSICAL_SETTLE)
            .underlying(SWAP_XCCY)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_exercise_withInfo() {
    Swaption base = sut();
    Swaption test = base.selectExerciseDate(EXPIRY_08_10, REF_DATA);
    assertThat(test.getExpiryDate()).isEqualTo(AdjustableDate.of(EXPIRY_08_10, ADJUSTMENT));
    assertThat(test.getExerciseInfo()).isEmpty();
    assertThat(test.getUnderlying()).isEqualTo(SWAP.replaceStartDate(START_08_13));
    assertThat(base.exercise(EXPIRY_08_10, REF_DATA)).isEqualTo(test.getUnderlying());
  }

  @Test
  public void test_exercise_withoutInfo_matchUnadjusted() {
    Swaption base = sut2().toBuilder().expiryDate(AdjustableDate.of(date(2014, 6, 7), ADJUSTMENT)).build();
    Swaption test = base.selectExerciseDate(date(2014, 6, 7), REF_DATA);
    assertThat(test.getExpiryDate()).isEqualTo(AdjustableDate.of(date(2014, 6, 7), ADJUSTMENT));
    assertThat(test.getExerciseInfo()).isEmpty();
    assertThat(test.getUnderlying()).isEqualTo(base.getUnderlying());
    assertThat(base.exercise(date(2014, 6, 7), REF_DATA)).isEqualTo(test.getUnderlying());
  }

  @Test
  public void test_exercise_withoutInfo_matchAdjusted() {
    Swaption base = sut2().toBuilder().expiryDate(AdjustableDate.of(date(2014, 6, 7), ADJUSTMENT)).build();
    Swaption test = base.selectExerciseDate(date(2014, 6, 9), REF_DATA);
    assertThat(test.getExpiryDate()).isEqualTo(AdjustableDate.of(date(2014, 6, 9)));
    assertThat(test.getExerciseInfo()).isEmpty();
    assertThat(test.getUnderlying()).isEqualTo(base.getUnderlying());
    assertThat(base.exercise(date(2014, 6, 9), REF_DATA)).isEqualTo(test.getUnderlying());
  }

  @Test
  public void test_exercise_withoutInfo_matchAdjustedAndUnadjusted() {
    Swaption base = sut2();
    LocalDate baseExpiryDate = base.getExpiryDate().getUnadjusted();
    Swaption test = base.selectExerciseDate(baseExpiryDate, REF_DATA);
    assertThat(test.getExpiryDate()).isEqualTo(base.getExpiryDate());
    assertThat(test.getExerciseInfo()).isEmpty();
    assertThat(test.getUnderlying()).isEqualTo(base.getUnderlying());
    assertThat(base.exercise(baseExpiryDate, REF_DATA)).isEqualTo(test.getUnderlying());
  }

  @Test
  public void test_exercise_withoutInfo_noMatch() {
    Swaption base = sut2();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> base.exercise(base.getExpiryDate().getUnadjusted().minusYears(1), REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve_europeanNoInfo() {
    Swaption base = sut2();
    ResolvedSwaption test = base.resolve(REF_DATA);
    assertThat(test.getExpiry())
        .isEqualTo(ADJUSTMENT.adjust(EXPIRY_06_11, REF_DATA).atTime(EXPIRY_TIME2).atZone(ZONE2));
    assertThat(test.getLongShort()).isEqualTo(SHORT);
    assertThat(test.getSwaptionSettlement()).isEqualTo(CASH_SETTLE);
    assertThat(test.getUnderlying()).isEqualTo(SWAP2.resolve(REF_DATA));
    // check 3 days from expiry to swap start picked up correctly
    assertThat(test.getExerciseInfo().getDates())
        .containsExactly(
            SwaptionExerciseDate.of(EXPIRY_06_11, EXPIRY_06_11, START_06_16));

  }

  @Test
  public void test_resolve_bermudan() {
    Swaption base = sut();
    ResolvedSwaption test = base.resolve(REF_DATA);
    assertThat(test.getExpiry()).isEqualTo(ADJUSTMENT.adjust(EXPIRY_06_10, REF_DATA).atTime(EXPIRY_TIME).atZone(ZONE));
    assertThat(test.getLongShort()).isEqualTo(LONG);
    assertThat(test.getSwaptionSettlement()).isEqualTo(PHYSICAL_SETTLE);
    assertThat(test.getUnderlying()).isEqualTo(SWAP.resolve(REF_DATA));
    assertThat(test.getExerciseInfo().getDates())
        .containsExactly(
            SwaptionExerciseDate.of(EXPIRY_06_10, EXPIRY_06_10, START_06_12),
            SwaptionExerciseDate.of(EXPIRY_07_10, EXPIRY_07_10, START_07_14),
            SwaptionExerciseDate.of(EXPIRY_08_11_ADJ, EXPIRY_08_10, START_08_13));

  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static Swaption sut() {
    return Swaption.builder()
        .expiryDate(ADJUSTABLE_EXPIRY_DATE)
        .expiryTime(EXPIRY_TIME)
        .expiryZone(ZONE)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .exerciseInfo(SwaptionExercise.ofBermudan(EXPIRY_06_10, EXPIRY_08_10, ADJUSTMENT, P1M, OFFSET))
        .underlying(SWAP)
        .build();
  }

  static Swaption sut2() {
    return Swaption.builder()
        .expiryDate(AdjustableDate.of(EXPIRY_06_11, ADJUSTMENT))
        .expiryTime(EXPIRY_TIME2)
        .expiryZone(ZONE2)
        .longShort(SHORT)
        .swaptionSettlement(CASH_SETTLE)
        .underlying(SWAP2)
        .build();
  }

}
