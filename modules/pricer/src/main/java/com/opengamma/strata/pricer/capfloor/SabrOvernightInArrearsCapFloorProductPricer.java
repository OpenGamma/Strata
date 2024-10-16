/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapFloorLeg;
import com.opengamma.strata.product.capfloor.ResolvedOvernightInArrearsCapFloor;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Pricer for overnight rate in arrears cap/floor products in SABR model.
 */
public class SabrOvernightInArrearsCapFloorProductPricer {

  /**
   * Default implementation.
   */
  public static final SabrOvernightInArrearsCapFloorProductPricer DEFAULT =
      new SabrOvernightInArrearsCapFloorProductPricer(
          SabrOvernightInArrearsCapFloorLegPricer.DEFAULT,
          DiscountingSwapLegPricer.DEFAULT);
  /**
   * The pricer for {@link OvernightInArrearsCapFloorLeg}
   */
  private final SabrOvernightInArrearsCapFloorLegPricer capFloorLegPricer;
  /**
   * The pricer for {@link SwapLeg}.
   */
  private final DiscountingSwapLegPricer payLegPricer;

  /**
   * Creates an instance.
   *
   * @param capFloorLegPricer the pricer for {@link OvernightInArrearsCapFloorLeg}
   * @param payLegPricer the pricer for {@link SwapLeg}
   */
  public SabrOvernightInArrearsCapFloorProductPricer(
      SabrOvernightInArrearsCapFloorLegPricer capFloorLegPricer,
      DiscountingSwapLegPricer payLegPricer) {

    this.capFloorLegPricer = ArgChecker.notNull(capFloorLegPricer, "capFloorLegPricer");
    this.payLegPricer = ArgChecker.notNull(payLegPricer, "payLegPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the cap/floor leg pricer.
   *
   * @return the cap/floor leg pricer
   */
  public SabrOvernightInArrearsCapFloorLegPricer getCapFloorLegPricer() {
    return capFloorLegPricer;
  }

  /**
   * Gets the pay leg pricer.
   *
   * @return the pay leg pricer
   */
  public DiscountingSwapLegPricer getPayLegPricer() {
    return payLegPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the overnight rate in arrears cap/floor product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The cap/floor leg and pay leg are typically in the same currency,
   * thus the present value is expressed as a single currency amount in most cases.
   *
   * @param capFloor the cap/floor product
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg = capFloorLegPricer.presentValue(
        capFloor.getCapFloorLeg(),
        ratesProvider,
        volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(pvCapFloorLeg);
    }
    CurrencyAmount pvPayLeg = payLegPricer.presentValue(capFloor.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(pvCapFloorLeg).plus(pvPayLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value for each caplet/floorlet of the overnight rate in arrears cap/floor product.
   * <p>
   * The present value of each caplet/floorlet is the value on the valuation date.
   * The result is returned using the payment currency of the leg.
   * <p>
   * The present value will not be calculated for the pay leg if the product has one.
   *
   * @param capFloor the cap/floor product
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present values
   */
  public OvernightInArrearsCapletFloorletPeriodCurrencyAmounts presentValueCapletFloorletPeriods(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    return capFloorLegPricer.presentValueCapletFloorletPeriods(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value rates sensitivity of the overnight rate in arrears cap/floor product.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e., the sensitivity to the
   * curve nodes with the SABR model parameters unchanged.
   * This sensitivity does not include a potential re-calibration of the model parameters to the raw market data.
   *
   * @param capFloor the cap/floor product
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyModel(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    PointSensitivityBuilder pvSensiCapFloorLeg = capFloorLegPricer.presentValueSensitivityRatesStickyModel(
        capFloor.getCapFloorLeg(),
        ratesProvider,
        volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return pvSensiCapFloorLeg;
    }
    PointSensitivityBuilder pvSensiPayLeg = payLegPricer.presentValueSensitivity(capFloor.getPayLeg().get(), ratesProvider);
    return pvSensiCapFloorLeg.combinedWith(pvSensiPayLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the overnight rate in arrears cap/floor product.
   *
   * @param capFloor the cap/floor product
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    CurrencyAmount ceCapFloorLeg = capFloorLegPricer.presentValue(
        capFloor.getCapFloorLeg(),
        ratesProvider,
        volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ceCapFloorLeg);
    }
    MultiCurrencyAmount cePayLeg = payLegPricer.currencyExposure(capFloor.getPayLeg().get(), ratesProvider);
    return cePayLeg.plus(ceCapFloorLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the current cash of the overnight rate in arrears cap/floor product.
   *
   * @param capFloor the cap/floor product
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    CurrencyAmount ccCapFloorLeg = capFloorLegPricer.currentCash(
        capFloor.getCapFloorLeg(),
        ratesProvider,
        volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ccCapFloorLeg);
    }
    CurrencyAmount ccPayLeg = payLegPricer.currentCash(capFloor.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(ccPayLeg).plus(ccCapFloorLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value volatility sensitivity of the overnight rata in arrears cap/floor product.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   *
   * @param capFloor the cap/floor product
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    return capFloorLegPricer.presentValueSensitivityModelParamsSabr(
        capFloor.getCapFloorLeg(),
        ratesProvider,
        volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward rates for each caplet/floorlet of the overnight rate in arrears cap/floor.
   *
   * @param capFloor the cap/floor
   * @param ratesProvider the rates provider
   * @return the forward rates
   */
  public OvernightInArrearsCapletFloorletPeriodAmounts forwardRates(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider) {

    return capFloorLegPricer.forwardRates(capFloor.getCapFloorLeg(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the implied volatilities for each caplet/floorlet of the overnight rate in arrears cap/floor.
   *
   * @param capFloor the cap/floor
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the implied volatilities
   */
  public OvernightInArrearsCapletFloorletPeriodAmounts impliedVolatilities(
      ResolvedOvernightInArrearsCapFloor capFloor,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    return capFloorLegPricer.impliedVolatilities(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
  }

}
