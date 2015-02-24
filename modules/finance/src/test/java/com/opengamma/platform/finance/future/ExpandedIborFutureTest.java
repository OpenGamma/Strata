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
 * 
 */
@Test
public class ExpandedIborFutureTest {
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

  private static final double TOL = 1.0e-13;

  /**
   * Test builder.
   */
  public void builderTest() {
    ExpandedIborFuture expIborFuture = ExpandedIborFuture.builder().currency(GBP).notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M).rate(RATE_OBS_GBP).roundingDecimalPlaces(ROUNDING).build();
    assertEquals(NOTIONAL_1, expIborFuture.getNotional());
    assertEquals(GBP, expIborFuture.getCurrency());
    assertEquals(ACCRUAL_FACTOR_2M, expIborFuture.getAccrualFactor());
    assertEquals(ROUNDING, expIborFuture.getRoundingDecimalPlaces());
    assertEquals(RATE_OBS_GBP, expIborFuture.getRate());
    ExpandedIborFuture expIborFutureRe = expIborFuture.expand(); // returns itself
    assertEquals(expIborFuture, expIborFutureRe);
  }

  /**
   * Accrual factor is not set, computed from tenor of index. 
   */
  public void noAccFactorTest() {
    ExpandedIborFuture expIborFuture = ExpandedIborFuture.builder().currency(GBP).notional(NOTIONAL_1)
        .rate(RATE_OBS_GBP).roundingDecimalPlaces(ROUNDING).build();
    assertEquals(ACCRUAL_FACTOR_2M, expIborFuture.getAccrualFactor(), TOL);
  }

  /**
   * Notioanl is not set, 0.0 is used by default. 
   */
  public void noNotionalTest() {
    ExpandedIborFuture expIborFuture = ExpandedIborFuture.builder().currency(GBP).accrualFactor(ACCRUAL_FACTOR_2M)
        .rate(RATE_OBS_GBP).roundingDecimalPlaces(ROUNDING).build();
    assertEquals(0.0, expIborFuture.getNotional());
  }

  /**
   * Currency is not set, exception is thrown. 
   */
  public void noCurrencyTest() {
    assertThrowsIllegalArg(() -> ExpandedIborFuture.builder().notional(NOTIONAL_1).accrualFactor(ACCRUAL_FACTOR_2M)
        .rate(RATE_OBS_GBP).roundingDecimalPlaces(ROUNDING).build());
  }

  /**
   * rate is not set, exception is thrown. 
   */
  public void noRateTest() {
    assertThrowsIllegalArg(() -> ExpandedIborFuture.builder().currency(GBP).notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M).roundingDecimalPlaces(ROUNDING).build());
  }

  /**
   * rate and accrual factor are not set, exception is thrown. 
   */
  public void noRateAccFactorTest() {
    assertThrowsIllegalArg(() -> ExpandedIborFuture.builder().currency(GBP).notional(NOTIONAL_1)
        .roundingDecimalPlaces(ROUNDING).build());
  }

  /**
   * rate is not set, accrual factor is not computed because index is week-based. Thus exception is thrown. 
   */
  public void noAccFactorWeekIborTest() {
    assertThrowsIllegalArg(() -> ExpandedIborFuture.builder().currency(GBP).notional(NOTIONAL_1)
        .rate(RATE_OBS_GBP_1W).roundingDecimalPlaces(ROUNDING).build());
  }

  /**
   * Coverage test. 
   */
  public void coverageTest() {
    ExpandedIborFuture expIborFuture1 = ExpandedIborFuture.builder().currency(GBP).notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M).rate(RATE_OBS_GBP).roundingDecimalPlaces(ROUNDING).build();
    coverImmutableBean(expIborFuture1);
    ExpandedIborFuture expIborFuture2 = ExpandedIborFuture.builder().currency(USD).notional(NOTIONAL_2)
        .accrualFactor(ACCRUAL_FACTOR_3M).rate(RATE_OBS_USD).build();
    coverBeanEquals(expIborFuture1, expIborFuture2);
  }

  /**
   * Serialization test. 
   */
  public void serializationTest() {
    ExpandedIborFuture expIborFuture = ExpandedIborFuture.builder().currency(GBP).notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M).rate(RATE_OBS_GBP).roundingDecimalPlaces(ROUNDING).build();
    assertSerialization(expIborFuture);
  }
}
