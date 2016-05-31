/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.value.Rounding;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link IborFuture}.
 */
@Test
public class IborFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL = 1_000d;
  private static final double NOTIONAL2 = 2_000d;
  private static final double ACCRUAL_FACTOR = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR2 = TENOR_2M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2015, 6, 15);
  private static final LocalDate LAST_TRADE_DATE2 = date(2015, 6, 18);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "IborFuture");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "IborFuture2");

  //-------------------------------------------------------------------------
  public void test_builder() {
    IborFuture test = sut();
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getIndex(), USD_LIBOR_3M);
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getFixingDate(), LAST_TRADE_DATE);
  }

  public void test_builder_defaults() {
    IborFuture test = IborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(GBP)
        .notional(NOTIONAL)
        .lastTradeDate(LAST_TRADE_DATE)
        .index(GBP_LIBOR_2M)
        .build();
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR2);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getIndex(), GBP_LIBOR_2M);
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getFixingDate(), LAST_TRADE_DATE);
  }

  public void test_builder_noIndex() {
    assertThrowsIllegalArg(() -> IborFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(GBP)
        .lastTradeDate(LAST_TRADE_DATE)
        .rounding(ROUNDING)
        .build());
  }

  public void test_builder_noCurrency() {
    IborFuture test = IborFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .index(GBP_LIBOR_2M)
        .lastTradeDate(LAST_TRADE_DATE)
        .rounding(ROUNDING)
        .build();
    assertEquals(GBP, test.getCurrency());
  }

  public void test_builder_noLastTradeDate() {
    assertThrowsIllegalArg(() -> IborFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(GBP)
        .index(GBP_LIBOR_2M)
        .rounding(ROUNDING)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    IborFuture test = sut();
    ResolvedIborFuture expected = ResolvedIborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .iborRate(IborRateComputation.of(USD_LIBOR_3M, LAST_TRADE_DATE, REF_DATA))
        .rounding(ROUNDING)
        .build();
    assertEquals(test.resolve(REF_DATA), expected);
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
  static IborFuture sut() {
    return IborFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .lastTradeDate(LAST_TRADE_DATE)
        .index(USD_LIBOR_3M)
        .rounding(ROUNDING)
        .build();
  }

  static IborFuture sut2() {
    return IborFuture.builder()
        .securityId(SECURITY_ID2)
        .currency(GBP)
        .notional(NOTIONAL2)
        .accrualFactor(ACCRUAL_FACTOR2)
        .lastTradeDate(LAST_TRADE_DATE2)
        .index(GBP_LIBOR_2M)
        .build();
  }

}
