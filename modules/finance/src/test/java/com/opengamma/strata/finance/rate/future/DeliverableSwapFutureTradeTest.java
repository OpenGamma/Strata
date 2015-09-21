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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

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
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.UnitSecurity;
import com.opengamma.strata.finance.rate.swap.FixedRateCalculation;
import com.opengamma.strata.finance.rate.swap.IborRateCalculation;
import com.opengamma.strata.finance.rate.swap.NotionalSchedule;
import com.opengamma.strata.finance.rate.swap.PaymentSchedule;
import com.opengamma.strata.finance.rate.swap.RateCalculationSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLeg;

/**
 * Test {@link DeliverableSwapFutureTrade}.
 */
@Test
public class DeliverableSwapFutureTradeTest {
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
  private static final Security<Swap> SWAP_SECURITY = UnitSecurity.builder(SWAP).standardId(SWAP_ID).build();
  private static final SecurityLink<Swap> SWAP_RESOLVED = SecurityLink.resolved(SWAP_SECURITY);
  private static final SecurityLink<Swap> SWAP_RESOLVABLE = SecurityLink.resolvable(SWAP_ID, Swap.class);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final DeliverableSwapFuture DSF_PRODUCT_RESOLVED = DeliverableSwapFuture.builder()
      .deliveryDate(DELIVERY_DATE)
      .lastTradeDate(LAST_TRADE_DATE)
      .notional(NOTIONAL)
      .rounding(ROUNDING)
      .underlyingLink(SWAP_RESOLVED)
      .build();
  private static final DeliverableSwapFuture DSF_PRODUCT_RESOLVABLE = DeliverableSwapFuture.builder()
      .deliveryDate(DELIVERY_DATE)
      .lastTradeDate(LAST_TRADE_DATE)
      .notional(NOTIONAL)
      .rounding(ROUNDING)
      .underlyingLink(SWAP_RESOLVABLE)
      .build();
  private static final StandardId DSF_ID = StandardId.of("OG-Ticker", "DSF1");
  private static final Security<DeliverableSwapFuture> DSF_SECURITY_RESOLVABLE = UnitSecurity
      .builder(DSF_PRODUCT_RESOLVABLE)
      .standardId(DSF_ID)
      .build();
  private static final Security<DeliverableSwapFuture> DSF_SECURITY_RESOLVED = UnitSecurity
      .builder(DSF_PRODUCT_RESOLVED)
      .standardId(DSF_ID)
      .build();
  private static final SecurityLink<DeliverableSwapFuture> DSF_RESOLVABLE =
      SecurityLink.resolvable(DSF_ID, DeliverableSwapFuture.class);
  private static final SecurityLink<DeliverableSwapFuture> DSF_RESOLVED_PART = SecurityLink.resolved(DSF_SECURITY_RESOLVABLE);
  private static final SecurityLink<DeliverableSwapFuture> DSF_RESOLVED = SecurityLink.resolved(DSF_SECURITY_RESOLVED);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(LocalDate.of(2014,6, 12))
      .settlementDate(LocalDate.of(2014, 6, 14))
      .build();
  private static final long QUANTITY = 100L;
  private static final double TRADE_PRICE = 0.99;

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      if (identifier.equals(DSF_ID)) {
        return (T) DSF_SECURITY_RESOLVED;
      }
      assertEquals(identifier, SWAP_ID);
      return (T) SWAP_SECURITY;
    }
  };

  //-------------------------------------------------------------------------
  public void test_builder_resolvable() {
    DeliverableSwapFutureTrade test = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), DSF_RESOLVABLE);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getTradePrice(), TRADE_PRICE);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
  }

  public void test_builder_resolved() {
    DeliverableSwapFutureTrade test = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), DSF_RESOLVED);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getTradePrice(), TRADE_PRICE);
    assertEquals(test.getProduct(), DSF_PRODUCT_RESOLVED);
    assertEquals(test.getSecurity(), DSF_SECURITY_RESOLVED);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolvable() {
    DeliverableSwapFutureTrade test = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    DeliverableSwapFutureTrade expected = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), expected);
  }

  public void test_resolveLinks_resolved_part() {
    DeliverableSwapFutureTrade test = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVED_PART)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    DeliverableSwapFutureTrade expected = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    assertEquals(test.resolveLinks(RESOLVER), expected);
  }

  public void test_resolveLinks_resolved() {
    DeliverableSwapFutureTrade test = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    assertSame(test.resolveLinks(RESOLVER), test);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DeliverableSwapFutureTrade test1 = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    coverImmutableBean(test1);
    DeliverableSwapFutureTrade test2 = DeliverableSwapFutureTrade.builder()
        .quantity(10L)
        .securityLink(DSF_RESOLVABLE)
        .tradePrice(1.01)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    DeliverableSwapFutureTrade test = DeliverableSwapFutureTrade.builder()
        .quantity(QUANTITY)
        .securityLink(DSF_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .tradePrice(TRADE_PRICE)
        .build();
    assertSerialization(test);
  }
}
