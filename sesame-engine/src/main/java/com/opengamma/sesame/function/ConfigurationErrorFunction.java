/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import java.lang.reflect.Method;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * If a function can't be built because of a configuration error an instance of this function is used in the
 * {@link Graph} instead.
 */
public class ConfigurationErrorFunction {

  public static final String CONFIG_ERROR = "Configuration Error";
  public static final FunctionMetadata METADATA;
  
  static {
    try {
      Method doNothing = ConfigurationErrorFunction.class.getMethod("doNothing");
      METADATA = new FunctionMetadata(doNothing);
    } catch (NoSuchMethodException e) {
      // won't happen but need to throw the exception to convince the compiler
      throw new OpenGammaRuntimeException("Unexpected problem", e);
    }
  }

  @Output(CONFIG_ERROR)
  public Result<?> doNothing() {
    return Result.failure(FailureStatus.ERROR, CONFIG_ERROR);
  }
}
