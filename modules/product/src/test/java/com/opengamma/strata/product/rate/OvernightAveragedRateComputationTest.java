/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.OvernightIndices.CHF_TOIS;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndexObservation;

/**
 * Test.
 */
@SuppressWarnings("deprecation")
public class OvernightAveragedRateComputationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @Test
  public void test_of_noRateCutoff() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    OvernightAveragedRateComputation expected = OvernightAveragedRateComputation.builder()
        .index(USD_FED_FUND)
        .fixingCalendar(USD_FED_FUND.getFixingCalendar().resolve(REF_DATA))
        .startDate(date(2016, 2, 24))
        .endDate(date(2016, 3, 24))
        .rateCutOffDays(0)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_noRateCutoff_tomNext() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(CHF_TOIS, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    OvernightAveragedRateComputation expected = OvernightAveragedRateComputation.builder()
        .index(CHF_TOIS)
        .fixingCalendar(CHF_TOIS.getFixingCalendar().resolve(REF_DATA))
        .startDate(date(2016, 2, 23))
        .endDate(date(2016, 3, 23))
        .rateCutOffDays(0)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_rateCutoff_0() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), 0, REF_DATA);
    OvernightAveragedRateComputation expected = OvernightAveragedRateComputation.builder()
        .index(USD_FED_FUND)
        .fixingCalendar(USD_FED_FUND.getFixingCalendar().resolve(REF_DATA))
        .startDate(date(2016, 2, 24))
        .endDate(date(2016, 3, 24))
        .rateCutOffDays(0)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_rateCutoff_2() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), 2, REF_DATA);
    OvernightAveragedRateComputation expected = OvernightAveragedRateComputation.builder()
        .index(USD_FED_FUND)
        .fixingCalendar(USD_FED_FUND.getFixingCalendar().resolve(REF_DATA))
        .startDate(date(2016, 2, 24))
        .endDate(date(2016, 3, 24))
        .rateCutOffDays(2)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_badDateOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightAveragedRateComputation.of(
            USD_FED_FUND, date(2016, 2, 24), date(2016, 2, 24), REF_DATA));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightAveragedRateComputation.of(
            USD_FED_FUND, date(2016, 2, 25), date(2016, 2, 24), REF_DATA));
  }

  @Test
  public void test_of_rateCutoff_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightAveragedRateComputation.of(
            USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), -1, REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_calculate() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertThat(test.calculateEffectiveFromFixing(date(2016, 2, 24)))
        .isEqualTo(USD_FED_FUND.calculateEffectiveFromFixing(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculateFixingFromEffective(date(2016, 2, 24)))
        .isEqualTo(USD_FED_FUND.calculateFixingFromEffective(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculatePublicationFromFixing(date(2016, 2, 24)))
        .isEqualTo(USD_FED_FUND.calculatePublicationFromFixing(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculateMaturityFromFixing(date(2016, 2, 24)))
        .isEqualTo(USD_FED_FUND.calculateMaturityFromFixing(date(2016, 2, 24), REF_DATA));
    assertThat(test.calculateMaturityFromEffective(date(2016, 2, 24)))
        .isEqualTo(USD_FED_FUND.calculateMaturityFromEffective(date(2016, 2, 24), REF_DATA));
  }

  @Test
  public void test_observeOn() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertThat(test.observeOn(date(2016, 2, 24)))
        .isEqualTo(OvernightIndexObservation.of(USD_FED_FUND, date(2016, 2, 24), REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(USD_FED_FUND);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    coverImmutableBean(test);
    OvernightAveragedRateComputation test2 =
        OvernightAveragedRateComputation.of(GBP_SONIA, date(2014, 6, 3), date(2014, 7, 3), 3, REF_DATA);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightAveragedRateComputation test =
        OvernightAveragedRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertSerialization(test);
  }

}
