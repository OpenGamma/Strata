/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.ObservableSource;

/**
 * The definition of how to calibrate a group of curves.
 * <p>
 * A curve group contains information that allows a group of curves to be calibrated.
 * <p>
 * In Strata v2, this type was converted to an interface.
 * If migrating, change your code to {@link RatesCurveGroupDefinition}.
 */
public interface CurveGroupDefinition {

  /**
   * Gets the name of the curve group.
   * 
   * @return the group name
   */
  public abstract CurveGroupName getName();

  /**
   * Creates an identifier that can be used to resolve this definition.
   * 
   * @param source  the source of data
   * @return the curve, empty if not found
   */
  public abstract MarketDataId<? extends CurveGroup> createGroupId(ObservableSource source);

}
