/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.CurrencyValuesArray;
import com.opengamma.strata.data.scenario.MultiCurrencyValuesArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ValuesArray;

/**
 * Static utility methods useful when writing calculation functions.
 */
public final class FunctionUtils {

  // Private constructor because this only contains static helper methods.
  private FunctionUtils() {
  }

  /**
   * Returns a collector which can be used at the end of a stream of results to build a {@link ScenarioArray}.
   *
   * @param <T> the type of the results in the stream
   * @return a collector used to create a {@code CurrencyAmountList} from a stream of {@code CurrencyAmount}
   */
  public static <T> Collector<T, List<T>, ScenarioArray<T>> toScenarioArray() {
    // edited to compile in Eclipse
    return Collector.of(
        ArrayList<T>::new,
        (a, b) -> a.add(b),
        (l, r) -> {
          l.addAll(r);
          return l;
        },
        list -> buildResult(list));
  }

  @SuppressWarnings("unchecked")
  private static <T, R> ScenarioArray<T> buildResult(List<T> results) {
    return ScenarioArray.of(results);
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
   * Returns a collector that builds a scenario result based on {@code Double}.
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

  /**
   * Checks if a map of results contains a value for a key, and if it does inserts it into the map for a different key.
   *
   * @param existingKey  a key for which the map possibly contains a value
   * @param newKey  the key which is inserted into the map
   * @param mutableMeasureMap  a mutable map of values, keyed by measure
   */
  public static void duplicateResult(Measure existingKey, Measure newKey, Map<Measure, Result<?>> mutableMeasureMap) {
    Result<?> result = mutableMeasureMap.get(existingKey);

    if (result == null) {
      return;
    }
    mutableMeasureMap.put(newKey, result);
  }

}
