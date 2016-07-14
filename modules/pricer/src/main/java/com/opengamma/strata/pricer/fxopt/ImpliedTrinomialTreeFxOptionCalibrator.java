/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.util.function.Function;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.impl.volatility.local.ImpliedTrinomialTreeLocalVolatilityCalculator;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

/**
 * Utilities to calibrate implied trinomial tree to Black volatilities of FX options.
 */
public class ImpliedTrinomialTreeFxOptionCalibrator {

  /**
   * Number of time steps.
   */
  private final int nSteps;

  /**
   * Calibrator with the specified number of time steps.
   * 
   * @param nSteps  number of time steps
   */
  public ImpliedTrinomialTreeFxOptionCalibrator(int nSteps) {
    ArgChecker.isTrue(nSteps > 1, "the number of steps should be greater than 1");
    this.nSteps = nSteps;
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains number of time steps.
   * 
   * @return number of time steps
   */
  public int getNumberOfSteps() {
    return nSteps;
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate trinomial tree to Black volatilities by using a vanilla option.
   * <p>
   * {@code ResolvedFxVanillaOption} is typically the underlying option of an exotic instrument to price using the 
   * calibrated tree, and is used to ensure that the grid points properly cover the lifetime of the target option.
   * 
   * @param option  the vanilla option
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the trinomial tree data
   */
  public RecombiningTrinomialTreeData calibrateTrinomialTree(
      ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    double timeToExpiry = volatilities.relativeTime(option.getExpiry());
    CurrencyPair currencyPair = option.getUnderlying().getCurrencyPair();
    return calibrateTrinomialTree(timeToExpiry, currencyPair, ratesProvider, volatilities);
  }

  /**
   * Calibrate trinomial tree to Black volatilities.
   * <p>
   * {@code timeToExpiry} determines the coverage of the resulting trinomial tree.
   * Thus this should match the time to expiry of the target instrument to price using the calibrated tree.
   * 
   * @param timeToExpiry  the time to expiry
   * @param currencyPair  the currency pair
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the trinomial tree data
   */
  public RecombiningTrinomialTreeData calibrateTrinomialTree(
      double timeToExpiry,
      CurrencyPair currencyPair,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    if (timeToExpiry <= 0d) {
      throw new IllegalArgumentException("option expired");
    }
    Currency ccyBase = currencyPair.getBase();
    Currency ccyCounter = currencyPair.getCounter();
    double todayFx = ratesProvider.fxRate(currencyPair);
    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);
    Function<Double, Double> interestRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        return counterDiscountFactors.zeroRate(t);
      }
    };
    Function<Double, Double> dividendRate = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        return baseDiscountFactors.zeroRate(t);
      }
    };
    Function<DoublesPair, Double> impliedVolSurface = new Function<DoublesPair, Double>() {
      @Override
      public Double apply(DoublesPair tk) {
        double dfBase = baseDiscountFactors.discountFactor(tk.getFirst());
        double dfCounter = counterDiscountFactors.discountFactor(tk.getFirst());
        double forward = todayFx * dfBase / dfCounter;
        return volatilities.volatility(currencyPair, tk.getFirst(), tk.getSecond(), forward);
      }
    };
    ImpliedTrinomialTreeLocalVolatilityCalculator localVol =
        new ImpliedTrinomialTreeLocalVolatilityCalculator(nSteps, timeToExpiry);
    return localVol.calibrateImpliedVolatility(impliedVolSurface, todayFx, interestRate, dividendRate);
  }

  //-------------------------------------------------------------------------
  private void validate(
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ArgChecker.isTrue(
        ratesProvider.getValuationDate().isEqual(volatilities.getValuationDateTime().toLocalDate()),
        "Volatility and rate data must be for the same date");
  }

}
