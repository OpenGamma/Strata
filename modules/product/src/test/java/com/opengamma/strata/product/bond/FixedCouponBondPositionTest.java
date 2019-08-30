/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;

/**
 * Test {@link FixedCouponBondPosition}.
 */
public class FixedCouponBondPositionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PositionInfo POSITION_INFO = PositionInfo.builder()
      .id(StandardId.of("A", "B"))
      .build();
  private static final PositionInfo POSITION_INFO2 = PositionInfo.builder()
      .id(StandardId.of("A", "C"))
      .build();
  private static final double QUANTITY = 10;
  private static final FixedCouponBond PRODUCT = FixedCouponBondTest.sut();
  private static final FixedCouponBond PRODUCT2 = FixedCouponBondTest.sut2();

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_resolved() {
    FixedCouponBondPosition test = sut();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.getLongQuantity()).isCloseTo(QUANTITY, offset(0d));
    assertThat(test.getShortQuantity()).isCloseTo(0d, offset(0d));
    assertThat(test.getQuantity()).isCloseTo(QUANTITY, offset(0d));
    assertThat(test.withInfo(POSITION_INFO).getInfo()).isEqualTo(POSITION_INFO);
    assertThat(test.withQuantity(129).getQuantity()).isCloseTo(129d, offset(0d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    FixedCouponBondPosition trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.BOND)
        .currencies(Currency.EUR)
        .description("Bond x 10")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withQuantity() {
    FixedCouponBondPosition base = sut();
    double quantity = 75343d;
    FixedCouponBondPosition computed = base.withQuantity(quantity);
    FixedCouponBondPosition expected = FixedCouponBondPosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(quantity)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    ResolvedFixedCouponBondTrade expected = ResolvedFixedCouponBondTrade.builder()
        .info(POSITION_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .build();
    assertThat(sut().resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static FixedCouponBondPosition sut() {
    return FixedCouponBondPosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
  }

  static FixedCouponBondPosition sut2() {
    return FixedCouponBondPosition.builder()
        .info(POSITION_INFO2)
        .product(PRODUCT2)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
  }

}
