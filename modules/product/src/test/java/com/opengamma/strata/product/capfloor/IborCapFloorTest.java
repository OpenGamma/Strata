/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Test {@link IborCapFloor}.
 */
@Test
public class IborCapFloorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate START = LocalDate.of(2011, 3, 17);
  private static final LocalDate END = LocalDate.of(2016, 3, 17);
  private static final IborRateCalculation RATE_CALCULATION = IborRateCalculation.of(EUR_EURIBOR_3M);
  private static final Frequency FREQUENCY = Frequency.P3M;
  private static final BusinessDayAdjustment BUSS_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE = PeriodicSchedule.builder()
      .startDate(START)
      .endDate(END)
      .frequency(FREQUENCY)
      .businessDayAdjustment(BUSS_ADJ)
      .build();
  private static final DaysAdjustment PAYMENT_OFFSET = DaysAdjustment.ofBusinessDays(2, EUTA);
  private static final ValueSchedule CAP = ValueSchedule.of(0.0325);
  private static final ValueSchedule NOTIONAL = ValueSchedule.of(1.0e6);
  private static final IborCapFloorLeg CAPFLOOR_LEG = IborCapFloorLeg.builder()
      .calculation(RATE_CALCULATION)
      .capSchedule(CAP)
      .notional(NOTIONAL)
      .paymentDateOffset(PAYMENT_OFFSET)
      .paymentSchedule(SCHEDULE)
      .payReceive(RECEIVE)
      .build();
  private static final SwapLeg PAY_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(SCHEDULE)
      .calculation(
          FixedRateCalculation.of(0.001, ACT_360))
      .paymentSchedule(
          PaymentSchedule.builder().paymentFrequency(FREQUENCY).paymentDateOffset(DaysAdjustment.NONE).build())
      .notionalSchedule(
          NotionalSchedule.of(EUR, NOTIONAL))
      .build();
  private static final SwapLeg PAY_LEG_XCCY = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(SCHEDULE)
      .calculation(
          IborRateCalculation.of(GBP_LIBOR_3M))
      .paymentSchedule(
          PaymentSchedule.builder().paymentFrequency(FREQUENCY).paymentDateOffset(DaysAdjustment.NONE).build())
      .notionalSchedule(
          NotionalSchedule.of(GBP, NOTIONAL))
      .build();

  public void test_of_oneLeg() {
    IborCapFloor test = IborCapFloor.of(CAPFLOOR_LEG);
    assertEquals(test.getCapFloorLeg(), CAPFLOOR_LEG);
    assertEquals(test.getPayLeg().isPresent(), false);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(EUR));
    assertEquals(test.allIndices(), ImmutableSet.of(EUR_EURIBOR_3M));
  }

  public void test_of_twoLegs() {
    IborCapFloor test = IborCapFloor.of(CAPFLOOR_LEG, PAY_LEG);
    assertEquals(test.getCapFloorLeg(), CAPFLOOR_LEG);
    assertEquals(test.getPayLeg().get(), PAY_LEG);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(EUR));
    assertEquals(test.allIndices(), ImmutableSet.of(EUR_EURIBOR_3M));
  }

  public void test_of_twoLegs_xccy() {
    IborCapFloor test = IborCapFloor.of(CAPFLOOR_LEG, PAY_LEG_XCCY);
    assertEquals(test.getCapFloorLeg(), CAPFLOOR_LEG);
    assertEquals(test.getPayLeg().get(), PAY_LEG_XCCY);
    assertEquals(test.allPaymentCurrencies(), ImmutableSet.of(GBP, EUR));
    assertEquals(test.allIndices(), ImmutableSet.of(GBP_LIBOR_3M, EUR_EURIBOR_3M));
  }

  public void test_resolve_oneLeg() {
    IborCapFloor base = IborCapFloor.of(CAPFLOOR_LEG);
    ResolvedIborCapFloor test = base.resolve(REF_DATA);
    assertEquals(test.getCapFloorLeg(), CAPFLOOR_LEG.resolve(REF_DATA));
    assertEquals(test.getPayLeg().isPresent(), false);
  }

  public void test_resolve_twoLegs() {
    IborCapFloor base = IborCapFloor.of(CAPFLOOR_LEG, PAY_LEG);
    ResolvedIborCapFloor test = base.resolve(REF_DATA);
    assertEquals(test.getCapFloorLeg(), CAPFLOOR_LEG.resolve(REF_DATA));
    assertEquals(test.getPayLeg().get(), PAY_LEG.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborCapFloor test1 = IborCapFloor.of(CAPFLOOR_LEG);
    coverImmutableBean(test1);
    IborCapFloorLeg capFloor = IborCapFloorLeg.builder()
        .calculation(RATE_CALCULATION)
        .floorSchedule(CAP)
        .notional(NOTIONAL)
        .paymentDateOffset(PAYMENT_OFFSET)
        .paymentSchedule(SCHEDULE)
        .payReceive(RECEIVE)
        .build();
    IborCapFloor test2 = IborCapFloor.of(capFloor, PAY_LEG);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborCapFloor test = IborCapFloor.of(CAPFLOOR_LEG);
    assertSerialization(test);
  }

}
