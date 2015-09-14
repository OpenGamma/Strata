/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.result;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.joda.beans.Bean;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.finance.ProductTrade;
import com.opengamma.strata.finance.SecurityTrade;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.function.StandardComponents;
import com.opengamma.strata.report.ReportCalculationResults;

/**
 * Evaluates a path describing a value to be shown in a trade report.
 */
public class ValuePathEvaluator {

  /** The separator used in the value path */
  private static final String PATH_SEPARATOR = "\\.";

  private final ImmutableList<TokenEvaluator<?>> tokenEvaluators = ImmutableList.of(
      new CurrencyAmountTokenEvaluator(),
      new MapTokenEvaluator(),
      new CurveCurrencyParameterSensitivitiesTokenEvaluator(),
      new CurveCurrencyParameterSensitivityTokenEvaluator(),
      new TradeTokenEvaluator(),
      new BeanTokenEvaluator(),
      new IterableTokenEvaluator());

  /**
   * Gets the measure encoded in a value path, if present. 
   * 
   * @param valuePath  the value path
   * @return the measure, if present
   */
  public Optional<Measure> measure(String valuePath) {
    try {
      Queue<String> tokens = tokenize(valuePath);
      ValueRootType rootType = ValueRootType.parseToken(tokens.remove());
      if (rootType != ValueRootType.MEASURES) {
        return Optional.empty();
      }
      Measure measure = Measure.of(tokens.remove());
      return Optional.of(measure);
    } catch (Exception ex) {
      return Optional.empty();
    }
  }

  /**
   * Evaluates a value path against a set of results, returning the resolved result for
   * each trade.
   * 
   * @param valuePath  the value path
   * @param results  the calculation results
   * @return the list of resolved results for each trade
   */
  public List<Result<?>> evaluate(String valuePath, ReportCalculationResults results) {
    Queue<String> tokens = tokenize(valuePath);
    IntFunction<Result<?>> rootResultSupplier;
    try {
      ValueRootType rootType = ValueRootType.parseToken(tokens.remove());
      switch (rootType) {
        case MEASURES:
          rootResultSupplier = getMeasureSupplier(tokens, results);
          break;
        case PRODUCT:
          rootResultSupplier = getProductSupplier(results);
          break;
        case TRADE:
          rootResultSupplier = getTradeSupplier(results);
          break;
        default:
          throw new IllegalArgumentException(Messages.format("Unsupported root: {}", rootType.token()));
      }
    } catch (Exception ex) {
      rootResultSupplier = i -> Result.failure(FailureReason.INVALID_INPUT, ex.getMessage());
    }
    return IntStream.range(0, results.getCalculationResults().getRowCount())
        .mapToObj(rootResultSupplier)
        .map(r -> evaluate(r, new LinkedList<String>(tokens)))
        .collect(Collectors.toList());
  }

  /**
   * Gets the supported tokens on the given object.
   * 
   * @param object  the object for which to return the valid tokens
   * @return the tokens
   */
  public Set<String> tokens(Object object) {
    // This must mirror the main evaluate method implementation
    Object evalObject = object;
    Set<String> tokens = new HashSet<String>();
    Optional<TokenEvaluator<Object>> evaluator = getEvaluator(evalObject.getClass());
    if (evalObject instanceof Bean && !isTypeSpecificEvaluator(evaluator)) {
      Bean bean = (Bean) evalObject;
      if (bean.propertyNames().size() == 1) {
        String onlyProperty = Iterables.getOnlyElement(bean.propertyNames());
        tokens.add(onlyProperty);
        evalObject = bean.property(onlyProperty).get();
        evaluator = getEvaluator(evalObject.getClass());
      }
    }
    if (evaluator.isPresent()) {
      tokens.addAll(evaluator.get().tokens(evalObject));
    }
    return tokens;
  }

  //-------------------------------------------------------------------------
  // splits a value path into tokens for processing
  private Queue<String> tokenize(String valuePath) {
    String[] tokens = valuePath.split(PATH_SEPARATOR);
    return new LinkedList<String>(Arrays.asList(tokens));
  }

  // gets the result supplier for products
  private IntFunction<Result<?>> getProductSupplier(ReportCalculationResults results) {
    return i -> {
      Trade trade = results.getTrades().get(i);
      if (trade instanceof ProductTrade) {
        return Result.success(((ProductTrade<?>) trade).getProduct());
      }
      if (trade instanceof SecurityTrade) {
        return Result.success(((SecurityTrade<?>) trade).getProduct());
      }
      return Result.failure(FailureReason.INVALID_INPUT, "Trade does not contain a product");
    };
  }

  // gets the result supplier for trades
  private IntFunction<Result<?>> getTradeSupplier(ReportCalculationResults results) {
    return i -> Result.success(results.getTrades().get(i));
  }

  // gets the result supplier for measures
  private IntFunction<Result<?>> getMeasureSupplier(Queue<String> tokens, ReportCalculationResults results) {
    if (tokens.isEmpty() || Strings.nullToEmpty(tokens.peek()).trim().isEmpty()) {
      return i -> {
        Trade trade = results.getTrades().get(i);
        Set<Measure> validMeasures = StandardComponents.pricingRules().configuredMeasures(trade);
        List<String> measureNames = validMeasures.stream()
            .map(m -> m.toString())
            .collect(Collectors.toList());
        measureNames.sort(null);
        return Result.failure(FailureReason.INVALID_INPUT, "No measure specified. Use one of: {}", validMeasures);
      };
    }
    String measureToken = tokens.remove();
    Measure measure;
    try {
      measure = Measure.of(measureToken);
    } catch (Exception ex) {
      return i -> Result.failure(FailureReason.INVALID_INPUT, "Invalid measure name: {}", measureToken);
    }
    Column requiredColumn = Column.of(measure);
    int columnIdx = results.getColumns().indexOf(requiredColumn);
    if (columnIdx == -1) {
      return i -> Result.failure(FailureReason.INVALID_INPUT, "Measure not present: {}", measure);
    }
    return i -> results.getCalculationResults().get(i, columnIdx);
  }

  // evaluates a sequence of tokens against a result
  private Result<?> evaluate(Result<?> rootResult, Queue<String> tokens) {
    Result<?> result = rootResult;
    while (result.isSuccess() && !tokens.isEmpty()) {
      result = evaluateValue(result.getValue(), tokens);
    }
    return result;
  }

  // performs a single step of evaluation against an object
  private Result<?> evaluateValue(Object object, Queue<String> tokens) {
    Optional<TokenEvaluator<Object>> evaluator = getEvaluator(object.getClass());
    if (object instanceof Bean && !isTypeSpecificEvaluator(evaluator)) {
      Bean bean = (Bean) object;
      if (bean.propertyNames().size() == 1 && !evaluator.get().tokens(bean).contains(tokens.peek())) {
        // Allow single properties to be skipped over in the value path
        String singlePropertyName = Iterables.getOnlyElement(bean.propertyNames());
        return Result.success(bean.property(singlePropertyName).get());
      }
    }
    if (!evaluator.isPresent()) {
      return Result.failure(FailureReason.INVALID_INPUT, "Unable to drill into type {} to evaluate: {}",
          object.getClass().getSimpleName(), String.join(PATH_SEPARATOR, tokens));
    }
    return evaluator.get().evaluate(object, tokens.remove().toLowerCase());
  }

  @SuppressWarnings("unchecked")
  private Optional<TokenEvaluator<Object>> getEvaluator(Class<?> targetClazz) {
    return tokenEvaluators.stream()
        .filter(e -> e.getTargetType().isAssignableFrom(targetClazz))
        .map(e -> (TokenEvaluator<Object>) e)
        .findFirst();
  }

  private boolean isTypeSpecificEvaluator(Optional<TokenEvaluator<Object>> evaluator) {
    return evaluator.isPresent() && !Bean.class.equals(evaluator.get().getTargetType());
  }

}
