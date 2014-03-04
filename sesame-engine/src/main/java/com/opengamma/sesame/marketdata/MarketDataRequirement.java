/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

/**
 * A requirement for a particular piece of market data. It will generally
 * be for the current value (and this is the assumed default), but can also
 * represent a request for historic data (either from a timeseries or tick store).
 * TODO does this give us anything over and above an ID bundle?
 * @deprecated use {@link MarketDataFn2}
 */
@Deprecated
public interface MarketDataRequirement {
}
