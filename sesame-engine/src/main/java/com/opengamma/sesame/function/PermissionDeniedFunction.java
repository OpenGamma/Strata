/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import org.apache.commons.lang.StringUtils;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function to handle the situation where the user does not have permission to view an input.
 * <p>
 * This handles permission denied on trades, positions and securities.
 * It always returns a failure result with status {@link FailureStatus#PERMISSION_DENIED}.
 * <p>
 * This class is immutable and thread-safe.
 */
public class PermissionDeniedFunction implements InvokableFunction {

  /**
   * The message.
   */
  private final String _message;

  /**
   * Creates the function.
   * 
   * @param message  the message to include in the result of the function
   */
  public PermissionDeniedFunction(String message) {
    _message = StringUtils.defaultIfBlank(message, "Unknown reason");
  }

  //-------------------------------------------------------------------------
  /**
   * Always returns a failure result with status {@link FailureStatus#PERMISSION_DENIED}.
   *
   * @param env  ignored
   * @param input  ignored
   * @param args  ignored
   * @return a failure result with status {@link FailureStatus#PERMISSION_DENIED}
   */
  @Override
  public Object invoke(Environment env, Object input, FunctionArguments args) {
    return Result.failure(FailureStatus.PERMISSION_DENIED, "Permission Denied: {}", _message);
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

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return "PERMISSION_DENIED: " + _message;
  }

}
