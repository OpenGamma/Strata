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
 * Test {@link BondFutureOptionPosition}.
 */
@Test
public class BondFutureOptionPositionTest {

  private static final ReferenceData REF_DATA = ReferenceData.minimal();
  private static final PositionInfo POSITION_INFO = PositionInfo.builder()
      .id(StandardId.of("A", "B"))
      .build();
  private static final PositionInfo POSITION_INFO2 = PositionInfo.builder()
      .id(StandardId.of("A", "C"))
      .build();
  private static final double QUANTITY = 10;
  private static final BondFutureOption PRODUCT = BondFutureOptionTest.sut();
  private static final BondFutureOption PRODUCT2 = BondFutureOptionTest.sut2();

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    BondFutureOptionPosition test = sut();
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
    BondFutureOptionPosition tes = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.BOND_FUTURE_OPTION)
        .currencies(Currency.USD)
        .description("BondFutureOption x 10")
        .build();
    assertEquals(tes.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    BondFutureOptionPosition base = sut();
    double quantity = 75343d;
    BondFutureOptionPosition computed = base.withQuantity(quantity);
    BondFutureOptionPosition expected = BondFutureOptionPosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(quantity)
        .build();
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedBondFutureOptionTrade expected = ResolvedBondFutureOptionTrade.builder()
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
  static BondFutureOptionPosition sut() {
    return BondFutureOptionPosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
  }

  static BondFutureOptionPosition sut2() {
    return BondFutureOptionPosition.builder()
        .info(POSITION_INFO2)
        .product(PRODUCT2)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
  }

}
