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

  /**
   * A constant for use where a vendor is required but no data is expected to be requested.
   */
  public static final MarketDataVendor NONE = of("None");

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
