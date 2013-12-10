/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class FunctionResultGeneratorTest {

  @Test
  public void anyFailures() {
    FunctionResult<String> success1 = FunctionResultGenerator.success("success 1");
    FunctionResult<String> success2 = FunctionResultGenerator.success("success 1");
    FunctionResult<Object> failure1 = FunctionResultGenerator.failure(FailureStatus.MISSING_DATA, "failure 1");
    FunctionResult<Object> failure2 = FunctionResultGenerator.failure(FailureStatus.ERROR, "failure 2");
    assertTrue(FunctionResultGenerator.anyFailures(failure1, failure2));
    assertTrue(FunctionResultGenerator.anyFailures(failure1, success1));
    assertFalse(FunctionResultGenerator.anyFailures(success1, success2));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void propagateFailures() {
    FunctionResult<String> success1 = FunctionResultGenerator.success("success 1");
    FunctionResult<String> success2 = FunctionResultGenerator.success("success 1");
    FunctionResult<Object> failure1 = FunctionResultGenerator.failure(FailureStatus.MISSING_DATA, "failure 1");
    FunctionResult<Object> failure2 = FunctionResultGenerator.failure(FailureStatus.ERROR, "failure 2");
    FunctionResult<Object> composite1 = FunctionResultGenerator.propagateFailures(success1,
                                                                                  success2,
                                                                                  failure1,
                                                                                  failure2);
    List failures = ((MultipleFailureFunctionResult) composite1).getFailures();
    assertEquals(Lists.newArrayList(failure1, failure2), failures);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void propagateSuccesses() {
    FunctionResult<String> success1 = FunctionResultGenerator.success("success 1");
    FunctionResult<String> success2 = FunctionResultGenerator.success("success 1");
    FunctionResultGenerator.propagateFailures(success1, success2);
  }
}
