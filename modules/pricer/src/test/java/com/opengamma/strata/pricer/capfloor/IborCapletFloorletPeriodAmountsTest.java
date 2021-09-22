/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.rate.IborRateComputation;

/**
 * Test {@link IborCapletFloorletPeriodAmounts}.
 */
class IborCapletFloorletPeriodAmountsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate FIXING = LocalDate.of(2011, 1, 3);
  private static final double NOTIONAL = 1_000_000;
  private static final double STRIKE = 0.01;
  private static final IborRateComputation RATE_COMP = IborRateComputation.of(EUR_EURIBOR_3M, FIXING, REF_DATA);
  private static final IborCapletFloorletPeriod CAPLET_LONG = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(NOTIONAL)
      .iborRate(RATE_COMP)
      .build();
  private static final IborCapletFloorletPeriod CAPLET_SHORT = IborCapletFloorletPeriod.builder()
      .caplet(STRIKE)
      .startDate(RATE_COMP.getEffectiveDate())
      .endDate(RATE_COMP.getMaturityDate())
      .yearFraction(RATE_COMP.getYearFraction())
      .notional(NOTIONAL * -1)
      .iborRate(RATE_COMP)
      .build();
  private static final Map<IborCapletFloorletPeriod, Double> CAPLET_DOUBLE_MAP = ImmutableMap.of(CAPLET_LONG, 1d);

  @Test
  void test_of() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.getAmounts()).isEqualTo(CAPLET_DOUBLE_MAP);
  }

  @Test
  void test_findAmount() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.findAmount(CAPLET_LONG)).contains(1d);
  }

  @Test
  void test_findAmountEmpty() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.findAmount(CAPLET_SHORT)).isEmpty();
  }

  @Test
  void test_getAmount() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.getAmount(CAPLET_LONG)).isEqualTo(1d);
  }

  @Test
  void test_getAmountThrows() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAmount(CAPLET_SHORT))
        .withMessage("Could not find double amount for " + CAPLET_SHORT);
  }

  @Test
  void coverage() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertSerialization(test);
  }
}
