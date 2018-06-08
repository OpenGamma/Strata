/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

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
 * Test {@link BondFuturePosition}.
 */
@Test
public class BondFuturePositionTest {

  private static final ReferenceData REF_DATA = ReferenceData.minimal();
  private static final PositionInfo POSITION_INFO = PositionInfo.builder()
      .id(StandardId.of("A", "B"))
      .build();
  private static final PositionInfo POSITION_INFO2 = PositionInfo.builder()
      .id(StandardId.of("A", "C"))
      .build();
  private static final double QUANTITY = 10;
  private static final BondFuture PRODUCT = BondFutureTest.sut();
  private static final BondFuture PRODUCT2 = BondFutureTest.sut2();

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    BondFuturePosition test = sut();
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
    BondFuturePosition tes = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.BOND_FUTURE)
        .currencies(Currency.USD)
        .description("BondFuture x 10")
        .build();
    assertEquals(tes.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    BondFuturePosition base = sut();
    double quantity = 75343d;
    BondFuturePosition computed = base.withQuantity(quantity);
    BondFuturePosition expected = BondFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(quantity)
        .build();
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedBondFutureTrade expected = ResolvedBondFutureTrade.builder()
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
  static BondFuturePosition sut() {
    return BondFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
  }

  static BondFuturePosition sut2() {
    return BondFuturePosition.builder()
        .info(POSITION_INFO2)
        .product(PRODUCT2)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
  }

}
