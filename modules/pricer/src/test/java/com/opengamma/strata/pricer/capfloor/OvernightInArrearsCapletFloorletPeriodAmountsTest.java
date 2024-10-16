/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.basics.index.OvernightIndices.EUR_ESTR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;

/**
 * Test {@link OvernightInArrearsCapletFloorletPeriodAmounts}.
 */
public class OvernightInArrearsCapletFloorletPeriodAmountsTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate START = LocalDate.of(2011, 1, 5);
  private static final LocalDate END = LocalDate.of(2011, 7, 5);
  private static final double NOTIONAL = 1_000_000;
  private static final double STRIKE = 0.01;
  private static final OvernightCompoundedRateComputation RATE_COMP = OvernightCompoundedRateComputation.of(
      EUR_ESTR,
      START,
      END,
      REF_DATA);
  private static final OvernightInArrearsCapletFloorletPeriod CAPLET_LONG =
      OvernightInArrearsCapletFloorletPeriod.builder()
          .caplet(STRIKE)
          .startDate(START)
          .endDate(END)
          .yearFraction(0.5)
          .notional(NOTIONAL)
          .overnightRate(RATE_COMP)
          .build();
  private static final OvernightInArrearsCapletFloorletPeriod CAPLET_SHORT =
      OvernightInArrearsCapletFloorletPeriod.builder()
          .caplet(STRIKE)
          .startDate(START)
          .endDate(END)
          .yearFraction(0.5)
          .notional(NOTIONAL * -1)
          .overnightRate(RATE_COMP)
          .build();
  private static final Map<OvernightInArrearsCapletFloorletPeriod, Double> CAPLET_DOUBLE_MAP = ImmutableMap.of(
      CAPLET_LONG,
      1d);

  @Test
  void test_of() {
    OvernightInArrearsCapletFloorletPeriodAmounts test =
        OvernightInArrearsCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.getAmounts()).isEqualTo(CAPLET_DOUBLE_MAP);
  }

  @Test
  void test_findAmount() {
    OvernightInArrearsCapletFloorletPeriodAmounts test =
        OvernightInArrearsCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.findAmount(CAPLET_LONG)).contains(1d);
  }

  @Test
  void test_findAmountEmpty() {
    OvernightInArrearsCapletFloorletPeriodAmounts test =
        OvernightInArrearsCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.findAmount(CAPLET_SHORT)).isEmpty();
  }

  @Test
  void test_getAmount() {
    OvernightInArrearsCapletFloorletPeriodAmounts test =
        OvernightInArrearsCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThat(test.getAmount(CAPLET_LONG)).isEqualTo(1d);
  }

  @Test
  void test_getAmountThrows() {
    OvernightInArrearsCapletFloorletPeriodAmounts test =
        OvernightInArrearsCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAmount(CAPLET_SHORT))
        .withMessage("Could not find double amount for " + CAPLET_SHORT);
  }

  @Test
  void coverage() {
    OvernightInArrearsCapletFloorletPeriodAmounts test =
        OvernightInArrearsCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    OvernightInArrearsCapletFloorletPeriodAmounts test =
        OvernightInArrearsCapletFloorletPeriodAmounts.of(CAPLET_DOUBLE_MAP);
    assertSerialization(test);
  }
}
