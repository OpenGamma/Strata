/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.capfloor;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.swap.SwapLegType.FIXED;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Test {@link ExpandedIborCapFloor}.
 */
@Test
public class ExpandedIborCapFloorTest {

  private static final double STRIKE = 0.0125;
  private static final double NOTIONAL = 1.0e6;
  private static final IborCapletFloorletPeriod PERIOD_1 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 3, 17))
      .endDate(LocalDate.of(2011, 6, 17))
      .unadjustedStartDate(LocalDate.of(2011, 3, 17))
      .unadjustedEndDate(LocalDate.of(2011, 6, 17))
      .paymentDate(LocalDate.of(2011, 6, 21))
      .rateObservation(
          IborRateObservation.of(EUR_EURIBOR_3M, LocalDate.of(2011, 6, 15)))
      .yearFraction(0.2556)
      .build();
  private static final IborCapletFloorletPeriod PERIOD_2 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 6, 17))
      .endDate(LocalDate.of(2011, 9, 19))
      .unadjustedStartDate(LocalDate.of(2011, 6, 17))
      .unadjustedEndDate(LocalDate.of(2011, 9, 17))
      .paymentDate(LocalDate.of(2011, 9, 21))
      .rateObservation(
          IborRateObservation.of(EUR_EURIBOR_3M, LocalDate.of(2011, 9, 15)))
      .yearFraction(0.2611)
      .build();
  private static final IborCapletFloorletPeriod PERIOD_3 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 9, 19))
      .endDate(LocalDate.of(2011, 12, 19))
      .unadjustedStartDate(LocalDate.of(2011, 9, 17))
      .unadjustedEndDate(LocalDate.of(2011, 12, 17))
      .paymentDate(LocalDate.of(2011, 12, 21))
      .rateObservation(
          IborRateObservation.of(EUR_EURIBOR_3M, LocalDate.of(2011, 12, 15)))
      .yearFraction(0.2528)
      .build();
  private static final IborCapletFloorletPeriod PERIOD_4 = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .notional(NOTIONAL)
      .currency(EUR)
      .startDate(LocalDate.of(2011, 12, 19))
      .endDate(LocalDate.of(2012, 3, 19))
      .unadjustedStartDate(LocalDate.of(2011, 12, 17))
      .unadjustedEndDate(LocalDate.of(2012, 3, 17))
      .paymentDate(LocalDate.of(2012, 3, 21))
      .rateObservation(
          IborRateObservation.of(EUR_EURIBOR_3M, LocalDate.of(2012, 3, 15)))
      .yearFraction(0.2528)
      .build();
  private static final ExpandedIborCapFloorLeg CAPFLOOR_LEG = ExpandedIborCapFloorLeg.builder()
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
          .rateObservation(FixedRateObservation.of(RATE))
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
          .rateObservation(FixedRateObservation.of(RATE))
          .build())
      .dayCount(ACT_365F)
      .currency(EUR)
      .notional(-NOTIONAL)
      .build();
  private static final ExpandedSwapLeg PAY_LEG = ExpandedSwapLeg.builder()
      .paymentPeriods(PAY_PERIOD_1, PAY_PERIOD_2)
      .type(FIXED)
      .payReceive(PAY)
      .build();

  public void test_of_oneLeg() {
    ExpandedIborCapFloor test = ExpandedIborCapFloor.of(CAPFLOOR_LEG);
    assertEquals(test.getCapFloorLeg(), CAPFLOOR_LEG);
    assertEquals(test.getPayLeg().isPresent(), false);
  }

  public void test_of_twoLegs() {
    ExpandedIborCapFloor test = ExpandedIborCapFloor.of(CAPFLOOR_LEG, PAY_LEG);
    assertEquals(test.getCapFloorLeg(), CAPFLOOR_LEG);
    assertEquals(test.getPayLeg().get(), PAY_LEG);
  }

  public void test_expand() {
    ExpandedIborCapFloor base = ExpandedIborCapFloor.of(CAPFLOOR_LEG, PAY_LEG);
    assertSame(base.expand(), base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedIborCapFloor test1 = ExpandedIborCapFloor.of(CAPFLOOR_LEG, PAY_LEG);
    coverImmutableBean(test1);
    ExpandedIborCapFloorLeg capFloor = ExpandedIborCapFloorLeg.builder()
        .capletFloorletPeriods(PERIOD_1)
        .payReceive(PAY)
        .build();
    ExpandedIborCapFloor test2 = ExpandedIborCapFloor.of(capFloor);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedIborCapFloor test = ExpandedIborCapFloor.of(CAPFLOOR_LEG);
    assertSerialization(test);
  }

}
