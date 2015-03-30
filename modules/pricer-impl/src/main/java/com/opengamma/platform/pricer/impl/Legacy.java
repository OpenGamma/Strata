/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl;

import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;

/**
 * Static utilities to convert types to legacy types.
 */
public final class Legacy {

  /**
   * Restricted constructor.
   */
  private Legacy() {
  }

  //-------------------------------------------------------------------------
  /**
   * Converts the currency to the legacy object.
   * 
   * @param currency  the currency
   * @return the same currency
   */
  public static com.opengamma.util.money.Currency currency(Currency currency) {
    return com.opengamma.util.money.Currency.of(currency.getCode());
  }

  /**
   * Converts an IBOR-like index to the legacy object.
   * 
   * @param index  the index
   * @return the same index
   */
  public static com.opengamma.analytics.financial.instrument.index.IborIndex iborIndex(IborIndex index) {
    com.opengamma.analytics.financial.instrument.index.IborIndex converted = LegacyIndices.IBOR.get(index.getName());
    if (converted == null) {
      throw new IllegalArgumentException("Unknown index: " + index);
    }
    return converted;
  }

  /**
   * Converts an overnight index to the legacy object.
   * 
   * @param index  the index
   * @return the same index
   */
  public static IndexON overnightIndex(OvernightIndex index) {
    com.opengamma.analytics.financial.instrument.index.IndexON converted = LegacyIndices.OVERNIGHT.get(index.getName());
    if (converted == null) {
      throw new IllegalArgumentException("Unknown index: " + index);
    }
    return converted;
  }

}
