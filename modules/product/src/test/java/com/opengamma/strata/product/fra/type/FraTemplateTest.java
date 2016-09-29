/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra.type;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Test {@link FraTemplate}.
 */
@Test
public class FraTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FraConvention FRA_GBP_LIBOR_3M = FraConvention.of(GBP_LIBOR_3M);
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  //-------------------------------------------------------------------------
  public void test_of_PeriodIndex() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), GBP_LIBOR_3M);
    assertEquals(test.getPeriodToStart(), Period.ofMonths(2));
    assertEquals(test.getPeriodToEnd(), Period.ofMonths(5));  // defaulted
    assertEquals(test.getConvention(), FRA_GBP_LIBOR_3M);
  }

  public void test_of_PeriodPeriodConvention() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), Period.ofMonths(4), FRA_GBP_LIBOR_3M);
    assertEquals(test.getPeriodToStart(), Period.ofMonths(2));
    assertEquals(test.getPeriodToEnd(), Period.ofMonths(4));
    assertEquals(test.getConvention(), FRA_GBP_LIBOR_3M);
  }

  public void test_builder_defaults() {
    FraTemplate test = FraTemplate.builder()
        .periodToStart(Period.ofMonths(2))
        .convention(FRA_GBP_LIBOR_3M)
        .build();
    assertEquals(test.getPeriodToStart(), Period.ofMonths(2));
    assertEquals(test.getPeriodToEnd(), Period.ofMonths(5));  // defaulted
    assertEquals(test.getConvention(), FRA_GBP_LIBOR_3M);
  }

  public void test_builder_insufficientInfo() {
    assertThrowsIllegalArg(() -> FraTemplate.builder().convention(FRA_GBP_LIBOR_3M).build());
    assertThrowsIllegalArg(() -> FraTemplate.builder().periodToStart(Period.ofMonths(2)).build());
  }

  //-------------------------------------------------------------------------
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
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

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
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), GBP_LIBOR_3M);
    coverImmutableBean(test);
    FraTemplate test2 = FraTemplate.of(Period.ofMonths(3), Period.ofMonths(6), FraConvention.of(USD_LIBOR_3M));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FraTemplate test = FraTemplate.of(Period.ofMonths(2), GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
