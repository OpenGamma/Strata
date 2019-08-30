/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Test {@link FraTemplate}.
 */
public class FraTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FraConvention FRA_GBP_LIBOR_3M = FraConvention.of(GBP_LIBOR_3M);
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_PeriodIndex() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), GBP_LIBOR_3M);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ofMonths(2));
    assertThat(test.getPeriodToEnd()).isEqualTo(Period.ofMonths(5));  // defaulted
    assertThat(test.getConvention()).isEqualTo(FRA_GBP_LIBOR_3M);
  }

  @Test
  public void test_of_PeriodPeriodConvention() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), Period.ofMonths(4), FRA_GBP_LIBOR_3M);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ofMonths(2));
    assertThat(test.getPeriodToEnd()).isEqualTo(Period.ofMonths(4));
    assertThat(test.getConvention()).isEqualTo(FRA_GBP_LIBOR_3M);
  }

  @Test
  public void test_builder_defaults() {
    FraTemplate test = FraTemplate.builder()
        .periodToStart(Period.ofMonths(2))
        .convention(FRA_GBP_LIBOR_3M)
        .build();
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ofMonths(2));
    assertThat(test.getPeriodToEnd()).isEqualTo(Period.ofMonths(5));  // defaulted
    assertThat(test.getConvention()).isEqualTo(FRA_GBP_LIBOR_3M);
  }

  @Test
  public void test_builder_insufficientInfo() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FraTemplate.builder().convention(FRA_GBP_LIBOR_3M).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FraTemplate.builder().periodToStart(Period.ofMonths(2)).build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    FraTemplate base = FraTemplate.of(Period.ofMonths(3), Period.ofMonths(6), FRA_GBP_LIBOR_3M);
    LocalDate tradeDate = LocalDate.of(2015, 5, 4); // trade date is a holiday!
    FraTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  @Test
  public void test_createTrade_paymentOffset() {
    FraConvention convention = ((ImmutableFraConvention) FRA_GBP_LIBOR_3M).toBuilder()
        .paymentDateOffset(PLUS_TWO_DAYS)
        .build();
    FraTemplate base = FraTemplate.of(Period.ofMonths(3), Period.ofMonths(6), convention);
    LocalDate tradeDate = LocalDate.of(2015, 5, 4); // trade date is a holiday!
    FraTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Fra expected = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 8, 5))
        .endDate(date(2015, 11, 5))
        .paymentDate(AdjustableDate.of(date(2015, 8, 7), PLUS_TWO_DAYS.getAdjustment()))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), GBP_LIBOR_3M);
    coverImmutableBean(test);
    FraTemplate test2 = FraTemplate.of(Period.ofMonths(3), Period.ofMonths(6), FraConvention.of(USD_LIBOR_3M));
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
