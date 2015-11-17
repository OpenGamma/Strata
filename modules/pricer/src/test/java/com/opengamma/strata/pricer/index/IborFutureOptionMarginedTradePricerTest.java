/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.index.IborFutureOption;
import com.opengamma.strata.product.index.IborFutureOptionTrade;

/**
 * Tests {@link IborFutureOptionMarginedTradePricer}
 */
@Test
public class IborFutureOptionMarginedTradePricerTest {

  private static final LocalDate VALUATION_DATE = date(2015, 2, 17);

  private static final IborFutureOption FUTURE_OPTION_PRODUCT = IborFutureDummyData.IBOR_FUTURE_OPTION_2;
  private static final StandardId OPTION_SECURITY_ID = StandardId.of("OG-Ticker", "OptionSec");
  private static final Security<IborFutureOption> IBOR_FUTURE_OPTION_SECURITY =
      UnitSecurity.builder(FUTURE_OPTION_PRODUCT).standardId(OPTION_SECURITY_ID).build();
  private static final LocalDate TRADE_DATE = date(2015, 2, 16);
  private static final long OPTION_QUANTITY = 12345;
  private static final double TRADE_PRICE = 0.0100;
  private static final IborFutureOptionTrade FUTURE_OPTION_TRADE_TD = IborFutureOptionTrade.builder()
      .tradeInfo(TradeInfo.builder()
          .tradeDate(VALUATION_DATE)
          .build())
      .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY))
      .quantity(OPTION_QUANTITY)
      .initialPrice(TRADE_PRICE)
      .build();
  private static final IborFutureOptionTrade FUTURE_OPTION_TRADE = IborFutureOptionTrade.builder()
      .tradeInfo(TradeInfo.builder()
          .tradeDate(TRADE_DATE)
          .build())
      .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY))
      .quantity(OPTION_QUANTITY)
      .initialPrice(TRADE_PRICE)
      .build();

  private static final DiscountingIborFutureProductPricer FUTURE_PRICER = DiscountingIborFutureProductPricer.DEFAULT;
  private static final NormalIborFutureOptionMarginedProductPricer OPTION_PRODUCT_PRICER =
      new NormalIborFutureOptionMarginedProductPricer(FUTURE_PRICER);
  private static final NormalIborFutureOptionMarginedTradePricer OPTION_TRADE_PRICER =
      new NormalIborFutureOptionMarginedTradePricer(OPTION_PRODUCT_PRICER);

  private static final double TOLERANCE_PV = 1.0E-2;

  // ----------     present value     ----------
  public void presentValue_from_no_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    IborFutureOptionTrade trade = IborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().build())
        .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY))
        .quantity(OPTION_QUANTITY)
        .initialPrice(TRADE_PRICE)
        .build();
    assertThrowsIllegalArg(() -> OPTION_TRADE_PRICER.presentValue(trade, VALUATION_DATE, optionPrice, lastClosingPrice));
  }

  public void presentValue_from_no_trade_price() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    IborFutureOptionTrade trade = IborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(VALUATION_DATE).build())
        .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY))
        .quantity(OPTION_QUANTITY)
        .build();
    assertThrowsIllegalArg(() -> OPTION_TRADE_PRICER.presentValue(trade, VALUATION_DATE, optionPrice, lastClosingPrice));
  }

  public void presentValue_from_option_price_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE_TD, VALUATION_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, TRADE_PRICE)) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

  public void presentVSalue_from_option_price_after_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(FUTURE_OPTION_TRADE, VALUATION_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(FUTURE_OPTION_PRODUCT, lastClosingPrice)) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

}
