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

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.SequenceDate;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.index.IborFutureTrade;

/**
 * Tests {@link IborFutureTemplate}.
 */
public class IborFutureTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborFutureContractSpec SPEC = IborFutureContractSpecs.USD_LIBOR_3M_IMM_CME;
  private static final IborFutureContractSpec SPEC2 = IborFutureContractSpecs.GBP_LIBOR_3M_IMM_ICE;
  private static final YearMonth YEAR_MONTH = YearMonth.of(2016, 6);
  private static final SequenceDate SEQ_DATE = SequenceDate.base(YearMonth.of(2016, 6));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    IborFutureTemplate test = IborFutureTemplate.of(SEQ_DATE, SPEC);
    assertThat(test.getSequenceDate()).isEqualTo(SEQ_DATE);
    assertThat(test.getContractSpec()).isEqualTo(SPEC);
    assertThat(test.getIndex()).isEqualTo(SPEC.getIndex());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    IborFutureTemplate base = IborFutureTemplate.of(SEQ_DATE, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double quantity = 3;
    double price = 0.99;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    IborFutureTrade trade = base.createTrade(date, secId, quantity, price, REF_DATA);
    IborFutureTrade expected = SPEC.createTrade(date, secId, SEQ_DATE, quantity, price, REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_calculateReferenceDateFromTradeDate() {
    IborFutureTemplate base = IborFutureTemplate.of(SEQ_DATE, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    LocalDate expected = LocalDate.of(2016, 6, 15);
    assertThat(base.calculateReferenceDateFromTradeDate(date, REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborFutureTemplate test = IborFutureTemplate.of(SEQ_DATE, SPEC);
    coverImmutableBean(test);
    IborFutureTemplate test2 = IborFutureTemplate.of(SequenceDate.full(YEAR_MONTH), SPEC2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborFutureTemplate test = IborFutureTemplate.of(SEQ_DATE, SPEC);
    assertSerialization(test);
  }

}
