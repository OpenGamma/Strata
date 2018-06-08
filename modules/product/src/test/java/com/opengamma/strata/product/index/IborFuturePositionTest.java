/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;

/**
 * Test {@link IborFuturePosition}.
 */
@Test
public class IborFuturePositionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PositionInfo POSITION_INFO = PositionInfo.builder()
      .id(StandardId.of("A", "B"))
      .build();
  private static final PositionInfo POSITION_INFO2 = PositionInfo.builder()
      .id(StandardId.of("A", "C"))
      .build();
  private static final double QUANTITY = 10;
  private static final IborFuture PRODUCT = IborFutureTest.sut();
  private static final IborFuture PRODUCT2 = IborFutureTest.sut2();

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    IborFuturePosition test = sut();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), POSITION_INFO);
    assertEquals(test.getLongQuantity(), QUANTITY, 0d);
    assertEquals(test.getShortQuantity(), 0d, 0d);
    assertEquals(test.getQuantity(), QUANTITY, 0d);
    assertEquals(test.withInfo(POSITION_INFO).getInfo(), POSITION_INFO);
    assertEquals(test.withQuantity(129).getQuantity(), 129d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    IborFuturePosition tes = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.IBOR_FUTURE)
        .currencies(Currency.USD)
        .description("IborFuture x 10")
        .build();
    assertEquals(tes.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    IborFuturePosition base = sut();
    double quantity = 75343d;
    IborFuturePosition computed = base.withQuantity(quantity);
    IborFuturePosition expected = IborFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(quantity)
        .build();
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedIborFutureTrade expected = ResolvedIborFutureTrade.builder()
        .info(POSITION_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
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
  static IborFuturePosition sut() {
    return IborFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
  }

  static IborFuturePosition sut2() {
    return IborFuturePosition.builder()
        .info(POSITION_INFO2)
        .product(PRODUCT2)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
  }

}
