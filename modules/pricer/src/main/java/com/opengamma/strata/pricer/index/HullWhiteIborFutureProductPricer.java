/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.index.IborFuture;
import com.opengamma.strata.product.index.ResolvedIborFuture;

/**
 * Pricer for for Ibor future products.
 * <p>
 * This function provides the ability to price a {@link IborFuture} based on
 * Hull-White one-factor model with piecewise constant volatility.  
 * <p> 
 * Reference: Henrard M., Eurodollar Futures and Options: Convexity Adjustment in HJM One-Factor Model. March 2005.
 * Available at <a href="http://ssrn.com/abstract=682343">http://ssrn.com/abstract=682343</a>
 */
public class HullWhiteIborFutureProductPricer extends AbstractIborFutureProductPricer {

  /**
  * Default implementation.
  */
  public static final HullWhiteIborFutureProductPricer DEFAULT = new HullWhiteIborFutureProductPricer();

  /**
  * Creates an instance.
  */
  public HullWhiteIborFutureProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
  * Calculates the price of the Ibor future product.
  * <p>
  * The price of the product is the price on the valuation date.
  * 
  * @param future  the future
  * @param ratesProvider  the rates provider
  * @param hwProvider  the Hull-White model parameter provider
  * @return the price of the product, in decimal form
  */
  public double price(
      ResolvedIborFuture future,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    double parRate = parRate(future, ratesProvider, hwProvider);
    return 1d - parRate;
  }

  /**
  * Calculates the convexity adjustment (to the price) of the Ibor future product.
  * <p>
  * The convexity adjustment of the product is the value on the valuation date.
  * 
  * @param future  the future
  * @param ratesProvider  the rates provider
  * @param hwProvider  the Hull-White model parameter provider
  * @return the convexity adjustment, in decimal form
  */
  public double convexityAdjustment(
      ResolvedIborFuture future,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    IborIndexObservation obs = future.getIborRate().getObservation();
    double forward = ratesProvider.iborIndexRates(future.getIndex()).rate(obs);
    double parRate = parRate(future, ratesProvider, hwProvider);
    return forward - parRate;
  }

  /**
  * Calculates the par rate of the Ibor future product.
  * <p>
  * The par rate is given by ({@code 1 - price}).
  * The par rate of the product is the value on the valuation date.
  * 
  * @param future  the future
  * @param ratesProvider  the rates provider
  * @param hwProvider  the Hull-White model parameter provider
  * @return the par rate of the product, in decimal form
  */
  public double parRate(
      ResolvedIborFuture future,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    IborIndexObservation obs = future.getIborRate().getObservation();
    double forward = ratesProvider.iborIndexRates(future.getIndex()).rate(obs);
    LocalDate fixingStartDate = obs.getEffectiveDate();
    LocalDate fixingEndDate = obs.getMaturityDate();
    double fixingYearFraction = obs.getYearFraction();
    double convexity = hwProvider.futuresConvexityFactor(future.getLastTradeDate(), fixingStartDate, fixingEndDate);
    return convexity * forward - (1d - convexity) / fixingYearFraction;
  }

  /**
  * Calculates the price sensitivity of the Ibor future product.
  * <p>
  * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
  * 
  * @param future  the future
  * @param ratesProvider  the rates provider
  * @param hwProvider  the Hull-White model parameter provider
  * @return the price curve sensitivity of the product
  */
  public PointSensitivities priceSensitivity(
      ResolvedIborFuture future,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    IborIndexObservation obs = future.getIborRate().getObservation();
    LocalDate fixingStartDate = obs.getEffectiveDate();
    LocalDate fixingEndDate = obs.getMaturityDate();
    double convexity = hwProvider.futuresConvexityFactor(future.getLastTradeDate(), fixingStartDate, fixingEndDate);
    IborRateSensitivity sensi = IborRateSensitivity.of(obs, -convexity);
    // The sensitivity should be to no currency or currency XXX. To avoid useless conversion, the dimension-less 
    // price sensitivity is reported in the future currency.
    return PointSensitivities.of(sensi);
  }

  /**
  * Calculates the price sensitivity to piecewise constant volatility parameters of the Hull-White model.
  * 
  * @param future  the future
  * @param ratesProvider  the rates provider
  * @param hwProvider  the Hull-White model parameter provider
  * @return the price parameter sensitivity of the product
  */
  public DoubleArray priceSensitivityHullWhiteParameter(
      ResolvedIborFuture future,
      RatesProvider ratesProvider,
      HullWhiteOneFactorPiecewiseConstantParametersProvider hwProvider) {

    IborIndexObservation obs = future.getIborRate().getObservation();
    double forward = ratesProvider.iborIndexRates(future.getIndex()).rate(obs);
    LocalDate fixingStartDate = obs.getEffectiveDate();
    LocalDate fixingEndDate = obs.getMaturityDate();
    double fixingYearFraction = obs.getYearFraction();
    DoubleArray convexityDeriv = hwProvider.futuresConvexityFactorAdjoint(
        future.getLastTradeDate(), fixingStartDate, fixingEndDate).getDerivatives();
    convexityDeriv = convexityDeriv.multipliedBy(-forward - 1d / fixingYearFraction);
    return convexityDeriv;
  }
}
