/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;

/**
 * Pricer for cap/floor legs in normal or Bachelier model.
 */
public class NormalIborCapFloorLegPricer
    extends VolatilityIborCapFloorLegPricer {

  /**
  * Default implementation.
  */
  public static final NormalIborCapFloorLegPricer DEFAULT =
      new NormalIborCapFloorLegPricer(NormalIborCapletFloorletPeriodPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param periodPricer  the pricer for {@link IborCapletFloorletPeriod}.
   */
  public NormalIborCapFloorLegPricer(NormalIborCapletFloorletPeriodPricer periodPricer) {
    super(periodPricer);
  }

}
