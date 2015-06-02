/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.analytics.convention.daycount.DayCountFactory;
import com.opengamma.analytics.financial.instrument.index.IndexON;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.analytics.math.curve.DoublesCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.interpolator.CurveExtrapolator;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;

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
   * Converts a day count to the equivalent legacy day count.
   *
   * @param dayCount  a day count
   * @return the equivalent legacy day count
   */
  public static com.opengamma.analytics.convention.daycount.DayCount dayCount(DayCount dayCount) {
    return DayCountFactory.of(dayCount.getName());
  }

  /**
   * Converts a legacy day count object to the new object.
   * 
   * @param dayCount  the day count
   * @return the equivalent new day count
   */
  public static DayCount dayCount(com.opengamma.analytics.convention.daycount.DayCount dayCount) {
    String name = dayCount.getName();
    name = name.replace("Actual", "Act");
    name = name.replace("Act/365", "Act/365F");
    name = name.replace(" Normal", "");
    return DayCount.of(name);
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
  public static OvernightIndex overnightIndex(IndexON index) {
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
  public static Map<Index, Curve> indexCurves(MulticurveProviderDiscount multicurve) {
    Map<Index, Curve> map = new HashMap<>();
    for (com.opengamma.analytics.financial.instrument.index.IborIndex index : multicurve.getIndexesIbor()) {
      map.put(iborIndex(index), curve(multicurve.getCurve(index)));
    }
    for (IndexON index : multicurve.getIndexesON()) {
      map.put(overnightIndex(index), curve(multicurve.getCurve(index)));
    }
    return map;
  }

  /**
   * Converts a multicurve to a map of currency to curve.
   * 
   * @param multicurve  the multicurve
   * @return the map
   */
  public static Map<Currency, Curve> discountCurves(MulticurveProviderDiscount multicurve) {
    Map<Currency, Curve> map = new HashMap<>();
    for (Currency currency : multicurve.getCurrencies()) {
      map.put(currency, curve(multicurve.getCurve(currency)));
    }
    return map;
  }

  /**
   * Converts a legacy curve to a new curve.
   * 
   * @param legacyCurve  the legacy curve
   * @return the curve
   */
  public static Curve curve(YieldAndDiscountCurve legacyCurve) {
    return curve(legacyCurve, CurveMetadata.of(legacyCurve.getName()));
  }

  /**
   * Converts a legacy curve to a new curve.
   *
   * @param legacyCurve  the legacy curve
   * @param curveMetadata  the curve metadata
   * @return the curve
   */
  public static Curve curve(YieldAndDiscountCurve legacyCurve, CurveMetadata curveMetadata) {
    if (legacyCurve instanceof YieldCurve) {
      YieldCurve yieldCurve = (YieldCurve) legacyCurve;
      DoublesCurve underlying = yieldCurve.getCurve();
      if (underlying instanceof InterpolatedDoublesCurve) {
        InterpolatedDoublesCurve idc = (InterpolatedDoublesCurve) underlying;
        Interpolator1D interpolator = idc.getInterpolator();
        if (interpolator instanceof CombinedInterpolatorExtrapolator) {
          CombinedInterpolatorExtrapolator cie = (CombinedInterpolatorExtrapolator) interpolator;
          return InterpolatedNodalCurve.builder()
              .metadata(curveMetadata)
              .xValues(idc.getXDataAsPrimitive())
              .yValues(idc.getYDataAsPrimitive())
              .extrapolatorLeft((CurveExtrapolator) cie.getLeftExtrapolator())
              .interpolator((CurveInterpolator) cie.getInterpolator())
              .extrapolatorRight((CurveExtrapolator) cie.getRightExtrapolator())
              .build();
        } else {
          return InterpolatedNodalCurve.builder()
              .metadata(curveMetadata)
              .xValues(idc.getXDataAsPrimitive())
              .yValues(idc.getYDataAsPrimitive())
              .interpolator((CurveInterpolator) interpolator)
              .build();
        }

      } else if (underlying instanceof ConstantDoublesCurve) {
        ConstantDoublesCurve cdc = (ConstantDoublesCurve) underlying;
        return ConstantNodalCurve.of(cdc.getName(), cdc.getYValue(0d));

      } else {
        throw new IllegalArgumentException("Unknown curve type: " + underlying.getClass());
      }
    } else {
      throw new IllegalArgumentException("Unknown curve type: " + legacyCurve.getClass());
    }
  }
}
