/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Payment;

/**
 * Test {@link ResolvedBondFuture}.
 */
@Test
public class ResolvedBondFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_builder_noDeliveryDate() {
    ResolvedBondFuture base = sut();
    ResolvedBondFuture test = ResolvedBondFuture.builder()
        .securityId(base.getSecurityId())
        .deliveryBasket(base.getDeliveryBasket())
        .conversionFactors(base.getConversionFactors())
        .firstNoticeDate(base.getFirstNoticeDate())
        .lastNoticeDate(base.getLastNoticeDate())
        .firstDeliveryDate(base.getFirstDeliveryDate())
        .lastDeliveryDate(base.getLastDeliveryDate())
        .lastTradeDate(base.getLastTradeDate())
        .rounding(base.getRounding())
        .build();
    assertEquals(test, base);
  }

  public void test_builder_fail() {
    ResolvedBondFuture base = sut();
    // wrong size
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .securityId(base.getSecurityId())
        .deliveryBasket(base.getDeliveryBasket().subList(0, 1))
        .conversionFactors(base.getConversionFactors())
        .firstNoticeDate(base.getFirstNoticeDate())
        .lastNoticeDate(base.getLastNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
        .build());
    // first notice date missing
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .securityId(base.getSecurityId())
        .deliveryBasket(base.getDeliveryBasket())
        .conversionFactors(base.getConversionFactors())
        .lastNoticeDate(base.getLastNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
        .build());
    // last notice date missing
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .securityId(base.getSecurityId())
        .deliveryBasket(base.getDeliveryBasket())
        .conversionFactors(base.getConversionFactors())
        .firstNoticeDate(base.getFirstNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
        .build());
    // basket list empty
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .securityId(base.getSecurityId())
        .firstNoticeDate(base.getFirstNoticeDate())
        .lastNoticeDate(base.getLastNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
        .build());
    // notional mismatch
    ResolvedFixedCouponBond bond0 = base.getDeliveryBasket().get(0);
    ResolvedFixedCouponBond bond1 = bond0.toBuilder().nominalPayment(Payment.of(USD, 100, date(2016, 6, 30))).build();
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .securityId(base.getSecurityId())
        .deliveryBasket(bond0, bond1)
        .conversionFactors(1d, 2d)
        .firstNoticeDate(base.getFirstNoticeDate())
        .firstDeliveryDate(base.getFirstDeliveryDate())
        .lastNoticeDate(base.getLastNoticeDate())
        .lastDeliveryDate(base.getLastDeliveryDate())
        .lastTradeDate(base.getLastTradeDate())
        .rounding(base.getRounding())
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedBondFuture sut() {
    return BondFutureTest.sut().resolve(REF_DATA);
  }

  static ResolvedBondFuture sut2() {
    return BondFutureTest.sut2().resolve(REF_DATA);
  }

}
