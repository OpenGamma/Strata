/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.mapping;

import java.util.Optional;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.marketdata.id.MarketDataVendor;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * Vendor ID mapping that returns the input ID if it has the vendor {@link MarketDataVendor#NO_RULE}
 * else it delegates to another instance to perform the mapping.
 */
public final class MissingDataAwareVendorIdMapping implements VendorIdMapping {

  /** Mapping used for IDs that don't have the vendor {@link MarketDataVendor#NO_RULE}. */
  private final VendorIdMapping delegate;

  /**
   * @param delegate mapping used for IDs that don't have the vendor {@link MarketDataVendor#NO_RULE}
   */
  public MissingDataAwareVendorIdMapping(VendorIdMapping delegate) {
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public Optional<ObservableId> idForVendor(ObservableId id) {
    return id.getMarketDataVendor().equals(MarketDataVendor.NO_RULE) ?
        Optional.of(id) :
        delegate.idForVendor(id);
  }
}
