/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * The result of a {@link TokenEvaluator} evaluating an expression against an object.
 * <p>
 * The result contains the result of the evaluation and the remaining tokens in the expression.
 */
public final class EvaluationResult {

  /**
   * The result of evaluating the expression against the object.
   */
  private final Result<?> result;
  /**
   * The tokens remaining in the expression after evaluation.
   */
  private final List<String> remainingTokens;

  //-------------------------------------------------------------------------
  /**
   * Creates the result of successfully evaluating a token against an object.
   *
   * @param value  the result of evaluating the expression against the object
   * @param remainingTokens  the tokens remaining in the expression after evaluation
   * @return the result of successfully evaluating a token against an object
   */
  public static EvaluationResult success(Object value, List<String> remainingTokens) {
    return new EvaluationResult(Result.success(value), remainingTokens);
  }

  /**
   * Creates a result for an unsuccessful evaluation of an expression.
   *
   * @param message  the error message
   * @param messageValues  values substituted into the error message. See {@link Messages#format(String, Object...)}
   *   for details
   * @return the result of an unsuccessful evaluation of an expression
   */
  public static EvaluationResult failure(String message, Object... messageValues) {
    String msg = Messages.format(message, messageValues);
    return new EvaluationResult(Result.failure(FailureReason.INVALID, msg), ImmutableList.of());
  }

  /**
   * Creates the result of evaluating a token against an object.
   *
   * @param result  the result of evaluating the expression against the object
   * @param remainingTokens  the tokens remaining in the expression after evaluation
   * @return the result of evaluating a token against an object
   */
  public static EvaluationResult of(Result<?> result, List<String> remainingTokens) {
    return new EvaluationResult(result, remainingTokens);
  }

  // restricted constructor
  private EvaluationResult(Result<?> result, List<String> remainingTokens) {
    this.result = result;
    this.remainingTokens = remainingTokens;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the result of evaluating the expression against the object.
   *
   * @return the result of evaluating the expression against the object
   */
  public Result<?> getResult() {
    return result;
  }

  /**
   * Returns the tokens remaining in the expression after evaluation.
   *
   * @return the tokens remaining in the expression after evaluation
   */
  public List<String> getRemainingTokens() {
    return remainingTokens;
  }

  /**
   * Returns true if evaluation of the whole expression is complete.
   * <p>
   * This occurs if the evaluation failed or all tokens in the expression have been consumed.
   *
   * @return returns true if evaluation of the whole expression is complete
   */
  public boolean isComplete() {
    return getResult().isFailure() || getRemainingTokens().isEmpty();
  }

}
