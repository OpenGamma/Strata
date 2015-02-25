package com.opengamma.platform.finance.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.collect.TestHelper.date;

import java.time.LocalDate;

import org.testng.annotations.Test;

@Test
public class IborFutureOptionTest {
  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR_3M = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 3, 18);
  private static final LocalDate LAST_TRADE_DATE_3 = date(2015, 9, 16);
  private static final int ROUNDING = 6;

  private static final IborFuture IBOR_FUTURE = IborFuture.builder().roundingDecimalPlaces(ROUNDING).currency(GBP)
      .index(GBP_LIBOR_2M).notional(NOTIONAL_1).lastTradeDate(LAST_TRADE_DATE_1).build();

  public void builderTest() {
    //    IborFutureOption iborFutureOption = IborFutureOption.builder().iborFuture(IBOR_FUTURE)
    //        .expirationDate(LAST_TRADE_DATE_1).isCall(true).strike(1.075).build();
  }

  /**
   * Expiry should not be after last trade. 
   */
  public void lateExpiryTest() {

  }

  public void coverageTest() {

  }

  public void serializationTest() {

  }
}
