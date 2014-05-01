/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function to handle the situation where the user doesn't have permission to view a position or trade's security.
 * It always returns a failure result with status {@link FailureStatus#PERMISSION_DENIED}.
 */
public class PermissionDeniedFunction implements InvokableFunction {

  private final String _message;

  /**
   * @param message message to include in the function's result
   */
  public PermissionDeniedFunction(String message) {
    _message = message;
  }

  /**
   * Always returns a failure result with status {@link FailureStatus#PERMISSION_DENIED}.
   *
   * @param env ignored
   * @param input ignored
   * @param args ignored
   * @return a failure result with status {@link FailureStatus#PERMISSION_DENIED}
   */
  @Override
  public Object invoke(Environment env, Object input, FunctionArguments args) {
    return Result.failure(FailureStatus.PERMISSION_DENIED, "Permission Denied. {}", _message);
  }

  @Override
  public Object getReceiver() {
    return this;
  }

  @Override
  public Object getUnderlyingReceiver() {
    return this;
  }

  @Override
  public Class<?> getDeclaringClass() {
    return getClass();
  }
}
