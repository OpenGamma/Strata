/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.view.DiscountFactors;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.ExpandedIborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDeposit;
import com.opengamma.strata.product.deposit.IborFixingDepositProduct;

/**
 * The methods associated to the pricing of Ibor fixing deposit by discounting.
 * <p>
 * This function provides the ability to price a {@link IborFixingDeposit}. Those products are synthetic deposits
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
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(IborFixingDepositProduct product, RatesProvider provider) {
    ExpandedIborFixingDeposit deposit = product.expand();
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
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(IborFixingDepositProduct product, RatesProvider provider) {
    ExpandedIborFixingDeposit deposit = product.expand();
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
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(IborFixingDepositProduct product, RatesProvider provider) {
    ExpandedIborFixingDeposit deposit = product.expand();
    return forwardRate(deposit, provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the deposit fair rate sensitivity to the curves.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par rate curve sensitivity
   */
  public PointSensitivities parRateSensitivity(IborFixingDepositProduct product, RatesProvider provider) {
    ExpandedIborFixingDeposit deposit = product.expand();
    return forwardRateSensitivity(deposit, provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the spread to be added to the deposit rate to have a zero present value.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(IborFixingDepositProduct product, RatesProvider provider) {
    ExpandedIborFixingDeposit deposit = product.expand();
    return forwardRate(deposit, provider) - deposit.getFixedRate();
  }

  /**
   * Calculates the par spread curve sensitivity.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(IborFixingDepositProduct product, RatesProvider provider) {
    ExpandedIborFixingDeposit deposit = product.expand();
    return forwardRateSensitivity(deposit, provider).build();
  }

  //-------------------------------------------------------------------------
  // query the forward rate
  private double forwardRate(ExpandedIborFixingDeposit product, RatesProvider provider) {
    IborIndexRates rates = provider.iborIndexRates(product.getFloatingRate().getIndex());
    // The IborFixingDeposit are fictitious instruments to anchor the beginning of the IborIndex forward curve. 
    // By using the 'rateIgnoringTimeSeries' method (instead of 'rate') we ensure that only the forward curve is involved.
    return rates.rateIgnoringTimeSeries(product.getFloatingRate().getFixingDate());
  }

  // query the forward rate sensitivity
  private PointSensitivityBuilder forwardRateSensitivity(ExpandedIborFixingDeposit product, RatesProvider provider) {
    IborIndexRates rates = provider.iborIndexRates(product.getFloatingRate().getIndex());
    return rates.rateIgnoringTimeSeriesPointSensitivity(product.getFloatingRate().getFixingDate());
  }

}
