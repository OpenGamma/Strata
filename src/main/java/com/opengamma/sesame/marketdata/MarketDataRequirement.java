/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Set;

import com.opengamma.id.ExternalIdBundle;

/**
 * A requirement for a particular piece of market data. It will generally
 * be for the current value (and this is the assumed default), but can also
 * represent a request for historic data (either from a timeseries or tick store).
 */
public interface MarketDataRequirement {

  String getDataField();

  Set<ExternalIdBundle> getIds();
}
