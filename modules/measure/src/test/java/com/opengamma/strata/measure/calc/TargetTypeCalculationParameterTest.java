/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.calc;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.TestingMeasures;
import com.opengamma.strata.calc.runner.CalculationParameter;
import com.opengamma.strata.calc.runner.TestParameter;
import com.opengamma.strata.calc.runner.TestParameter2;

/**
 * Test {@link TargetTypeCalculationParameter}.
 */
public class TargetTypeCalculationParameterTest {

  private static final CalculationParameter PARAM1 = new TestParameter();
  private static final CalculationParameter PARAM2 = new TestParameter();
  private static final CalculationParameter PARAM3 = new TestParameter();
  private static final CalculationParameter PARAM_OTHER = new TestParameter2();
  private static final TestTarget TARGET1 = new TestTarget();
  private static final TestTarget2 TARGET2 = new TestTarget2();
  private static final TestTarget3 TARGET3 = new TestTarget3();

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    TargetTypeCalculationParameter test = TargetTypeCalculationParameter.of(
        ImmutableMap.of(TestTarget.class, PARAM1, TestTarget2.class, PARAM2), PARAM3);
    assertThat(test.getQueryType()).isEqualTo(TestParameter.class);
    assertThat(test.getParameters()).hasSize(2);
    assertThat(test.getDefaultParameter()).isEqualTo(PARAM3);
    assertThat(test.queryType()).isEqualTo(TestParameter.class);
    assertThat(test.filter(TARGET1, TestingMeasures.PRESENT_VALUE)).isEqualTo(Optional.of(PARAM1));
    assertThat(test.filter(TARGET2, TestingMeasures.PRESENT_VALUE)).isEqualTo(Optional.of(PARAM2));
    assertThat(test.filter(TARGET3, TestingMeasures.PRESENT_VALUE)).isEqualTo(Optional.of(PARAM3));
  }

  @Test
  public void of_empty() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TargetTypeCalculationParameter.of(ImmutableMap.of(), PARAM3));
  }

  @Test
  public void of_badType() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TargetTypeCalculationParameter.of(ImmutableMap.of(TestTarget.class, PARAM_OTHER), PARAM3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    TargetTypeCalculationParameter test = TargetTypeCalculationParameter.of(
        ImmutableMap.of(TestTarget.class, PARAM1, TestTarget2.class, PARAM2), PARAM3);
    coverImmutableBean(test);
    TargetTypeCalculationParameter test2 = TargetTypeCalculationParameter.of(
        ImmutableMap.of(TestTarget.class, PARAM1), PARAM2);
    coverBeanEquals(test, test2);
  }

  //-------------------------------------------------------------------------
  private static class TestTarget implements CalculationTarget {
  }

  private static class TestTarget2 implements CalculationTarget {
  }

  private static class TestTarget3 implements CalculationTarget {
  }

}
