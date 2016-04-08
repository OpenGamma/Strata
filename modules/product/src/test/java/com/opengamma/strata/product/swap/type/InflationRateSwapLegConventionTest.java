/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.PayReceive.PAY;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.product.swap.InflationRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.PriceIndexCalculationMethod;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * Test {@link InflationRateSwapLegConvention}.
 */
@Test
public class InflationRateSwapLegConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.of(GB_HICP);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.isNotionalExchange(), false);
  }

  public void test_builder() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.builder().index(GB_HICP).build();
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.isNotionalExchange(), false);
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> IborRateSwapLegConvention.builder().build());
  }

  public void test_builderAllSpecified() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .currency(USD)
        .notionalExchange(true)
        .build();
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.isNotionalExchange(), true);
  }

  //-------------------------------------------------------------------------
  public void test_toLeg() {
    InflationRateSwapLegConvention base = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .build();
    LocalDate startDate = LocalDate.of(2015, 5, 5);
    LocalDate endDate = LocalDate.of(2020, 5, 5);
    RateCalculationSwapLeg test = base.toLeg(
        startDate, 
        endDate, 
        PAY, 
        Period.ofMonths(3), 
        BDA_MOD_FOLLOW, 
        DaysAdjustment.NONE, NOTIONAL_2M);
    
    RateCalculationSwapLeg expected = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .frequency(Frequency.TERM)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(BDA_MOD_FOLLOW)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(Frequency.TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(InflationRateCalculation.of(GB_HICP, 3, PriceIndexCalculationMethod.MONTHLY))
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .build();
    coverImmutableBean(test);
    InflationRateSwapLegConvention test2 = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .currency(GBP)
        .indexCalculationMethod(PriceIndexCalculationMethod.MONTHLY)
        .notionalExchange(true)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .build();
    assertSerialization(test);
  }

}
