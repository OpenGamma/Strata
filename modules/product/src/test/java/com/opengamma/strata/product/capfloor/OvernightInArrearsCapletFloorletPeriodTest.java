/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;

/**
 * Test {@link OvernightInArrearsCapletFloorletPeriod}.
 */
public class OvernightInArrearsCapletFloorletPeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double STRIKE = 0.04;
  private static final LocalDate START_UNADJ = LocalDate.of(2010, 10, 8);
  private static final LocalDate END_UNADJ = LocalDate.of(2011, 1, 8);
  private static final LocalDate START = LocalDate.of(2010, 10, 8);
  private static final LocalDate END = LocalDate.of(2011, 1, 10);
  private static final LocalDate PAYMENT = LocalDate.of(2011, 1, 13);
  private static final double NOTIONAL = 1.e6;
  private static final OvernightCompoundedRateComputation RATE_COMP =
      OvernightCompoundedRateComputation.of(GBP_SONIA, START, END, REF_DATA);
  private static final double YEAR_FRACTION = 0.251d; 

  @Test
  public void test_builder_min() {
    OvernightInArrearsCapletFloorletPeriod test = OvernightInArrearsCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .yearFraction(YEAR_FRACTION)
        .caplet(STRIKE)
        .overnightRate(RATE_COMP)
        .build();
    assertThat(test.getCaplet().getAsDouble()).isEqualTo(STRIKE);
    assertThat(test.getFloorlet()).isNotPresent();
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getStartDate()).isEqualTo(START);
    assertThat(test.getEndDate()).isEqualTo(END);
    assertThat(test.getPaymentDate()).isEqualTo(test.getEndDate());
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getOvernightRate()).isEqualTo(RATE_COMP);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getPutCall()).isEqualTo(PutCall.CALL);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
  }

  @Test
  public void test_builder_full() {
    OvernightInArrearsCapletFloorletPeriod test = OvernightInArrearsCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJ)
        .unadjustedEndDate(END_UNADJ)
        .paymentDate(PAYMENT)
        .yearFraction(YEAR_FRACTION)
        .currency(EUR)
        .floorlet(STRIKE)
        .overnightRate(RATE_COMP)
        .build();
    assertThat(test.getFloorlet().getAsDouble()).isEqualTo(STRIKE);
    assertThat(test.getCaplet()).isNotPresent();
    assertThat(test.getStrike()).isEqualTo(STRIKE);
    assertThat(test.getStartDate()).isEqualTo(START);
    assertThat(test.getEndDate()).isEqualTo(END);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START_UNADJ);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END_UNADJ);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT);
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getOvernightRate()).isEqualTo(RATE_COMP);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getPutCall()).isEqualTo(PutCall.PUT);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
  }

  @Test
  public void test_builder_fail() {
    // rate observation missing
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightInArrearsCapletFloorletPeriod.builder()
            .notional(NOTIONAL)
            .caplet(STRIKE)
            .build());
    // cap and floor missing
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightInArrearsCapletFloorletPeriod.builder()
            .notional(NOTIONAL)
            .overnightRate(RATE_COMP)
            .build());
    // cap and floor present
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightInArrearsCapletFloorletPeriod.builder()
            .notional(NOTIONAL)
            .caplet(STRIKE)
            .floorlet(STRIKE)
            .overnightRate(RATE_COMP)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightInArrearsCapletFloorletPeriod test = OvernightInArrearsCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .yearFraction(YEAR_FRACTION)
        .caplet(STRIKE)
        .overnightRate(RATE_COMP)
        .build();
    coverImmutableBean(test);
    OvernightInArrearsCapletFloorletPeriod test2 = OvernightInArrearsCapletFloorletPeriod.builder()
        .notional(2 * NOTIONAL)
        .startDate(START.plusDays(1))
        .endDate(END.plusDays(1))
        .yearFraction(1.5 * YEAR_FRACTION)
        .caplet(0.5 * STRIKE)
        .overnightRate(OvernightCompoundedRateComputation.of(OvernightIndices.AUD_AONIA, START, END, REF_DATA))
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightInArrearsCapletFloorletPeriod test = OvernightInArrearsCapletFloorletPeriod.builder()
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .yearFraction(YEAR_FRACTION)
        .caplet(STRIKE)
        .overnightRate(RATE_COMP)
        .build();
    assertSerialization(test);
  }

}
