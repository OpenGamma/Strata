/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.id;

import com.opengamma.strata.collect.type.TypedString;

// TODO Should this be a bean with vendor metadata? Set of schemes and the default scheme?
/**
 * Identifies a vendor of market data, for example Bloomberg or Reuters.
 */
public final class MarketDataVendor extends TypedString<MarketDataVendor> {

  /** A market data vendor used where a vendor is required but no data is expected to be requested. */
  public static final MarketDataVendor NONE = of("None");

  /** A market data vendor used to indicate there are no market data rules for a calculation. */
  public static final MarketDataVendor NO_RULE = of("NoMatchingMarketDataRule");

  private MarketDataVendor(String name) {
    super(name);
  }

  /**
   * Returns a vendor with the specified name.
   *
   * @param vendorName  the vendor name
   * @return a vendor with the specified name
   */
  public static MarketDataVendor of(String vendorName) {
    return new MarketDataVendor(vendorName);
  }
}
