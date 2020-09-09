/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.product.index.OvernightFutureTrade;

/**
 * Tests {@link OvernightFutureTemplate}.
 */
public class OvernightFutureTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final OvernightFutureContractSpec SPEC = OvernightFutureContractSpecs.USD_SOFR_3M_IMM_CME;
  private static final OvernightFutureContractSpec SPEC2 = OvernightFutureContractSpecs.GBP_SONIA_3M_IMM_ICE;
  private static final YearMonth YEAR_MONTH = YearMonth.of(2016, 6);
  private static final SequenceDate SEQ_DATE = SequenceDate.base(YearMonth.of(2016, 6));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    OvernightFutureTemplate test = OvernightFutureTemplate.of(SEQ_DATE, SPEC);
    assertThat(test.getSequenceDate()).isEqualTo(SEQ_DATE);
    assertThat(test.getContractSpec()).isEqualTo(SPEC);
    assertThat(test.getIndex()).isEqualTo(SPEC.getIndex());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    OvernightFutureTemplate base = OvernightFutureTemplate.of(SEQ_DATE, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    double quantity = 3;
    double price = 0.99;
    SecurityId secId = SecurityId.of("OG-Future", "GBP-LIBOR-3M-Jun16");
    OvernightFutureTrade trade = base.createTrade(date, secId, quantity, price, REF_DATA);
    OvernightFutureTrade expected = SPEC.createTrade(date, secId, SEQ_DATE, quantity, price, REF_DATA);
    assertThat(trade).isEqualTo(expected);
  }

  @Test
  public void test_calculateReferenceDateFromTradeDate() {
    OvernightFutureTemplate base = OvernightFutureTemplate.of(SEQ_DATE, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    LocalDate expected = LocalDate.of(2016, 6, 15);
    assertThat(base.calculateReferenceDateFromTradeDate(date, REF_DATA)).isEqualTo(expected);
  }

  @Test
  public void test_calculateLastFixingDateFromTradeDate() {
    OvernightFutureTemplate base = OvernightFutureTemplate.of(SEQ_DATE, SPEC);
    LocalDate date = LocalDate.of(2015, 10, 20);
    LocalDate expected = LocalDate.of(2016, 9, 20);
    assertThat(base.calculateLastFixingDateFromTradeDate(date, REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightFutureTemplate test = OvernightFutureTemplate.of(SEQ_DATE, SPEC);
    coverImmutableBean(test);
    OvernightFutureTemplate test2 = OvernightFutureTemplate.of(SequenceDate.full(YEAR_MONTH), SPEC2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightFutureTemplate test = OvernightFutureTemplate.of(SEQ_DATE, SPEC);
    assertSerialization(test);
  }

}
