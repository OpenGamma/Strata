/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@code SwaptionTrade}.
 */
@Test
public class SwaptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Swaption SWAPTION = SwaptionTest.sut();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 3, 14));
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(Currency.USD, -3150000d), date(2014, 3, 17));

  //-------------------------------------------------------------------------
  public void test_of() {
    SwaptionTrade test = SwaptionTrade.of(TRADE_INFO, SWAPTION, PREMIUM);
    assertEquals(test.getPremium(), PREMIUM);
    assertEquals(test.getProduct(), SWAPTION);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    SwaptionTrade test = sut();
    assertEquals(test.getPremium(), PREMIUM);
    assertEquals(test.getProduct(), SWAPTION);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    SwaptionTrade test = SwaptionTrade.of(TRADE_INFO, SWAPTION, PREMIUM);
    assertEquals(test.resolve(REF_DATA).getPremium(), PREMIUM.resolve(REF_DATA));
    assertEquals(test.resolve(REF_DATA).getProduct(), SWAPTION.resolve(REF_DATA));
    assertEquals(test.resolve(REF_DATA).getInfo(), TRADE_INFO);
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
  static SwaptionTrade sut() {
    return SwaptionTrade.builder()
        .premium(PREMIUM)
        .product(SWAPTION)
        .info(TRADE_INFO)
        .build();
  }

  static SwaptionTrade sut2() {
    return SwaptionTrade.builder()
        .premium(AdjustablePayment.of(CurrencyAmount.of(Currency.USD, -3050000d), LocalDate.of(2014, 3, 17)))
        .product(SwaptionTest.sut2())
        .build();
  }

}
