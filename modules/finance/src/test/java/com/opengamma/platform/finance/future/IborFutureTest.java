package com.opengamma.platform.finance.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.platform.finance.observation.IborRateObservation;

/**
 * Test IborFuture.
 */
@Test
public class IborFutureTest {
  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR_3M = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 3, 15);
  private static final int ROUNDING = 6;

  /**
   * Test builder with full list of initialization. 
   */
  public void builderTest() {
    IborFuture iborFuture = IborFuture.builder().notional(NOTIONAL_1).currency(GBP).accrualFactor(ACCRUAL_FACTOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_1).index(GBP_LIBOR_2M).roundingDecimalPlaces(ROUNDING).build();
    assertEquals(GBP, iborFuture.getCurrency());
    assertEquals(NOTIONAL_1, iborFuture.getNotional());
    assertEquals(ACCRUAL_FACTOR_2M, iborFuture.getAccrualFactor());
    assertEquals(LAST_TRADE_DATE_1, iborFuture.getLastTradeDate());
    assertEquals(GBP_LIBOR_2M, iborFuture.getIndex());
    assertEquals(ROUNDING, iborFuture.getRoundingDecimalPlaces());
  }

  /**
   * Test builder with minimum list of initialization. 
   */
  public void defaultTest() {
    IborFuture iborFuture = IborFuture.builder().currency(GBP).lastTradeDate(LAST_TRADE_DATE_1)
        .index(GBP_LIBOR_2M).build();
    assertEquals(GBP, iborFuture.getCurrency());
    assertEquals(0.0, iborFuture.getNotional()); // default notional is 0
    assertEquals(ACCRUAL_FACTOR_2M, iborFuture.getAccrualFactor());
    assertEquals(LAST_TRADE_DATE_1, iborFuture.getLastTradeDate());
    assertEquals(GBP_LIBOR_2M, iborFuture.getIndex());
    assertEquals(4, iborFuture.getRoundingDecimalPlaces());
  }

  /**
   * Test extend() method
   */
  public void expandTest() {
    IborFuture iborFuture = IborFuture.builder().notional(NOTIONAL_1).currency(GBP).lastTradeDate(LAST_TRADE_DATE_1)
        .index(GBP_LIBOR_2M).roundingDecimalPlaces(ROUNDING).build();
    ExpandedIborFuture expIborFuture = iborFuture.expand();
    IborRateObservation expectedRate = IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1);
    assertEquals(NOTIONAL_1, expIborFuture.getNotional());
    assertEquals(GBP, expIborFuture.getCurrency());
    assertEquals(ACCRUAL_FACTOR_2M, expIborFuture.getAccrualFactor());
    assertEquals(expectedRate, expIborFuture.getRate());
    assertEquals(ROUNDING, expIborFuture.getRoundingDecimalPlaces());
  }

  /**
   * index is null, exception thrown. 
   */
  public void noIndexTest() {
    assertThrowsIllegalArg(() -> IborFuture.builder().notional(NOTIONAL_1).currency(GBP)
        .lastTradeDate(LAST_TRADE_DATE_1).roundingDecimalPlaces(ROUNDING).build());
  }

  /**
   * currency is null, currency of index is used. 
   */
  public void noCurrencyTest() {
    IborFuture iborFuture = IborFuture.builder().notional(NOTIONAL_1).index(GBP_LIBOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_1).roundingDecimalPlaces(ROUNDING).build();
    assertEquals(GBP, iborFuture.getCurrency());
  }

  /**
   * last trade date is null, exception thrown. 
   */
  public void noLastTradeDateTest() {
    assertThrowsIllegalArg(() -> IborFuture.builder().notional(NOTIONAL_1).currency(GBP).index(GBP_LIBOR_2M)
        .roundingDecimalPlaces(ROUNDING).build());
  }
  /**
   * Coverage test. 
   */
  public void coverageTest() {
    IborFuture iborFuture1 = IborFuture.builder().notional(NOTIONAL_1).currency(USD).accrualFactor(ACCRUAL_FACTOR_3M)
        .lastTradeDate(LAST_TRADE_DATE_1).index(USD_LIBOR_3M).roundingDecimalPlaces(ROUNDING).build();
    coverImmutableBean(iborFuture1);
    IborFuture iborFuture2 = IborFuture.builder().notional(NOTIONAL_2).currency(GBP).accrualFactor(ACCRUAL_FACTOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_2).index(GBP_LIBOR_2M).build();
    coverBeanEquals(iborFuture1, iborFuture2);
  }

  /**
   * Serialization Test. 
   */
  public void serializationTest() {
    IborFuture iborFuture = IborFuture.builder().notional(NOTIONAL_1).currency(USD).accrualFactor(ACCRUAL_FACTOR_2M)
        .lastTradeDate(LAST_TRADE_DATE_1).index(GBP_LIBOR_2M).roundingDecimalPlaces(ROUNDING).build();
    assertSerialization(iborFuture);
  }
}
