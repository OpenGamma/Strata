/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2Y;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.common.PayReceive.RECEIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link OvernightIborSwapTemplate}.
 */
@Test
public class OvernightIborSwapTemplateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL_2M = 2_000_000d;

  private static final OvernightRateSwapLegConvention ON_LEG =
      OvernightRateSwapLegConvention.of(USD_FED_FUND, P6M, 2);
  private static final OvernightRateSwapLegConvention ON_LEG_2 =
      OvernightRateSwapLegConvention.of(GBP_SONIA, P3M, 0);
  private static final IborRateSwapLegConvention IBOR = IborRateSwapLegConvention.of(USD_LIBOR_3M);
  private static final IborRateSwapLegConvention IBOR2 = IborRateSwapLegConvention.of(GBP_LIBOR_3M);

  private static final DaysAdjustment SPOT_DATE_ADJUSTMENT_2 = DaysAdjustment.ofBusinessDays(2, USNY);
  private static final DaysAdjustment SPOT_DATE_ADJUSTMENT_0 = DaysAdjustment.ofBusinessDays(0, GBLO);

  private static final OvernightIborSwapConvention CONV =
      ImmutableOvernightIborSwapConvention.of("USD-Swap", ON_LEG, IBOR, SPOT_DATE_ADJUSTMENT_2);
  private static final OvernightIborSwapConvention CONV2 =
      ImmutableOvernightIborSwapConvention.of("GBP-Swap", ON_LEG_2, IBOR2, SPOT_DATE_ADJUSTMENT_0);

  //-------------------------------------------------------------------------
  public void test_of() {
    OvernightIborSwapTemplate test = OvernightIborSwapTemplate.of(TENOR_10Y, CONV);
    assertEquals(test.getPeriodToStart(), Period.ZERO);
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getConvention(), CONV);
  }

  public void test_of_period() {
    OvernightIborSwapTemplate test = OvernightIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertEquals(test.getPeriodToStart(), Period.ofMonths(3));
    assertEquals(test.getTenor(), TENOR_10Y);
    assertEquals(test.getConvention(), CONV);
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> OvernightIborSwapTemplate.builder()
        .tenor(TENOR_2Y)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_createTrade() {
    OvernightIborSwapTemplate base = OvernightIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    LocalDate tradeDate = LocalDate.of(2015, 5, 5);
    LocalDate startDate = date(2015, 8, 7);
    LocalDate endDate = date(2025, 8, 7);
    SwapTrade test = base.createTrade(tradeDate, BUY, NOTIONAL_2M, 0.25d, REF_DATA);
    Swap expected = Swap.of(
        ON_LEG.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d),
        IBOR.toLeg(startDate, endDate, RECEIVE, NOTIONAL_2M));
    assertEquals(test.getInfo().getTradeDate(), Optional.of(tradeDate));
    assertEquals(test.getProduct(), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIborSwapTemplate test = OvernightIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    coverImmutableBean(test);
    OvernightIborSwapTemplate test2 = OvernightIborSwapTemplate.of(Period.ofMonths(2), TENOR_2Y, CONV2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OvernightIborSwapTemplate test = OvernightIborSwapTemplate.of(Period.ofMonths(3), TENOR_10Y, CONV);
    assertSerialization(test);
  }

}
