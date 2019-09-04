/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.IborIndices.EUR_EURIBOR_1W;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1W;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
public class IborInterpolatedRateComputationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndex EUR_EURIBOR_2W = IborIndex.of("EUR-EURIBOR-2W");
  private static final LocalDate FIXING_DATE = date(2014, 6, 30);
  private static final IborIndexObservation GBP_LIBOR_1W_OBS = IborIndexObservation.of(GBP_LIBOR_1W, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_1M_OBS = IborIndexObservation.of(GBP_LIBOR_1M, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_OBS = IborIndexObservation.of(GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation EUR_EURIBOR_1W_OBS = IborIndexObservation.of(EUR_EURIBOR_1W, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation EUR_EURIBOR_2W_OBS = IborIndexObservation.of(EUR_EURIBOR_2W, FIXING_DATE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_OBS2 =
      IborIndexObservation.of(GBP_LIBOR_3M, FIXING_DATE.plusDays(1), REF_DATA);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_monthly() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    assertThat(test.getShortObservation()).isEqualTo(GBP_LIBOR_1M_OBS);
    assertThat(test.getLongObservation()).isEqualTo(GBP_LIBOR_3M_OBS);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_DATE);
  }

  @Test
  public void test_of_monthly_byObs() {
    IborInterpolatedRateComputation test = IborInterpolatedRateComputation.of(GBP_LIBOR_1M_OBS, GBP_LIBOR_3M_OBS);
    assertThat(test.getShortObservation()).isEqualTo(GBP_LIBOR_1M_OBS);
    assertThat(test.getLongObservation()).isEqualTo(GBP_LIBOR_3M_OBS);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_DATE);
  }

  @Test
  public void test_of_monthly_reverseOrder() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(GBP_LIBOR_3M, GBP_LIBOR_1M, FIXING_DATE, REF_DATA);
    assertThat(test.getShortObservation()).isEqualTo(GBP_LIBOR_1M_OBS);
    assertThat(test.getLongObservation()).isEqualTo(GBP_LIBOR_3M_OBS);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_DATE);
  }

  @Test
  public void test_of_weekly() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(EUR_EURIBOR_1W, EUR_EURIBOR_2W, FIXING_DATE, REF_DATA);
    assertThat(test.getShortObservation()).isEqualTo(EUR_EURIBOR_1W_OBS);
    assertThat(test.getLongObservation()).isEqualTo(EUR_EURIBOR_2W_OBS);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_DATE);
  }

  @Test
  public void test_of_weekly_reverseOrder() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(EUR_EURIBOR_2W, EUR_EURIBOR_1W, FIXING_DATE, REF_DATA);
    assertThat(test.getShortObservation()).isEqualTo(EUR_EURIBOR_1W_OBS);
    assertThat(test.getLongObservation()).isEqualTo(EUR_EURIBOR_2W_OBS);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_DATE);
  }

  @Test
  public void test_of_weekMonthCombination() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(GBP_LIBOR_1W, GBP_LIBOR_1M, FIXING_DATE, REF_DATA);
    assertThat(test.getShortObservation()).isEqualTo(GBP_LIBOR_1W_OBS);
    assertThat(test.getLongObservation()).isEqualTo(GBP_LIBOR_1M_OBS);
    assertThat(test.getFixingDate()).isEqualTo(FIXING_DATE);
  }

  @Test
  public void test_of_sameIndex() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> IborInterpolatedRateComputation.of(GBP_LIBOR_1M, GBP_LIBOR_1M, FIXING_DATE, REF_DATA));
  }

  @Test
  public void test_builder_indexOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborInterpolatedRateComputation.meta().builder()
            .set(IborInterpolatedRateComputation.meta().shortObservation(), GBP_LIBOR_3M_OBS)
            .set(IborInterpolatedRateComputation.meta().longObservation(), GBP_LIBOR_1M_OBS)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborInterpolatedRateComputation.meta().builder()
            .set(IborInterpolatedRateComputation.meta().shortObservation(), EUR_EURIBOR_2W_OBS)
            .set(IborInterpolatedRateComputation.meta().longObservation(), EUR_EURIBOR_1W_OBS)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborInterpolatedRateComputation.of(EUR_EURIBOR_2W_OBS, EUR_EURIBOR_1W_OBS));
  }

  @Test
  public void test_of_differentCurrencies() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> IborInterpolatedRateComputation.of(EUR_EURIBOR_2W, GBP_LIBOR_1M, FIXING_DATE, REF_DATA));
  }

  @Test
  public void test_of_differentFixingDates() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborInterpolatedRateComputation.meta().builder()
            .set(IborInterpolatedRateComputation.meta().shortObservation(), GBP_LIBOR_1M_OBS)
            .set(IborInterpolatedRateComputation.meta().longObservation(), GBP_LIBOR_3M_OBS2)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(GBP_LIBOR_1M, GBP_LIBOR_3M);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    coverImmutableBean(test);
    IborInterpolatedRateComputation test2 =
        IborInterpolatedRateComputation.of(USD_LIBOR_1M, USD_LIBOR_3M, date(2014, 7, 30), REF_DATA);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborInterpolatedRateComputation test =
        IborInterpolatedRateComputation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, FIXING_DATE, REF_DATA);
    assertSerialization(test);
  }

}
