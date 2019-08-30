/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.calc.TestingMeasures;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;

/**
 * Test {@link CalculationParameters}.
 */
public class CalculationParametersTest {

  private static final CalculationParameter PARAM = new TestParameter();
  private static final CalculationParameter PARAM2 = new TestParameter2();

  //-------------------------------------------------------------------------
  @Test
  public void of() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    assertThat(test.getParameters()).hasSize(1);
    assertThat(test.findParameter(TestParameter.class)).isEqualTo(Optional.of(PARAM));
  }

  @Test
  public void of_empty() {
    CalculationParameters test = CalculationParameters.of();
    assertThat(test.getParameters()).hasSize(0);
  }

  @Test
  public void of_list() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of(PARAM));
    assertThat(test.getParameters()).hasSize(1);
    assertThat(test.findParameter(TestParameter.class)).isEqualTo(Optional.of(PARAM));
  }

  @Test
  public void of_list_empty() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of());
    assertThat(test.getParameters()).hasSize(0);
  }

  @Test
  public void getParameter1() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of(PARAM));
    assertThat(test.getParameter(TestParameter.class)).isEqualTo(PARAM);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getParameter(TestParameter2.class));
    assertThatIllegalArgumentException().isThrownBy(() -> test.getParameter(TestInterfaceParameter.class));
    assertThat(test.findParameter(TestParameter.class)).isEqualTo(Optional.of(PARAM));
    assertThat(test.findParameter(TestParameter2.class)).isEqualTo(Optional.empty());
    assertThat(test.findParameter(TestInterfaceParameter.class)).isEqualTo(Optional.empty());
  }

  @Test
  public void getParameter2() {
    CalculationParameters test = CalculationParameters.of(ImmutableList.of(PARAM2));
    assertThat(test.getParameter(TestParameter2.class)).isEqualTo(PARAM2);
    assertThat(test.getParameter(TestInterfaceParameter.class)).isEqualTo(PARAM2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getParameter(TestParameter.class));
    assertThat(test.findParameter(TestParameter2.class)).isEqualTo(Optional.of(PARAM2));
    assertThat(test.findParameter(TestInterfaceParameter.class)).isEqualTo(Optional.of(PARAM2));
    assertThat(test.findParameter(TestParameter.class)).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    CalculationParameters test1 = CalculationParameters.of(PARAM);
    CalculationParameters test2 = CalculationParameters.of(ImmutableList.of());

    assertThat(test1.combinedWith(test2).getParameters()).hasSize(1);
    assertThat(test1.combinedWith(test2).getParameters().get(TestParameter.class)).isEqualTo(PARAM);

    assertThat(test2.combinedWith(test1).getParameters()).hasSize(1);
    assertThat(test2.combinedWith(test1).getParameters().get(TestParameter.class)).isEqualTo(PARAM);

    assertThat(test1.combinedWith(test1).getParameters()).hasSize(1);
    assertThat(test1.combinedWith(test1).getParameters().get(TestParameter.class)).isEqualTo(PARAM);
  }

  @Test
  public void test_with_add() {
    CalculationParameters test = CalculationParameters.of(PARAM).with(PARAM2);
    assertThat(test.getParameters()).hasSize(2);
  }

  @Test
  public void test_with_replace() {
    CalculationParameters test = CalculationParameters.of(PARAM).with(PARAM);
    assertThat(test.getParameters()).hasSize(1);
  }

  @Test
  public void test_without_typeFound() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    CalculationParameters filtered1 = test.without(TestParameter.class);
    assertThat(filtered1.getParameters()).hasSize(0);
  }

  @Test
  public void test_without_typeNotFound() {
    CalculationParameters test = CalculationParameters.empty();
    CalculationParameters filtered1 = test.without(TestParameter.class);
    assertThat(filtered1.getParameters()).hasSize(0);
  }

  @Test
  public void test_filter() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    TestTarget target = new TestTarget();

    CalculationParameters filtered1 = test.filter(target, TestingMeasures.PRESENT_VALUE);
    assertThat(filtered1.getParameters()).hasSize(1);
    assertThat(filtered1.getParameters().get(TestParameter.class)).isEqualTo(PARAM);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CalculationParameters test = CalculationParameters.of(PARAM);
    coverImmutableBean(test);
    CalculationParameters test2 = CalculationParameters.empty();
    coverBeanEquals(test, test2);
    assertThat(CalculationParameters.meta()).isNotNull();
  }

}
