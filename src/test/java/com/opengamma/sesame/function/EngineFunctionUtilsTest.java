/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.google.common.collect.Sets;
import com.opengamma.sesame.MarketData;
import com.opengamma.util.test.TestGroup;

@SuppressWarnings("ALL")
@Test(groups = TestGroup.UNIT)
public class EngineFunctionUtilsTest {

  /* package */ static final String VALUE_NAME = "ValueName";

  @FallbackImplementation(Long.class) interface I { }

  class C1 {

    @Output(VALUE_NAME)
    public Object execute(MarketData marketData, @Target Double target) {
      return null;
    }
  }

  class C2 {

    @Output(VALUE_NAME)
    public Object execute(MarketData marketData, @Target String target) {
      return null;
    }
  }

  @Test
  public void metadata() {
    assertEquals(Sets.newHashSet(VALUE_NAME), EngineFunctionUtils.getOutputs(C1.class));
    assertEquals(Long.class, EngineFunctionUtils.getDefaultImplementation(I.class));
  }

  @Test
  public void targetType() {
    assertEquals(Double.class, EngineFunctionUtils.getTargetType(C1.class));
    // TODO test class that indirectly implements OutputFunction
  }
}
