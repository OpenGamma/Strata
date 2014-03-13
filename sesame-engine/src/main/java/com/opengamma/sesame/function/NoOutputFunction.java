/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * A function that takes a {@link PositionOrTrade} and returns null.
 */
public class NoOutputFunction {

  public static final String NO_OUTPUT = "No Output";
  public static final FunctionMetadata METADATA;
  
  static {
    try {
      Method doNothing = NoOutputFunction.class.getMethod("doNothing");
      METADATA = new FunctionMetadata(doNothing);
    } catch (NoSuchMethodException e) {
      // won't happen but need to throw the exception to convince the compiler
      throw new OpenGammaRuntimeException("Unexpected problem", e);
    }
  }

  @Output(NO_OUTPUT)
  public Result<?> doNothing() {
    // TODO new status needed for this
    return Result.failure(FailureStatus.CALCULATION_FAILED, "No Calculation Possible");
  }
}
