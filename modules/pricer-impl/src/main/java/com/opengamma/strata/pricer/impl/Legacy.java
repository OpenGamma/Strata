/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
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
   * Converts a legacy IBOR-like index to the new object.
   * 
   * @param index  the index
   * @return the same index
   */
  public static IborIndex iborIndex(com.opengamma.analytics.financial.instrument.index.IborIndex index) {
    String name = LegacyIndices.IBOR.inverse().get(index);
    if (name == null) {
      throw new IllegalArgumentException("Unknown index: " + index.getName());
    }
    return IborIndex.of(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an Overnight index to the legacy object.
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

  /**
   * Converts a legacy Overnight index to the new object.
   * 
   * @param index  the index
   * @return the same index
   */
  public static OvernightIndex iborIndex(IndexON index) {
    String name = LegacyIndices.OVERNIGHT.inverse().get(index);
    if (name == null) {
      throw new IllegalArgumentException("Unknown index: " + index.getName());
    }
    return OvernightIndex.of(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a multicurve to a map of index to curve.
   * 
   * @param multicurve  the multicurve
   * @return the map
   */
  public static Map<Index, YieldAndDiscountCurve> indexCurves(MulticurveProviderDiscount multicurve) {
    Map<Index, YieldAndDiscountCurve> map = new HashMap<>();
    for (com.opengamma.analytics.financial.instrument.index.IborIndex index : multicurve.getIndexesIbor()) {
      map.put(iborIndex(index), multicurve.getCurve(index));
    }
    for (IndexON index : multicurve.getIndexesON()) {
      map.put(iborIndex(index), multicurve.getCurve(index));
    }
    return map;
  }

}
