/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations.function;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.engine.calculations.function.result.CurrencyAmountList;
import com.opengamma.strata.engine.calculations.function.result.DefaultScenarioResult;
import com.opengamma.strata.engine.calculations.function.result.MultiCurrencyAmountList;
import com.opengamma.strata.engine.calculations.function.result.ScenarioResult;

/**
 * Static utility methods useful when writing calculation functions.
 */
public final class FunctionUtils {

  // Private constructor because this only contains static helper methods.
  private FunctionUtils() {
  }

  /**
   * Returns a collector which can be used at the end of a stream of {@link MultiCurrencyAmount}
   * to build a {@link MultiCurrencyAmountList}.
   *
   * @return a collector used to create a {@code MultiCurrencyAmountList} from a stream of {@code MultiCurrencyAmount}
   */
  public static Collector<MultiCurrencyAmount, ImmutableList.Builder<MultiCurrencyAmount>, MultiCurrencyAmountList>
      toMultiCurrencyAmountList() {

    // edited to compile in Eclipse
    return Collector.of(
        ImmutableList.Builder<MultiCurrencyAmount>::new,
        (bld, v) -> bld.add(v),
        (l, r) -> l.addAll(r.build()),
        builder -> MultiCurrencyAmountList.of(builder.build()));
  }

  /**
   * Returns a collector which can be used at the end of a stream of {@link CurrencyAmount}
   * to build a {@link CurrencyAmountList}.
   *
   * @return a collector used to create a {@code CurrencyAmountList} from a stream of {@code CurrencyAmount}
   */
  public static Collector<CurrencyAmount, ImmutableList.Builder<CurrencyAmount>, CurrencyAmountList>
      toCurrencyAmountList() {

    // edited to compile in Eclipse
    return Collector.of(
        ImmutableList.Builder<CurrencyAmount>::new,
        (bld, v) -> bld.add(v),
        (l, r) -> l.addAll(r.build()),
        builder -> CurrencyAmountList.of(builder.build()));
  }

  /**
   * Returns a collector which can be used at the end of a stream of results to build a {@link ScenarioResult}
   * which will support automatic currency conversion where possible.
   * <p>
   * If the currency of the result shouldn't be converted, for example when the results contain notional
   * amounts, call {@link #toScenarioResult(boolean)} specifying {@code convertCurrencies = false}.
   * <p>
   * If the results are all instances of {@link CurrencyAmount} a {@link CurrencyAmountList} is created which
   * can be automatically converted to the reporting currency by the engine.
   * <p>
   * If the results are all instances of {@link MultiCurrencyAmount} a {@link MultiCurrencyAmountList} is created which
   * can be automatically converted to the reporting currency by the engine.
   *
   * @param <T> the type of the results in the stream
   * @return a collector used to create a {@code CurrencyAmountList} from a stream of {@code CurrencyAmount}
   */
  public static <T> Collector<T, List<T>, ScenarioResult<T>> toScenarioResult() {
    return toScenarioResult(true);
  }

  /**
   * Returns a collector which can be used at the end of a stream of results to build a {@link ScenarioResult}.
   * <p>
   * If {@code convertCurrencies} is true the returned result will support automatic currency conversion if
   * the underlying results support it.
   * <p>
   * If the results are all instances of {@link CurrencyAmount} and the {@code convertCurrencies}
   * flag is true a {@link CurrencyAmountList} is created. This can be automatically converted to the
   * reporting currency by the engine.
   * <p>
   * If the results are all instances of {@link MultiCurrencyAmount} and the {@code convertCurrencies}
   * flag is true a {@link MultiCurrencyAmountList} is created. This can be automatically converted to the
   * reporting currency by the engine.
   *
   * @param convertCurrencies  if this is true the results will be wrapped in an object supporting automatic
   *   currency conversion where possible. If the individual results cannot be automatically converted to
   *   another currency this flag has no effect
   * @param <T> the type of the results in the stream
   * @return a collector used to create a {@code CurrencyAmountList} from a stream of {@code CurrencyAmount}
   */
  public static <T> Collector<T, List<T>, ScenarioResult<T>> toScenarioResult(boolean convertCurrencies) {
    // edited to compile in Eclipse
    return Collector.of(
        ArrayList<T>::new,
        (a, b) -> a.add(b),
        (l, r) -> { l.addAll(r); return l; },
        list -> buildResult(list, convertCurrencies));
  }

  @SuppressWarnings("unchecked")
  private static <T> ScenarioResult<T> buildResult(List<T> results, boolean convertCurrencies) {
    // If currency conversion isn't required return a result that doesn't implement CurrencyConvertible
    // and the engine won't try to convert it.
    if (!convertCurrencies) {
      return DefaultScenarioResult.of(results);
    }
    // Build the set of all types in the results
    Set<Class<?>> resultTypes = results.stream().map(Object::getClass).collect(toSet());

    if (resultTypes.size() == 1 && resultTypes.contains(CurrencyAmount.class)) {
      // This casts are definitely safe, the results list only contains CurrencyAmounts
      List<CurrencyAmount> currencyAmounts = (List<CurrencyAmount>) results;
      return (ScenarioResult<T>) CurrencyAmountList.of(currencyAmounts);
    }
    if (resultTypes.size() == 1 && resultTypes.contains(MultiCurrencyAmount.class)) {
      // This casts are definitely safe, the results list only contains MultiCurrencyAmounts
      List<MultiCurrencyAmount> currencyAmounts = (List<MultiCurrencyAmount>) results;
      return (ScenarioResult<T>) MultiCurrencyAmountList.of(currencyAmounts);
    }
    return DefaultScenarioResult.of(results);
  }
}
