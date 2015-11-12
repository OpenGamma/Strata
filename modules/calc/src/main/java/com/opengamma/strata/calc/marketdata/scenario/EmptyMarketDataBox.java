/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.scenario;

import java.util.function.BiFunction;
import java.util.function.Function;

import com.opengamma.strata.basics.market.ScenarioMarketDataValue;
import com.opengamma.strata.collect.function.ObjIntFunction;
import com.opengamma.strata.collect.result.Result;

/**
 * A market data box that contains no data.
 */
class EmptyMarketDataBox implements MarketDataBox<Void> {

  /** The single shared instance of this class. */
  private static final EmptyMarketDataBox INSTANCE = new EmptyMarketDataBox();

  /**
   * Returns a market data box containing no data.
   *
   * @param <T>  the required type of the market data box
   * @return a market data box containing no data
   */
  @SuppressWarnings("unchecked")
  static <T> MarketDataBox<T> empty() {
    return (MarketDataBox<T>) INSTANCE;
  }

  @Override
  public Void getSingleValue() {
    throw new UnsupportedOperationException("Cannot get a value from an empty market data box");
  }

  @Override
  public ScenarioMarketDataValue<Void> getScenarioValue() {
    throw new UnsupportedOperationException("Cannot get a value from an empty market data box");
  }

  @Override
  public Void getValue(int scenarioIndex) {
    throw new UnsupportedOperationException("Cannot get a value from an empty market data box");
  }

  @Override
  public boolean isSingleValue() {
    throw new UnsupportedOperationException("Box is empty");
  }

  @Override
  public <R> Result<MarketDataBox<R>> apply(Function<Void, Result<R>> fn) {
    return Result.success(empty());
  }

  @Override
  public <U, R> Result<MarketDataBox<R>> combineWith(MarketDataBox<U> other, BiFunction<Void, U, Result<R>> fn) {
    return Result.success(empty());
  }

  @Override
  public <R> MarketDataBox<R> apply(int scenarioCount, ObjIntFunction<Void, R> fn) {
    return empty();
  }

  @Override
  public int getScenarioCount() {
    return 0;
  }

  @Override
  public Class<Void> getMarketDataType() {
    return Void.class;
  }
}
