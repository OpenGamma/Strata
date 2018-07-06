/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
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
import com.opengamma.strata.product.rate.OvernightRateComputation;
import com.opengamma.strata.product.swap.OvernightAccrualMethod;

/**
 * Test {@link OvernightFuture}.
 */
@Test
public class OvernightFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL = 5_000_000d;
  private static final double NOTIONAL2 = 10_000_000d;
  private static final double ACCRUAL_FACTOR = TENOR_1M.getPeriod().toTotalMonths() / 12.0;
  private static final double ACCRUAL_FACTOR2 = TENOR_3M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2018, 9, 28);
  private static final LocalDate START_DATE = date(2018, 9, 1);
  private static final LocalDate END_DATE = date(2018, 9, 30);
  private static final LocalDate LAST_TRADE_DATE2 = date(2018, 6, 15);
  private static final LocalDate START_DATE2 = date(2018, 3, 15);
  private static final LocalDate END_DATE2 = date(2018, 6, 15);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(5);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "OnFuture");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "OnFuture2");

  //-------------------------------------------------------------------------
  public void test_builder() {
    OvernightFuture test = sut();
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getIndex(), USD_FED_FUND);
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getAccrualMethod(), OvernightAccrualMethod.AVERAGED_DAILY);
  }

  public void test_builder_default() {
    OvernightFuture test = OvernightFuture.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .index(USD_FED_FUND)
        .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
        .build();
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getIndex(), USD_FED_FUND);
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getStartDate(), START_DATE);
    assertEquals(test.getEndDate(), END_DATE);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getAccrualMethod(), OvernightAccrualMethod.AVERAGED_DAILY);
  }

  public void test_builder_noIndex() {
    assertThrowsIllegalArg(() -> OvernightFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
        .rounding(ROUNDING)
        .build());
  }

  public void test_builder_wrongDateOrderDate() {
    assertThrowsIllegalArg(() -> OvernightFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .startDate(END_DATE)
        .endDate(START_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .index(USD_FED_FUND)
        .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
        .rounding(ROUNDING)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    OvernightFuture base = sut();
    ResolvedOvernightFuture expected = ResolvedOvernightFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .overnightRate(OvernightRateComputation.of(
            USD_FED_FUND, START_DATE, END_DATE, 0, OvernightAccrualMethod.AVERAGED_DAILY, REF_DATA))
        .lastTradeDate(LAST_TRADE_DATE)
        .rounding(ROUNDING)
        .build();
    assertEquals(base.resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightFuture test1 = sut();
    coverImmutableBean(test1);
    OvernightFuture test2 = sut2();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    OvernightFuture test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  static OvernightFuture sut() {
    return OvernightFuture.builder()
        .securityId(SECURITY_ID)
        .currency(USD)
        .notional(NOTIONAL)
        .accrualFactor(ACCRUAL_FACTOR)
        .startDate(START_DATE)
        .endDate(END_DATE)
        .lastTradeDate(LAST_TRADE_DATE)
        .index(USD_FED_FUND)
        .accrualMethod(OvernightAccrualMethod.AVERAGED_DAILY)
        .rounding(ROUNDING)
        .build();
  }

  static OvernightFuture sut2() {
    return OvernightFuture.builder()
        .securityId(SECURITY_ID2)
        .currency(GBP)
        .notional(NOTIONAL2)
        .accrualFactor(ACCRUAL_FACTOR2)
        .startDate(START_DATE2)
        .endDate(END_DATE2)
        .lastTradeDate(LAST_TRADE_DATE2)
        .index(GBP_SONIA)
        .accrualMethod(OvernightAccrualMethod.COMPOUNDED)
        .rounding(Rounding.none())
        .build();
  }

}
