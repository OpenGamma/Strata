/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_2M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link ResolvedIborFuture}.
 */
@Test
public class ResolvedIborFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFuture PRODUCT = IborFutureTest.sut();
  private static final IborFuture PRODUCT2 = IborFutureTest.sut2();
  private static final double NOTIONAL = 1_000d;
  private static final double ACCRUAL_FACTOR_2M = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 15);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "IborFuture");

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFuture test = sut();
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
    assertEquals(test.getNotional(), PRODUCT.getNotional());
    assertEquals(test.getAccrualFactor(), PRODUCT.getAccrualFactor());
    assertEquals(test.getLastTradeDate(), PRODUCT.getLastTradeDate());
    assertEquals(test.getIndex(), PRODUCT.getIndex());
    assertEquals(test.getRounding(), PRODUCT.getRounding());
    assertEquals(test.getIborRate(), IborRateComputation.of(PRODUCT.getIndex(), PRODUCT.getLastTradeDate(), REF_DATA));
  }

  public void test_builder_defaults() {
    ResolvedIborFuture test = ResolvedIborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(GBP)
        .notional(NOTIONAL)
        .iborRate(IborRateComputation.of(GBP_LIBOR_2M, LAST_TRADE_DATE, REF_DATA))
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR_2M);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getIndex(), GBP_LIBOR_2M);
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getIborRate(), IborRateComputation.of(GBP_LIBOR_2M, LAST_TRADE_DATE, REF_DATA));
  }

  public void test_builder_noObservation() {
    assertThrowsIllegalArg(() -> ResolvedIborFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(GBP)
        .rounding(ROUNDING)
        .build());
  }

  public void test_builder_noCurrency() {
    ResolvedIborFuture test = ResolvedIborFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .iborRate(IborRateComputation.of(GBP_LIBOR_2M, LAST_TRADE_DATE, REF_DATA))
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
    return PRODUCT.resolve(REF_DATA);
  }

  static ResolvedIborFuture sut2() {
    return PRODUCT2.resolve(REF_DATA);
  }

}
