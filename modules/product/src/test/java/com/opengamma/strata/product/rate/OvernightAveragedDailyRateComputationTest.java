/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

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
 * Test {@link OvernightAveragedDailyRateComputation}.
 */
public class OvernightAveragedDailyRateComputationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @Test
  public void test_of_noRateCutoff() {
    OvernightAveragedDailyRateComputation test = OvernightAveragedDailyRateComputation.of(
        USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertThat(test.getStartDate()).isEqualTo(date(2016, 2, 24));
    assertThat(test.getEndDate()).isEqualTo(date(2016, 3, 24));
    assertThat(test.getIndex()).isEqualTo(USD_FED_FUND);
    assertThat(test.getFixingCalendar()).isEqualTo(USD_FED_FUND.getFixingCalendar().resolve(REF_DATA));
  }

  @Test
  public void test_of_badDateOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightAveragedDailyRateComputation.of(
            USD_FED_FUND, date(2016, 2, 24), date(2016, 2, 24), REF_DATA));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightAveragedDailyRateComputation.of(
            USD_FED_FUND, date(2016, 2, 25), date(2016, 2, 24), REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_calculate() {
    OvernightAveragedDailyRateComputation test =
        OvernightAveragedDailyRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
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
    OvernightAveragedDailyRateComputation test =
        OvernightAveragedDailyRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertThat(test.observeOn(date(2016, 2, 24)))
        .isEqualTo(OvernightIndexObservation.of(USD_FED_FUND, date(2016, 2, 24), REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    OvernightAveragedDailyRateComputation test =
        OvernightAveragedDailyRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(USD_FED_FUND);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightAveragedDailyRateComputation test =
        OvernightAveragedDailyRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    coverImmutableBean(test);
    OvernightAveragedDailyRateComputation test2 =
        OvernightAveragedDailyRateComputation.of(GBP_SONIA, date(2014, 6, 3), date(2014, 7, 3), REF_DATA);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    OvernightAveragedDailyRateComputation test =
        OvernightAveragedDailyRateComputation.of(USD_FED_FUND, date(2016, 2, 24), date(2016, 3, 24), REF_DATA);
    assertSerialization(test);
  }

}
