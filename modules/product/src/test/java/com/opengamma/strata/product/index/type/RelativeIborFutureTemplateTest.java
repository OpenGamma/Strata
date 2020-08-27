/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index.type;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Tests {@link RelativeIborFutureTemplate}.
 */
public class RelativeIborFutureTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFutureContractSpec SPEC = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
  private static final IborFutureContractSpec SPEC2 = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME_SERIAL;
  private static final Period MIN_PERIOD = Period.ofMonths(2);
  private static final int NUMBER = 2;

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    RelativeIborFutureTemplate test = RelativeIborFutureTemplate.of(MIN_PERIOD, NUMBER, SPEC);
    assertThat(test.getMinimumPeriod()).isEqualTo(MIN_PERIOD);
    assertThat(test.getSequenceNumber()).isEqualTo(NUMBER);
    assertThat(test.getContractSpec()).isEqualTo(SPEC);
    assertThat(test.getIndex()).isEqualTo(SPEC.getIndex());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double quantity = 3;
    double price = 0.99;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    IborFutureTrade trade = base.createTrade(date, secId, quantity, price, REF_DATA);
    IborFutureTrade expected = SPEC.createTrade(date, secId, MIN_PERIOD, NUMBER, quantity, price, REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_calculateReferenceDateFromTradeDate() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);  // 2nd Quarterly IMM at least 2 months later from this date
    LocalDate expected = LocalDate.of(2016, 6, 15);  // 1st is March 2016, 2nd is Jun 2016
    assertThat(base.calculateReferenceDateFromTradeDate(date, REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_approximateMaturity() {
    IborFutureTemplate base = IborFutureTemplate.of(MIN_PERIOD, NUMBER, SPEC);
    assertThat(base.approximateMaturity(LocalDate.of(2015, 10, 20))).isCloseTo(0.5d, offset(0.1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    RelativeIborFutureTemplate test = RelativeIborFutureTemplate.of(MIN_PERIOD, NUMBER, SPEC);
    coverImmutableBean(test);
    RelativeIborFutureTemplate test2 = RelativeIborFutureTemplate.of(Period.ofMonths(3), NUMBER + 1, SPEC2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    RelativeIborFutureTemplate test = RelativeIborFutureTemplate.of(MIN_PERIOD, NUMBER, SPEC);
    assertSerialization(test);
  }

}
