/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.fra;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.BuySell.SELL;
import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.NZD;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.SAT_SUN;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.rate.fra.FraDiscountingMethod.AFMA;
import static com.opengamma.strata.product.rate.fra.FraDiscountingMethod.ISDA;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.TenorAdjustment;
import com.opengamma.strata.basics.index.ImmutableIborIndex;
import com.opengamma.strata.product.rate.IborInterpolatedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test.
 */
@Test
public class FraTest {

  private static final double NOTIONAL_1M = 1_000_000d;
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);
  private static final DaysAdjustment MINUS_FIVE_DAYS = DaysAdjustment.ofBusinessDays(-5, GBLO);

  //-------------------------------------------------------------------------
  public void test_builder() {
    Fra test = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getBuySell(), BUY);
    assertEquals(test.getCurrency(), GBP);  // defaulted
    assertEquals(test.getNotional(), NOTIONAL_1M, 0d);
    assertEquals(test.getStartDate(), date(2015, 6, 15));
    assertEquals(test.getEndDate(), date(2015, 9, 15));
    assertEquals(test.getBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getPaymentDate(), AdjustableDate.of(date(2015, 6, 15)));
    assertEquals(test.getFixedRate(), 0.25d, 0d);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getIndexInterpolated(), Optional.empty());
    assertEquals(test.getFixingDateOffset(), GBP_LIBOR_3M.getFixingDateOffset());  // defaulted
    assertEquals(test.getDayCount(), ACT_365F);  // defaulted
    assertEquals(test.getDiscounting(), ISDA);  // defaulted
  }

  public void test_builder_AUD() {
    ImmutableIborIndex dummyIndex = ImmutableIborIndex.builder()
        .name("AUD_INDEX")
        .currency(AUD)
        .dayCount(ACT_360)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .effectiveDateOffset(PLUS_TWO_DAYS)
        .maturityDateOffset(TenorAdjustment.ofLastDay(TENOR_3M, BDA_MOD_FOLLOW))
        .fixingCalendar(SAT_SUN)
        .build();
    Fra test = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .paymentDate(AdjustableDate.of(date(2015, 6, 16)))
        .fixedRate(0.25d)
        .index(dummyIndex)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    assertEquals(test.getBuySell(), BUY);
    assertEquals(test.getCurrency(), AUD);  // defaulted
    assertEquals(test.getNotional(), NOTIONAL_1M, 0d);
    assertEquals(test.getStartDate(), date(2015, 6, 15));
    assertEquals(test.getEndDate(), date(2015, 9, 15));
    assertEquals(test.getBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getPaymentDate(), AdjustableDate.of(date(2015, 6, 16)));
    assertEquals(test.getFixedRate(), 0.25d, 0d);
    assertEquals(test.getIndex(), dummyIndex);
    assertEquals(test.getIndexInterpolated(), Optional.empty());
    assertEquals(test.getFixingDateOffset(), MINUS_TWO_DAYS);
    assertEquals(test.getDayCount(), ACT_360);  // defaulted
    assertEquals(test.getDiscounting(), AFMA);  // defaulted
  }

  public void test_builder_NZD() {
    ImmutableIborIndex dummyIndex = ImmutableIborIndex.builder()
        .name("NZD")
        .currency(NZD)
        .dayCount(ACT_360)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .effectiveDateOffset(PLUS_TWO_DAYS)
        .maturityDateOffset(TenorAdjustment.ofLastDay(TENOR_3M, BDA_MOD_FOLLOW))
        .fixingCalendar(SAT_SUN)
        .build();
    Fra test = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .paymentDate(AdjustableDate.of(date(2015, 6, 16)))
        .fixedRate(0.25d)
        .index(dummyIndex)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    assertEquals(test.getBuySell(), BUY);
    assertEquals(test.getCurrency(), NZD);  // defaulted
    assertEquals(test.getNotional(), NOTIONAL_1M, 0d);
    assertEquals(test.getStartDate(), date(2015, 6, 15));
    assertEquals(test.getEndDate(), date(2015, 9, 15));
    assertEquals(test.getBusinessDayAdjustment(), Optional.empty());
    assertEquals(test.getPaymentDate(), AdjustableDate.of(date(2015, 6, 16)));
    assertEquals(test.getFixedRate(), 0.25d, 0d);
    assertEquals(test.getIndex(), dummyIndex);
    assertEquals(test.getIndexInterpolated(), Optional.empty());
    assertEquals(test.getFixingDateOffset(), MINUS_TWO_DAYS);
    assertEquals(test.getDayCount(), ACT_360);  // defaulted
    assertEquals(test.getDiscounting(), AFMA);  // defaulted
  }

  public void test_builder_datesInOrder() {
    assertThrowsIllegalArg(() -> Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 6, 14))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build());
  }

  public void test_builder_noIndex() {
    assertThrowsIllegalArg(() -> Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .fixedRate(0.25d)
        .build());
  }

  public void test_builder_noDates() {
    assertThrowsIllegalArg(() -> Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .endDate(date(2015, 9, 15))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_expand_Ibor() {
    Fra fra = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .paymentDate(AdjustableDate.of(date(2015, 6, 20), BDA_MOD_FOLLOW))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    ExpandedFra test = fra.expand();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), NOTIONAL_1M, 0d);
    assertEquals(test.getStartDate(), date(2015, 6, 15));
    assertEquals(test.getEndDate(), date(2015, 9, 15));
    assertEquals(test.getPaymentDate(), date(2015, 6, 22));
    assertEquals(test.getFixedRate(), 0.25d, 0d);
    assertEquals(test.getFloatingRate(), IborRateObservation.of(GBP_LIBOR_3M, date(2015, 6, 11)));
    assertEquals(test.getYearFraction(), ACT_365F.yearFraction(date(2015, 6, 15), date(2015, 9, 15)), 0d);
    assertEquals(test.getDiscounting(), ISDA);
  }

  public void test_expand_IborInterpolated() {
    Fra fra = Fra.builder()
        .buySell(SELL)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 12))
        .endDate(date(2015, 9, 5))
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .indexInterpolated(GBP_LIBOR_2M)
        .fixingDateOffset(MINUS_TWO_DAYS)
        .build();
    ExpandedFra test = fra.expand();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), -NOTIONAL_1M, 0d); // sell
    assertEquals(test.getStartDate(), date(2015, 6, 12));
    assertEquals(test.getEndDate(), date(2015, 9, 7));
    assertEquals(test.getPaymentDate(), date(2015, 6, 12));
    assertEquals(test.getFixedRate(), 0.25d, 0d);
    assertEquals(test.getFloatingRate(),
        IborInterpolatedRateObservation.of(GBP_LIBOR_2M, GBP_LIBOR_3M, date(2015, 6, 10)));
    assertEquals(test.getYearFraction(), ACT_365F.yearFraction(date(2015, 6, 12), date(2015, 9, 7)), 0d);
    assertEquals(test.getDiscounting(), ISDA);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Fra test = Fra.builder()
        .buySell(BUY)
        .notional(NOTIONAL_1M)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .build();
    coverImmutableBean(test);
    Fra test2 = Fra.builder()
        .buySell(SELL)
        .currency(USD)
        .notional(NOTIONAL_2M)
        .startDate(date(2015, 6, 16))
        .endDate(date(2015, 8, 17))
        .businessDayAdjustment(BDA_MOD_FOLLOW)
        .paymentDate(AdjustableDate.of(date(2015, 6, 17)))
        .dayCount(ACT_360)
        .fixedRate(0.30d)
        .index(GBP_LIBOR_2M)
        .indexInterpolated(GBP_LIBOR_3M)
        .fixingDateOffset(MINUS_FIVE_DAYS)
        .discounting(FraDiscountingMethod.NONE)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    Fra test = Fra.builder()
        .buySell(BUY)
        .startDate(date(2015, 6, 15))
        .endDate(date(2015, 9, 15))
        .fixedRate(0.25d)
        .index(GBP_LIBOR_3M)
        .notional(NOTIONAL_1M)
        .build();
    assertSerialization(test);
  }

}
