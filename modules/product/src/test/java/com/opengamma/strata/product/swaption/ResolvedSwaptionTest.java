/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.LongShort.LONG;
import static com.opengamma.strata.product.common.LongShort.SHORT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link ResolvedSwaption}.
 */
public class ResolvedSwaptionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 6, 12); // starts on 2014/6/19
  private static final double FIXED_RATE = 0.015;
  private static final double NOTIONAL = 100000000d;
  private static final ResolvedSwap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(TRADE_DATE, Tenor.TENOR_10Y, BuySell.BUY, NOTIONAL, FIXED_RATE, REF_DATA).getProduct().resolve(REF_DATA);
  private static final ZoneId EUROPE_LONDON = ZoneId.of("Europe/London");
  private static final ZonedDateTime EXPIRY = ZonedDateTime.of(2014, 6, 13, 11, 0, 0, 0, EUROPE_LONDON);
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2014, 6, 13);
  private static final LocalDate EXPIRY_DATE2 = LocalDate.of(2014, 6, 20);
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSwaptionSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE =
      CashSwaptionSettlement.of(SWAP.getLegs().get(0).getStartDate(), CashSwaptionSettlementMethod.PAR_YIELD);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedSwaption test = sut();
    assertThat(test.getExpiryDate()).isEqualTo(EXPIRY.toLocalDate());
    assertThat(test.getExpiry()).isEqualTo(EXPIRY);
    assertThat(test.getLongShort()).isEqualTo(LONG);
    assertThat(test.getSwaptionSettlement()).isEqualTo(PHYSICAL_SETTLE);
    assertThat(test.getExerciseInfo().getDates())
        .containsExactly(SwaptionExerciseDate.of(EXPIRY_DATE, EXPIRY_DATE, SWAP.getStartDate()));
    assertThat(test.getExerciseInfo().isAllDates()).isFalse();
    assertThat(test.getUnderlying()).isEqualTo(SWAP);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getIndex()).isEqualTo(USD_LIBOR_3M);
  }

  @Test
  public void test_bad() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedSwaption.builder()
            .longShort(LONG)
            .swaptionSettlement(PHYSICAL_SETTLE)
            .underlying(SWAP)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedSwaption.builder()
            .expiry(EXPIRY)
            .swaptionSettlement(PHYSICAL_SETTLE)
            .underlying(SWAP)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedSwaption.builder()
            .expiry(EXPIRY)
            .longShort(LONG)
            .underlying(SWAP)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedSwaption.builder()
            .expiry(EXPIRY)
            .longShort(LONG)
            .swaptionSettlement(PHYSICAL_SETTLE)
            .build());
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
  static ResolvedSwaption sut() {
    return ResolvedSwaption.builder()
        .expiry(EXPIRY)
        .longShort(LONG)
        .swaptionSettlement(PHYSICAL_SETTLE)
        .underlying(SWAP)
        .build();
  }

  static ResolvedSwaption sut2() {
    return ResolvedSwaption.builder()
        .expiry(EXPIRY.plusHours(1))
        .longShort(SHORT)
        .swaptionSettlement(CASH_SETTLE)
        .exerciseInfo(SwaptionExerciseDates.builder()
            .dates(
                SwaptionExerciseDate.of(EXPIRY_DATE, EXPIRY_DATE, EXPIRY_DATE),
                SwaptionExerciseDate.of(EXPIRY_DATE2, EXPIRY_DATE2, EXPIRY_DATE2))
            .allDates(true)
            .build())
        .underlying(FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
            .createTrade(LocalDate.of(2014, 6, 10), Tenor.TENOR_10Y, BuySell.BUY, 1d, FIXED_RATE, REF_DATA)
            .getProduct().resolve(REF_DATA))
        .build();
  }

}
