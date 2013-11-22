/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.sesame.FunctionResult;

// TODO a class for the type parameter?
// TODO type parameter for the value? Pair<status, value> -> MarketDataItem?
// or a type instead of the map? MarketDataItem getValue(MarketDataRequirement)
// TODO use this for values and series, type params for T or <T extends DateTimeSeries<T extends LocalDate, ?>>
// sub interfaces that specify the type params
// multiple builder impls
public interface MarketDataSingleResult extends FunctionResult<Map<MarketDataRequirement, MarketDataItem<?>>> {

  <T> MarketDataValue<T> getSingleValue();

  MarketDataStatus getStatus(MarketDataRequirement requirement);

  <T> MarketDataValue<T> getValue(MarketDataRequirement requirement);

  /**
   * Temporary method to allow conversion to the old-style market data bundle.
   *
   * @return a snapshot bundle with the data from the result
   */
  SnapshotDataBundle toSnapshot();
}

// TODO do I need abstract superclass and value/series impls for results builder, success and failure result impls?
