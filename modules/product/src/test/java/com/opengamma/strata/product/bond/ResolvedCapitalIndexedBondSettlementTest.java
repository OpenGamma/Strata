/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test {@link ResolvedCapitalIndexedBondSettlement}. 
 */
public class ResolvedCapitalIndexedBondSettlementTest {

  private static final LocalDate SETTLE_DATE = date(2018, 6, 1);
  private static final LocalDate SETTLE_DATE2 = date(2018, 6, 2);
  private static final double PRICE = 99.2;
  private static final double PRICE2 = 99.5;
  private static final BondPaymentPeriod SETTLE_PERIOD =
      KnownAmountBondPaymentPeriod.of(
          Payment.of(Currency.GBP, 100, SETTLE_DATE),
          SchedulePeriod.of(SETTLE_DATE.minusMonths(1), SETTLE_DATE));
  private static final BondPaymentPeriod SETTLE_PERIOD2 =
      KnownAmountBondPaymentPeriod.of(
          Payment.of(Currency.GBP, 200, SETTLE_DATE2),
          SchedulePeriod.of(SETTLE_DATE2.minusMonths(1), SETTLE_DATE2));

  @Test
  public void test_of() {
    ResolvedCapitalIndexedBondSettlement test = sut();
    assertThat(test.getSettlementDate()).isEqualTo(SETTLE_DATE);
    assertThat(test.getPrice()).isEqualTo(PRICE);
    assertThat(test.getPayment()).isEqualTo(SETTLE_PERIOD);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedCapitalIndexedBondSettlement sut() {
    return ResolvedCapitalIndexedBondSettlement.of(SETTLE_DATE, PRICE, SETTLE_PERIOD);
  }

  static ResolvedCapitalIndexedBondSettlement sut2() {
    return ResolvedCapitalIndexedBondSettlement.of(SETTLE_DATE2, PRICE2, SETTLE_PERIOD2);
  }

}
