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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Test {@link OvernightFuturePosition}.
 */
public class OvernightFuturePositionTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PositionInfo POSITION_INFO = PositionInfo.builder()
      .id(StandardId.of("A", "B"))
      .build();
  private static final PositionInfo POSITION_INFO2 = PositionInfo.builder()
      .id(StandardId.of("A", "C"))
      .build();
  private static final double QUANTITY = 10;
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

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    OvernightFuturePosition test = OvernightFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
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
    OvernightFuturePosition test = OvernightFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(POSITION_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.OVERNIGHT_FUTURE)
        .currencies(Currency.USD)
        .description("OnFuture x 10")
        .build();
    assertThat(test.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withQuantity() {
    OvernightFuturePosition base = OvernightFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
    double quantity = 75343d;
    OvernightFuturePosition computed = base.withQuantity(quantity);
    OvernightFuturePosition expected = OvernightFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(quantity)
        .build();
    assertThat(computed).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    OvernightFuturePosition base = OvernightFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
    ResolvedOvernightFutureTrade expected = ResolvedOvernightFutureTrade.builder()
        .info(POSITION_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .build();
    assertThat(base.resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightFuturePosition test1 = OvernightFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
    coverImmutableBean(test1);
    OvernightFuturePosition test2 = OvernightFuturePosition.builder()
        .info(POSITION_INFO2)
        .product(PRODUCT2)
        .longQuantity(100)
        .shortQuantity(50)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    OvernightFuturePosition test = OvernightFuturePosition.builder()
        .info(POSITION_INFO)
        .product(PRODUCT)
        .longQuantity(QUANTITY)
        .build();
    assertSerialization(test);
  }

}
