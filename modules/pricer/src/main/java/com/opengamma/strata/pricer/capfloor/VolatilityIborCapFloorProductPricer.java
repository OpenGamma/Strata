/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.ResolvedIborCapFloor;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Pricer for cap/floor products based on volatilities.
 * <p>
 * This function provides the ability to price {@link ResolvedIborCapFloor}. 
 * <p>
 * The pricing methodologies are defined in individual implementations of the
 * volatilities, {@link IborCapletFloorletVolatilities}. 
 */
public class VolatilityIborCapFloorProductPricer {

  /**
   * Default implementation.
   */
  public static final VolatilityIborCapFloorProductPricer DEFAULT =
      new VolatilityIborCapFloorProductPricer(VolatilityIborCapFloorLegPricer.DEFAULT, DiscountingSwapLegPricer.DEFAULT);
  /**
   * The pricer for {@link IborCapFloorLeg}.
   */
  private final VolatilityIborCapFloorLegPricer capFloorLegPricer;
  /**
   * The pricer for {@link SwapLeg}.
   */
  protected final DiscountingSwapLegPricer payLegPricer;

  /**
   * Creates an instance.
   * 
   * @param capFloorLegPricer  the pricer for {@link IborCapFloorLeg}
   * @param payLegPricer  the pricer for {@link SwapLeg}
   */
  public VolatilityIborCapFloorProductPricer(
      VolatilityIborCapFloorLegPricer capFloorLegPricer,
      DiscountingSwapLegPricer payLegPricer) {

    this.capFloorLegPricer = ArgChecker.notNull(capFloorLegPricer, "capFloorLegPricer");
    this.payLegPricer = ArgChecker.notNull(payLegPricer, "payLegPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor cap/floor product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The cap/floor leg and pay leg are typically in the same currency, thus the
   * present value gamma is expressed as a single currency amount in most cases.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValue(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(pvCapFloorLeg);
    }
    CurrencyAmount pvPayLeg = payLegPricer.presentValue(capFloor.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(pvCapFloorLeg).plus(pvPayLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the Ibor cap/floor product.
   * <p>
   * The present value of the product is the sensitivity value on the valuation date.
   * <p>
   * The cap/floor leg and pay leg are typically in the same currency, thus the
   * present value gamma is expressed as a single currency amount in most cases.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value delta
   */
  public MultiCurrencyAmount presentValueDelta(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValueDelta(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    return MultiCurrencyAmount.of(pvCapFloorLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value gamma of the Ibor cap/floor product.
   * <p>
   * The present value of the product is the sensitivity value on the valuation date.
   * <p>
   * The cap/floor leg and pay leg are typically in the same currency, thus the
   * present value gamma is expressed as a single currency amount in most cases.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value gamma
   */
  public MultiCurrencyAmount presentValueGamma(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValueGamma(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    return MultiCurrencyAmount.of(pvCapFloorLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value theta of the Ibor cap/floor product.
   * <p>
   * The present value of the product is the sensitivity value on the valuation date.
   * <p>
   * The cap/floor leg and pay leg are typically in the same currency, thus the
   * present value gamma is expressed as a single currency amount in most cases.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value theta
   */
  public MultiCurrencyAmount presentValueTheta(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValueTheta(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    return MultiCurrencyAmount.of(pvCapFloorLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value rates sensitivity of the Ibor cap/floor product.
   * <p>
   * The present value rates sensitivity of the product is the sensitivity
   * of the present value to the underlying curves.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityRates(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    PointSensitivityBuilder pvSensiCapFloorLeg =
        capFloorLegPricer.presentValueSensitivityRates(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return pvSensiCapFloorLeg;
    }
    PointSensitivityBuilder pvSensiPayLeg =
        payLegPricer.presentValueSensitivity(capFloor.getPayLeg().get(), ratesProvider);
    return pvSensiCapFloorLeg.combinedWith(pvSensiPayLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value volatility sensitivity of the Ibor cap/floor product.
   * <p>
   * The present value volatility sensitivity of the product is the sensitivity
   * of the present value to the volatility values.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsVolatility(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return capFloorLegPricer.presentValueSensitivityModelParamsVolatility(
        capFloor.getCapFloorLeg(), ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the Ibor cap/floor product.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount ceCapFloorLeg =
        capFloorLegPricer.presentValue(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ceCapFloorLeg);
    }
    MultiCurrencyAmount cePayLeg = payLegPricer.currencyExposure(capFloor.getPayLeg().get(), ratesProvider);
    return cePayLeg.plus(ceCapFloorLeg);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the current cash of the Ibor cap/floor product.
   * 
   * @param capFloor  the Ibor cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      ResolvedIborCapFloor capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount ccCapFloorLeg =
        capFloorLegPricer.currentCash(capFloor.getCapFloorLeg(), ratesProvider, volatilities);
    if (!capFloor.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ccCapFloorLeg);
    }
    CurrencyAmount ccPayLeg = payLegPricer.currentCash(capFloor.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(ccPayLeg).plus(ccCapFloorLeg);
  }

}
