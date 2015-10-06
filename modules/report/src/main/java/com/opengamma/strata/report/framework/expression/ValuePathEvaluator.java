/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.report.ReportCalculationResults;

/**
 * Evaluates a path describing a value to be shown in a trade report.
 * <p>
 * For example, if the expression is '{@code Product.index.name}' and the results contain {@link FraTrade} instances
 * the following calls will be made for each trade in the results:
 * <ul>
 *   <li>{@code FraTrade.getProduct()} returning a {@link Fra}</li>
 *   <li>{@code Fra.getIndex()} returning an {@link IborIndex}</li>
 *   <li>{@code IborIndex.getName()} returning the index name</li>
 * </ul>
 * The result of evaluating the expression is the index name.
 */
public class ValuePathEvaluator {

  /** The separator used in the value path. */
  private static final String PATH_SEPARATOR = "\\.";

  private static final ImmutableList<TokenEvaluator<?>> EVALUATORS = ImmutableList.of(
      new CurrencyAmountTokenEvaluator(),
      new MapTokenEvaluator(),
      new CurveCurrencyParameterSensitivitiesTokenEvaluator(),
      new CurveCurrencyParameterSensitivityTokenEvaluator(),
      new TradeTokenEvaluator(),
      new BeanTokenEvaluator(),
      new IterableTokenEvaluator());

  //-------------------------------------------------------------------------
  /**
   * Gets the measure encoded in a value path, if present.
   *
   * @param valuePath  the value path
   * @return the measure, if present
   */
  public static Optional<Measure> measure(String valuePath) {
    try {
      List<String> tokens = tokenize(valuePath);
      ValueRootType rootType = ValueRootType.parseToken(tokens.get(0));

      if (rootType != ValueRootType.MEASURES || tokens.size() < 2) {
        return Optional.empty();
      }
      Measure measure = Measure.of(tokens.get(1));
      return Optional.of(measure);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  /**
   * Evaluates a value path against a set of results, returning the resolved result for each trade.
   *
   * @param valuePath  the value path
   * @param results  the calculation results
   * @return the list of resolved results for each trade
   */
  public static List<Result<?>> evaluate(String valuePath, ReportCalculationResults results) {
    List<String> tokens = tokenize(valuePath);

    if (tokens.size() < 1) {
      return Collections.nCopies(
          results.getTrades().size(),
          Result.failure(FailureReason.INVALID_INPUT, "Column expressions must not be empty"));
    }
    int rowCount = results.getCalculationResults().getRowCount();
    return IntStream.range(0, rowCount)
        .mapToObj(rowIndex -> evaluate(tokens, RootEvaluator.INSTANCE, new ResultsRow(results, rowIndex)))
        .collect(toImmutableList());
  }

  // Tokens always has at least one token
  private static <T> Result<?> evaluate(List<String> tokens, TokenEvaluator<T> evaluator, T target) {
    EvaluationResult evaluationResult = evaluator.evaluate(target, tokens.get(0), tokens.subList(1, tokens.size()));

    if (evaluationResult.isComplete()) {
      return evaluationResult.getResult();
    }
    Object value = evaluationResult.getResult().getValue();
    Optional<TokenEvaluator<Object>> nextEvaluator = getEvaluator(value.getClass());

    return nextEvaluator.isPresent() ?
        evaluate(evaluationResult.getRemainingTokens(), nextEvaluator.get(), value) :
        noEvaluatorResult(value);
  }

  private static Result<?> noEvaluatorResult(Object value) {
    return Result.failure(
        FailureReason.INVALID_INPUT,
        "No evaluator available for objects of type {}",
        value.getClass().getName());
  }

  /**
   * Gets the supported tokens on the given object.
   *
   * @param object  the object for which to return the valid tokens
   * @return the tokens
   */
  public static Set<String> tokens(Object object) {
    return getEvaluator(object.getClass()).map(evaluator -> evaluator.tokens(object)).orElse(ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
  // splits a value path into tokens for processing
  private static List<String> tokenize(String valuePath) {
    String[] tokens = valuePath.split(PATH_SEPARATOR);
    return ImmutableList.copyOf(tokens);
  }

  @SuppressWarnings("unchecked")
  private static Optional<TokenEvaluator<Object>> getEvaluator(Class<?> targetClass) {
    return EVALUATORS.stream()
        .filter(e -> e.getTargetType().isAssignableFrom(targetClass))
        .map(e -> (TokenEvaluator<Object>) e)
        .findFirst();
  }

  //-------------------------------------------------------------------------
  private ValuePathEvaluator() {
  }

}
