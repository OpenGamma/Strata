/*
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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.PayReceive;

/**
 * Test {@link BulletPaymentTrade}.
 */
public class BulletPaymentTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
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
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    BulletPaymentTrade test = BulletPaymentTrade.of(TRADE_INFO, PRODUCT1);
    assertThat(test.getProduct()).isEqualTo(PRODUCT1);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    BulletPaymentTrade test = BulletPaymentTrade.of(TRADE_INFO, PRODUCT1);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(PRODUCT1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    BulletPaymentTrade trade = BulletPaymentTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT1)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BULLET_PAYMENT)
        .currencies(Currency.GBP)
        .description("Pay GBP 1k : 30Jun15")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    BulletPaymentTrade test = BulletPaymentTrade.of(TRADE_INFO, PRODUCT1);
    assertThat(test.resolve(REF_DATA).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.resolve(REF_DATA).getProduct()).isEqualTo(PRODUCT1.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    BulletPaymentTrade test = BulletPaymentTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT1)
        .build();
    coverImmutableBean(test);
    BulletPaymentTrade test2 = BulletPaymentTrade.builder()
        .product(PRODUCT2)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    BulletPaymentTrade test = BulletPaymentTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT1)
        .build();
    assertSerialization(test);
  }

}
