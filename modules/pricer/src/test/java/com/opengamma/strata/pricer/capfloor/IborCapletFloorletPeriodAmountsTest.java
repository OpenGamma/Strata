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

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.rate.IborRateComputation;

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
  private static final Map<IborCapletFloorletPeriod, Double> CAPLET_VALUE_MAP = ImmutableMap.of(CAPLET_LONG, 1d);

  @Test
  void test_of() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_VALUE_MAP);
    assertThat(test.getPeriodAmounts()).isEqualTo(CAPLET_VALUE_MAP);
  }

  @Test
  void coverage() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_VALUE_MAP);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    IborCapletFloorletPeriodAmounts test = IborCapletFloorletPeriodAmounts.of(CAPLET_VALUE_MAP);
    assertSerialization(test);
  }
}
