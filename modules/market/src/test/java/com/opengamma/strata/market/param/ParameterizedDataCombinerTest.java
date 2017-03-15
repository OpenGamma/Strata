/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ParameterizedDataCombiner}.
 */
@Test
public class ParameterizedDataCombinerTest {

  private static final TestingParameterizedData2 DATA1 = new TestingParameterizedData2(1d, 2d);
  private static final TestingParameterizedData DATA2 = new TestingParameterizedData(3d);
  private static final TestingParameterizedData2 DATA3 = new TestingParameterizedData2(4d, 5d);

  //-------------------------------------------------------------------------
  public void test_basics() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.getParameterCount(), 5);
    assertEquals(test.getParameter(0), 1d);
    assertEquals(test.getParameter(1), 2d);
    assertEquals(test.getParameter(2), 3d);
    assertEquals(test.getParameter(3), 4d);
    assertEquals(test.getParameter(4), 5d);
    assertEquals(test.getParameterMetadata(0), ParameterMetadata.empty());
    assertThrows(() -> test.getParameter(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.getParameter(5), IndexOutOfBoundsException.class);
    assertThrows(() -> test.getParameterMetadata(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.getParameterMetadata(5), IndexOutOfBoundsException.class);
    assertThrowsIllegalArg(() -> ParameterizedDataCombiner.of());
  }

  //-------------------------------------------------------------------------
  public void test_underlyingWithParameter0() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 0, -1d).getParameter(0), -1d);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 0, -1d).getParameter(1), 2d);
    assertEquals(test.underlyingWithParameter(1, TestingParameterizedData.class, 0, -1d).getParameter(0), 3d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 0, -1d).getParameter(0), 4d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 0, -1d).getParameter(1), 5d);
  }

  public void test_underlyingWithParameter1() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 1, -1d).getParameter(0), 1d);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 1, -1d).getParameter(1), -1d);
    assertEquals(test.underlyingWithParameter(1, TestingParameterizedData.class, 1, -1d).getParameter(0), 3d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 1, -1d).getParameter(0), 4d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 1, -1d).getParameter(1), 5d);
  }

  public void test_underlyingWithParameter2() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 2, -1d).getParameter(0), 1d);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 2, -1d).getParameter(1), 2d);
    assertEquals(test.underlyingWithParameter(1, TestingParameterizedData.class, 2, -1d).getParameter(0), -1d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 2, -1d).getParameter(0), 4d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 2, -1d).getParameter(1), 5d);
  }

  public void test_underlyingWithParameter3() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 3, -1d).getParameter(0), 1d);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 3, -1d).getParameter(1), 2d);
    assertEquals(test.underlyingWithParameter(1, TestingParameterizedData.class, 3, -1d).getParameter(0), 3d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 3, -1d).getParameter(0), -1d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 3, -1d).getParameter(1), 5d);
  }

  public void test_underlyingWithParameter4() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 4, -1d).getParameter(0), 1d);
    assertEquals(test.underlyingWithParameter(0, TestingParameterizedData2.class, 4, -1d).getParameter(1), 2d);
    assertEquals(test.underlyingWithParameter(1, TestingParameterizedData.class, 4, -1d).getParameter(0), 3d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 4, -1d).getParameter(0), 4d);
    assertEquals(test.underlyingWithParameter(2, TestingParameterizedData2.class, 4, -1d).getParameter(1), -1d);
  }

  //-------------------------------------------------------------------------
  public void test_underlyingWithPerturbation() {
    ParameterPerturbation perturbation = (i, v, m) -> v + i + 0.5d;
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.underlyingWithPerturbation(0, TestingParameterizedData2.class, perturbation).getParameter(0), 1.5d);
    assertEquals(test.underlyingWithPerturbation(0, TestingParameterizedData2.class, perturbation).getParameter(1), 3.5d);
    assertEquals(test.underlyingWithPerturbation(1, TestingParameterizedData.class, perturbation).getParameter(0), 5.5d);
    assertEquals(test.underlyingWithPerturbation(2, TestingParameterizedData2.class, perturbation).getParameter(0), 7.5d);
    assertEquals(test.underlyingWithPerturbation(2, TestingParameterizedData2.class, perturbation).getParameter(1), 9.5d);
  }

  //-------------------------------------------------------------------------
  public void test_withParameter() {
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    assertEquals(test.withParameter(
        ParameterizedData.class, 0, -1d),
        ImmutableList.of(DATA1.withParameter(0, -1d), DATA2, DATA3));
    assertEquals(test.withParameter(
        ParameterizedData.class, 1, -1d),
        ImmutableList.of(DATA1.withParameter(1, -1d), DATA2, DATA3));
    assertEquals(test.withParameter(
        ParameterizedData.class, 2, -1d),
        ImmutableList.of(DATA1, DATA2.withParameter(0, -1d), DATA3));
    assertEquals(test.withParameter(
        ParameterizedData.class, 3, -1d),
        ImmutableList.of(DATA1, DATA2, DATA3.withParameter(0, -1d)));
    assertEquals(test.withParameter(
        ParameterizedData.class, 4, -1d),
        ImmutableList.of(DATA1, DATA2, DATA3.withParameter(1, -1d)));
  }

  //-------------------------------------------------------------------------
  public void test_withPerturbation() {
    ParameterPerturbation perturbation = (i, v, m) -> v + i + 0.5d;
    ParameterizedDataCombiner test = ParameterizedDataCombiner.of(DATA1, DATA2, DATA3);
    List<ParameterizedData> perturbed = test.withPerturbation(ParameterizedData.class, perturbation);
    assertEquals(perturbed.get(0), new TestingParameterizedData2(1.5d, 3.5d));
    assertEquals(perturbed.get(1), new TestingParameterizedData(5.5d));
    assertEquals(perturbed.get(2), new TestingParameterizedData2(7.5d, 9.5d));
  }

}
