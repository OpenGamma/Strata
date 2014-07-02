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
import com.opengamma.sesame.config.EmptyFunctionArguments;
import com.opengamma.sesame.config.FunctionArguments;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;
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
  private final CycleMarketDataFactory _cycleMarketDataFactory;
  private final VersionCorrection _configVersionCorrection;
  private final Set<Pair<Integer, Integer>> _traceCells;
  private final Set<String> _traceOutputs;
  private final FunctionArguments _functionArguments;
  private final Map<Class<?>, Object> _scenarioArguments;
  private final boolean _captureInputs;

  // TODO use a Cell class instead of Pair<Integer, Integer>
  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        CycleMarketDataFactory cycleMarketDataFactory) {
    this(valuationTime,
         configVersionCorrection,
         cycleMarketDataFactory,
         EmptyFunctionArguments.INSTANCE,
         Collections.<Class<?>, Object>emptyMap());
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        CycleMarketDataFactory cycleMarketDataFactory,
                        FunctionArguments functionArguments,
                        Map<Class<?>, Object> scenarioArguments) {
    this(valuationTime,
         configVersionCorrection, cycleMarketDataFactory,
         functionArguments,
         scenarioArguments,
         Collections.<Pair<Integer, Integer>>emptySet(),
         Collections.<String>emptySet());
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        CycleMarketDataFactory cycleMarketDataFactory,
                        FunctionArguments functionArguments,
                        Map<Class<?>, Object> scenarioArguments,
                        Set<Pair<Integer, Integer>> traceCells,
                        Set<String> traceOutputs) {
    this(valuationTime, configVersionCorrection, cycleMarketDataFactory, functionArguments,
         scenarioArguments, traceCells, traceOutputs, false);
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        CycleMarketDataFactory cycleMarketDataFactory,
                        boolean captureInputs) {
    this(valuationTime, configVersionCorrection, cycleMarketDataFactory, EmptyFunctionArguments.INSTANCE,
         ImmutableMap.<Class<?>, Object>of(), ImmutableSet.<Pair<Integer, Integer>>of(),
         ImmutableSet.<String>of(), captureInputs);
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        CycleMarketDataFactory cycleMarketDataFactory,
                        FunctionArguments functionArguments,
                        Map<Class<?>, Object> scenarioArguments, boolean captureInputs) {
    this(valuationTime, configVersionCorrection, cycleMarketDataFactory, functionArguments,
         scenarioArguments, ImmutableSet.<Pair<Integer, Integer>>of(),
         ImmutableSet.<String>of(), captureInputs);
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        CycleMarketDataFactory cycleMarketDataFactory,
                        FunctionArguments functionArguments,
                        Map<Class<?>, Object> scenarioArguments,
                        Set<Pair<Integer, Integer>> traceCells,
                        Set<String> traceOutputs,
                        boolean captureInputs) {
    _functionArguments = ArgumentChecker.notNull(functionArguments, "functionArguments");
    _scenarioArguments = ImmutableMap.copyOf(ArgumentChecker.notNull(scenarioArguments, "scenarioArguments"));
    _configVersionCorrection = ArgumentChecker.notNull(configVersionCorrection, "configVersionCorrection");
    _valuationTime = ArgumentChecker.notNull(valuationTime, "valuationTime");
    _cycleMarketDataFactory = ArgumentChecker.notNull(cycleMarketDataFactory, "cycleMarketDataFactory");
    _traceCells = ImmutableSet.copyOf(ArgumentChecker.notNull(traceCells, "traceCells"));
    _traceOutputs = ImmutableSet.copyOf(ArgumentChecker.notNull(traceOutputs, "traceOutputs"));
    _captureInputs = captureInputs;
  }

  ZonedDateTime getValuationTime() {
    return _valuationTime;
  }

  CycleMarketDataFactory getCycleMarketDataFactory() {
    return _cycleMarketDataFactory;
  }

  boolean isTracingEnabled(String output) {
    return _traceOutputs.contains(output);
  }

  VersionCorrection getConfigVersionCorrection() {
    return _configVersionCorrection;
  }

  FunctionArguments getFunctionArguments() {
    return _functionArguments;
  }

  Map<Class<?>, Object> getScenarioArguments() {
    return _scenarioArguments;
  }

  boolean isTracingEnabled(int rowIndex, int colIndex) {
    return _traceCells.contains(Pairs.of(rowIndex, colIndex));
  }

  public boolean isCaptureInputs() {
    return _captureInputs;
  }
}
