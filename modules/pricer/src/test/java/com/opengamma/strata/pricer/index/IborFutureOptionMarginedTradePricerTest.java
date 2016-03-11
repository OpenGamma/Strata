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
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.index.ResolvedIborFutureOption;
import com.opengamma.strata.product.index.ResolvedIborFutureOptionTrade;

/**
 * Tests {@link IborFutureOptionMarginedTradePricer}
 */
@Test
public class IborFutureOptionMarginedTradePricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VAL_DATE = date(2015, 2, 17);

  private static final ResolvedIborFutureOption OPTION = IborFutureDummyData.IBOR_FUTURE_OPTION_2.resolve(REF_DATA);
  private static final StandardId OPTION_ID = StandardId.of("OG-Ticker", "OptionSec");
  private static final LocalDate TRADE_DATE = date(2015, 2, 16);
  private static final long OPTION_QUANTITY = 12345;
  private static final double TRADE_PRICE = 0.0100;
  private static final ResolvedIborFutureOptionTrade OPTION_TRADE_TD = ResolvedIborFutureOptionTrade.builder()
      .tradeInfo(TradeInfo.builder()
          .tradeDate(VAL_DATE)
          .build())
      .product(OPTION)
      .securityStandardId(OPTION_ID)
      .quantity(OPTION_QUANTITY)
      .price(TRADE_PRICE)
      .build();
  private static final ResolvedIborFutureOptionTrade OPTION_TRADE = ResolvedIborFutureOptionTrade.builder()
      .tradeInfo(TradeInfo.builder()
          .tradeDate(TRADE_DATE)
          .build())
      .product(OPTION)
      .securityStandardId(OPTION_ID)
      .quantity(OPTION_QUANTITY)
      .price(TRADE_PRICE)
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
    ResolvedIborFutureOptionTrade trade = ResolvedIborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().build())
        .product(OPTION)
        .securityStandardId(OPTION_ID)
        .quantity(OPTION_QUANTITY)
        .price(TRADE_PRICE)
        .build();
    assertThrowsIllegalArg(() -> OPTION_TRADE_PRICER.presentValue(trade, VAL_DATE, optionPrice, lastClosingPrice));
  }

  public void presentValue_from_no_trade_price() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    ResolvedIborFutureOptionTrade trade = ResolvedIborFutureOptionTrade.builder()
        .tradeInfo(TradeInfo.builder().tradeDate(VAL_DATE).build())
        .product(OPTION)
        .securityStandardId(OPTION_ID)
        .quantity(OPTION_QUANTITY)
        .build();
    assertThrowsIllegalArg(() -> OPTION_TRADE_PRICER.presentValue(trade, VAL_DATE, optionPrice, lastClosingPrice));
  }

  public void presentValue_from_option_price_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(OPTION_TRADE_TD, VAL_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(OPTION, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(OPTION, TRADE_PRICE)) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

  public void presentVSalue_from_option_price_after_trade_date() {
    double optionPrice = 0.0125;
    double lastClosingPrice = 0.0150;
    CurrencyAmount pvComputed = OPTION_TRADE_PRICER
        .presentValue(OPTION_TRADE, VAL_DATE, optionPrice, lastClosingPrice);
    double pvExpected = (OPTION_PRODUCT_PRICER.marginIndex(OPTION, optionPrice) -
        OPTION_PRODUCT_PRICER.marginIndex(OPTION, lastClosingPrice)) * OPTION_QUANTITY;
    assertEquals(pvComputed.getAmount(), pvExpected, TOLERANCE_PV);
  }

}
