/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ParameterizedDataCombiner}.
 */
public class ParameterizedDataCombinerTest {

  private static final TestingParameterizedData2 DATA1 = new TestingParameterizedData2(1d, 2d);
  private static final TestingParameterizedData DATA2 = new TestingParameterizedData(3d);
  private static final TestingParameterizedData2 DATA3 = new TestingParameterizedData2(4d, 5d);

  //-------------------------------------------------------------------------
  @Test
  public void test_basics() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.getParameterCount()).isEqualTo(5);
    assertThat(test.getParameter(0)).isEqualTo(1d);
    assertThat(test.getParameter(1)).isEqualTo(2d);
    assertThat(test.getParameter(2)).isEqualTo(3d);
    assertThat(test.getParameter(3)).isEqualTo(4d);
    assertThat(test.getParameter(4)).isEqualTo(5d);
    assertThat(test.getParameterMetadata(0)).isEqualTo(ParameterMetadata.empty());
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> test.getParameter(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> test.getParameter(5));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> test.getParameterMetadata(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> test.getParameterMetadata(5));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ParameterizedDataCombiner.of());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_underlyingWithParameter0() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 0, -1d).getParameter(0)).isEqualTo(-1d);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 0, -1d).getParameter(1)).isEqualTo(2d);
    assertThat(test.underlyingWithParameter(1, TestingParameterizedData.class, 0, -1d).getParameter(0)).isEqualTo(3d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 0, -1d).getParameter(0)).isEqualTo(4d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 0, -1d).getParameter(1)).isEqualTo(5d);
  }

  @Test
  public void test_underlyingWithParameter1() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 1, -1d).getParameter(0)).isEqualTo(1d);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 1, -1d).getParameter(1)).isEqualTo(-1d);
    assertThat(test.underlyingWithParameter(1, TestingParameterizedData.class, 1, -1d).getParameter(0)).isEqualTo(3d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 1, -1d).getParameter(0)).isEqualTo(4d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 1, -1d).getParameter(1)).isEqualTo(5d);
  }

  @Test
  public void test_underlyingWithParameter2() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 2, -1d).getParameter(0)).isEqualTo(1d);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 2, -1d).getParameter(1)).isEqualTo(2d);
    assertThat(test.underlyingWithParameter(1, TestingParameterizedData.class, 2, -1d).getParameter(0)).isEqualTo(-1d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 2, -1d).getParameter(0)).isEqualTo(4d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 2, -1d).getParameter(1)).isEqualTo(5d);
  }

  @Test
  public void test_underlyingWithParameter3() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 3, -1d).getParameter(0)).isEqualTo(1d);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 3, -1d).getParameter(1)).isEqualTo(2d);
    assertThat(test.underlyingWithParameter(1, TestingParameterizedData.class, 3, -1d).getParameter(0)).isEqualTo(3d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 3, -1d).getParameter(0)).isEqualTo(-1d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 3, -1d).getParameter(1)).isEqualTo(5d);
  }

  @Test
  public void test_underlyingWithParameter4() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 4, -1d).getParameter(0)).isEqualTo(1d);
    assertThat(test.underlyingWithParameter(0, TestingParameterizedData2.class, 4, -1d).getParameter(1)).isEqualTo(2d);
    assertThat(test.underlyingWithParameter(1, TestingParameterizedData.class, 4, -1d).getParameter(0)).isEqualTo(3d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 4, -1d).getParameter(0)).isEqualTo(4d);
    assertThat(test.underlyingWithParameter(2, TestingParameterizedData2.class, 4, -1d).getParameter(1)).isEqualTo(-1d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_underlyingWithPerturbation() {
    ParameterPerturbation perturbation = (i, v, m) -> v + i + 0.5d;
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.underlyingWithPerturbation(0, TestingParameterizedData2.class, perturbation).getParameter(0)).isEqualTo(1.5d);
    assertThat(test.underlyingWithPerturbation(0, TestingParameterizedData2.class, perturbation).getParameter(1)).isEqualTo(3.5d);
    assertThat(test.underlyingWithPerturbation(1, TestingParameterizedData.class, perturbation).getParameter(0)).isEqualTo(5.5d);
    assertThat(test.underlyingWithPerturbation(2, TestingParameterizedData2.class, perturbation).getParameter(0)).isEqualTo(7.5d);
    assertThat(test.underlyingWithPerturbation(2, TestingParameterizedData2.class, perturbation).getParameter(1)).isEqualTo(9.5d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withParameter() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertThat(test.withParameter(
        ParameterizedData.class, 0, -1d)).containsExactly(DATA1.withParameter(0, -1d), DATA2, DATA3);
    assertThat(test.withParameter(
        ParameterizedData.class, 1, -1d)).containsExactly(DATA1.withParameter(1, -1d), DATA2, DATA3);
    assertThat(test.withParameter(
        ParameterizedData.class, 2, -1d)).containsExactly(DATA1, DATA2.withParameter(0, -1d), DATA3);
    assertThat(test.withParameter(
        ParameterizedData.class, 3, -1d)).containsExactly(DATA1, DATA2, DATA3.withParameter(0, -1d));
    assertThat(test.withParameter(
        ParameterizedData.class, 4, -1d)).containsExactly(DATA1, DATA2, DATA3.withParameter(1, -1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withPerturbation() {
    ParameterPerturbation perturbation = (i, v, m) -> v + i + 0.5d;
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    List<ParameterizedData> perturbed = test.withPerturbation(ParameterizedData.class, perturbation);
    assertThat(perturbed.get(0)).isEqualTo(new TestingParameterizedData2(1.5d, 3.5d));
    assertThat(perturbed.get(1)).isEqualTo(new TestingParameterizedData(5.5d));
    assertThat(perturbed.get(2)).isEqualTo(new TestingParameterizedData2(7.5d, 9.5d));
  }

}
