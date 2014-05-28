/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

import com.opengamma.sesame.Environment;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.FailureStatus;
import com.opengamma.util.result.Result;

/**
 * Function to handle the situation where the the input supplied is invalid.
 * <p>
 * This handles invalid inputs when creating a view.
 * An input is invalid if the type of the input cannot be matched to a configured function.
 * It always returns a failure result with status {@link FailureStatus#INVALID_INPUT}.
 * <p>
 * This class is immutable and thread-safe.
 */
public class InvalidInputFunction implements InvokableFunction {

  /**
   * The message.
   */
  private final String _message;

  /**
   * Creates the function.
   * 
   * @param message  the message to include in the result of the function
   */
  public InvalidInputFunction(String message) {
    _message = ArgumentChecker.notBlank(message, "message");
  }

  //-------------------------------------------------------------------------
  /**
   * Always returns a failure result with status {@link FailureStatus#INVALID_INPUT}.
   *
   * @param env  ignored
   * @param input  ignored
   * @param args  ignored
   * @return a failure result with status {@link FailureStatus#INVALID_INPUT}
   */
  @Override
  public Object invoke(Environment env, Object input, FunctionArguments args) {
    return Result.failure(FailureStatus.INVALID_INPUT, "Invalid input: {}", _message);
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
    return "INVALID_INPUT: " + _message;
  }

}
