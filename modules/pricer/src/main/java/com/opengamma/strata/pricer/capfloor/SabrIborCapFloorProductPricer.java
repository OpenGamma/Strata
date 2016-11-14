/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloor;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Pricer for cap/floor products in SABR model.
 */
public class SabrIborCapFloorProductPricer
    extends VolatilityIborCapFloorProductPricer {

  /**
   * Default implementation.
   */
  public static final SabrIborCapFloorProductPricer DEFAULT =
      new SabrIborCapFloorProductPricer(SabrIborCapFloorLegPricer.DEFAULT, DiscountingSwapLegPricer.DEFAULT);

  /**
   * The leg pricer.
   */
  private final SabrIborCapFloorLegPricer capFloorLegPricer;

  /**
   * Creates an instance.
   * 
   * @param capFloorLegPricer  the pricer for {@link IborCapFloorLeg}
   * @param payLegPricer  the pricer for {@link SwapLeg}
   */
  public SabrIborCapFloorProductPricer(SabrIborCapFloorLegPricer capFloorLegPricer, DiscountingSwapLegPricer payLegPricer) {
    super(capFloorLegPricer, payLegPricer);
    this.capFloorLegPricer = capFloorLegPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value rates sensitivity of the Ibor cap/floor product.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the 
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential 
   * re-calibration of the model parameters to the raw market data.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyModel(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    PointSensitivityBuilder pvSensiCapFloorLeg =
        capFloorLegPricer.presentValueSensitivityRatesStickyModel(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return pvSensiCapFloorLeg;
    }
    PointSensitivityBuilder pvSensiPayLeg = payLegPricer.presentValueSensitivity(capFloor.getPayLeg().get(), ratesProvider);
    return pvSensiCapFloorLeg.combinedWith(pvSensiPayLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value volatility sensitivity of the Ibor cap/floor product.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    return capFloorLegPricer.presentValueSensitivityModelParamsSabr(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
  }

}
