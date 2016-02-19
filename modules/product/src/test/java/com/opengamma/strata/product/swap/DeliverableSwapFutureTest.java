/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link DeliverableSwapFuture}.
 */
@Test
public class DeliverableSwapFutureTest {

  private static final IborIndex INDEX = IborIndices.USD_LIBOR_3M;
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.SAT_SUN);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, HolidayCalendars.SAT_SUN);
  private static final LocalDate START_DATE = LocalDate.of(2014, 9, 12);
  private static final Swap SWAP = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
      .createTrade(START_DATE, Tenor.TENOR_10Y, BuySell.SELL, 1d, 0.015).getProduct();
  private static final LocalDate LAST_TRADE_DATE = LocalDate.of(2014, 9, 5);
  private static final LocalDate DELIVERY_DATE = LocalDate.of(2014, 9, 9);
  private static final double NOTIONAL = 100000;

  //-------------------------------------------------------------------------
  public void test_builder() {
    DeliverableSwapFuture test = DeliverableSwapFuture.builder()
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(SWAP)
        .build();
    assertEquals(test.getDeliveryDate(), DELIVERY_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getUnderlyingSwap(), SWAP);
  }

  public void test_builder_deliveryAfterStart() {
    assertThrowsIllegalArg(() -> DeliverableSwapFuture.builder()
        .notional(NOTIONAL)
        .deliveryDate(LocalDate.of(2014, 9, 19))
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(SWAP)
        .build());
  }

  public void test_builder_tradeAfterdelivery() {
    assertThrowsIllegalArg(() -> DeliverableSwapFuture.builder()
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LocalDate.of(2014, 9, 11))
        .underlyingSwap(SWAP)
        .build());
  }

  public void test_builder_notUnitNotional() {
    SwapLeg fixedLeg10 = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(P6M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P6M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(USD, 10d))
        .calculation(FixedRateCalculation.builder()
            .dayCount(THIRTY_U_360)
            .rate(ValueSchedule.of(0.015))
            .build())
        .build();
    SwapLeg iborLeg500 = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(P1M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(USD)
            .amount(ValueSchedule.of(500d))
            .finalExchange(true)
            .initialExchange(true)
            .build())
        .calculation(IborRateCalculation.builder()
            .index(INDEX)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.SAT_SUN, BDA_P))
            .build())
        .build();
    Swap swap1 = Swap.of(fixedLeg10, SWAP.getLeg(PAY).get());
    Swap swap2 = Swap.of(SWAP.getLeg(RECEIVE).get(), iborLeg500);
    assertThrowsIllegalArg(() -> DeliverableSwapFuture.builder()
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(swap1)
        .build());
    assertThrowsIllegalArg(() -> DeliverableSwapFuture.builder()
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(swap2)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DeliverableSwapFuture test1 = DeliverableSwapFuture.builder()
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(SWAP)
        .build();
    coverImmutableBean(test1);
    SwapLeg iborLeg = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .startDate(LocalDate.of(2014, 9, 12))
            .endDate(LocalDate.of(2016, 9, 12))
            .frequency(P1M)
            .businessDayAdjustment(BDA_MF)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.builder()
            .currency(USD)
            .amount(ValueSchedule.of(1d))
            .finalExchange(true)
            .initialExchange(true)
            .build())
        .calculation(IborRateCalculation.builder()
            .index(INDEX)
            .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.SAT_SUN, BDA_P))
            .build())
        .build();
    Swap swap1 = Swap.of(SWAP.getLeg(RECEIVE).get(), iborLeg);
    DeliverableSwapFuture test2 = DeliverableSwapFuture.builder()
        .notional(20000L)
        .deliveryDate(LocalDate.of(2014, 9, 5))
        .lastTradeDate(LocalDate.of(2014, 9, 2))
        .underlyingSwap(swap1)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    DeliverableSwapFuture test = DeliverableSwapFuture.builder()
        .notional(NOTIONAL)
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .underlyingSwap(SWAP)
        .build();
    assertSerialization(test);
  }

}
