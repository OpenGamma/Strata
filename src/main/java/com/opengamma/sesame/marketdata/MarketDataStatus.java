/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

/**
 * Status value of a particular piece of market data.
 * The value will only be available when the status is AVAILABLE.
 */
public enum MarketDataStatus {

  /**
   * Market data value is available.
   */
  AVAILABLE,
  /**
   * Market data value is not available.
   * It has been requested from a provider but was not been found.
   */
  UNAVAILABLE,
  /**
   * Market data value is not currently available.
   * It has been requested from a provider but has not yet been returned.
   * In a later cycle this value could either become AVAILABLE or UNAVAILABLE.
   */
  PENDING,
  /**
   * Request made for market data results which were not in the original request.
   */
  NOT_REQUESTED,

}
