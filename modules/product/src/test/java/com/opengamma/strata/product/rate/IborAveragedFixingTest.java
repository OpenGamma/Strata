/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.OptionalDouble;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.IborIndexObservation;

/**
 * Test.
 */
public class IborAveragedFixingTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final IborIndexObservation GBP_LIBOR_3M_OBS =
      IborIndexObservation.of(GBP_LIBOR_3M, date(2014, 6, 30), REF_DATA);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_date() {
    IborAveragedFixing test = IborAveragedFixing.of(GBP_LIBOR_3M_OBS);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .observation(GBP_LIBOR_3M_OBS)
        .fixedRate(null)
        .weight(1)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_of_date_fixedRate() {
    IborAveragedFixing test = IborAveragedFixing.of(GBP_LIBOR_3M_OBS, 0.05);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .observation(GBP_LIBOR_3M_OBS)
        .fixedRate(0.05)
        .weight(1)
        .build();
    assertThat(test).isEqualTo(expected);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.of(0.05));
  }

  @Test
  public void test_of_date_fixedRate_null() {
    IborAveragedFixing test = IborAveragedFixing.of(GBP_LIBOR_3M_OBS, null);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .observation(GBP_LIBOR_3M_OBS)
        .fixedRate(null)
        .weight(1)
        .build();
    assertThat(test).isEqualTo(expected);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.empty());
  }

  @Test
  public void test_of_date_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.of(null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.of(null, 0.05));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.of(null, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofDaysInResetPeriod() {
    IborAveragedFixing test = IborAveragedFixing.ofDaysInResetPeriod(
        GBP_LIBOR_3M_OBS, date(2014, 7, 2), date(2014, 8, 2));
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .observation(GBP_LIBOR_3M_OBS)
        .fixedRate(null)
        .weight(31)
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_ofDaysInResetPeriod_fixedRate() {
    IborAveragedFixing test = IborAveragedFixing.ofDaysInResetPeriod(
        GBP_LIBOR_3M_OBS, date(2014, 7, 2), date(2014, 9, 2), 0.06);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .observation(GBP_LIBOR_3M_OBS)
        .fixedRate(0.06)
        .weight(62)
        .build();
    assertThat(test).isEqualTo(expected);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.of(0.06));
  }

  @Test
  public void test_ofDaysInResetPeriod_fixedRate_null() {
    IborAveragedFixing test = IborAveragedFixing.ofDaysInResetPeriod(
        GBP_LIBOR_3M_OBS, date(2014, 7, 2), date(2014, 9, 2), null);
    IborAveragedFixing expected = IborAveragedFixing.builder()
        .observation(GBP_LIBOR_3M_OBS)
        .fixedRate(null)
        .weight(62)
        .build();
    assertThat(test).isEqualTo(expected);
    assertThat(test.getFixedRate()).isEqualTo(OptionalDouble.empty());
  }

  @Test
  public void test_ofDaysInResetPeriod_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(null, date(2014, 7, 2), date(2014, 8, 2)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(GBP_LIBOR_3M_OBS, null, date(2014, 8, 2)));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(GBP_LIBOR_3M_OBS, date(2014, 7, 2), null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(null, null, null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(null, date(2014, 7, 2), date(2014, 8, 2), 0.05));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(GBP_LIBOR_3M_OBS, null, date(2014, 8, 2), 0.05));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(GBP_LIBOR_3M_OBS, date(2014, 7, 2), null, 0.05));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborAveragedFixing.ofDaysInResetPeriod(null, null, null, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborAveragedFixing test = IborAveragedFixing.of(GBP_LIBOR_3M_OBS);
    coverImmutableBean(test);
  }

  @Test
  public void test_serialization() {
    IborAveragedFixing test = IborAveragedFixing.of(GBP_LIBOR_3M_OBS);
    assertSerialization(test);
  }

}
