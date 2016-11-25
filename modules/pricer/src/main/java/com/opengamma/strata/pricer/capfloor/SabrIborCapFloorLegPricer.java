/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorLeg;

/**
 * Pricer for cap/floor legs in SABR model.
 */
public class SabrIborCapFloorLegPricer
    extends VolatilityIborCapFloorLegPricer {

  /**
  * Default implementation.
  */
  public static final SabrIborCapFloorLegPricer DEFAULT =
      new SabrIborCapFloorLegPricer(SabrIborCapletFloorletPeriodPricer.DEFAULT);

  /**
   * The period pricer.
   */
  private final SabrIborCapletFloorletPeriodPricer periodPricer;

  /**
   * Creates an instance.
   * 
   * @param periodPricer  the pricer for {@link IborCapletFloorletPeriod}.
   */
  public SabrIborCapFloorLegPricer(SabrIborCapletFloorletPeriodPricer periodPricer) {
    super(periodPricer);
    this.periodPricer = periodPricer;
  }

  /**
   * Calculates the present value sensitivity of the Ibor cap/floor leg to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the 
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential 
   * re-calibration of the model parameters to the raw market data.
   * 
   * @param capFloorLeg  the Ibor cap/floor leg
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyModel(
      ResolvedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueSensitivityRatesStickyModel(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.combinedWith(c2))
        .get();
  }

  /**
   * Calculates the present value sensitivity to the SABR model parameters of the Ibor cap/floor.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param capFloorLeg  the Ibor cap/floor
   * @param ratesProvider  the rates provider
   * @param volatilities the volatilities
   * @return the point sensitivity to the SABR model parameters
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueSensitivityModelParamsSabr(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.combinedWith(c2))
        .get();
  }

}
