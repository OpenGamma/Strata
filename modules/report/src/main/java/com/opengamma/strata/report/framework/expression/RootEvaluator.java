/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Evaluator that evaluates the first token in the expression.
 * <p>
 * The supported values for the first token are enumerated in {@link ValueRootType}.
 */
class RootEvaluator extends TokenEvaluator<ResultsRow> {

  /** The single shared instance of this class. */
  static final RootEvaluator INSTANCE = new RootEvaluator();

  private static final ImmutableSet<String> TOKENS = ImmutableSet.of(
      ValueRootType.MEASURES.token(),
      ValueRootType.TRADE.token(),
      ValueRootType.PRODUCT.token());

  @Override
  public Class<?> getTargetType() {
    // This isn't used because the root parser has special treatment
    return ResultsRow.class;
  }

  @Override
  public Set<String> tokens(ResultsRow target) {
    return TOKENS;
  }

  @Override
  public EvaluationResult evaluate(ResultsRow resultsRow, String firstToken, List<String> remainingTokens) {
    ValueRootType rootType = ValueRootType.parseToken(firstToken);

    switch (rootType) {
      case MEASURES:
        return remainingTokens.isEmpty() ?
            EvaluationResult.failure("A measure name must be specified when selecting a measure") :
            EvaluationResult.of(
                resultsRow.getResult(remainingTokens.get(0)),
                remainingTokens.subList(1, remainingTokens.size()));
      case PRODUCT:
        return EvaluationResult.of(resultsRow.getProduct(), remainingTokens);
      case TRADE:
        return EvaluationResult.success(resultsRow.getTrade(), remainingTokens);
      default:
        throw new IllegalArgumentException("Unknown root token '" + rootType.token() + "'");
    }
  }

}
