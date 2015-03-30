/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.fra;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.BuySell.SELL;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.date;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.finance.TradeInfo;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.finance.rate.fra.FraDiscountingMethod;
import com.opengamma.strata.finance.rate.fra.FraTrade;

/**
 * Basic dummy objects used when the data within is not important.
 */
public class FraDummyData {

  /**
   * The notional.
   */
  public static final double NOTIONAL = 1_000_000d;
  /**
   *  Business day convention: modified following
   */
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  /**
   * Fixing offset: -2
   */
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);

  /**
   * Fra, default discounting method. 
   */
  public static final Fra FRA = Fra.builder()
      .buySell(BUY)
      .notional(NOTIONAL)
      .paymentDate(AdjustableDate.of(date(2014, 12, 12), BDA_MOD_FOLLOW))
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .fixingOffset(MINUS_TWO_DAYS)
      .build();

  /**
   * Fra, AFMA discounting method. 
   */
  public static final Fra FRA_AFMA = Fra.builder()
      .buySell(SELL)
      .notional(NOTIONAL)
      .paymentDate(AdjustableDate.of(date(2014, 12, 12), BDA_MOD_FOLLOW))
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .fixingOffset(MINUS_TWO_DAYS)
      .discounting(FraDiscountingMethod.AFMA)
      .build();

  /**
   * Fra, NONE discounting method. 
   */
  public static final Fra FRA_NONE = Fra.builder()
      .buySell(BUY)
      .notional(NOTIONAL)
      .paymentDate(AdjustableDate.of(date(2014, 12, 12), BDA_MOD_FOLLOW))
      .startDate(date(2014, 9, 12))
      .endDate(date(2014, 12, 12))
      .index(GBP_LIBOR_3M)
      .fixedRate(0.0125)
      .currency(Currency.GBP)
      .fixingOffset(MINUS_TWO_DAYS)
      .discounting(FraDiscountingMethod.NONE)
      .build();

  /**
   * Fra trade.
   */
  public static final FraTrade FRA_TRADE = FraTrade.builder()
      .standardId(StandardId.of("OG-Trade", "1"))
      .tradeInfo(TradeInfo.builder().tradeDate(date(2014, 6, 30)).build())
      .product(FRA)
      .build();

}
