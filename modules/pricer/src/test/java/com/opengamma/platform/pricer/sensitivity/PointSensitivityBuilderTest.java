/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import static org.testng.Assert.assertEquals;

import java.util.function.DoubleUnaryOperator;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.Currency;

/**
 * Test.
 */
@Test
public class PointSensitivityBuilderTest {

  private static final class MockPointSensitivityBuilder implements PointSensitivityBuilder {
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
    public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
      throw new UnsupportedOperationException();
    }
  }

  public void test_multipliedBy() {
    MockPointSensitivityBuilder test = new MockPointSensitivityBuilder();
    test.multipliedBy(6);
    assertEquals(test.value, 12d * 6);
  }

}
