/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fra;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.fra.Fra;
import com.opengamma.strata.product.fra.FraDiscountingMethod;
import com.opengamma.strata.product.fra.FraTrade;

/**
 * Basic dummy objects used when the data within is not important.
 */
public class FraDummyData {

  /**
   * The notional.
   */
  public static final double NOTIONAL = 1_000_000d;

  /**
   * Fra, default discounting method.
   */
  public static final Fra FRA = Fra.builder()
      .buySell(BUY)
      .notional(NOTIONAL)
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .build();

  /**
   * Fra, AFMA discounting method.
   */
  public static final Fra FRA_AFMA = Fra.builder()
      .buySell(SELL)
      .notional(NOTIONAL)
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .discounting(FraDiscountingMethod.AFMA)
      .build();

  /**
   * Fra, NONE discounting method.
   */
  public static final Fra FRA_NONE = Fra.builder()
      .buySell(BUY)
      .notional(NOTIONAL)
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .discounting(FraDiscountingMethod.NONE)
      .build();

  /**
   * Fra trade.
   */
  public static final FraTrade FRA_TRADE = FraTrade.builder()
      .info(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
      .product(FRA)
      .build();

}
