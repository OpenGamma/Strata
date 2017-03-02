/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDeposit;

/**
 * The methods associated to the pricing of Ibor fixing deposit by discounting.
 * <p>
 * This provides the ability to price {@link ResolvedIborFixingDeposit}. Those products are synthetic deposits
 * which are used for curve calibration purposes; they should not be used as actual trades.
 */
public class DiscountingIborFixingDepositProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingIborFixingDepositProductPricer DEFAULT =
      new DiscountingIborFixingDepositProductPricer();

  /**
   * Creates an instance.
   */
  public DiscountingIborFixingDepositProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor fixing deposit product.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(ResolvedIborFixingDeposit deposit, RatesProvider provider) {
    Currency currency = deposit.getCurrency();
    if (provider.getValuationDate().isAfter(deposit.getEndDate())) {
      return CurrencyAmount.of(currency, 0.0d);
    }
    double forwardRate = forwardRate(deposit, provider);
    double discountFactor = provider.discountFactor(currency, deposit.getEndDate());
    double fv = deposit.getNotional() * deposit.getYearFraction() * (deposit.getFixedRate() - forwardRate);
    double pv = discountFactor * fv;
    return CurrencyAmount.of(currency, pv);
  }

  /**
   * Calculates the present value sensitivity of the Ibor fixing product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedIborFixingDeposit deposit, RatesProvider provider) {
    double forwardRate = forwardRate(deposit, provider);
    DiscountFactors discountFactors = provider.discountFactors(deposit.getCurrency());
    double discountFactor = discountFactors.discountFactor(deposit.getEndDate());
    // sensitivity
    PointSensitivityBuilder sensiFwd = forwardRateSensitivity(deposit, provider)
        .multipliedBy(-discountFactor * deposit.getNotional() * deposit.getYearFraction());
    PointSensitivityBuilder sensiDsc = discountFactors.zeroRatePointSensitivity(deposit.getEndDate())
        .multipliedBy(deposit.getNotional() * deposit.getYearFraction() * (deposit.getFixedRate() - forwardRate));
    return sensiFwd.combinedWith(sensiDsc).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the deposit fair rate given the start and end time and the accrual factor.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(ResolvedIborFixingDeposit deposit, RatesProvider provider) {
    return forwardRate(deposit, provider);
  }

  /**
   * Calculates the deposit fair rate sensitivity to the curves.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par rate curve sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedIborFixingDeposit deposit, RatesProvider provider) {
    return forwardRateSensitivity(deposit, provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the spread to be added to the deposit rate to have a zero present value.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(ResolvedIborFixingDeposit deposit, RatesProvider provider) {
    return forwardRate(deposit, provider) - deposit.getFixedRate();
  }

  /**
   * Calculates the par spread curve sensitivity.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedIborFixingDeposit deposit, RatesProvider provider) {
    return forwardRateSensitivity(deposit, provider).build();
  }

  //-------------------------------------------------------------------------
  // query the forward rate
  private double forwardRate(ResolvedIborFixingDeposit product, RatesProvider provider) {
    IborIndexRates rates = provider.iborIndexRates(product.getFloatingRate().getIndex());
    // The IborFixingDeposit are fictitious instruments to anchor the beginning of the IborIndex forward curve.
    // By using the 'rateIgnoringTimeSeries' method (instead of 'rate') we ensure that only the forward curve is involved.
    return rates.rateIgnoringFixings(product.getFloatingRate().getObservation());
  }

  // query the forward rate sensitivity
  private PointSensitivityBuilder forwardRateSensitivity(ResolvedIborFixingDeposit product, RatesProvider provider) {
    IborIndexRates rates = provider.iborIndexRates(product.getFloatingRate().getIndex());
    return rates.rateIgnoringFixingsPointSensitivity(product.getFloatingRate().getObservation());
  }

}
