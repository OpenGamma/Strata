/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;

/**
 * Test {@link ResolvedOvernightInArrearsCapFloor}.
 */
public class ResolvedOvernightInArrearsCapFloorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double STRIKE = 0.0125;
  private static final double NOTIONAL = 1.0e6;
  private static final OvernightInArrearsCapletFloorletPeriod PERIOD_1 = OvernightInArrearsCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 3, 17))
      .endDate(LocalDate.of(2011, 6, 17))
      .unadjustedStartDate(LocalDate.of(2011, 3, 17))
      .unadjustedEndDate(LocalDate.of(2011, 6, 17))
      .paymentDate(LocalDate.of(2011, 6, 21))
      .overnightRate(OvernightCompoundedRateComputation.of(
          EUR_ESTR,
          LocalDate.of(2011, 3, 17),
          LocalDate.of(2011, 6, 17),
          REF_DATA))
      .yearFraction(0.2556)
      .build();
  private static final OvernightInArrearsCapletFloorletPeriod PERIOD_2 = OvernightInArrearsCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 6, 17))
      .endDate(LocalDate.of(2011, 9, 19))
      .unadjustedStartDate(LocalDate.of(2011, 6, 17))
      .unadjustedEndDate(LocalDate.of(2011, 9, 17))
      .paymentDate(LocalDate.of(2011, 9, 21))
      .overnightRate(OvernightCompoundedRateComputation.of(
          EUR_ESTR,
          LocalDate.of(2011, 6, 17),
          LocalDate.of(2011, 9, 17),
          REF_DATA))
      .yearFraction(0.2611)
      .build();
  private static final OvernightInArrearsCapletFloorletPeriod PERIOD_3 = OvernightInArrearsCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 9, 19))
      .endDate(LocalDate.of(2011, 12, 19))
      .unadjustedStartDate(LocalDate.of(2011, 9, 17))
      .unadjustedEndDate(LocalDate.of(2011, 12, 17))
      .paymentDate(LocalDate.of(2011, 12, 21))
      .overnightRate(OvernightCompoundedRateComputation.of(
          EUR_ESTR,
          LocalDate.of(2011, 9, 17),
          LocalDate.of(2011, 12, 17),
          REF_DATA))
      .yearFraction(0.2528)
      .build();
  private static final OvernightInArrearsCapletFloorletPeriod PERIOD_4 = OvernightInArrearsCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 12, 19))
      .endDate(LocalDate.of(2012, 3, 19))
      .unadjustedStartDate(LocalDate.of(2011, 12, 17))
      .unadjustedEndDate(LocalDate.of(2012, 3, 17))
      .paymentDate(LocalDate.of(2012, 3, 21))
      .overnightRate(OvernightCompoundedRateComputation.of(
          EUR_ESTR,
          LocalDate.of(2011, 12, 17),
          LocalDate.of(2012, 3, 17),
          REF_DATA))
      .yearFraction(0.2528)
      .build();
  static final ResolvedOvernightInArrearsCapFloorLeg CAPFLOOR_LEG = ResolvedOvernightInArrearsCapFloorLeg.builder()
      .capletFloorletPeriods(PERIOD_1, PERIOD_2, PERIOD_3, PERIOD_4)
      .payReceive(RECEIVE)
      .build();

  private static final double RATE = 0.015;
  private static final RatePaymentPeriod PAY_PERIOD_1 = RatePaymentPeriod.builder()
      .paymentDate(LocalDate.of(2011, 9, 21))
      .accrualPeriods(RateAccrualPeriod.builder()
          .startDate(LocalDate.of(2011, 3, 17))
          .endDate(LocalDate.of(2011, 9, 19))
          .yearFraction(0.517)
          .rateComputation(FixedRateComputation.of(RATE))
          .build())
      .dayCount(ACT_365F)
      .currency(EUR)
      .notional(-NOTIONAL)
      .build();
  private static final RatePaymentPeriod PAY_PERIOD_2 = RatePaymentPeriod.builder()
      .paymentDate(LocalDate.of(2012, 3, 21))
      .accrualPeriods(RateAccrualPeriod.builder()
          .startDate(LocalDate.of(2011, 9, 19))
          .endDate(LocalDate.of(2012, 3, 19))
          .yearFraction(0.505)
          .rateComputation(FixedRateComputation.of(RATE))
          .build())
      .dayCount(ACT_365F)
      .currency(EUR)
      .notional(-NOTIONAL)
      .build();
  static final ResolvedSwapLeg PAY_LEG = ResolvedSwapLeg.builder()
      .paymentPeriods(PAY_PERIOD_1, PAY_PERIOD_2)
      .type(FIXED)
      .payReceive(PAY)
      .build();

  //-------------------------------------------------------------------------
  @Test
  public void test_of_oneLeg() {
    ResolvedOvernightInArrearsCapFloor test = ResolvedOvernightInArrearsCapFloor.of(CAPFLOOR_LEG);
    assertThat(test.getCapFloorLeg()).isEqualTo(CAPFLOOR_LEG);
    assertThat(test.getPayLeg()).isNotPresent();
    assertThat(test.allPaymentCurrencies()).containsOnly(EUR);
    assertThat(test.allIndices()).containsOnly(EUR_ESTR);
  }

  @Test
  public void test_of_twoLegs() {
    ResolvedOvernightInArrearsCapFloor test = ResolvedOvernightInArrearsCapFloor.of(CAPFLOOR_LEG, PAY_LEG);
    assertThat(test.getCapFloorLeg()).isEqualTo(CAPFLOOR_LEG);
    assertThat(test.getPayLeg().get()).isEqualTo(PAY_LEG);
    assertThat(test.allPaymentCurrencies()).containsOnly(EUR);
    assertThat(test.allIndices()).containsOnly(EUR_ESTR);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ResolvedOvernightInArrearsCapFloor test1 = ResolvedOvernightInArrearsCapFloor.of(CAPFLOOR_LEG, PAY_LEG);
    coverImmutableBean(test1);
    ResolvedOvernightInArrearsCapFloorLeg capFloor = ResolvedOvernightInArrearsCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_1)
        .payReceive(PAY)
        .build();
    ResolvedOvernightInArrearsCapFloor test2 = ResolvedOvernightInArrearsCapFloor.of(capFloor);
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    ResolvedOvernightInArrearsCapFloor test = ResolvedOvernightInArrearsCapFloor.of(CAPFLOOR_LEG);
    assertSerialization(test);
  }

}
