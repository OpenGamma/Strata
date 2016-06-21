/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Set;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.product.fra.Fra;

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
   * @param target  the object against which to evaluate the token
   * @param functions  the calculation functions
   * @param firstToken  the first token of the expression
   * @param remainingTokens  the remaining tokens in the expression, possibly empty
   * @return the result of the evaluation
   */
  public abstract EvaluationResult evaluate(
      T target,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens);

  //-------------------------------------------------------------------------
  /**
   * Generates a failure result for an invalid token.
   * 
   * @param object  the object against which the failure occurred
   * @param token  the invalid token
   * @return the failure result
   */
  protected EvaluationResult invalidTokenFailure(T object, String token) {
    return tokenFailure("Invalid", object, token);
  }

  /**
   * Generates a failure result for an ambiguous token.
   * 
   * @param object  the object against which the failure occurred.
   * @param token  the ambiguous token
   * @return the failure result
   */
  protected EvaluationResult ambiguousTokenFailure(T object, String token) {
    return tokenFailure("Ambiguous", object, token);
  }

  // produces a failure result
  private EvaluationResult tokenFailure(String reason, T object, String token) {
    List<String> orderedValidTokens = tokens(object).stream().sorted().collect(toImmutableList());
    return EvaluationResult.failure(
        "{} field '{}' in type {}. Use one of: {}",
        reason,
        token,
        object.getClass().getName(),
        orderedValidTokens);
  }

}
