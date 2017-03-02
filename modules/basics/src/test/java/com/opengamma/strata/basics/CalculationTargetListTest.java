/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.io.Serializable;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link CalculationTargetList}.
 */
@Test
public class CalculationTargetListTest {

  private static final CalculationTarget TARGET1 = new TestTarget(1);
  private static final CalculationTarget TARGET2 = new TestTarget(2);

  //-------------------------------------------------------------------------
  public void test_array0() {
    CalculationTargetList test = CalculationTargetList.of();
    assertEquals(test.getTargets(), ImmutableList.of());
  }

  public void test_array2() {
    CalculationTargetList test = CalculationTargetList.of(TARGET1, TARGET2);
    assertEquals(test.getTargets(), ImmutableList.of(TARGET1, TARGET2));
  }

  public void test_collection1() {
    CalculationTargetList test = CalculationTargetList.of(ImmutableList.of(TARGET1));
    assertEquals(test.getTargets(), ImmutableList.of(TARGET1));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CalculationTargetList test = CalculationTargetList.of(TARGET1, TARGET2);
    coverImmutableBean(test);
  }

  public void test_serialization() {
    CalculationTargetList test = CalculationTargetList.of(TARGET1, TARGET2);
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  static class TestTarget implements CalculationTarget, Serializable {
    private static final long serialVersionUID = 1L;
    private final int value;

    public TestTarget(int value) {
      this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TestTarget && value == ((TestTarget) obj).value;
    }

    @Override
    public int hashCode() {
      return value;
    }
  }

}
