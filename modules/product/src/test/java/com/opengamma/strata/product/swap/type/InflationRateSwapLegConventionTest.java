/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
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

  private static final Period LAG_3M = Period.ofMonths(3);
  private static final Period LAG_4M = Period.ofMonths(4);
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.of(GB_HICP, LAG_3M, BDA_MOD_FOLLOW);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getLag(), LAG_3M);
    assertEquals(test.getIndexCalculationMethod(), PriceIndexCalculationMethod.MONTHLY);
    assertEquals(test.isNotionalExchange(), false);
    assertEquals(test.getCurrency(), GBP);
  }

  public void test_builder() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .lag(LAG_3M)
        .build();
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getLag(), LAG_3M);
    assertEquals(test.getIndexCalculationMethod(), PriceIndexCalculationMethod.MONTHLY);
    assertEquals(test.isNotionalExchange(), false);
    assertEquals(test.getCurrency(), GBP);
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> IborRateSwapLegConvention.builder().build());
  }

  public void test_builderAllSpecified() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .lag(LAG_3M)
        .indexCalculationMethod(PriceIndexCalculationMethod.INTERPOLATED)
        .notionalExchange(true)
        .build();
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getLag(), LAG_3M);
    assertEquals(test.getIndexCalculationMethod(), PriceIndexCalculationMethod.INTERPOLATED);
    assertEquals(test.isNotionalExchange(), true);
    assertEquals(test.getCurrency(), GBP);
  }

  //-------------------------------------------------------------------------
  public void test_toLeg() {
    InflationRateSwapLegConvention base = InflationRateSwapLegConvention.of(GB_HICP, LAG_3M, BDA_MOD_FOLLOW);
    LocalDate startDate = LocalDate.of(2015, 5, 5);
    LocalDate endDate = LocalDate.of(2020, 5, 5);
    RateCalculationSwapLeg test = base.toLeg(
        startDate,
        endDate,
        PAY,
        NOTIONAL_2M);

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
        .lag(LAG_3M)
        .build();
    coverImmutableBean(test);
    InflationRateSwapLegConvention test2 = InflationRateSwapLegConvention.builder()
        .index(GB_HICP)
        .lag(LAG_4M)
        .indexCalculationMethod(PriceIndexCalculationMethod.INTERPOLATED)
        .notionalExchange(true)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    InflationRateSwapLegConvention test = InflationRateSwapLegConvention.of(GB_HICP, LAG_3M, BDA_MOD_FOLLOW);
    assertSerialization(test);
  }

}
