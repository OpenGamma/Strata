/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.payment;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link BulletPaymentTrade}.
 */
@Test
public class BulletPaymentTradeTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final BulletPayment PRODUCT1 = BulletPayment.builder()
      .payReceive(PayReceive.PAY)
      .value(GBP_P1000)
      .date(AdjustableDate.of(DATE_2015_06_30))
      .build();
  private static final BulletPayment PRODUCT2 = BulletPayment.builder()
      .payReceive(PayReceive.RECEIVE)
      .value(GBP_P1000)
      .date(AdjustableDate.of(DATE_2015_06_30))
      .build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    BulletPaymentTrade test = BulletPaymentTrade.builder()
        .product(PRODUCT1)
        .build();
    assertEquals(test.getTradeInfo(), TradeInfo.EMPTY);
    assertEquals(test.getProduct(), PRODUCT1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BulletPaymentTrade test = BulletPaymentTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(PRODUCT1)
        .build();
    coverImmutableBean(test);
    BulletPaymentTrade test2 = BulletPaymentTrade.builder()
        .product(PRODUCT2)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    BulletPaymentTrade test = BulletPaymentTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
        .product(PRODUCT1)
        .build();
    assertSerialization(test);
  }

}
