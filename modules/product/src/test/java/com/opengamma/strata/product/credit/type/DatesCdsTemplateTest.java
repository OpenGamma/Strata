/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.product.credit.Cds;
import com.opengamma.strata.product.credit.CdsTrade;

/**
 * Test {@link DatesCdsTemplate}.
 */
public class DatesCdsTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final LocalDate START = LocalDate.of(2016, 2, 21);
  private static final LocalDate END = LocalDate.of(2019, 5, 16);
  private static final CdsConvention CONV1 = CdsConventions.EUR_GB_STANDARD;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "BCD");

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    DatesCdsTemplate test = DatesCdsTemplate.of(START, END, CONV1);
    assertThat(test.getStartDate()).isEqualTo(START);
    assertThat(test.getEndDate()).isEqualTo(END);
    assertThat(test.getConvention()).isEqualTo(CONV1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    DatesCdsTemplate base = DatesCdsTemplate.of(START, END, CONV1);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    CdsTrade test = base.createTrade(LEGAL_ENTITY, tradeDate, BUY, NOTIONAL_2M, 0.05d, REF_DATA);
    Cds expected = Cds.of(BUY, LEGAL_ENTITY, CONV1.getCurrency(), NOTIONAL_2M, START, END, Frequency.P3M,
        CONV1.getSettlementDateOffset().getCalendar(), 0.05d);
    PeriodicSchedule sch1 = expected.getPaymentSchedule();
    expected = expected.toBuilder()
        .paymentSchedule(sch1.toBuilder()
            .startDateBusinessDayAdjustment(sch1.getBusinessDayAdjustment())
            .rollConvention(RollConventions.DAY_20)
            .build())
        .build();
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
    assertThat(test.getUpfrontFee()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_createTrade_withFee() {
    DatesCdsTemplate base = DatesCdsTemplate.of(START, END, CONV1);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    AdjustablePayment payment =
        AdjustablePayment.of(EUR, NOTIONAL_2M, CONV1.getSettlementDateOffset().adjust(tradeDate, REF_DATA));
    CdsTrade test = base.createTrade(LEGAL_ENTITY, tradeDate, BUY, NOTIONAL_2M, 0.05d, payment, REF_DATA);
    Cds expected = Cds.of(BUY, LEGAL_ENTITY, CONV1.getCurrency(), NOTIONAL_2M, START, END, Frequency.P3M,
        CONV1.getSettlementDateOffset().getCalendar(), 0.05d);
    PeriodicSchedule sch1 = expected.getPaymentSchedule();
    expected = expected.toBuilder()
        .paymentSchedule(sch1.toBuilder()
            .startDateBusinessDayAdjustment(sch1.getBusinessDayAdjustment())
            .rollConvention(RollConventions.DAY_20)
            .build())
        .build();
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getUpfrontFee()).isEqualTo(Optional.of(payment));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DatesCdsTemplate test1 = DatesCdsTemplate.of(START, END, CONV1);
    coverImmutableBean(test1);
    DatesCdsTemplate test2 =
        DatesCdsTemplate.of(LocalDate.of(2015, 5, 20), LocalDate.of(2025, 6, 20), CdsConventions.USD_STANDARD);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    DatesCdsTemplate test = DatesCdsTemplate.of(START, END, CONV1);
    assertSerialization(test);
  }

}
