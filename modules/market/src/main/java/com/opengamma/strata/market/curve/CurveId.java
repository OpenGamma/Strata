/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.data.NamedMarketDataId;
import com.opengamma.strata.data.ObservableSource;

/**
 * Abstracts all kinds of identifier that can be used to access a curve by name.
 */
public interface CurveId
    extends NamedMarketDataId<Curve> {

  /**
   * Gets the curve group name.
   * 
   * @return the name
   */
  public abstract CurveGroupName getCurveGroupName();

  /**
   * Gets the curve name.
   * 
   * @return the name
   */
  public abstract CurveName getCurveName();

  /**
   * Gets the source of observable market data.
   * 
   * @return the source
   */
  public abstract ObservableSource getObservableSource();

  @Override
  public default Class<Curve> getMarketDataType() {
    return Curve.class;
  }

  @Override
  public default MarketDataName<Curve> getMarketDataName() {
    return getCurveName();
  }

}
