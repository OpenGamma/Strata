/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Set;

/**
 * Evaluates a token against an object to produce another object.
 * <p>
 * The token may be part of an expression which traverses a graph of objects.
 */
public interface TokenEvaluator<T> {

  /**
   * Gets the type against which tokens can be evaluated in this implementation.
   * 
   * @return the evaluation type
   */
  Class<?> getTargetType();

  /**
   * Gets the set of supported token for the given object.
   * 
   * @param object  the object against which tokens may be evaluated
   * @return  the set of supported tokens
   */
  Set<String> tokens(T object);

  /**
   * Evaluates a token against a given object.
   * 
   * @param object  the object against which to evaluate the token
   * @param token  the token
   * @return the result of the evaluation
   */
  Object evaluate(T object, String token);

}
