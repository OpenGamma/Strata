/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.Messages;

/**
 * Contains utilities for loading market data from input files.
 */
public final class LoaderUtils {

  /**
   * Attempts to locate a rate index by reference name.
   * <p>
   * This utility searches {@link IborIndex}, {@link OvernightIndex}, {@link FxIndex}
   * and {@link PriceIndex}.
   * 
   * @param reference  the reference name
   * @return the resolved rate index
   */
  public static Index findIndex(String reference) {
    if (IborIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return IborIndex.of(reference);

    } else if (OvernightIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return OvernightIndex.of(reference);

    } else if (PriceIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return PriceIndex.of(reference);

    } else if (FxIndex.extendedEnum().lookupAll().containsKey(reference)) {
      return FxIndex.of(reference);

    } else {
      throw new IllegalArgumentException(Messages.format("No index found for reference: {}", reference));
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private LoaderUtils() {
  }

}
