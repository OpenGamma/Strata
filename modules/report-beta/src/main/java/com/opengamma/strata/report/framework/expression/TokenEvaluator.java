/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.finance.rate.fra.Fra;

/**
 * Evaluates a token against an object to produce another object.
 * <p>
 * Tokens are taken from expressions in a report template. These expressions tell the reporting framework
 * how to navigate a tree of data to find values to include in the report.
 * <p>
 * For example, if the token is '{@code index}' and the object is a {@link Fra}the method {@code Fra.getIndex()}
 * will be invoked and the result will contain an {@link IborIndex}.
 *
 * @param <T>  the type of the target
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

  // produces a failure result
  private Result<?> tokenFailure(String reason, T object, String token) {
    List<String> orderedValidTokens = new ArrayList<String>(tokens(object));
    orderedValidTokens.sort(null);
    return Result.failure(FailureReason.INVALID_INPUT, "{} field: {}. Use one of: {}", reason, token, tokens(object));
  }

}
