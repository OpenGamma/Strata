package com.opengamma.platform.finance.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test IborFutureOption. 
 */
@Test
public class IborFutureOptionTest {
  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 9, 16);
  private static final int ROUNDING = 6;
  private static final IborFuture IBOR_FUTURE_1 = IborFuture.builder().roundingDecimalPlaces(ROUNDING).currency(GBP)
      .index(GBP_LIBOR_2M).notional(NOTIONAL_1).lastTradeDate(LAST_TRADE_DATE_1).build();
  private static final IborFuture IBOR_FUTURE_2 = IborFuture.builder().index(GBP_LIBOR_3M).notional(NOTIONAL_2)
      .lastTradeDate(LAST_TRADE_DATE_2).build();

  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final double STRIKE_PRICE = 1.075;
  private static final boolean IS_CALL = true;

  /**
   * Test builder. 
   */
  public void builderTest() {
    IborFutureOption iborFutureOption = IborFutureOption.builder().iborFuture(IBOR_FUTURE_1)
        .expirationDate(EXPIRY_DATE).isCall(IS_CALL).strikePrice(STRIKE_PRICE).build();
    assertEquals(IBOR_FUTURE_1, iborFutureOption.getIborFuture());
    assertEquals(STRIKE_PRICE, iborFutureOption.getStrikePrice());
    assertEquals(EXPIRY_DATE, iborFutureOption.getExpirationDate());
  }

  /**
   * Test expand method. 
   */
  public void expandTest() {
    IborFutureOption iborFutureOption = IborFutureOption.builder().iborFuture(IBOR_FUTURE_1)
        .expirationDate(EXPIRY_DATE).isCall(IS_CALL).strikePrice(STRIKE_PRICE).build();
    assertEquals(IBOR_FUTURE_1, iborFutureOption.getIborFuture());
    assertEquals(STRIKE_PRICE, iborFutureOption.getStrikePrice());
    assertEquals(EXPIRY_DATE, iborFutureOption.getExpirationDate());
    ExpandedIborFutureOption expandedIborFutureOption = iborFutureOption.expand();
    assertEquals(IBOR_FUTURE_1.expand(), expandedIborFutureOption.getExpandedIborFuture());
    assertEquals(STRIKE_PRICE, expandedIborFutureOption.getStrikePrice());
    assertEquals(EXPIRY_DATE, expandedIborFutureOption.getExpirationDate());
  }

  /**
   * Expiry should not be after last trade. 
   */
  public void lateExpiryTest() {
    assertThrowsIllegalArg(() -> IborFutureOption.builder().iborFuture(IBOR_FUTURE_1)
        .expirationDate(LAST_TRADE_DATE_2).isCall(IS_CALL).strikePrice(STRIKE_PRICE).build());
  }

  /**
   * Coverage test. 
   */
  public void coverageTest() {
    IborFutureOption iborFutureOption1 = IborFutureOption.builder().iborFuture(IBOR_FUTURE_1)
        .expirationDate(EXPIRY_DATE).isCall(IS_CALL).strikePrice(STRIKE_PRICE).build();
    coverImmutableBean(iborFutureOption1);
    IborFutureOption iborFutureOption2 = IborFutureOption.builder().iborFuture(IBOR_FUTURE_2)
        .expirationDate(LAST_TRADE_DATE_1).isCall(!IS_CALL).strikePrice(STRIKE_PRICE).build();
    coverBeanEquals(iborFutureOption1, iborFutureOption2);
  }

  /**
   * Serialization Test.
   */
  public void serializationTest() {
    IborFutureOption iborFutureOption = IborFutureOption.builder().iborFuture(IBOR_FUTURE_1)
        .expirationDate(EXPIRY_DATE).isCall(IS_CALL).strikePrice(STRIKE_PRICE).build();
    assertSerialization(iborFutureOption);
  }
}
