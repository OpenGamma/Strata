/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.runner.function.result.DefaultScenarioResult;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;

/**
 * Static utility methods useful when writing calculation functions.
 */
public final class FunctionUtils {

  // Private constructor because this only contains static helper methods.
  private FunctionUtils() {
  }

  /**
   * Returns a collector which can be used at the end of a stream of {@link FxConvertible}
   * to build a {@link FxConvertibleList}.
   *
   * @return a collector used to create a {@code FxConvertibleList} from a stream of {@code FxConvertible}
   */
  public static Collector<FxConvertible<?>, ImmutableList.Builder<FxConvertible<?>>, FxConvertibleList>
      toFxConvertibleList() {

    // edited to compile in Eclipse
    return Collector.of(
        ImmutableList.Builder<FxConvertible<?>>::new,
        (bld, v) -> bld.add(v),
        (l, r) -> l.addAll(r.build()),
        builder -> FxConvertibleList.of(builder.build()));
  }

  /**
   * Returns a collector which can be used at the end of a stream of results to build a {@link ScenarioResult}
   * which will support automatic currency conversion where possible.
   * <p>
   * If the currency of the result shouldn't be converted, for example when the results contain notional
   * amounts, call {@link #toScenarioResult(boolean)} specifying {@code convertCurrencies = false}.
   * <p>
   * If the results are all instances of {@link FxConvertible} and the {@code convertCurrencies}
   * flag is true an {@link FxConvertibleList} is created. This can be automatically converted to the
   * reporting currency by the engine.
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
   * If the results are all instances of {@link FxConvertible} and the {@code convertCurrencies}
   * flag is true an {@link FxConvertibleList} is created. This can be automatically converted to the
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

  /**
   * Returns a collector which can be used at the end of a stream of {@link MultiCurrencyAmount} to build a
   * {@link MultiCurrencyValuesArray}.
   * <p>
   * If a function invokes a pricer for each scenario and the pricer returns {@link MultiCurrencyAmount} it
   * is more efficient to use to this collector that to return a collection of {@link MultiCurrencyAmount} instances.
   *
   * @return a collector which can be used at the end of a stream of {@link MultiCurrencyAmount} to build a
   *   {@link MultiCurrencyValuesArray}
   */
  public static Collector<MultiCurrencyAmount, List<MultiCurrencyAmount>, MultiCurrencyValuesArray> toMultiCurrencyArray() {
    return Collector.of(
        ArrayList<MultiCurrencyAmount>::new,
        (a, b) -> a.add(b),
        (l, r) -> { l.addAll(r); return l; },
        list -> MultiCurrencyValuesArray.of(list));
  }

  @SuppressWarnings("unchecked")
  private static <T, R> ScenarioResult<T> buildResult(List<T> results, boolean convertCurrencies) {
    // If currency conversion isn't required return a result that doesn't implement CurrencyConvertible
    // and the engine won't try to convert it.
    if (!convertCurrencies) {
      return DefaultScenarioResult.of(results);
    }
    // If all the results are FxConvertible wrap in a type that implements CurrencyConvertible
    if (results.stream().allMatch(FxConvertible.class::isInstance)) {
      List<FxConvertible<R>> convertibleResults = (List<FxConvertible<R>>) results;
      return (ScenarioResult<T>) FxConvertibleList.of(convertibleResults);
    }
    return DefaultScenarioResult.of(results);
  }
}
