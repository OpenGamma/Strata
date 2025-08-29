/*
 * Copyright (C) 2025 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2Y;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_SOFR;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link OvernightOvernightSwapTemplate}.
 */
public class OvernightOvernightSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;

  private static final OvernightRateSwapLegConvention ON_LEG =
      OvernightRateSwapLegConvention.of(USD_SOFR, P6M, 2);
  private static final OvernightRateSwapLegConvention ON_LEG_2 =
      OvernightRateSwapLegConvention.of(EUR_EONIA, P3M, 0);
  private static final OvernightRateSwapLegConvention ON_LEG_FLAT =
      OvernightRateSwapLegConvention.of(USD_FED_FUND, P6M, 2);
  private static final OvernightRateSwapLegConvention ON_LEG_2_FLAT =
      OvernightRateSwapLegConvention.of(EUR_ESTR, P3M, 0);

  private static final DaysAdjustment SPOT_DATE_ADJUSTMENT_2 = DaysAdjustment.ofBusinessDays(2, USNY);
  private static final DaysAdjustment SPOT_DATE_ADJUSTMENT_0 = DaysAdjustment.ofBusinessDays(0, GBLO);

  private static final OvernightOvernightSwapConvention CONV =
      ImmutableOvernightOvernightSwapConvention.of("USD-Swap", ON_LEG, ON_LEG_FLAT, SPOT_DATE_ADJUSTMENT_2);
  private static final OvernightOvernightSwapConvention CONV2 =
      ImmutableOvernightOvernightSwapConvention.of("GBP-Swap", ON_LEG_2, ON_LEG_2_FLAT, SPOT_DATE_ADJUSTMENT_0);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    OvernightOvernightSwapTemplate test = OvernightOvernightSwapTemplate.of(TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ZERO);
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  @Test
  public void test_of_period() {
    OvernightOvernightSwapTemplate test = OvernightOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertThat(test.getPeriodToStart()).isEqualTo(Period.ofMonths(3));
    assertThat(test.getTenor()).isEqualTo(TENOR_10Y);
    assertThat(test.getConvention()).isEqualTo(CONV);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightOvernightSwapTemplate.builder()
            .tenor(TENOR_2Y)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createTrade() {
    OvernightOvernightSwapTemplate base = OvernightOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        ON_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        ON_LEG_FLAT.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertThat(test.getInfo().getTradeDate()).isEqualTo(Optional.of(tradeDate));
    assertThat(test.getProduct()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightOvernightSwapTemplate test = OvernightOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    coverImmutableBean(test);
    OvernightOvernightSwapTemplate test2 = OvernightOvernightSwapTemplate.of(Period.ofMonths(2), TENOR_2Y, CONV2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightOvernightSwapTemplate test = OvernightOvernightSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertSerialization(test);
  }

}
