/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.id.VersionCorrection;
import com.opengamma.sesame.config.FunctionArguments;
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
  private final Set<Pair<Integer, Integer>> _traceCells;
  private final Set<String> _traceOutputs;
  private final FunctionArguments _functionArguments;
  private final Map<Class<?>, Object> _scenarioArguments;

  // TODO use a Cell class instead of Pair<Integer, Integer>
  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        MarketDataSource marketDataSource) {
    this(valuationTime,
         configVersionCorrection,
         marketDataSource,
         FunctionArguments.EMPTY,
         Collections.<Class<?>, Object>emptyMap());
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        MarketDataSource marketDataSource,
                        FunctionArguments functionArguments,
                        Map<Class<?>, Object> scenarioArguments) {
    this(valuationTime,
         configVersionCorrection, marketDataSource,
         functionArguments,
         scenarioArguments,
         Collections.<Pair<Integer, Integer>>emptySet(),
         Collections.<String>emptySet());
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        MarketDataSource marketDataSource,
                        FunctionArguments functionArguments,
                        Map<Class<?>, Object> scenarioArguments,
                        Set<Pair<Integer, Integer>> traceCells,
                        Set<String> traceOutputs) {
    _functionArguments = ArgumentChecker.notNull(functionArguments, "functionArguments");
    _scenarioArguments = ImmutableMap.copyOf(ArgumentChecker.notNull(scenarioArguments, "scenarioArguments"));
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

  /* package */ VersionCorrection getConfigVersionCorrection() {
    return _configVersionCorrection;
  }

  /* package */ FunctionArguments getFunctionArguments() {
    return _functionArguments;
  }

  /* package */ Map<Class<?>, Object> getScenarioArguments() {
    return _scenarioArguments;
  }

  /* package */ boolean isTracingEnabled(int rowIndex, int colIndex) {
    return _traceCells.contains(Pairs.of(rowIndex, colIndex));
  }
}
