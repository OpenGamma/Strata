/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function.marketdata;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.financial.provider.curve.CurveBuildingBlockBundle;
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderDiscount;
import com.opengamma.analytics.math.curve.ConstantDoublesCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.market.curve.CurveGroup;
import com.opengamma.strata.pricer.impl.Legacy;

/**
 * Utility methods for market data tests.
 */
public class MarketDataTestUtils {

  public static final String CURVE_GROUP_NAME = "group name";

  private MarketDataTestUtils() {
  }

  /**
   * @return an empty {@code CurveGroup}
   */
  public static CurveGroup curveGroup() {
    MulticurveProviderDiscount multicurve = new MulticurveProviderDiscount(FxMatrix.empty());
    return new CurveGroup(multicurve, new CurveBuildingBlockBundle());
  }

  /**
   * Creates a yield curve, adds it to a curve group as a discounting curve and returns it.
   *
   * @param constantValue  the constant value of the curve
   * @param currency  the curve currency
   * @param curveGroup  a group to add the curve to
   * @return the new curve
   */
  public static YieldCurve discountingCurve(double constantValue, Currency currency, CurveGroup curveGroup) {
    ConstantDoublesCurve curve = ConstantDoublesCurve.from(constantValue, currency + " Discounting");
    YieldCurve yieldCurve = YieldCurve.from(curve);
    curveGroup.getMulticurveProvider().setCurve(currency, yieldCurve);
    return yieldCurve;
  }

  /**
   * Creates a yield curve, adds it to a curve group as a forward curve for an IBOR index and returns it.
   *
   * @param constantValue  the constant value of the curve
   * @param index  the curve index
   * @param curveGroup  a group to add the curve to
   * @return the new curve
   */
  public static YieldCurve iborIndexCurve(double constantValue, IborIndex index, CurveGroup curveGroup) {
    ConstantDoublesCurve curve = ConstantDoublesCurve.from(constantValue, index.getName());
    YieldCurve yieldCurve = YieldCurve.from(curve);
    curveGroup.getMulticurveProvider().setCurve(Legacy.iborIndex(index), yieldCurve);
    return yieldCurve;
  }

  /**
   * Creates a yield curve, adds it to a curve group as a forward curve for an overnight index and returns it.
   *
   * @param constantValue  the constant value of the curve
   * @param index  the curve index
   * @param curveGroup  a group to add the curve to
   * @return the new curve
   */
  public static YieldCurve overnightIndexCurve(double constantValue, OvernightIndex index, CurveGroup curveGroup) {
    ConstantDoublesCurve curve = ConstantDoublesCurve.from(constantValue, index.getName());
    YieldCurve yieldCurve = YieldCurve.from(curve);
    curveGroup.getMulticurveProvider().setCurve(Legacy.overnightIndex(index), yieldCurve);
    return yieldCurve;
  }
}
