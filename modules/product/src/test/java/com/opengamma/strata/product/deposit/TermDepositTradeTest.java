/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.deposit;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;

/**
 * Test {@link TermDepositTrade}. 
 */
public class TermDepositTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final TermDeposit DEPOSIT = TermDeposit.builder()
      .buySell(BuySell.BUY)
      .currency(GBP)
      .notional(100_000_000d)
      .startDate(LocalDate.of(2015, 1, 19))
      .endDate(LocalDate.of(2015, 7, 19))
      .businessDayAdjustment(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO))
      .dayCount(ACT_365F)
      .rate(0.0250)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    TermDepositTrade test = TermDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertThat(test.getProduct()).isEqualTo(DEPOSIT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertThat(test.getProduct()).isEqualTo(DEPOSIT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    TermDepositTrade trade = TermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.TERM_DEPOSIT)
        .currencies(Currency.GBP)
        .description("6M GBP 100mm Deposit 2.5% : 19Jan15-19Jul15")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    TermDepositTrade test = TermDepositTrade.of(TRADE_INFO, DEPOSIT);
    assertThat(test.resolve(REF_DATA).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.resolve(REF_DATA).getProduct()).isEqualTo(DEPOSIT.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    TermDepositTrade test1 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    TermDepositTrade test2 = TermDepositTrade.builder()
        .product(DEPOSIT)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    TermDepositTrade test = TermDepositTrade.builder()
        .product(DEPOSIT)
        .info(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
