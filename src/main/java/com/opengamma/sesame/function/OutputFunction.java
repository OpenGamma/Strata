/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function;

/**
 * Implemented by all functions that produce values for an item in a portfolio.
 * All implementations must be annotated with {@link OutputName}.
 * @param <TTarget> The type of the argument to {@link #execute}, a trade, position or security
 * @param <TResult> The return type of {@link #execute}
 */
public interface OutputFunction<TTarget, TResult> {

  /**
   * Executes the function
   *
   *
   *
   * @param target The target of the function, a trade, position or security
   * @return The function result
   */
  // todo - would be nice if this was actually FunctionResult<TResult> execute(TTarget target)
  TResult execute(TTarget target);
}
