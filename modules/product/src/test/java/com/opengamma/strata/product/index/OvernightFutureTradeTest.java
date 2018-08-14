/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradedPrice;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Test {@link OvernightFutureTrade}.
 */
@Test
public class OvernightFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = date(2018, 3, 18);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(TRADE_DATE);
  private static final double NOTIONAL = 5_000_000d;
  private static final double NOTIONAL2 = 10_000_000d;
  private static final double ACCRUAL_FACTOR = TENOR_1M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR2 = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2018, 9, 28);
  private static final LocalDate START_DATE = date(2018, 9, 1);
  private static final LocalDate END_DATE = date(2018, 9, 30);
  private static final LocalDate LAST_TRADE_DATE2 = date(2018, 6, 15);
  private static final LocalDate START_DATE2 = date(2018, 3, 15);
  private static final LocalDate END_DATE2 = date(2018, 6, 15);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(5);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "OnFuture");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "OnFuture2");
  private static final OvernightFuture PRODUCT = OvernightFuture.builder()
      .securityId(SECURITY_ID)
      .currency(USD)
      .notional(NOTIONAL)
      .accrualFactor(ACCRUAL_FACTOR)
      .startDate(START_DATE)
      .endDate(END_DATE)
      .lastTradeDate(LAST_TRADE_DATE)
      .index(USD_FED_FUND)
      .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
      .rounding(ROUNDING)
      .build();
  private static final OvernightFuture PRODUCT2 = OvernightFuture.builder()
      .securityId(SECURITY_ID2)
      .currency(GBP)
      .notional(NOTIONAL2)
      .accrualFactor(ACCRUAL_FACTOR2)
      .startDate(START_DATE2)
      .endDate(END_DATE2)
      .lastTradeDate(LAST_TRADE_DATE2)
      .index(GBP_SONIA)
      .accrualMethod(OvernightAccrualMethod.COMPOUNDED)
      .rounding(Rounding.none())
      .build();
  private static final double QUANTITY = 35;
  private static final double QUANTITY2 = 36;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;

  //-------------------------------------------------------------------------
  public void test_builder() {
    OvernightFutureTrade test = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
    assertEquals(test.withQuantity(0.9129).getQuantity(), 0.9129d, 1e-10);
    assertEquals(test.withPrice(0.9129).getPrice(), 0.9129d, 1e-10);
  }

  public void test_builder_badPrice() {
    assertThrowsIllegalArg(() -> OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(2.1)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    OvernightFutureTrade trade = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.OVERNIGHT_FUTURE)
        .currencies(Currency.USD)
        .description("OnFuture x 35")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    OvernightFutureTrade test = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    ResolvedOvernightFutureTrade resolved = test.resolve(REF_DATA);
    assertEquals(resolved.getInfo(), TRADE_INFO);
    assertEquals(resolved.getProduct(), PRODUCT.resolve(REF_DATA));
    assertEquals(resolved.getQuantity(), QUANTITY);
    assertEquals(resolved.getTradedPrice(), Optional.of(TradedPrice.of(TRADE_DATE, PRICE)));
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    OvernightFutureTrade base = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    double quantity = 65243;
    OvernightFutureTrade computed = base.withQuantity(quantity);
    OvernightFutureTrade expected = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertEquals(computed, expected);
  }

  public void test_withPrice() {
    OvernightFutureTrade base = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    double price = 0.95;
    OvernightFutureTrade computed = base.withPrice(price);
    OvernightFutureTrade expected = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(price)
        .build();
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightFutureTrade test1 = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    coverImmutableBean(test1);
    OvernightFutureTrade test2 = OvernightFutureTrade.builder()
        .info(TradeInfo.empty())
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    OvernightFutureTrade test = OvernightFutureTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertSerialization(test);
  }

}
