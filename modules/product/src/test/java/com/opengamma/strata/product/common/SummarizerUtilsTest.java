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
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
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
@Test
public class SummarizerUtilsTest {

  public void test_date() {
    assertEquals(SummarizerUtils.date(LocalDate.of(2017, 10, 12)), "12Oct17");
    assertEquals(SummarizerUtils.date(LocalDate.of(2017, 4, 3)), "3Apr17");
  }

  public void test_dateRange() {
    assertEquals(SummarizerUtils.dateRange(LocalDate.of(2017, 10, 12), LocalDate.of(2019, 12, 12)), "12Oct17-12Dec19");
  }

  public void test_datePeriod() {
    assertEquals(SummarizerUtils.datePeriod(LocalDate.of(2017, 10, 12), LocalDate.of(2019, 10, 12)), "2Y");
    assertEquals(SummarizerUtils.datePeriod(LocalDate.of(2017, 10, 12), LocalDate.of(2019, 12, 12)), "2Y2M");
  }

  public void test_under1MonthPeriod() {
    assertEquals(SummarizerUtils.datePeriod(LocalDate.of(2014, 8, 6), LocalDate.of(2014, 8, 24)), "18D");
  }

  //-------------------------------------------------------------------------
  public void test_amount() {
    assertEquals(SummarizerUtils.amount(amount(GBP, 12.34)), "GBP 12.34");
    assertEquals(SummarizerUtils.amount(GBP, 12.34), "GBP 12.34");
    assertEquals(SummarizerUtils.amount(GBP, 123), "GBP 123");
    assertEquals(SummarizerUtils.amount(GBP, 1230), "GBP 1,230");
    assertEquals(SummarizerUtils.amount(GBP, 12300), "GBP 12,300");
    assertEquals(SummarizerUtils.amount(GBP, 123000), "GBP 123k");
    assertEquals(SummarizerUtils.amount(GBP, 1230000), "GBP 1.23mm");
  }

  //-------------------------------------------------------------------------
  public void test_percent() {
    assertEquals(SummarizerUtils.percent(0.12), "12%");
    assertEquals(SummarizerUtils.percent(0.123), "12.3%");
    assertEquals(SummarizerUtils.percent(0.1234), "12.34%");
    assertEquals(SummarizerUtils.percent(0.12345), "12.345%");
    assertEquals(SummarizerUtils.percent(0.123456), "12.3456%");
    assertEquals(SummarizerUtils.percent(0.1234564), "12.3456%");
    assertEquals(SummarizerUtils.percent(0.1234567), "12.3457%");
  }

  //-------------------------------------------------------------------------
  public void test_payReceive() {
    assertEquals(SummarizerUtils.payReceive(PayReceive.PAY), "Pay");
    assertEquals(SummarizerUtils.payReceive(PayReceive.RECEIVE), "Rec");
  }

  //-------------------------------------------------------------------------
  public void test_fx() {
    assertEquals(SummarizerUtils.fx(amount(GBP, 100), amount(USD, -121)), "Rec GBP 100 @ GBP/USD 1.21");
    assertEquals(SummarizerUtils.fx(amount(GBP, -80), amount(USD, -100)), "Pay USD 100 @ GBP/USD 1.25");
    assertEquals(SummarizerUtils.fx(amount(GBP, -2000), amount(JPY, -302640)), "Pay GBP 2k @ GBP/JPY 151.32");
  }

  //-------------------------------------------------------------------------
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
    assertEquals(SummarizerUtils.summary(position, ProductType.SECURITY, description, GBP), expected);
  }

  //-------------------------------------------------------------------------
  public void test_summary_trade() {
    StandardId id = StandardId.of("X", "Y");
    Trade trade = new Trade() {
      @Override
      public TradeInfo getInfo() {
        return TradeInfo.builder().id(id).build();
      }

      @Override
      public Trade withInfo(TradeInfo info) {
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
    assertEquals(SummarizerUtils.summary(trade, ProductType.FRA, description, GBP), expected);
  }

  //-------------------------------------------------------------------------
  private CurrencyAmount amount(Currency currency, double amount) {
    return CurrencyAmount.of(currency, amount);
  }

  public void coverage() {
    coverPrivateConstructor(SummarizerUtils.class);
  }

}
