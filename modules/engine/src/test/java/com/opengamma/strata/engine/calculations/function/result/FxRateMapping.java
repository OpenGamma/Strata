/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations.function.result;

import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId;
import com.opengamma.strata.basics.market.FxRateKey;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMapping;

/**
 * Market data mapping for FX rates used in tests.
 */
public final class FxRateMapping implements MarketDataMapping<FxRate, FxRateKey> {

  /** The shared instance of this stateless class. */
  public static final FxRateMapping INSTANCE = new FxRateMapping();

  private FxRateMapping() {
  }

  @Override
  public Class<? extends FxRateKey> getMarketDataKeyType() {
    return FxRateKey.class;
  }

  @Override
  public MarketDataId<FxRate> getIdForKey(FxRateKey key) {
    return FxRateId.of(key.getPair());
  }
}
