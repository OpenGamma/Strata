/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.INTERPOLATED;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.MONTHLY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link FixedInflationSwapTemplate}.
 */
public class FixedInflationSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Period LAG_3M = Period.ofMonths(3);
  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_ONE_DAY = DaysAdjustment.ofBusinessDays(1, GBLO);

  private static final String NAME = "GBP-Swap";
  private static final String NAME2 = "USD-Swap";
  private static final FixedRateSwapLegConvention FIXED =
      FixedRateSwapLegConvention.of(GBP, ACT_360, P6M, BDA_FOLLOW);
  private static final FixedRateSwapLegConvention FIXED2 =
      FixedRateSwapLegConvention.of(USD, ACT_365F, P3M, BDA_MOD_FOLLOW);
  private static final InflationRateSwapLegConvention INFL =
      InflationRateSwapLegConvention.of(GB_HICP, LAG_3M, MONTHLY, BDA_MOD_FOLLOW);
  private static final InflationRateSwapLegConvention INFL2 =
      InflationRateSwapLegConvention.of(US_CPI_U, LAG_3M, INTERPOLATED, BDA_MOD_FOLLOW);
  private static final FixedInflationSwapConvention CONV = ImmutableFixedInflationSwapConvention.of(
      NAME,
      FIXED,
      INFL,
      PLUS_ONE_DAY);
  private static final FixedInflationSwapConvention CONV2 = ImmutableFixedInflationSwapConvention.of(
      NAME2,
      FIXED2,
      INFL2,
      PLUS_ONE_DAY);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FixedInflationSwapTemplate test = FixedInflationSwapTemplate.of(TENOR_10Y, CONV);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  @Test
  public void test_of_period() {
    FixedInflationSwapTemplate test = FixedInflationSwapTemplate.of(TENOR_10Y, CONV);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedIborSwapTemplate.builder()
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    FixedInflationSwapTemplate base = FixedInflationSwapTemplate.of(TENOR_10Y, CONV);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 5, 6); // T+1
    LocalDate endDate = date(2025, 5, 6);
    SwapTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        FIXED.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        INFL.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct().getLegs().get(0)).isEqualTo(expected.getLegs().get(0));
    assertThat(test.getProduct().getLegs().get(1)).isEqualTo(expected.getLegs().get(1));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FixedInflationSwapTemplate test = FixedInflationSwapTemplate.of(TENOR_10Y, CONV);
    coverImmutableBean(test);
    FixedInflationSwapTemplate test2 = FixedInflationSwapTemplate.of(TENOR_10Y, CONV2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FixedInflationSwapTemplate test = FixedInflationSwapTemplate.of(TENOR_10Y, CONV);
    assertSerialization(test);
  }

}
