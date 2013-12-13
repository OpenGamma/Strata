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
import com.opengamma.sesame.marketdata.MarketDataFactory;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;

/**
 *
 */
public final class CycleArguments {

  // TODO currency? or is that a view property
  // TODO function arguments for the output function

  private final ZonedDateTime _valuationTime;
  private final MarketDataFactory _marketDataFactory;
  private final Set<Pair<Integer, Integer>> _traceFunctions;
  private final VersionCorrection _configVersionCorrection;

  public CycleArguments(ZonedDateTime valuationTime,
                        VersionCorrection configVersionCorrection,
                        MarketDataFactory marketDataFactory) {
    this(valuationTime, marketDataFactory, configVersionCorrection, Collections.<Pair<Integer, Integer>>emptySet());
  }

  public CycleArguments(ZonedDateTime valuationTime,
                        MarketDataFactory marketDataFactory,
                        VersionCorrection configVersionCorrection,
                        Set<Pair<Integer, Integer>> traceFunctions) {
    _configVersionCorrection = ArgumentChecker.notNull(configVersionCorrection, "configVersionCorrection");
    _valuationTime = ArgumentChecker.notNull(valuationTime, "valuationTime");
    _marketDataFactory = ArgumentChecker.notNull(marketDataFactory, "marketDataSpecification");
    _traceFunctions = ImmutableSet.copyOf(ArgumentChecker.notNull(traceFunctions, "traceFunctions"));
  }

  public ZonedDateTime getValuationTime() {
    return _valuationTime;
  }

  public MarketDataFactory getMarketDataFactory() {
    return _marketDataFactory;
  }

  // TODO this is an awful name
  public Set<Pair<Integer, Integer>> getTraceFunctions() {
    return _traceFunctions;
  }

  public VersionCorrection getConfigVersionCorrection() {
    return _configVersionCorrection;
  }
}
