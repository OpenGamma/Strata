/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.index;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.model.HullWhiteOneFactorPiecewiseConstantParametersProvider;
import com.opengamma.strata.pricer.rate.IborRateSensitivity;
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
 * 
 * <h4>Price</h4>
 * The price of an Ibor future is based on the interest rate of the underlying index.
 * It is defined as {@code (100 - percentRate)}.
 * <p>
 * Strata uses <i>decimal prices</i> for Ibor futures in the trade model, pricers and market data.
 * The decimal price is based on the decimal rate equivalent to the percentage.
 * For example, a price of 99.32 implies an interest rate of 0.68% which is represented in Strata by 0.9932.
 */
public class HullWhiteIborFutureProductPricer {

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
   * Calculates the number related to Ibor futures product on which the daily margin is computed.
   * <p>
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param price  the price of the product, in decimal form
   * @return the index
   */
  double marginIndex(ResolvedIborFuture future, double price) {
    return price * future.getNotional() * future.getAccrualFactor();
  }

  /**
   * Calculates the margin index sensitivity of the Ibor future product.
   * <p>
   * The margin index sensitivity is the sensitivity of the margin index to the underlying curves.
   * For two consecutive settlement prices C1 and C2, the daily margin is computed as 
   *    {@code (marginIndex(future, C2) - marginIndex(future, C1))}.
   * 
   * @param future  the future
   * @param priceSensitivity  the price sensitivity of the product
   * @return the index sensitivity
   */
  PointSensitivities marginIndexSensitivity(ResolvedIborFuture future, PointSensitivities priceSensitivity) {
    return priceSensitivity.multipliedBy(future.getNotional() * future.getAccrualFactor());
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
  public PointSensitivities priceSensitivityRates(
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
  public DoubleArray priceSensitivityModelParamsHullWhite(
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
