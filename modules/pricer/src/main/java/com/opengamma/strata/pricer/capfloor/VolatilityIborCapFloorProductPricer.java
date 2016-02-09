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
import com.opengamma.strata.market.view.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapLegPricer;
import com.opengamma.strata.product.capfloor.ExpandedIborCapFloor;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.IborCapFloorProduct;
import com.opengamma.strata.product.swap.SwapLeg;

/**
 * Pricer for cap/floor products based on volatilities.
 * <p>
 * This function provides the ability to price {@link IborCapFloorProduct}. 
 * <p>
 * The pricing methodologies are defined in individual implementations of the volatilities, {@link IborCapletFloorletVolatilities}. 
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
  private final DiscountingSwapLegPricer payLegPricer;

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
   * Calculates the present value of the cap/floor product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * Cap/floor leg and pay leg are typically in the same currency. Thus the present value is expressed as a 
   * single currency amount in most cases.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value
   */
  public MultiCurrencyAmount presentValue(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    ExpandedIborCapFloor expanded = capFloor.expand();
    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValue(expanded.getCapFloorLeg(), ratesProvider, volatilities);
    if (!expanded.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(pvCapFloorLeg);
    }
    CurrencyAmount pvPayLeg = payLegPricer.presentValue(expanded.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(pvCapFloorLeg).plus(pvPayLeg);
  }

  /**
   * Calculates the present value delta of the cap/floor product.
   * <p>
   * The present value of the product is the sensitivity value on the valuation date.
   * <p>
   * Cap/floor leg and pay leg are typically in the same currency. Thus the present value delta is expressed as a 
   * single currency amount in most cases.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value delta
   */
  public MultiCurrencyAmount presentValueDelta(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValueDelta(capFloor.expand().getCapFloorLeg(), ratesProvider, volatilities);
    return MultiCurrencyAmount.of(pvCapFloorLeg);
  }

  /**
   * Calculates the present value gamma of the cap/floor product.
   * <p>
   * The present value of the product is the sensitivity value on the valuation date.
   * <p>
   * Cap/floor leg and pay leg are typically in the same currency. Thus the present value gamma is expressed as a 
   * single currency amount in most cases.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value gamma
   */
  public MultiCurrencyAmount presentValueGamma(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValueGamma(capFloor.expand().getCapFloorLeg(), ratesProvider, volatilities);
    return MultiCurrencyAmount.of(pvCapFloorLeg);
  }

  /**
   * Calculates the present value theta of the cap/floor product.
   * <p>
   * The present value of the product is the sensitivity value on the valuation date.
   * <p>
   * Cap/floor leg and pay leg are typically in the same currency. Thus the present value theta is expressed as a 
   * single currency amount in most cases.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value theta
   */
  public MultiCurrencyAmount presentValueTheta(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    CurrencyAmount pvCapFloorLeg =
        capFloorLegPricer.presentValueTheta(capFloor.expand().getCapFloorLeg(), ratesProvider, volatilities);
    return MultiCurrencyAmount.of(pvCapFloorLeg);
  }

  /**
   * Calculates the present value curve sensitivity of the cap/floor product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to the underlying curves.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    ExpandedIborCapFloor expanded = capFloor.expand();
    PointSensitivityBuilder pvSensiCapFloorLeg =
        capFloorLegPricer.presentValueSensitivity(expanded.getCapFloorLeg(), ratesProvider, volatilities);
    if (!expanded.getPayLeg().isPresent()) {
      return pvSensiCapFloorLeg;
    }
    PointSensitivityBuilder pvSensiPayLeg =
        payLegPricer.presentValueSensitivity(expanded.getPayLeg().get(), ratesProvider);
    return pvSensiCapFloorLeg.combinedWith(pvSensiPayLeg);
  }

  /**
   * Calculates the present value volatility sensitivity of the cap/floor product.
   * <p>
   * The present value volatility sensitivity of the product is the sensitivity of the present value to the volatility 
   * values.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityVolatility(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    return capFloorLegPricer.presentValueSensitivityVolatility(
        capFloor.expand().getCapFloorLeg(), ratesProvider, volatilities);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the cap/floor product.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    ExpandedIborCapFloor expanded = capFloor.expand();
    CurrencyAmount ceCapFloorLeg =
        capFloorLegPricer.presentValue(expanded.getCapFloorLeg(), ratesProvider, volatilities);
    if (!expanded.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ceCapFloorLeg);
    }
    MultiCurrencyAmount cePayLeg = payLegPricer.currencyExposure(expanded.getPayLeg().get(), ratesProvider);
    return cePayLeg.plus(ceCapFloorLeg);
  }

  /**
   * Calculates the current cash of the cap/floor product.
   * 
   * @param capFloor  the cap/floor product
   * @param ratesProvider  the rates provider
   * @param volatilities  the caplet/floorlet volatilities
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(
      IborCapFloorProduct capFloor,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    ExpandedIborCapFloor expanded = capFloor.expand();
    CurrencyAmount ccCapFloorLeg =
        capFloorLegPricer.currentCash(expanded.getCapFloorLeg(), ratesProvider, volatilities);
    if (!expanded.getPayLeg().isPresent()) {
      return MultiCurrencyAmount.of(ccCapFloorLeg);
    }
    CurrencyAmount ccPayLeg = payLegPricer.currentCash(expanded.getPayLeg().get(), ratesProvider);
    return MultiCurrencyAmount.of(ccPayLeg).plus(ccCapFloorLeg);
  }

}
