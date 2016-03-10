/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ReferenceData;

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
        .deliveryBasket(base.getDeliveryBasket().subList(0, 1))
        .conversionFactors(base.getConversionFactors())
        .firstNoticeDate(base.getFirstNoticeDate())
        .lastNoticeDate(base.getLastNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
        .build());
    // first notice date missing
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .deliveryBasket(base.getDeliveryBasket())
        .conversionFactors(base.getConversionFactors())
        .lastNoticeDate(base.getLastNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
        .build());
    // last notice date missing
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .deliveryBasket(base.getDeliveryBasket())
        .conversionFactors(base.getConversionFactors())
        .firstNoticeDate(base.getFirstNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
        .build());
    // basket list empty
    assertThrowsIllegalArg(() -> ResolvedBondFuture.builder()
        .firstNoticeDate(base.getFirstNoticeDate())
        .lastNoticeDate(base.getLastNoticeDate())
        .lastTradeDate(base.getLastTradeDate())
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
