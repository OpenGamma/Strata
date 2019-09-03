/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link PointSensitivityBuilder}.
 */
public class PointSensitivityBuilderTest {

  private static final DummyPointSensitivity SENS = DummyPointSensitivity.of(Currency.GBP, date(2015, 6, 30), 12);

  @Test
  public void test_of_array_size0() {
    PointSensitivities test = PointSensitivityBuilder.of().build();
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_of_array_size1() {
    PointSensitivities test = PointSensitivityBuilder.of(SENS).build();
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities().get(0)).isEqualTo(SENS);
  }

  @Test
  public void test_of_array_size2() {
    PointSensitivities test = PointSensitivityBuilder.of(SENS, SENS).build();
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities().get(0)).isEqualTo(SENS);
    assertThat(test.getSensitivities().get(1)).isEqualTo(SENS);
  }

  @Test
  public void test_of_list_size0() {
    PointSensitivities test = PointSensitivityBuilder.of(ImmutableList.of()).build();
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_of_list_size1() {
    PointSensitivities test = PointSensitivityBuilder.of(ImmutableList.of(SENS)).build();
    assertThat(test.size()).isEqualTo(1);
    assertThat(test.getSensitivities().get(0)).isEqualTo(SENS);
  }

  @Test
  public void test_of_list_size2() {
    PointSensitivities test = PointSensitivityBuilder.of(ImmutableList.of(SENS, SENS)).build();
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.getSensitivities().get(0)).isEqualTo(SENS);
    assertThat(test.getSensitivities().get(1)).isEqualTo(SENS);
  }

  @Test
  public void test_multipliedBy() {
    TestingPointSensitivityBuilder test = new TestingPointSensitivityBuilder();
    test.multipliedBy(6);
    assertThat(test.value).isEqualTo(12d * 6);
  }

  private static final class TestingPointSensitivityBuilder implements PointSensitivityBuilder {
    private double value = 12d;

    @Override
    public PointSensitivityBuilder withCurrency(Currency currency) {
      throw new UnsupportedOperationException();
    }

    @Override
    public PointSensitivityBuilder mapSensitivity(DoubleUnaryOperator operator) {
      value = operator.applyAsDouble(value);
      return this;
    }

    @Override
    public PointSensitivityBuilder normalize() {
      throw new UnsupportedOperationException();
    }

    @Override
    public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
      return combination;
    }

    @Override
    public PointSensitivityBuilder cloned() {
      throw new UnsupportedOperationException();
    }
  }

}
