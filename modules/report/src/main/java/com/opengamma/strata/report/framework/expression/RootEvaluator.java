/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import java.util.List;
import java.util.Set;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.calc.runner.CalculationFunctions;

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
      ValueRootType.PRODUCT.token(),
      ValueRootType.SECURITY.token(),
      ValueRootType.TRADE.token(),
      ValueRootType.POSITION.token(),
      ValueRootType.TARGET.token());

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
  public EvaluationResult evaluate(
      ResultsRow resultsRow,
      CalculationFunctions functions,
      String firstToken,
      List<String> remainingTokens) {

    ValueRootType rootType = ValueRootType.parseToken(firstToken);
    switch (rootType) {
      case MEASURES:
        return evaluateMeasures(resultsRow, functions, remainingTokens);
      case PRODUCT:
        return EvaluationResult.of(resultsRow.getProduct(), remainingTokens);
      case SECURITY:
        return EvaluationResult.of(resultsRow.getSecurity(), remainingTokens);
      case TRADE:
        return EvaluationResult.of(resultsRow.getTrade(), remainingTokens);
      case POSITION:
        return EvaluationResult.of(resultsRow.getPosition(), remainingTokens);
      case TARGET:
        return EvaluationResult.success(resultsRow.getTarget(), remainingTokens);
      default:
        throw new IllegalArgumentException("Unknown root token '" + rootType.token() + "'");
    }
  }

  // find the result starting from a measure
  private EvaluationResult evaluateMeasures(
      ResultsRow resultsRow,
      CalculationFunctions functions,
      List<String> remainingTokens) {

    // if no measures, return list of valid measures
    if (remainingTokens.isEmpty() || Strings.nullToEmpty(remainingTokens.get(0)).trim().isEmpty()) {
      List<String> measureNames = ResultsRow.measureNames(resultsRow.getTarget(), functions);
      return EvaluationResult.failure("No measure specified. Use one of: {}", measureNames);
    }
    // evaluate the measure name
    String measureToken = remainingTokens.get(0);
    return EvaluationResult.of(
        resultsRow.getResult(measureToken), remainingTokens.subList(1, remainingTokens.size()));
  }

}
