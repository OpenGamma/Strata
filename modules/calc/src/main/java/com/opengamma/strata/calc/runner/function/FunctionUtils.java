/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner.function;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.runner.function.result.CurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.DefaultScenarioResult;
import com.opengamma.strata.calc.runner.function.result.FxConvertibleList;
import com.opengamma.strata.calc.runner.function.result.MultiCurrencyValuesArray;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ValuesArray;

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
  public static <T extends FxConvertible<?>> Collector<T, ImmutableList.Builder<T>, FxConvertibleList<T>> toFxConvertibleList() {

    // edited to compile in Eclipse
    return Collector.of(
        ImmutableList.Builder<T>::new,
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
        (l, r) -> {
          l.addAll(r);
          return l;
        },
        list -> buildResult(list, convertCurrencies));
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

  //-------------------------------------------------------------------------
  /**
   * Returns a collector that builds a multi-currency scenerio result.
   * <p>
   * This is used at the end of a stream to collect per-scenario instances of {@link MultiCurrencyAmount}
   * into a single instance of {@link MultiCurrencyValuesArray}, which is designed to be space-efficient.
   *
   * @return a collector used at the end of a stream of {@link MultiCurrencyAmount}
   *   to build a {@link MultiCurrencyValuesArray}
   */
  public static Collector<MultiCurrencyAmount, List<MultiCurrencyAmount>, MultiCurrencyValuesArray> toMultiCurrencyValuesArray() {
    return Collector.of(
        ArrayList<MultiCurrencyAmount>::new,
        (a, b) -> a.add(b),
        (l, r) -> {
          l.addAll(r);
          return l;
        },
        list -> MultiCurrencyValuesArray.of(list));
  }

  /**
   * Returns a collector that builds a single-currency scenerio result.
   * <p>
   * This is used at the end of a stream to collect per-scenario instances of {@link CurrencyAmount}
   * into a single instance of {@link CurrencyValuesArray}, which is designed to be space-efficient.
   *
   * @return a collector used at the end of a stream of {@link CurrencyAmount} to build a {@link CurrencyValuesArray}
   */
  public static Collector<CurrencyAmount, List<CurrencyAmount>, CurrencyValuesArray> toCurrencyValuesArray() {
    return Collector.of(
        ArrayList<CurrencyAmount>::new,
        (a, b) -> a.add(b),
        (l, r) -> {
          l.addAll(r);
          return l;
        },
        list -> CurrencyValuesArray.of(list));
  }

  /**
   * Returns a collector that builds a scenerio result based on {@code Double}.
   * <p>
   * This is used at the end of a stream to collect per-scenario instances of {@code Double}
   * into a single instance of {@link ValuesArray}, which is designed to be space-efficient.
   * <p>
   * Note that {@link DoubleStream} does not support collectors, which makes this less efficient
   * than it should be.
   *
   * @return a collector used at the end of a stream of {@link Double} to build a {@link ValuesArray}
   */
  public static Collector<Double, List<Double>, ValuesArray> toValuesArray() {
    return Collector.of(
        ArrayList<Double>::new,
        (a, b) -> a.add(b),
        (l, r) -> {
          l.addAll(r);
          return l;
        },
        list -> ValuesArray.of(list));
  }

}
