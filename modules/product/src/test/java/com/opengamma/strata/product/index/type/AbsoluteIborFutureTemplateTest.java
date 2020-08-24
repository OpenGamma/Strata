/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Tests {@link AbsoluteIborFutureTemplate}.
 */
public class AbsoluteIborFutureTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFutureContractSpec SPEC = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
  private static final IborFutureContractSpec SPEC2 = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME_SERIAL;
  private static final YearMonth YEAR_MONTH = YearMonth.of(2016, 6);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    AbsoluteIborFutureTemplate test = AbsoluteIborFutureTemplate.of(YEAR_MONTH, SPEC);
    assertThat(test.getYearMonth()).isEqualTo(YEAR_MONTH);
    assertThat(test.getContractSpec()).isEqualTo(SPEC);
    assertThat(test.getIndex()).isEqualTo(SPEC.getIndex());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    IborFutureTemplate base = IborFutureTemplate.of(YEAR_MONTH, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double quantity = 3;
    double price = 0.99;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    IborFutureTrade trade = base.createTrade(date, secId, quantity, price, REF_DATA);
    IborFutureTrade expected = SPEC.createTrade(date, secId, YEAR_MONTH, quantity, price, REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_calculateReferenceDateFromTradeDate() {
    IborFutureTemplate base = IborFutureTemplate.of(YEAR_MONTH, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    LocalDate expected = LocalDate.of(2016, 6, 15);
    assertThat(base.calculateReferenceDateFromTradeDate(date, REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_approximateMaturity() {
    IborFutureTemplate base = IborFutureTemplate.of(YEAR_MONTH, SPEC);
    assertThat(base.approximateMaturity(LocalDate.of(2015, 10, 20))).isCloseTo(8d / 12d, offset(0.1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    AbsoluteIborFutureTemplate test = AbsoluteIborFutureTemplate.of(YEAR_MONTH, SPEC);
    coverImmutableBean(test);
    AbsoluteIborFutureTemplate test2 = AbsoluteIborFutureTemplate.of(YEAR_MONTH.plusMonths(1), SPEC2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    AbsoluteIborFutureTemplate test = AbsoluteIborFutureTemplate.of(YEAR_MONTH, SPEC);
    assertSerialization(test);
  }

}
