/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Pricer for caplet/floorlet in a log-normal or Black model.
 * <p>
 * The value of the caplet/floorlet after expiry is a fixed payoff amount. The value is zero if valuation date is 
 * after payment date of the caplet/floorlet.
 */
public class BlackIborCapletFloorletPeriodPricer
    extends VolatilityIborCapletFloorletPeriodPricer {

  /**
   * Default implementation.
   */
  public static final BlackIborCapletFloorletPeriodPricer DEFAULT = new BlackIborCapletFloorletPeriodPricer();

  @Override
  protected void validate(IborCapletFloorletVolatilities volatilities) {
    ArgChecker.isTrue(volatilities instanceof BlackIborCapletFloorletVolatilities, "volatilities must be Black volatilities");
  }

}
