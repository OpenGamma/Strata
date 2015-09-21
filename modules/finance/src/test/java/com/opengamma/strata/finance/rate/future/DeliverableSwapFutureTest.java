/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.future;

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
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.SecurityLink;
import com.opengamma.strata.finance.UnitSecurity;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;

/**
 * Test {@link DeliverableSwapFuture}.
 */
@Test
public class DeliverableSwapFutureTest {
  private static final IborIndex INDEX = IborIndices.USD_LIBOR_3M;
  private static final NotionalSchedule UNIT_NOTIONAL = NotionalSchedule.of(USD, 1d);
  private static final BusinessDayAdjustment BDA_MF = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HolidayCalendars.SAT_SUN);
  private static final BusinessDayAdjustment BDA_P = BusinessDayAdjustment.of(PRECEDING, HolidayCalendars.SAT_SUN);
  private static final SwapLeg FIXED_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
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
      .notionalSchedule(UNIT_NOTIONAL)
      .calculation(FixedRateCalculation.builder()
          .dayCount(THIRTY_U_360)
          .rate(ValueSchedule.of(0.015))
          .build())
      .build();
  private static final SwapLeg IBOR_LEG = RateCalculationSwapLeg.builder()
      .payReceive(RECEIVE)
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
      .notionalSchedule(UNIT_NOTIONAL)
      .calculation(IborRateCalculation.builder()
          .index(INDEX)
          .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, HolidayCalendars.SAT_SUN, BDA_P))
          .build())
      .build();
  private static final Swap SWAP = Swap.of(FIXED_LEG, IBOR_LEG);
  private static final LocalDate LAST_TRADE_DATE = LocalDate.of(2014, 9, 5);
  private static final LocalDate DELIVERY_DATE = LocalDate.of(2014, 9, 9);
  private static final double NOTIONAL = 100000;

  private static final StandardId SWAP_ID = StandardId.of("OG-Ticker", "Swap1");
  private static final Security<Swap> SECURITY = UnitSecurity.builder(SWAP).standardId(SWAP_ID).build();
  private static final SecurityLink<Swap> RESOLVED = SecurityLink.resolved(SECURITY);
  private static final SecurityLink<Swap> RESOLVABLE = SecurityLink.resolvable(SWAP_ID, Swap.class);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    DeliverableSwapFuture test = DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVABLE)
        .build();
    assertEquals(test.getDeliveryDate(), DELIVERY_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getUnderlyingLink(), RESOLVABLE);
    assertThrows(() -> test.getCurrency(), IllegalStateException.class);
    assertThrows(() -> test.getUnderlyingSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getUnderlying(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    DeliverableSwapFuture test = DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVED)
        .build();
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDeliveryDate(), DELIVERY_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getUnderlyingLink(), RESOLVED);
    assertEquals(test.getUnderlyingSecurity(), SECURITY);
    assertEquals(test.getUnderlying(), SWAP);
  }

  public void test_builder_deliveryAfterStart() {
    assertThrowsIllegalArg(() -> DeliverableSwapFuture.builder()
        .deliveryDate(LocalDate.of(2014, 9, 19))
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVED)
        .build());
  }

  public void test_builder_tradeAfterdelivery() {
    assertThrowsIllegalArg(() ->
    DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LocalDate.of(2014, 9, 11))
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVED)
        .build());
  }

  public void test_builder_notUnitNotional() {
    SwapLeg fixedLeg10 = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
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
        .payReceive(RECEIVE)
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
    Swap swap1 = Swap.of(fixedLeg10, IBOR_LEG);
    Security<Swap> security1 = UnitSecurity.builder(swap1).standardId(SWAP_ID).build();
    SecurityLink<Swap> resolved1 = SecurityLink.resolved(security1);
    Swap swap2 = Swap.of(FIXED_LEG, iborLeg500);
    Security<Swap> security2 = UnitSecurity.builder(swap2).standardId(SWAP_ID).build();
    SecurityLink<Swap> resolved2 = SecurityLink.resolved(security2);
    assertThrowsIllegalArg(() -> DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(resolved1)
        .build());
    assertThrowsIllegalArg(() -> DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(resolved2)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    DeliverableSwapFuture test = DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVABLE)
        .build();
    DeliverableSwapFuture expected = DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVED)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        assertEquals(identifier, SWAP_ID.getStandardId());
        return (T) SECURITY;
      }
    };
    assertEquals(test.resolveLinks(resolver), expected);
  }

  public void test_resolveLinks_resolved() {
    DeliverableSwapFuture test = DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVED)
        .build();
    LinkResolver resolver = new LinkResolver() {
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
        fail();  // not invoked because link is already resolved
        return null;
      }
    };
    assertSame(test.resolveLinks(resolver), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DeliverableSwapFuture test1 = DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVED)
        .build();
    coverImmutableBean(test1);
    SwapLeg iborLeg = RateCalculationSwapLeg.builder()
        .payReceive(RECEIVE)
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
    Swap swap1 = Swap.of(FIXED_LEG, iborLeg);
    Security<Swap> security1 = UnitSecurity.builder(swap1).standardId(SWAP_ID).build();
    SecurityLink<Swap> resolved1 = SecurityLink.resolved(security1);
    DeliverableSwapFuture test2 = DeliverableSwapFuture.builder()
        .deliveryDate(LocalDate.of(2014, 9, 5))
        .lastTradeDate(LocalDate.of(2014, 9, 2))
        .notional(20000L)
        .rounding(Rounding.none())
        .underlyingLink(resolved1)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    DeliverableSwapFuture test = DeliverableSwapFuture.builder()
        .deliveryDate(DELIVERY_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .underlyingLink(RESOLVED)
        .build();
    assertSerialization(test);
  }

}
