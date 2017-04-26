/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FxBinaryOptionTrade}.
 */
@Test
public class FxBinaryOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL = 1.0e6;
  private static final FxBinaryOption PRODUCT = FxBinaryOptionTest.sut();
  private static final FxBinaryOption PRODUCT2 = FxBinaryOptionTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 11, 12));
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.05), date(2014, 11, 14));

  //-------------------------------------------------------------------------
  public void test_builder() {
    FxBinaryOptionTrade test = sut();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getPremium(), PREMIUM);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxBinaryOptionTrade test = sut();
    ResolvedFxBinaryOptionTrade expected = ResolvedFxBinaryOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .premium(PREMIUM.resolve(REF_DATA))
        .build();
    assertEquals(test.resolve(REF_DATA), expected);
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
  static FxBinaryOptionTrade sut() {
    return FxBinaryOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
  }

  static FxBinaryOptionTrade sut2() {
    AdjustablePayment premium = AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.01), date(2014, 11, 13));
    return FxBinaryOptionTrade.builder()
        .product(PRODUCT2)
        .premium(premium)
        .build();
  }

}
