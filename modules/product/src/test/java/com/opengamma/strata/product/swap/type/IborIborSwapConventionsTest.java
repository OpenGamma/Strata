/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * Test {@link IborIborSwapConventions}.
 * <p>
 * These tests  match the table 18.1 in the following guide:
 * https://developers.opengamma.com/quantitative-research/Interest-Rate-Instruments-and-Market-Conventions.pdf
 */
public class IborIborSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public static Object[][] data_spot_lag() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, 2},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, 2},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, 2},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, 2},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, 2},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, 2},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, 2},
    };
  }

  @ParameterizedTest
  @MethodSource("data_spot_lag")
  public void test_spot_lag(ImmutableIborIborSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, Frequency.P6M},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, Frequency.P3M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, Frequency.P1M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, Frequency.P3M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, Frequency.P6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, Frequency.P6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, Frequency.P1M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, Frequency.P3M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, Frequency.P1M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, Frequency.P3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_period(IborIborSwapConvention convention, Frequency frequency) {
    assertThat(convention.getSpreadLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_count() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, CompoundingMethod.FLAT},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, CompoundingMethod.FLAT},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, CompoundingMethod.NONE},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, CompoundingMethod.NONE}
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_count")
  public void test_composition(IborIborSwapConvention convention, CompoundingMethod comp) {
    assertThat(convention.getSpreadLeg().getCompoundingMethod()).isEqualTo(comp);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_spread_leg() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, IborIndices.USD_LIBOR_3M},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, IborIndices.USD_LIBOR_1M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, IborIndices.JPY_LIBOR_1M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, IborIndices.JPY_LIBOR_3M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_1M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_3M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_1M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spread_leg")
  public void test_float_leg(IborIborSwapConvention convention, IborIndex index) {
    assertThat(convention.getSpreadLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_flat_leg() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, IborIndices.USD_LIBOR_6M},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, IborIndices.USD_LIBOR_3M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, IborIndices.JPY_LIBOR_6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_6M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_6M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, IborIndices.JPY_TIBOR_EUROYEN_6M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_6M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, IborIndices.JPY_TIBOR_JAPAN_6M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_flat_leg")
  public void test_flat_leg(IborIborSwapConvention convention, IborIndex index) {
    assertThat(convention.getFlatLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_convention() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.USD_LIBOR_1M_LIBOR_3M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, BusinessDayConventions.MODIFIED_FOLLOWING},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_convention")
  public void test_day_convention(IborIborSwapConvention convention, BusinessDayConvention dayConvention) {
    assertThat(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_stub_ibor() {
    return new Object[][] {
        {IborIborSwapConventions.USD_LIBOR_3M_LIBOR_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_1M_LIBOR_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_3M_LIBOR_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_EUROYEN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_LIBOR_6M_TIBOR_JAPAN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_1M_TIBOR_EUROYEN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_EUROYEN_3M_TIBOR_EUROYEN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_1M_TIBOR_JAPAN_6M, Tenor.TENOR_8M},
        {IborIborSwapConventions.JPY_TIBOR_JAPAN_3M_TIBOR_JAPAN_6M, Tenor.TENOR_8M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_stub_ibor")
  public void test_stub_ibor(IborIborSwapConvention convention, Tenor tenor) {
    LocalDate tradeDate = LocalDate.of(2015, 10, 20);
    SwapTrade swap = convention.createTrade(tradeDate, tenor, BuySell.BUY, 1, 0.01, REF_DATA);
    ResolvedSwap swapResolved = swap.getProduct().resolve(REF_DATA);
    LocalDate endDate = swapResolved.getLeg(PayReceive.PAY).get().getEndDate();
    assertThat(endDate.isAfter(tradeDate.plus(tenor).minusMonths(1))).isTrue();
    assertThat(endDate.isBefore(tradeDate.plus(tenor).plusMonths(1))).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(IborIborSwapConventions.class);
    coverPrivateConstructor(StandardIborIborSwapConventions.class);
  }

}
