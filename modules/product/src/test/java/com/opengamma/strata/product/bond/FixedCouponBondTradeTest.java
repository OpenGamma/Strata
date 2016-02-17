/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;

/**
 * Test {@link FixedCouponBondTrade}.
 */
@Test
public class FixedCouponBondTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final StandardId SECURITY_ID = StandardId.of("OG-Ticker", "GOVT1-BOND1");
  private static final LocalDate TRADE_DATE = LocalDate.of(2015, 3, 25);
  private static final LocalDate SETTLEMENT_DATE = LocalDate.of(2015, 3, 30);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(SETTLEMENT_DATE)
      .build();
  private static final long QUANTITY = 10;
  private static final double NOTIONAL = 1.0e7;
  private static final FixedCouponBond PRODUCT = FixedCouponBondTest.sut();
  private static final Security<FixedCouponBond> BOND_SECURITY =
      UnitSecurity.builder(PRODUCT).standardId(SECURITY_ID).build();
  private static final Payment UPFRONT_PAYMENT = Payment.of(
      CurrencyAmount.of(EUR, -NOTIONAL * QUANTITY * 0.99), SETTLEMENT_DATE);

  private static final SecurityLink<FixedCouponBond> SECURITY_LINK_RESOLVED = SecurityLink.resolved(BOND_SECURITY);
  private static final SecurityLink<FixedCouponBond> SECURITY_LINK_RESOLVABLE =
      SecurityLink.resolvable(SECURITY_ID, FixedCouponBond.class);

  private static final LinkResolver RESOLVER = new LinkResolver() {
    @SuppressWarnings("unchecked")
    @Override
    public <T extends IdentifiableBean> T resolve(StandardId identifier, TypeToken<T> targetType) {
      assertEquals(identifier, SECURITY_ID);
      return (T) BOND_SECURITY;
    }
  };

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    FixedCouponBondTrade test = sut();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurity(), BOND_SECURITY);
    assertEquals(test.getSecurityLink(), SECURITY_LINK_RESOLVED);
    assertEquals(test.getPayment(), UPFRONT_PAYMENT);
  }

  public void test_builder_resolvable() {
    FixedCouponBondTrade test = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getSecurityLink(), SECURITY_LINK_RESOLVABLE);
    assertThrows(() -> test.getSecurity(), IllegalStateException.class);
    assertThrows(() -> test.getProduct(), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolved() {
    FixedCouponBondTrade base = sut();
    assertEquals(base.resolveLinks(RESOLVER), base);
  }

  public void test_resolveLinks_resolvable() {
    FixedCouponBondTrade base = FixedCouponBondTrade.builder()
        .securityLink(SECURITY_LINK_RESOLVABLE)
        .tradeInfo(TRADE_INFO)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    FixedCouponBondTrade expected = sut();
    assertEquals(base.resolveLinks(RESOLVER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedFixedCouponBondTrade expected = ResolvedFixedCouponBondTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .securityStandardId(SECURITY_ID)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedCouponBondTrade test = sut();
    coverImmutableBean(test);
    coverBeanEquals(test, sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static FixedCouponBondTrade sut() {
    return FixedCouponBondTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityLink(SECURITY_LINK_RESOLVED)
        .quantity(QUANTITY)
        .payment(UPFRONT_PAYMENT)
        .build();
  }

  static FixedCouponBondTrade sut2() {
    return FixedCouponBondTrade.builder()
        .securityLink(SecurityLink.resolved(UnitSecurity.builder(PRODUCT)
            .standardId(StandardId.of("Ticker", "GOV1-BND1"))
            .build()))
        .quantity(100L)
        .payment(Payment.of(CurrencyAmount.of(EUR, -NOTIONAL * QUANTITY * 0.99), SETTLEMENT_DATE))
        .build();
  }

}
