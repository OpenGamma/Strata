/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * Evaluates a token against an object to produce another object.
 * <p>
 * The token may be part of an expression which traverses a graph of objects.
 */
public abstract class TokenEvaluator<T> {

  /**
   * Gets the type against which tokens can be evaluated in this implementation.
   * 
   * @return the evaluation type
   */
  public abstract Class<?> getTargetType();

  /**
   * Gets the set of supported token for the given object.
   * 
   * @param object  the object against which tokens may be evaluated
   * @return  the set of supported tokens
   */
  public abstract Set<String> tokens(T object);

  /**
   * Evaluates a token against a given object.
   * 
   * @param object  the object against which to evaluate the token
   * @param token  the token
   * @return the result of the evaluation
   */
  public abstract Result<?> evaluate(T object, String token);
  
  //-------------------------------------------------------------------------
  /**
   * Generates a failure result for an invalid token.
   * 
   * @param object  the object against which the failure occurred
   * @param token  the invalid token
   * @return the failure result
   */
  protected Result<?> invalidTokenFailure(T object, String token) {
    return tokenFailure("Invalid", object, token);
  }
  
  /**
   * Generates a failure result for an ambiguous token.
   * 
   * @param object  the object against which the failure occurred.
   * @param token  the ambiguous token
   * @return the failure result
   */
  protected Result<?> ambiguousTokenFailure(T object, String token) {
    return tokenFailure("Ambiguous", object, token);
  }
  
  private Result<?> tokenFailure(String reason, T object, String token) {
    List<String> orderedValidTokens = new ArrayList<String>(tokens(object));
    orderedValidTokens.sort(null);
    return Result.failure(FailureReason.INVALID_INPUT, "{} field: {}. Use one of: {}",
        reason, token, tokens(object));
  }

}
