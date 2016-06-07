/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.util.function.DoubleUnaryOperator;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link PointSensitivityBuilder}.
 */
@Test
public class PointSensitivityBuilderTest {

  private static final DummyPointSensitivity SENS = DummyPointSensitivity.of(Currency.GBP, date(2015, 6, 30), 12);

  public void test_of_array_size0() {
    PointSensitivities test = PointSensitivityBuilder.of().build();
    assertEquals(test.size(), 0);
  }

  public void test_of_array_size1() {
    PointSensitivities test = PointSensitivityBuilder.of(SENS).build();
    assertEquals(test.size(), 1);
    assertEquals(test.getSensitivities().get(0), SENS);
  }

  public void test_of_array_size2() {
    PointSensitivities test = PointSensitivityBuilder.of(SENS, SENS).build();
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities().get(0), SENS);
    assertEquals(test.getSensitivities().get(1), SENS);
  }

  public void test_of_list_size0() {
    PointSensitivities test = PointSensitivityBuilder.of(ImmutableList.of()).build();
    assertEquals(test.size(), 0);
  }

  public void test_of_list_size1() {
    PointSensitivities test = PointSensitivityBuilder.of(ImmutableList.of(SENS)).build();
    assertEquals(test.size(), 1);
    assertEquals(test.getSensitivities().get(0), SENS);
  }

  public void test_of_list_size2() {
    PointSensitivities test = PointSensitivityBuilder.of(ImmutableList.of(SENS, SENS)).build();
    assertEquals(test.size(), 2);
    assertEquals(test.getSensitivities().get(0), SENS);
    assertEquals(test.getSensitivities().get(1), SENS);
  }

  public void test_multipliedBy() {
    TestingPointSensitivityBuilder test = new TestingPointSensitivityBuilder();
    test.multipliedBy(6);
    assertEquals(test.value, 12d * 6);
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
