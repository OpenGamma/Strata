/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.id;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveGroupName;

/**
 * Market data ID identifying a curve.
 */
public interface CurveId extends MarketDataId<Curve> {

  @Override
  public default Class<Curve> getMarketDataType() {
    return Curve.class;
  }

  /**
   * Returns the name of the curve group to which the curve belongs.
   *
   * @return the name of the curve group to which the curve belongs
   */
  public abstract CurveGroupName getCurveGroupName();
}
