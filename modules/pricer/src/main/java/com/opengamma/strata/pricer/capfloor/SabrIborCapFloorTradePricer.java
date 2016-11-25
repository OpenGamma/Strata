/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloor;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloorTrade;

/**
 * Pricer for cap/floor trades in SABR model.
 */
public class SabrIborCapFloorTradePricer
    extends VolatilityIborCapFloorTradePricer {

  /**
   * Default implementation.
   */
  public static final SabrIborCapFloorTradePricer DEFAULT =
      new SabrIborCapFloorTradePricer(SabrIborCapFloorProductPricer.DEFAULT, DiscountingPaymentPricer.DEFAULT);

  /**
   * The pricer for {@link ResolvedIborCapFloor}.
   */
  private final SabrIborCapFloorProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedIborCapFloor}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public SabrIborCapFloorTradePricer(SabrIborCapFloorProductPricer productPricer, DiscountingPaymentPricer paymentPricer) {
    super(productPricer, paymentPricer);
    this.productPricer = productPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value rates sensitivity of the Ibor cap/floor trade.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the 
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential 
   * re-calibration of the model parameters to the raw market data.
   * 
   * @param trade  the Ibor cap/floor trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivityRatesStickyModel(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    PointSensitivityBuilder pvSensiProduct =
        productPricer.presentValueSensitivityRatesStickyModel(trade.getProduct(), ratesProvider, volatilities);
    if (!trade.getPremium().isPresent()) {
      return pvSensiProduct.build();
    }
    PointSensitivityBuilder pvSensiPremium =
        paymentPricer.presentValueSensitivity(trade.getPremium().get(), ratesProvider);
    return pvSensiProduct.combinedWith(pvSensiPremium).build();
  }

  /**
   * Calculates the present value volatility sensitivity of the Ibor cap/floor trade.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param trade  the Ibor cap/floor trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedIborCapFloorTrade trade,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    return productPricer.presentValueSensitivityModelParamsSabr(trade.getProduct(), ratesProvider, volatilities);
  }

}
