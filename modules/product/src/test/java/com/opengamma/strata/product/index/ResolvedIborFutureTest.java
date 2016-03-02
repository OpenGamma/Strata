/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Test {@link ResolvedIborFuture}.
 */
@Test
public class ResolvedIborFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_1 = 1_000d;
  private static final double NOTIONAL_2 = 2_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR_3M = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE_1 = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE_2 = date(2015, 3, 15);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFuture test = ResolvedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .accrualFactor(ACCRUAL_FACTOR_2M)
        .observation(IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1, REF_DATA))
        .rounding(ROUNDING)
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), NOTIONAL_1);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR_2M);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE_1);
    assertEquals(test.getIndex(), GBP_LIBOR_2M);
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getObservation(), IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1, REF_DATA));
  }

  public void test_builder_defaults() {
    ResolvedIborFuture test = ResolvedIborFuture.builder()
        .currency(GBP)
        .observation(IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1, REF_DATA))
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), 0.0);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR_2M);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE_1);
    assertEquals(test.getIndex(), GBP_LIBOR_2M);
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getObservation(), IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1, REF_DATA));
  }

  public void test_builder_noObservation() {
    assertThrowsIllegalArg(() -> ResolvedIborFuture.builder()
        .notional(NOTIONAL_1)
        .currency(GBP)
        .rounding(ROUNDING)
        .build());
  }

  public void test_builder_noCurrency() {
    ResolvedIborFuture test = ResolvedIborFuture.builder()
        .notional(NOTIONAL_1)
        .observation(IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1, REF_DATA))
        .rounding(ROUNDING)
        .build();
    assertEquals(GBP, test.getCurrency());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedIborFuture sut() {
    return ResolvedIborFuture.builder()
        .currency(GBP)
        .notional(NOTIONAL_1)
        .observation(IborRateObservation.of(GBP_LIBOR_2M, LAST_TRADE_DATE_1, REF_DATA))
        .rounding(ROUNDING)
        .build();
  }

  static ResolvedIborFuture sut2() {
    return ResolvedIborFuture.builder()
        .currency(USD)
        .notional(NOTIONAL_2)
        .accrualFactor(ACCRUAL_FACTOR_3M)
        .observation(IborRateObservation.of(USD_LIBOR_3M, LAST_TRADE_DATE_2, REF_DATA))
        .rounding(ROUNDING)
        .build();
  }

}
