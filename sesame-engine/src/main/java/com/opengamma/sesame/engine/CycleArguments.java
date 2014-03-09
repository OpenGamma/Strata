/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collections;
import java.util.Set;

import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableSet;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.util.tuple.Pairs;

/**
 * TODO this will probably need to be a joda bean for serialization
 */
public final class CycleArguments {

  // TODO function arguments for the output functions
  // TODO portfolio version correction

  private final ZonedDateTime _valuationTime;
  private final MarketDataSource _marketDataSource;
  private final VersionCorrection _configVersionCorrection;
  // TODO this isn't part of the key used for caching. put in a subclass or something?
  private final Set<Pair<Integer, Integer>> _traceCells;
  private final Set<String> _traceOutputs;

  // TODO use a Cell class instead of Pair<Integer, Integer>
  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        MarketDataSource marketDataSource) {
    this(valuationTime,
         marketDataSource,
         configVersionCorrection,
         Collections.<Pair<Integer, Integer>>emptySet(),
         Collections.<String>emptySet());
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        MarketDataSource marketDataSource,
                        VersionCorrection configVersionCorrection,
                        Set<Pair<Integer, Integer>> traceCells,
                        Set<String> traceOutputs) {
    _configVersionCorrection = ArgumentChecker.notNull(configVersionCorrection, "configVersionCorrection");
    _valuationTime = ArgumentChecker.notNull(valuationTime, "valuationTime");
    _marketDataSource = ArgumentChecker.notNull(marketDataSource, "marketDataSource");
    _traceCells = ImmutableSet.copyOf(ArgumentChecker.notNull(traceCells, "traceCells"));
    _traceOutputs = ImmutableSet.copyOf(ArgumentChecker.notNull(traceOutputs, "traceOutputs"));
  }

  /* package */ ZonedDateTime getValuationTime() {
    return _valuationTime;
  }

  /* package */ MarketDataSource getMarketDataSource() {
    return _marketDataSource;
  }

  /* package */ boolean isTracingEnabled(String output) {
    return _traceOutputs.contains(output);
  }

  /* package */ boolean isTracingEnabled(int rowIndex, int colIndex) {
    return _traceCells.contains(Pairs.of(rowIndex, colIndex));
  }

  /* package */ VersionCorrection getConfigVersionCorrection() {
    return _configVersionCorrection;
  }
}
