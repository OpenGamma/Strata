/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
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
 * Test {@link ResolvedOvernightFuture}.
 */
@Test
public class ResolvedOvernightFutureTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL = 1_000_000d;
  private static final double ACCRUAL_FACTOR_1M = TENOR_1M.getPeriod().toTotalMonths() / 12.0;
  private static final LocalDate LAST_TRADE_DATE = date(2018, 6, 29);
  private static final LocalDate START_DATE = date(2018, 6, 1);
  private static final LocalDate END_DATE = date(2018, 6, 29);
  private static final Rounding ROUNDING = Rounding.ofDecimalPlaces(6);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "OnFuture");
  private static final OvernightRateComputation RATE_COMPUTATION = OvernightRateComputation.of(
      USD_FED_FUND, START_DATE, END_DATE, 0, OvernightAccrualMethod.AVERAGED_DAILY, REF_DATA);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedOvernightFuture test = ResolvedOvernightFuture.builder()
        .currency(USD)
        .accrualFactor(ACCRUAL_FACTOR_1M)
        .lastTradeDate(LAST_TRADE_DATE)
        .overnightRate(RATE_COMPUTATION)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .securityId(SECURITY_ID)
        .build();
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR_1M);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getIndex(), USD_FED_FUND);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getOvernightRate(), RATE_COMPUTATION);
    assertEquals(test.getRounding(), ROUNDING);
    assertEquals(test.getSecurityId(), SECURITY_ID);
  }

  public void test_builder_default() {
    ResolvedOvernightFuture test = ResolvedOvernightFuture.builder()
        .accrualFactor(ACCRUAL_FACTOR_1M)
        .lastTradeDate(LAST_TRADE_DATE)
        .overnightRate(RATE_COMPUTATION)
        .notional(NOTIONAL)
        .securityId(SECURITY_ID)
        .build();
    assertEquals(test.getAccrualFactor(), ACCRUAL_FACTOR_1M);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getIndex(), USD_FED_FUND);
    assertEquals(test.getLastTradeDate(), LAST_TRADE_DATE);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getOvernightRate(), RATE_COMPUTATION);
    assertEquals(test.getRounding(), Rounding.none());
    assertEquals(test.getSecurityId(), SECURITY_ID);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ResolvedOvernightFuture test1 = ResolvedOvernightFuture.builder()
        .currency(USD)
        .accrualFactor(ACCRUAL_FACTOR_1M)
        .lastTradeDate(LAST_TRADE_DATE)
        .overnightRate(RATE_COMPUTATION)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .securityId(SECURITY_ID)
        .build();
    coverImmutableBean(test1);
    ResolvedOvernightFuture test2 = ResolvedOvernightFuture.builder()
        .currency(GBP)
        .accrualFactor(0.25)
        .lastTradeDate(date(2018, 9, 28))
        .overnightRate(OvernightRateComputation.of(
            GBP_SONIA, date(2018, 9, 1), date(2018, 9, 30), 0, OvernightAccrualMethod.AVERAGED_DAILY, REF_DATA))
        .notional(1.0e8)
        .securityId(SecurityId.of("OG-Test", "OnFuture2"))
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ResolvedOvernightFuture test = ResolvedOvernightFuture.builder()
        .currency(USD)
        .accrualFactor(ACCRUAL_FACTOR_1M)
        .lastTradeDate(LAST_TRADE_DATE)
        .overnightRate(RATE_COMPUTATION)
        .notional(NOTIONAL)
        .rounding(ROUNDING)
        .securityId(SECURITY_ID)
        .build();
    assertSerialization(test);
  }

}
