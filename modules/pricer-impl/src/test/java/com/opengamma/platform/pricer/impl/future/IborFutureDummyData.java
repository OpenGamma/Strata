/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.collect.TestHelper.date;

import java.time.LocalDate;

import com.opengamma.basics.PutCall;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.SecurityLink;
import com.opengamma.platform.finance.TradeInfo;
import com.opengamma.platform.finance.future.IborFuture;
import com.opengamma.platform.finance.future.IborFutureOption;
import com.opengamma.platform.finance.future.IborFutureOptionSecurityTrade;
import com.opengamma.platform.finance.future.IborFutureSecurityTrade;

/**
 * Ibor future data.
 */
public class IborFutureDummyData {

  private static final double NOTIONAL = 100_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 17);
  private static final int ROUNDING = 4;
  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final double MULTIPLIER = 35.0;
  private static final double INITIAL_PRICE = 1.015;
  private static final StandardId TRADE_ID = StandardId.of("OG-Trade", "1");
  private static final SecurityLink<IborFuture> SECURITY_LINK =
      SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG"), IborFuture.class);
  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final double STRIKE_PRICE = 1.075;
  private static final double MULTIPLIER_OPTION = 65.0;
  private static final double INITIAL_PRICE_OPTION = 0.065;
  private static final SecurityLink<IborFutureOption> SECURITY_LINK_OPTION =
      SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG"), IborFutureOption.class);
  private static final StandardId TRADE_ID_OPTION = StandardId.of("OG-Trade", "2");

  /**
   * An IborFuture.
   */
  public static final IborFuture IBOR_FUTURE = IborFuture.builder()
      .currency(GBP)
      .notional(NOTIONAL)
      .lastTradeDate(LAST_TRADE_DATE)
      .index(GBP_LIBOR_2M)
      .accrualFactor(ACCRUAL_FACTOR_2M)
      .roundingDecimalPlaces(ROUNDING)
      .build();

  /**
   * An IborFutureSecurityTrade.
   */
  public static final IborFutureSecurityTrade IBOR_FUTURE_SECURITY_TRADE = IborFutureSecurityTrade.builder()
      .standardId(TRADE_ID)
      .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
      .securityLink(SECURITY_LINK)
      .multiplier(MULTIPLIER)
      .initialPrice(INITIAL_PRICE)
      .build();

  /**
   * An IborFutureOption.
   */
  public static final IborFutureOption IBOR_FUTURE_OPTION = IborFutureOption.builder()
      .putCall(PutCall.CALL)
      .expirationDate(EXPIRY_DATE)
      .strikePrice(STRIKE_PRICE)
      .iborFuture(IBOR_FUTURE)
      .build();

  /**
   * An IborFutureOptionSecurityTrade.
   */
  public static final IborFutureOptionSecurityTrade IBOR_FUTURE_OPTION_SECURITY_TRADE =
      IborFutureOptionSecurityTrade.builder()
          .standardId(TRADE_ID_OPTION)
          .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
          .securityLink(SECURITY_LINK_OPTION)
          .multiplier(MULTIPLIER_OPTION)
          .initialPrice(INITIAL_PRICE_OPTION)
          .build();

}
