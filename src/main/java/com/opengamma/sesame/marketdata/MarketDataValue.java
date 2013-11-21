/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

// TODO factory method(s) for creating instances?
// TODO what does this give us over the raw market data value? delete if no obvious reason appears
public interface MarketDataValue<T> {

  T getValue();
}
