/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame;

import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class MultipleFailureFunctionResultTest {

  @Test
  public void sameType() {
    FunctionResult<Object> failure1 = FunctionResultGenerator.failure(FailureStatus.MISSING_DATA, "message 1");
    FunctionResult<Object> failure2 = FunctionResultGenerator.failure(FailureStatus.MISSING_DATA, "message 2");
    FunctionResult<Object> failure3 = FunctionResultGenerator.failure(FailureStatus.MISSING_DATA, "message 3");
    List<FunctionResult<?>> failures = Lists.<FunctionResult<?>>newArrayList(failure1, failure2, failure3);
    MultipleFailureFunctionResult<Object> composite = new MultipleFailureFunctionResult<>(failures);
    assertEquals(FailureStatus.MISSING_DATA, composite.getStatus());
    assertEquals("message 1\nmessage 2\nmessage 3", composite.getFailureMessage());
  }

  @Test
  public void differentTypes() {
    FunctionResult<Object> failure1 = FunctionResultGenerator.failure(FailureStatus.MISSING_DATA, "message 1");
    FunctionResult<Object> failure2 = FunctionResultGenerator.failure(FailureStatus.CALCULATION_FAILED, "message 2");
    FunctionResult<Object> failure3 = FunctionResultGenerator.failure(FailureStatus.ERROR, "message 3");
    List<FunctionResult<?>> failures = Lists.<FunctionResult<?>>newArrayList(failure1, failure2, failure3);
    MultipleFailureFunctionResult<Object> composite = new MultipleFailureFunctionResult<>(failures);
    assertEquals(FailureStatus.MULTIPLE, composite.getStatus());
    assertEquals("message 1\nmessage 2\nmessage 3", composite.getFailureMessage());
  }
}
