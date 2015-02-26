package com.opengamma.platform.pricer.impl.futures;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.collect.TestHelper.date;

import java.time.LocalDate;

import com.opengamma.collect.id.Link;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.future.IborFuture;
import com.opengamma.platform.finance.future.IborFutureSecurity;
import com.opengamma.platform.finance.future.IborFutureSecurityTrade;

/**
 * Ibor futures data
 */
public class IborFuturesDummyData {
  private static final double NOTIONAL = 1_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 17);
  private static final int ROUNDING = 4;
  /**
   * IborFuture
   */
  public static final IborFuture IBOR_FUTURE = IborFuture.builder().notional(NOTIONAL).lastTradeDate(LAST_TRADE_DATE)
      .index(GBP_LIBOR_2M).currency(GBP).accrualFactor(ACCRUAL_FACTOR_2M).roundingDecimalPlaces(ROUNDING).build();

  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final double MULTIPLIER = 35.0;
  private static final double INITIAL_PRICE = 1.015;
  private static final StandardId TRADE_ID = StandardId.of("OG-Trade", "1");
  private static final Link<IborFutureSecurity> SECURITY_LINK = Link.resolvable(StandardId.of("OG-Ticker", "OG"),
      IborFutureSecurity.class);
  /**
   * IborFutureSecurityTrade
   */
  public static final IborFutureSecurityTrade IBOR_FUTURE_SECURITY_TRADE = IborFutureSecurityTrade.builder()
      .initialPrice(INITIAL_PRICE).standardId(TRADE_ID).multiplier(MULTIPLIER).tradeDate(TRADE_DATE)
      .securityLink(SECURITY_LINK).build();
}
