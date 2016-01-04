/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.PayReceive.RECEIVE;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapIndices;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Test {@link Cms}.
 */
@Test
public class CmsTest {

  private static final double NOTIONAL = 1.0e6;
  private static final SwapIndex INDEX = SwapIndices.EUR_EURIBOR_1100_10Y;
  private static final LocalDate START = LocalDate.of(2015, 10, 21);
  private static final LocalDate END = LocalDate.of(2017, 10, 21);
  private static final Frequency FREQUENCY = Frequency.P12M;
  private static final BusinessDayAdjustment BUSS_ADJ_EUR =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, EUTA);
  private static final PeriodicSchedule SCHEDULE_EUR =
      PeriodicSchedule.of(START, END, FREQUENCY, BUSS_ADJ_EUR, StubConvention.SHORT_INITIAL, RollConventions.NONE);
  private static final ValueSchedule STRIKE = ValueSchedule.of(0.0125);
  private static final CmsLeg CMS_LEG = CmsLeg.builder()
      .capSchedule(STRIKE)
      .index(INDEX)
      .notional(NOTIONAL)
      .payReceive(RECEIVE)
      .periodicSchedule(SCHEDULE_EUR)
      .build();
  private static final SwapLeg PAY_LEG = RateCalculationSwapLeg.builder()
      .payReceive(PAY)
      .accrualSchedule(SCHEDULE_EUR)
      .calculation(
          FixedRateCalculation.of(0.01, ACT_360))
      .paymentSchedule(
          PaymentSchedule.builder().paymentFrequency(FREQUENCY).paymentDateOffset(DaysAdjustment.NONE).build())
      .notionalSchedule(
          NotionalSchedule.of(CurrencyAmount.of(EUR, NOTIONAL)))
      .build();

  public void test_builder_twoLegs() {
    Cms test = Cms.builder().cmsLeg(CMS_LEG).payLeg(PAY_LEG).build();
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertEquals(test.getPayLeg().get(), PAY_LEG);
  }

  public void test_builder_oneLeg() {
    Cms test = Cms.builder().cmsLeg(CMS_LEG).build();
    assertEquals(test.getCmsLeg(), CMS_LEG);
    assertFalse(test.getPayLeg().isPresent());
  }

  public void test_expand_twoLegs() {
    Cms base = Cms.builder().cmsLeg(CMS_LEG).payLeg(PAY_LEG).build();
    ExpandedCms test = base.expand();
    assertEquals(test.getCmsLeg(), CMS_LEG.expand());
    assertEquals(test.getPayLeg().get(), PAY_LEG.expand());
  }

  public void test_expand_oneLeg() {
    Cms base = Cms.builder().cmsLeg(CMS_LEG).build();
    ExpandedCms test = base.expand();
    assertEquals(test.getCmsLeg(), CMS_LEG.expand());
    assertFalse(test.getPayLeg().isPresent());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Cms test1 = Cms.builder().cmsLeg(CMS_LEG).payLeg(PAY_LEG).build();
    coverImmutableBean(test1);
    Cms test2 = Cms.builder()
        .cmsLeg(CmsLeg.builder()
            .floorSchedule(STRIKE)
            .index(INDEX)
            .notional(NOTIONAL)
            .payReceive(RECEIVE)
            .periodicSchedule(SCHEDULE_EUR)
            .build())
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    Cms test = Cms.builder().cmsLeg(CMS_LEG).payLeg(PAY_LEG).build();
    assertSerialization(test);
  }

}
