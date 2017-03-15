/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;

/**
 * Pricer for cap/floor legs in log-normal or Black model.
 */
public class BlackIborCapFloorLegPricer
    extends VolatilityIborCapFloorLegPricer {

  /**
   * Default implementation.
   */
  public static final BlackIborCapFloorLegPricer DEFAULT =
      new BlackIborCapFloorLegPricer(BlackIborCapletFloorletPeriodPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param periodPricer  the pricer for {@link IborCapletFloorletPeriod}.
   */
  public BlackIborCapFloorLegPricer(BlackIborCapletFloorletPeriodPricer periodPricer) {
    super(periodPricer);
  }

}
