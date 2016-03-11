/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.swap.KnownAmountPaymentPeriod;

/**
 * Test {@link CapitalIndexedBondTrade}. 
 */
@Test
public class CapitalIndexedBondTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final long QUANTITY = 10;
  private static final double PRICE = 0.995;
  private static final BusinessDayAdjustment SCHEDULE_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final CapitalIndexedBond PRODUCT = CapitalIndexedBondTest.sut();
  private static final CapitalIndexedBond PRODUCT1 = CapitalIndexedBondTest.sut1();
  private static final LocalDate START = PRODUCT.getPeriodicSchedule().getStartDate();
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "BOND1");
  private static final Security<CapitalIndexedBond> SECURITY = UnitSecurity.builder(PRODUCT).standardId(SECURITY_ID).build();
  private static final Security<CapitalIndexedBond> SECURITY1 = UnitSecurity.builder(PRODUCT1).standardId(SECURITY_ID).build();
  private static final SecurityLink<CapitalIndexedBond> LINK_RESOLVABLE = 
      SecurityLink.resolvable(SECURITY_ID, CapitalIndexedBond.class);
  private static final SecurityLink<CapitalIndexedBond> LINK_RESOLVED = SecurityLink.resolved(SECURITY);
  private static final SecurityLink<CapitalIndexedBond> LINK_RESOLVED1 = SecurityLink.resolved(SECURITY1);
  private static final LocalDate TRADE = START.plusDays(7);
  private static final LocalDate SETTLEMENT_DATE = SCHEDULE_ADJ.adjust(TRADE, REF_DATA);
  private static final TradeInfo TRADE_INFO =
      TradeInfo.builder().tradeDate(TRADE).settlementDate(SETTLEMENT_DATE).build();
  private static final TradeInfo TRADE_INFO_EARLY =
      TradeInfo.builder().tradeDate(date(2008, 1, 1)).settlementDate(date(2008, 1, 1)).build();

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertEquals(identifier, SECURITY_ID);
      return (T) SECURITY;
    }
  };

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .cleanPrice(PRICE)
        .build();
    assertEquals(test.getCleanPrice(), PRICE);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getSecurityLink(), LINK_RESOLVED);
  }

  public void test_builder_resolvable() {
    CapitalIndexedBondTrade test = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .cleanPrice(PRICE)
        .build();
    assertEquals(test.getCleanPrice(), PRICE);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), LINK_RESOLVABLE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolved() {
    CapitalIndexedBondTrade base = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .cleanPrice(PRICE)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), base);
  }

  public void test_resolveLinks_resolvable() {
    CapitalIndexedBondTrade base = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .cleanPrice(PRICE)
        .build();
    CapitalIndexedBondTrade expected = CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .cleanPrice(PRICE)
        .build();
    assertEquals(base.resolveLinks(RESOLVER), expected);
  }

  private static final CapitalIndexedBondPaymentPeriod SETTLEMENT = CapitalIndexedBondPaymentPeriod.builder()
      .startDate(SCHEDULE_ADJ.adjust(START, REF_DATA))
      .unadjustedStartDate(START)
      .endDate(SETTLEMENT_DATE)
      .currency(USD)
      .rateObservation(PRODUCT.getRateCalculation().createRateObservation(SETTLEMENT_DATE, PRODUCT.getStartIndexValue()))
      .notional(
          -PRODUCT.getNotional() * QUANTITY *
              (PRICE + PRODUCT.resolve(REF_DATA).accruedInterest(SETTLEMENT_DATE) / PRODUCT.getNotional()))
      .realCoupon(1d)
      .build();

  private static final KnownAmountPaymentPeriod SETTLEMENT1 = KnownAmountPaymentPeriod
      .builder()
      .startDate(SCHEDULE_ADJ.adjust(START, REF_DATA))
      .unadjustedStartDate(START)
      .endDate(SETTLEMENT_DATE)
      .payment(
          Payment.of(USD, -PRODUCT1.getNotional() * QUANTITY *
              (PRICE + PRODUCT1.resolve(REF_DATA).accruedInterest(SETTLEMENT_DATE) / PRODUCT1.getNotional()),
              SETTLEMENT_DATE))
      .build();

  public void test_resolve() {
    ResolvedCapitalIndexedBondTrade test = sut().resolve(REF_DATA);
    ResolvedCapitalIndexedBondTrade expected = ResolvedCapitalIndexedBondTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .securityStandardId(SECURITY_ID)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT)
        .build();
    assertEquals(test, expected);
  }

  public void test_resolve1() {
    ResolvedCapitalIndexedBondTrade test = sut1().resolve(REF_DATA);
    ResolvedCapitalIndexedBondTrade expected = ResolvedCapitalIndexedBondTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT1.resolve(REF_DATA))
        .securityStandardId(SECURITY_ID)
        .quantity(QUANTITY)
        .settlement(SETTLEMENT1)
        .build();
    assertEquals(test, expected);
  }

  public void test_resolve_invalid() {
    CapitalIndexedBondTrade test = sut().toBuilder().tradeInfo(TRADE_INFO_EARLY).cleanPrice(PRICE).build();
    assertThrowsIllegalArg(() -> test.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    CapitalIndexedBondTrade test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  static CapitalIndexedBondTrade sut() {
    return CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .cleanPrice(PRICE)
        .build();
  }

  static CapitalIndexedBondTrade sut1() {
    return CapitalIndexedBondTrade.builder()
        .securityLink(LINK_RESOLVED1)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .cleanPrice(PRICE)
        .build();
  }

  static CapitalIndexedBondTrade sut2() {
    return CapitalIndexedBondTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(PRODUCT)
            .standardId(StandardId.of("Ticker", "GOV1-BND1"))
            .build()))
        .quantity(100L)
        .tradeInfo(TradeInfo.builder().settlementDate(START.plusDays(7)).build())
        .build();
  }

}
