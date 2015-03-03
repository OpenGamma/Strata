package com.opengamma.platform.finance.future;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.index.IborIndices.GBP_LIBOR_1W;
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
 * Test ExpandedIborFutureOption. 
 */
@Test
public class ExpandedIborFutureOptionTest {
  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR_3M = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 3, 15);
  private static final int ROUNDING = 6;
  private static final IborRateObservation RATE_OBS_GBP = IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1);
  private static final IborRateObservation RATE_OBS_USD = IborRateObservation.of(USD_LIBOR_3M, LAST_TRADE_DATE_2);
  private static final IborRateObservation RATE_OBS_GBP_1W = IborRateObservation.of(GBP_LIBOR_1W, LAST_TRADE_DATE_1);

  private static final ExpandedIborFuture EXPANDED_IBOR_FUTURE_1 = ExpandedIborFuture.builder().currency(GBP)
      .notional(NOTIONAL_1).accrualFactor(ACCRUAL_FACTOR_2M).rate(RATE_OBS_GBP).roundingDecimalPlaces(ROUNDING).build();
  private static final ExpandedIborFuture EXPANDED_IBOR_FUTURE_2 = ExpandedIborFuture.builder().currency(GBP)
      .notional(NOTIONAL_2).accrualFactor(ACCRUAL_FACTOR_3M).rate(RATE_OBS_GBP_1W).build();

  private static final LocalDate EXPIRY_DATE = date(2015, 5, 20);
  private static final double STRIKE_PRICE = 1.075;
  private static final boolean IS_CALL = true;

  /**
   * Test builder. 
   */
  public void builderTest() {
    ExpandedIborFutureOption option = ExpandedIborFutureOption.builder().expandedIborFuture(EXPANDED_IBOR_FUTURE_1)
        .expirationDate(EXPIRY_DATE).strikePrice(STRIKE_PRICE).isCall(IS_CALL).build();
    assertEquals(option.getExpandedIborFuture(), EXPANDED_IBOR_FUTURE_1);
    assertEquals(option.getExpirationDate(), EXPIRY_DATE);
    assertEquals(option.getStrikePrice(), STRIKE_PRICE);
    assertEquals(option.isIsCall(), IS_CALL);
    assertEquals(option.expand(), option);
  }

  /**
   * Expiry should not be after last trade. 
   */
  public void lateExpiryTest() {
    ExpandedIborFuture iborFutureUSD = ExpandedIborFuture.builder().currency(USD).notional(NOTIONAL_2)
        .accrualFactor(ACCRUAL_FACTOR_3M).rate(RATE_OBS_USD).build();
    assertThrowsIllegalArg(() -> ExpandedIborFutureOption.builder().expandedIborFuture(iborFutureUSD)
        .expirationDate(EXPIRY_DATE).strikePrice(STRIKE_PRICE).isCall(IS_CALL).build());
  }

  /**
   * Coverage test. 
   */
  public void coverageTest() {
    ExpandedIborFutureOption option1 = ExpandedIborFutureOption.builder().expandedIborFuture(EXPANDED_IBOR_FUTURE_1)
        .expirationDate(EXPIRY_DATE).strikePrice(STRIKE_PRICE).isCall(IS_CALL).build();
    coverImmutableBean(option1);
    ExpandedIborFutureOption option2 = ExpandedIborFutureOption.builder().expandedIborFuture(EXPANDED_IBOR_FUTURE_2)
        .expirationDate(LAST_TRADE_DATE_1).strikePrice(STRIKE_PRICE).isCall(!IS_CALL).build();
    coverBeanEquals(option1, option2);
  }

  /**
   * Serialization Test.
   */
  public void serializationTest() {
    ExpandedIborFutureOption option = ExpandedIborFutureOption.builder().expandedIborFuture(EXPANDED_IBOR_FUTURE_1)
        .expirationDate(EXPIRY_DATE).strikePrice(STRIKE_PRICE).isCall(IS_CALL).build();
    assertSerialization(option);
  }
}
