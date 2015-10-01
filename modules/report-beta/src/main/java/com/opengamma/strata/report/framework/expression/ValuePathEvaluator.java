/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.expression;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import org.joda.beans.Bean;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.type.TypedString;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.finance.ProductTrade;
import com.opengamma.strata.finance.SecurityTrade;
import com.opengamma.strata.finance.Trade;
import com.opengamma.strata.finance.rate.fra.Fra;
import com.opengamma.strata.finance.rate.fra.FraTrade;
import com.opengamma.strata.function.StandardComponents;
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
    IntFunction<Result<?>> rootResultSupplier;
    // The first token is Measure, Product or Trade. It is consumed by this method
    List<String> remainingTokens = tokens.subList(1, tokens.size());

    try {
      ValueRootType rootType = ValueRootType.parseToken(tokens.get(0));

      switch (rootType) {
        case MEASURES:
          rootResultSupplier = getMeasureSupplier(remainingTokens, results);
          // The second token after "Measure" is the measure name which is consumed by the measure supplier
          remainingTokens = remainingTokens.subList(1, remainingTokens.size());
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
    // This is necessary to make the compiler happy
    List<String> finalRemainingTokens = remainingTokens;

    return IntStream.range(0, results.getCalculationResults().getRowCount())
        .mapToObj(rootResultSupplier)
        .map(r -> evaluate(r, finalRemainingTokens))
        .collect(toImmutableList());
  }

  /**
   * Gets the supported tokens on the given object.
   * 
   * @param object  the object for which to return the valid tokens
   * @return the tokens
   */
  public static Set<String> tokens(Object object) {
    // This must mirror the main evaluate method implementation
    Object evalObject = object;
    Set<String> tokens = new HashSet<>();
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
  private static List<String> tokenize(String valuePath) {
    String[] tokens = valuePath.split(PATH_SEPARATOR);
    return ImmutableList.copyOf(tokens);
  }

  /**
   * Returns a function whose input is the index of a trade in the results and whose return value contains
   * the trade's product.
   * <p>
   * If the trade is not a {@link ProductTrade} a failure result is returned.
   *
   * @param results  calculation results containing the trades for which the calculations were performed
   * @return a function whose input is the index of a trade in the results and whose return value contains
   *   the trade's product
   */
  private static IntFunction<Result<?>> getProductSupplier(ReportCalculationResults results) {
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

  /**
   * Returns a function whose input is the index of a trade in the results and whose return value contains the trade.
   *
   * @param results  calculation results containing the trades for which the calculations were performed
   * @return a function whose input is the index of a trade in the results and whose return value contains the trade
   */
  private static IntFunction<Result<?>> getTradeSupplier(ReportCalculationResults results) {
    return i -> Result.success(results.getTrades().get(i));
  }

  /**
   * Returns a function whose input is the index of a trade in the results and whose return value contains
   * the calculated value for a measure.
   *
   * @param results  calculation results containing the trades for which the calculations were performed
   * @return a function whose input is the index of a trade in the results and whose return value contains
   *   the calculated value for a measure
   */
  private static IntFunction<Result<?>> getMeasureSupplier(List<String> tokens, ReportCalculationResults results) {
    if (tokens.isEmpty() || Strings.nullToEmpty(tokens.get(0)).trim().isEmpty()) {
      return i -> {
        Trade trade = results.getTrades().get(i);
        Set<Measure> validMeasures = StandardComponents.pricingRules().configuredMeasures(trade);
        List<String> measureNames = validMeasures.stream()
            .map(TypedString::toString)
            .collect(toImmutableList());
        measureNames.sort(Ordering.natural());
        return Result.failure(FailureReason.INVALID_INPUT, "No measure specified. Use one of: {}", validMeasures);
      };
    }
    String measureToken = tokens.get(0);
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

  /**
   * Evaluates an expression to extract a value from an object.
   * <p>
   * For example, if the root value is a {@link Fra} and the expression is '{@code index.name}', the tokens will be
   * {@code ['index', 'name']} and this method will call:
   * <ul>
   *   <li>{@code Fra.getIndex()}, returning an {@code IborIndex}</li>
   *   <li>{@code IborIndex.getName()} returning the index name</li>
   * </ul>
   * The return value of this method will be the index name.
   *
   * @param rootObject  the object against which the expression is evaluated
   * @param tokens  the individual tokens making up the expression
   * @return the result of evaluating the expression against the object
   */
  static private Result<?> evaluate(Result<?> rootObject, List<String> tokens) {
    Result<?> result = rootObject;

    for (String token : tokens) {
      if (result.isFailure()) {
        return result;
      }
      Object value = result.getValue();
      Optional<TokenEvaluator<Object>> evaluator = getEvaluator(value.getClass());

      if (!evaluator.isPresent()) {
        return Result.failure(
            FailureReason.INVALID_INPUT,
            "Failed to evaluate value. Path: {}. No evaluator found for type {}",
            String.join(PATH_SEPARATOR, tokens),
            value.getClass().getSimpleName());
      }
      if (value instanceof Bean && !isTypeSpecificEvaluator(evaluator)) {
        Bean bean = (Bean) value;

        if (bean.propertyNames().size() == 1 && !evaluator.get().tokens(bean).contains(token)) {
          // Allow single properties to be skipped over in the value path
          String singlePropertyName = Iterables.getOnlyElement(bean.propertyNames());
          value = bean.property(singlePropertyName).get();
          evaluator = getEvaluator(value.getClass());

          if (!evaluator.isPresent()) {
            return Result.failure(
                FailureReason.INVALID_INPUT,
                "Failed to evaluate value. Path: {}. No evaluator found for type {}",
                String.join(PATH_SEPARATOR, tokens),
                value.getClass().getSimpleName());
          }
        }
      }
      result = evaluator.get().evaluate(value, token.toLowerCase());
    }
    return result;
  }

  @SuppressWarnings("unchecked")
  private static Optional<TokenEvaluator<Object>> getEvaluator(Class<?> targetClazz) {
    return EVALUATORS.stream()
        .filter(e -> e.getTargetType().isAssignableFrom(targetClazz))
        .map(e -> (TokenEvaluator<Object>) e)
        .findFirst();
  }

  private static boolean isTypeSpecificEvaluator(Optional<TokenEvaluator<Object>> evaluator) {
    return evaluator.isPresent() && !Bean.class.equals(evaluator.get().getTargetType());
  }

  //-------------------------------------------------------------------------
  // restricted constrctor
  private ValuePathEvaluator() {
  }

}
