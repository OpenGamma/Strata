/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * Factory methods for creating {@link MarketDataKey} instances for common market data types.
 */
public final class MarketDataKeys {

  // Private constructor as this class only contains factory methods
  private MarketDataKeys() {
  }

  /**
   * Returns a market data key for the forward curve for an index.
   *
   * @param index  the index
   * @return a market data key for the forward curve for the index
   */
  public static MarketDataKey<?> indexCurve(Index index) {
    if (index instanceof IborIndex) {
      return IborIndexRatesKey.of((IborIndex) index);
    } else if (index instanceof OvernightIndex) {
      return OvernightIndexRatesKey.of((OvernightIndex) index);
    } else if (index instanceof RateIndex) {
      return RateIndexCurveKey.of((RateIndex) index);
    } else {
      // TODO Support FX and inflation curves when they're added
      throw new IllegalArgumentException("No curve key can be created for index " + index.getName());
    }
  }

  /**
   * Returns a market data key for discount factors.
   *
   * @param currency  the currency of the curve
   * @return a market data key for discount factors
   */
  public static DiscountFactorsKey discountFactors(Currency currency) {
    return DiscountFactorsKey.of(currency);
  }

  // TODO fxRate(CurrencyPair)
  // TODO fxRate(Currency, Currency)
  // TODO quote(StandardId)
  // TODO There must be others
}
