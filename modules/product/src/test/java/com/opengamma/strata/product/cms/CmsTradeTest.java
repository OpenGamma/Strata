/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link CmsTrade}.
 */
@Test
public class CmsTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = LocalDate.of(2015, 9, 21);
  private static final LocalDate SETTLE_DATE = LocalDate.of(2015, 9, 23);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).settlementDate(SETTLE_DATE).build();
  private static final AdjustablePayment PREMIUM = AdjustablePayment.of(CurrencyAmount.of(EUR, -0.001 * 1.0e6), SETTLE_DATE);

  private static final Cms PRODUCT_CAP = Cms.of(CmsTest.sutCap().getCmsLeg());
  private static final Cms PRODUCT_CAP2 = CmsTest.sutCap();
  private static final Cms PRODUCT_FLOOR = CmsTest.sutFloor();

  //-------------------------------------------------------------------------
  public void test_builder() {
    CmsTrade test = sut();
    assertEquals(test.getPremium().get(), PREMIUM);
    assertEquals(test.getProduct(), PRODUCT_CAP);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
  }

  public void test_builder_noPrem() {
    CmsTrade test = CmsTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT_CAP2)
        .build();
    assertFalse(test.getPremium().isPresent());
    assertEquals(test.getProduct(), PRODUCT_CAP2);
    assertEquals(test.getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    CmsTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.CMS)
        .currencies(Currency.EUR)
        .description("2Y EUR 1mm Rec EUR-EURIBOR-1100-10Y Cap 1.25% / Pay Premium : 21Oct15-21Oct17")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  public void test_summarize_floor() {
    CmsTrade trade = CmsTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT_FLOOR)
        .premium(PREMIUM)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.CMS)
        .currencies(Currency.EUR)
        .description("2Y EUR 1mm Rec EUR-EURIBOR-1100-10Y Floor 1.25% / Pay Premium : 21Oct15-21Oct17")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  public void test_summarize_singleLeg() {
    CmsTrade trade = CmsTrade.builder()
        .product(Cms.of(CmsLegTest.sutCap()))
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.CMS)
        .currencies(Currency.EUR)
        .description("2Y EUR 1mm Rec EUR-EURIBOR-1100-10Y Cap 1.25% : 21Oct15-21Oct17")
        .build();

    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedCmsTrade expected = ResolvedCmsTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT_CAP.resolve(REF_DATA))
        .premium(PREMIUM.resolve(REF_DATA))
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
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
  static CmsTrade sut() {
    return CmsTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT_CAP)
        .premium(PREMIUM)
        .build();
  }

  static CmsTrade sut2() {
    return CmsTrade.builder()
        .product(PRODUCT_CAP2)
        .build();
  }

}
