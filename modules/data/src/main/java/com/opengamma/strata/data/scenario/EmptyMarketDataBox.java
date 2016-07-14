/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data.scenario;

import java.io.Serializable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.opengamma.strata.collect.function.ObjIntFunction;

/**
 * A market data box that contains no data.
 */
class EmptyMarketDataBox
    implements MarketDataBox<Void>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /** The single shared instance of this class. */
  private static final EmptyMarketDataBox INSTANCE = new EmptyMarketDataBox();

  /**
   * Obtains an instance containing no data.
   *
   * @param <T>  the required type of the market data box
   * @return a market data box containing no data
   */
  @SuppressWarnings("unchecked")
  static <T> MarketDataBox<T> empty() {
    return (MarketDataBox<T>) INSTANCE;
  }

  //-------------------------------------------------------------------------
  @Override
  public Void getSingleValue() {
    throw new UnsupportedOperationException("Cannot get a value from an empty market data box");
  }

  @Override
  public ScenarioArray<Void> getScenarioValue() {
    throw new UnsupportedOperationException("Cannot get a value from an empty market data box");
  }

  @Override
  public Void getValue(int scenarioIndex) {
    throw new UnsupportedOperationException("Cannot get a value from an empty market data box");
  }

  @Override
  public boolean isSingleValue() {
    return true;
  }

  @Override
  public <R> MarketDataBox<R> map(Function<Void, R> fn) {
    return empty();
  }

  @Override
  public <U, R> MarketDataBox<R> combineWith(MarketDataBox<U> other, BiFunction<Void, U, R> fn) {
    return empty();
  }

  @Override
  public <R> MarketDataBox<R> mapWithIndex(int scenarioCount, ObjIntFunction<Void, R> fn) {
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

  @Override
  public Stream<Void> stream() {
    return Stream.empty();
  }
}
