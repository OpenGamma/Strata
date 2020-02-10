/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.PositionInfo;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.SecurityPosition;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link SummarizerUtils}.
 */
public class SummarizerUtilsTest {

  @Test
  public void test_date() {
    assertThat(SummarizerUtils.date(LocalDate.of(2017, 10, 12))).isEqualTo("12Oct17");
    assertThat(SummarizerUtils.date(LocalDate.of(2017, 4, 3))).isEqualTo("3Apr17");
  }

  @Test
  public void test_dateRange() {
    assertThat(SummarizerUtils.dateRange(LocalDate.of(2017, 10, 12), LocalDate.of(2019, 12, 12))).isEqualTo("12Oct17-12Dec19");
  }

  @Test
  public void test_datePeriod() {
    assertThat(SummarizerUtils.datePeriod(LocalDate.of(2017, 10, 12), LocalDate.of(2019, 10, 12))).isEqualTo("2Y");
    assertThat(SummarizerUtils.datePeriod(LocalDate.of(2017, 10, 12), LocalDate.of(2019, 12, 12))).isEqualTo("2Y2M");
  }

  @Test
  public void test_under1MonthPeriod() {
    assertThat(SummarizerUtils.datePeriod(LocalDate.of(2014, 8, 6), LocalDate.of(2014, 8, 24))).isEqualTo("18D");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_amount() {
    assertThat(SummarizerUtils.amount(amount(GBP, 12.34))).isEqualTo("GBP 12.34");
    assertThat(SummarizerUtils.amount(GBP, 12.34)).isEqualTo("GBP 12.34");
    assertThat(SummarizerUtils.amount(GBP, 123)).isEqualTo("GBP 123");
    assertThat(SummarizerUtils.amount(GBP, 1230)).isEqualTo("GBP 1,230");
    assertThat(SummarizerUtils.amount(GBP, 12300)).isEqualTo("GBP 12,300");
    assertThat(SummarizerUtils.amount(GBP, 123000)).isEqualTo("GBP 123k");
    assertThat(SummarizerUtils.amount(GBP, 1230000)).isEqualTo("GBP 1.23mm");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_percent() {
    assertThat(SummarizerUtils.percent(0.12)).isEqualTo("12%");
    assertThat(SummarizerUtils.percent(0.123)).isEqualTo("12.3%");
    assertThat(SummarizerUtils.percent(0.1234)).isEqualTo("12.34%");
    assertThat(SummarizerUtils.percent(0.12345)).isEqualTo("12.345%");
    assertThat(SummarizerUtils.percent(0.123456)).isEqualTo("12.3456%");
    assertThat(SummarizerUtils.percent(0.1234564)).isEqualTo("12.3456%");
    assertThat(SummarizerUtils.percent(0.1234567)).isEqualTo("12.3457%");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_payReceive() {
    assertThat(SummarizerUtils.payReceive(PayReceive.PAY)).isEqualTo("Pay");
    assertThat(SummarizerUtils.payReceive(PayReceive.RECEIVE)).isEqualTo("Rec");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_fx() {
    assertThat(SummarizerUtils.fx(amount(GBP, 100), amount(USD, -121))).isEqualTo("Rec GBP 100 @ GBP/USD 1.21");
    assertThat(SummarizerUtils.fx(amount(GBP, -80), amount(USD, -100))).isEqualTo("Pay USD 100 @ GBP/USD 1.25");
    assertThat(SummarizerUtils.fx(amount(GBP, -2000), amount(JPY, -302640))).isEqualTo("Pay GBP 2k @ GBP/JPY 151.32");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summary_position() {
    StandardId id = StandardId.of("X", "Y");
    SecurityPosition position = SecurityPosition.builder()
        .securityId(SecurityId.of("A", "B"))
        .longQuantity(123)
        .info(PositionInfo.of(id))
        .build();
    String description = "desc";
    PortfolioItemSummary expected = PortfolioItemSummary.of(
        id,
        PortfolioItemType.POSITION,
        ProductType.SECURITY,
        ImmutableSet.of(GBP),
        description);
    assertThat(SummarizerUtils.summary(position, ProductType.SECURITY, description, GBP)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summary_trade() {
    StandardId id = StandardId.of("X", "Y");
    Trade trade = new Trade() {
      @Override
      public TradeInfo getInfo() {
        return TradeInfo.builder().id(id).build();
      }

      @Override
      public Trade withInfo(PortfolioItemInfo info) {
        return this;
      }
    };
    String description = "desc";
    PortfolioItemSummary expected = PortfolioItemSummary.of(
        id,
        PortfolioItemType.TRADE,
        ProductType.FRA,
        ImmutableSet.of(GBP),
        description);
    assertThat(SummarizerUtils.summary(trade, ProductType.FRA, description, GBP)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount amount(Currency currency, double amount) {
    return CurrencyAmount.of(currency, amount);
  }

  @Test
  public void coverage() {
    coverPrivateConstructor(SummarizerUtils.class);
  }

}
