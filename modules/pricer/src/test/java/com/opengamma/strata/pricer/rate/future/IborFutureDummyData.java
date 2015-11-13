/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.future;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.Security;
import com.opengamma.strata.product.SecurityLink;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.UnitSecurity;
import com.opengamma.strata.product.common.FutureOptionPremiumStyle;
import com.opengamma.strata.product.rate.future.IborFuture;
import com.opengamma.strata.product.rate.future.IborFutureOption;
import com.opengamma.strata.product.rate.future.IborFutureOptionTrade;
import com.opengamma.strata.product.rate.future.IborFutureTrade;

/**
 * Ibor future data.
 */
public class IborFutureDummyData {

  private static final double NOTIONAL = 100_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 17);
  private static final int ROUNDING = 4;
  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final long FUTURE_QUANTITY = 35;
  private static final double FUTURE_INITIAL_PRICE = 1.015;
  private static final StandardId FUTURE_SECURITY_ID = StandardId.of("OG-Ticker", "Future");

  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final double STRIKE_PRICE = 1.075;
  private static final double STRIKE_PRICE_2 = 0.99;
  private static final long OPTION_QUANTITY = 65L;
  private static final double OPTION_INITIAL_PRICE = 0.065;
  private static final StandardId OPTION_SECURITY_ID = StandardId.of("OG-Ticker", "Option");

  /**
   * An IborFuture.
   */
  public static final IborFuture IBOR_FUTURE = IborFuture.builder()
      .currency(GBP)
      .notional(NOTIONAL)
      .lastTradeDate(LAST_TRADE_DATE)
      .index(GBP_LIBOR_2M)
      .accrualFactor(ACCRUAL_FACTOR_2M)
      .rounding(Rounding.ofDecimalPlaces(ROUNDING))
      .build();

  /**
   * Security for Ibor future.
   */
  private static final Security<IborFuture> IBOR_FUTURE_SECURITY =
      UnitSecurity.builder(IBOR_FUTURE)
          .standardId(FUTURE_SECURITY_ID)
          .build();

  /**
   * An IborFutureTrade.
   */
  public static final IborFutureTrade IBOR_FUTURE_TRADE = IborFutureTrade.builder()
      .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
      .securityLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY))
      .quantity(FUTURE_QUANTITY)
      .initialPrice(FUTURE_INITIAL_PRICE)
      .build();

  /**
   * An IborFutureOption.
   */
  public static final IborFutureOption IBOR_FUTURE_OPTION = IborFutureOption.builder()
      .putCall(PutCall.CALL)
      .strikePrice(STRIKE_PRICE)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(LocalTime.of(11, 0))
      .expiryZone(ZoneId.of("Europe/London"))
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY))
      .build();

  /**
   * An IborFutureOption.
   */
  public static final IborFutureOption IBOR_FUTURE_OPTION_2 = IborFutureOption.builder()
      .putCall(PutCall.CALL)
      .strikePrice(STRIKE_PRICE_2)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(LocalTime.of(11, 0))
      .expiryZone(ZoneId.of("Europe/London"))
      .premiumStyle(FutureOptionPremiumStyle.DAILY_MARGIN)
      .underlyingLink(SecurityLink.resolved(IBOR_FUTURE_SECURITY))
      .build();

  /**
   * Security for Ibor future option.
   */
  private static final Security<IborFutureOption> IBOR_FUTURE_OPTION_SECURITY =
      UnitSecurity.builder(IBOR_FUTURE_OPTION)
          .standardId(OPTION_SECURITY_ID)
          .build();

  /**
   * An IborFutureOptionTrade.
   */
  public static final IborFutureOptionTrade IBOR_FUTURE_OPTION_TRADE =
      IborFutureOptionTrade.builder()
          .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
          .securityLink(SecurityLink.resolved(IBOR_FUTURE_OPTION_SECURITY))
          .quantity(OPTION_QUANTITY)
          .initialPrice(OPTION_INITIAL_PRICE)
          .build();

}
