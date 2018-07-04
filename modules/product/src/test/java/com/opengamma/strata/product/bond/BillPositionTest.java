/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;

/**
 * Test {@link BillPosition}.
 */
@Test
public class BillPositionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PositionInfo POSITION_INFO1 = PositionInfo.builder()
      .id(StandardId.of("A", "B"))
      .build();
  private static final PositionInfo POSITION_INFO2 = PositionInfo.builder()
      .id(StandardId.of("A", "C"))
      .build();
  private static final double QUANTITY1 = 10;
  private static final double QUANTITY2 = 30;
  private static final Bill PRODUCT1 = BillTest.US_BILL;
  private static final Bill PRODUCT2 = BillTest.BILL_2;

  public void test_builder_of() {
    BillPosition test = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .shortQuantity(QUANTITY2)
        .build();
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getId(), POSITION_INFO1.getId());
    assertEquals(test.getInfo(), POSITION_INFO1);
    assertEquals(test.getLongQuantity(), QUANTITY1);
    assertEquals(test.getShortQuantity(), QUANTITY2);
    assertEquals(test.getProduct(), PRODUCT1);
    assertEquals(test.getQuantity(), QUANTITY1 - QUANTITY2);
    assertEquals(test.getSecurityId(), PRODUCT1.getSecurityId());
    BillPosition test1 = BillPosition.ofLongShort(POSITION_INFO1, PRODUCT1, QUANTITY1, QUANTITY2);
    assertEquals(test, test1);
  }

  public void test_summarize() {
    BillPosition base = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO1.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.BILL)
        .currencies(USD)
        .description("Bill2019-05-23 x 10")
        .build();
    assertEquals(base.summarize(), expected);
  }

  public void test_withInfo() {
    BillPosition base = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .build();
    BillPosition computed1 = base.withInfo(POSITION_INFO2);
    BillPosition expected1 = BillPosition.builder()
        .info(POSITION_INFO2)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .build();
    assertEquals(computed1, expected1);
  }

  public void test_withQuantity() {
    BillPosition base = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .build();
    double quantity = 1234d;
    BillPosition computed1 = base.withQuantity(quantity);
    BillPosition expected1 = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(quantity)
        .build();
    assertEquals(computed1, expected1);
    BillPosition computed2 = base.withQuantity(-quantity);
    BillPosition expected2 = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .shortQuantity(quantity)
        .build();
    assertEquals(computed2, expected2);
  }

  public void test_resolve() {
    BillPosition base = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .build();
    ResolvedBillTrade computed = base.resolve(REF_DATA);
    ResolvedBillTrade expected = ResolvedBillTrade.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1.resolve(REF_DATA))
        .quantity(QUANTITY1)
        .build();
    assertEquals(computed, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    BillPosition test1 = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .build();
    coverImmutableBean(test1);
    BillPosition test2 = BillPosition.builder()
        .info(POSITION_INFO2)
        .product(PRODUCT2)
        .shortQuantity(QUANTITY1)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    BillPosition test = BillPosition.builder()
        .info(POSITION_INFO1)
        .product(PRODUCT1)
        .longQuantity(QUANTITY1)
        .build();
    assertSerialization(test);
  }

}
