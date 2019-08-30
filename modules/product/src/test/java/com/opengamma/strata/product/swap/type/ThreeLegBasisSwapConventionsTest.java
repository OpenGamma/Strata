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
 * Test {@link ThreeLegBasisSwapConventions}.
 */
public class ThreeLegBasisSwapConventionsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public static Object[][] data_spot_lag() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, 2}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spot_lag")
  public void test_spot_lag(ImmutableThreeLegBasisSwapConvention convention, int lag) {
    assertThat(convention.getSpotDateOffset().getDays()).isEqualTo(lag);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_period() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, Frequency.P3M},
    };
  }

  @ParameterizedTest
  @MethodSource("data_period")
  public void test_period(ThreeLegBasisSwapConvention convention, Frequency frequency) {
    assertThat(convention.getSpreadFloatingLeg().getPaymentFrequency()).isEqualTo(frequency);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_count() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, CompoundingMethod.NONE}
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_count")
  public void test_composition(ThreeLegBasisSwapConvention convention, CompoundingMethod comp) {
    assertThat(convention.getSpreadLeg().getCompoundingMethod()).isEqualTo(comp);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_spread_floating_leg() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, IborIndices.EUR_EURIBOR_3M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_spread_floating_leg")
  public void test_spread_floating_leg(ThreeLegBasisSwapConvention convention, IborIndex index) {
    assertThat(convention.getSpreadFloatingLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_flat_floating_leg() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, IborIndices.EUR_EURIBOR_6M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_flat_floating_leg")
  public void test_flat_floating_leg(ThreeLegBasisSwapConvention convention, IborIndex index) {
    assertThat(convention.getFlatFloatingLeg().getIndex()).isEqualTo(index);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_day_convention() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, BusinessDayConventions.MODIFIED_FOLLOWING}
    };
  }

  @ParameterizedTest
  @MethodSource("data_day_convention")
  public void test_day_convention(ThreeLegBasisSwapConvention convention, BusinessDayConvention dayConvention) {
    assertThat(convention.getSpreadLeg().getAccrualBusinessDayAdjustment().getConvention()).isEqualTo(dayConvention);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_stub_ibor() {
    return new Object[][] {
        {ThreeLegBasisSwapConventions.EUR_FIXED_1Y_EURIBOR_3M_EURIBOR_6M, Tenor.TENOR_8M}
    };
  }

  @ParameterizedTest
  @MethodSource("data_stub_ibor")
  public void test_stub_ibor(ThreeLegBasisSwapConvention convention, Tenor tenor) {
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
    coverPrivateConstructor(ThreeLegBasisSwapConventions.class);
    coverPrivateConstructor(StandardThreeLegBasisSwapConventions.class);
  }

}
