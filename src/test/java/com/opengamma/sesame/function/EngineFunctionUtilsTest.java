/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.sesame.MarketData;
import com.opengamma.util.test.TestGroup;

@SuppressWarnings("ALL")
@Test(groups = TestGroup.UNIT)
public class EngineFunctionUtilsTest {

  /* package */ static final String VALUE_NAME = "ValueName";

  @DefaultImplementation(Long.class) interface I { }

  @OutputName(VALUE_NAME)
  class C1 implements OutputFunction<Double, Object> {

    @Override
    public Object execute(MarketData marketData, Double target) {
      return null;
    }
  }

  @OutputName(VALUE_NAME)
  class C2<T> implements OutputFunction<T, Object> {

    @Override
    public Object execute(MarketData marketData, T target) {
      return null;
    }
  }

  @Test
  public void metadata() {
    assertEquals(VALUE_NAME, EngineFunctionUtils.getOutputName(C1.class));
    assertEquals(Long.class, EngineFunctionUtils.getDefaultImplementation(I.class));
  }

  @Test
  public void targetType() {
    assertEquals(Double.class, EngineFunctionUtils.getTargetType(C1.class));
    // TODO test class that indirectly implements OutputFunction
  }
}
