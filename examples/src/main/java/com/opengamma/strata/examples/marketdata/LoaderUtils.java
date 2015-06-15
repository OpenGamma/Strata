/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.Messages;

/**
 * Contains utilities for loading market data from input files.
 */
public final class LoaderUtils {

  /**
   * Restricted constructor
   */
  private LoaderUtils() {
  }

  //-------------------------------------------------------------------------
  /**
   * Attempts to locate a rate index by reference name.
   * <p>
   * This utility searches both {@link IborIndex} and {@link OvernightIndex}.
   * 
   * @param reference  the reference name
   * @return the resolved rate index
   */
  public static RateIndex findIndex(String reference) {
    if (IborIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return IborIndex.of(reference);
    }
    if (OvernightIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return OvernightIndex.of(reference);
    }
    throw new IllegalArgumentException(Messages.format("No index found for reference: {}", reference));
  }

}
