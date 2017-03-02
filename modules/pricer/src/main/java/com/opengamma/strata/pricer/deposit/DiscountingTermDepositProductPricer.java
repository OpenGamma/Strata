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
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;

/**
 * The methods associated to the pricing of term deposit by discounting.
 * <p>
 * This provides the ability to price {@link ResolvedTermDeposit}.
 */
public class DiscountingTermDepositProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingTermDepositProductPricer DEFAULT = new DiscountingTermDepositProductPricer();

  /**
   * Creates an instance.
   */
  public DiscountingTermDepositProductPricer() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(ResolvedTermDeposit deposit, RatesProvider provider) {
    Currency currency = deposit.getCurrency();
    if (provider.getValuationDate().isAfter(deposit.getEndDate())) {
      return CurrencyAmount.of(currency, 0.0d);
    }
    DiscountFactors discountFactors = provider.discountFactors(currency);
    double dfStart = discountFactors.discountFactor(deposit.getStartDate());
    double dfEnd = discountFactors.discountFactor(deposit.getEndDate());
    double pvStart = initialAmount(deposit, provider) * dfStart;
    double pvEnd = (deposit.getNotional() + deposit.getInterest()) * dfEnd;
    double pv = pvEnd - pvStart;
    return CurrencyAmount.of(currency, pv);
  }

  // the initial amount is the same as the principal, but zero if the start date has passed
  // the caller must negate the result of this method if required
  private double initialAmount(ResolvedTermDeposit deposit, RatesProvider provider) {
    return provider.getValuationDate().isAfter(deposit.getStartDate()) ? 0d : deposit.getNotional();
  }

  /**
   * Calculates the present value sensitivity by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedTermDeposit deposit, RatesProvider provider) {
    Currency currency = deposit.getCurrency();
    // backward sweep
    double dfEndBar = deposit.getNotional() + deposit.getInterest();
    double dfStartBar = -initialAmount(deposit, provider);
    // sensitivity
    DiscountFactors discountFactors = provider.discountFactors(currency);
    PointSensitivityBuilder sensStart = discountFactors.zeroRatePointSensitivity(deposit.getStartDate())
        .multipliedBy(dfStartBar);
    PointSensitivityBuilder sensEnd = discountFactors.zeroRatePointSensitivity(deposit.getEndDate())
        .multipliedBy(dfEndBar);
    return sensStart.combinedWith(sensEnd).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the deposit fair rate given the start and end time and the accrual factor.
   * <p>
   * When the deposit has already started the number may not be meaningful as the remaining period
   * is not in line with the accrual factor.
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(ResolvedTermDeposit deposit, RatesProvider provider) {
    Currency currency = deposit.getCurrency();
    DiscountFactors discountFactors = provider.discountFactors(currency);
    double dfStart = discountFactors.discountFactor(deposit.getStartDate());
    double dfEnd = discountFactors.discountFactor(deposit.getEndDate());
    double accrualFactor = deposit.getYearFraction();
    return (dfStart / dfEnd - 1d) / accrualFactor;
  }

  /**
   * Calculates the par rate curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par rate curve sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedTermDeposit deposit, RatesProvider provider) {
    return parSpreadSensitivity(deposit, provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the spread to be added to the deposit rate to have a zero present value.
   * <p>
   * The calculation is based on both the initial and final payments.
   * Thus the resulting number may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(ResolvedTermDeposit deposit, RatesProvider provider) {
    double parRate = parRate(deposit, provider);
    return parRate - deposit.getRate();
  }

  /**
   * Calculates the par spread curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param deposit  the product
   * @param provider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedTermDeposit deposit, RatesProvider provider) {
    Currency currency = deposit.getCurrency();
    double accrualFactorInv = 1d / deposit.getYearFraction();
    double dfStart = provider.discountFactor(currency, deposit.getStartDate());
    double dfEndInv = 1d / provider.discountFactor(currency, deposit.getEndDate());
    DiscountFactors discountFactors = provider.discountFactors(currency);
    PointSensitivityBuilder sensStart = discountFactors.zeroRatePointSensitivity(deposit.getStartDate())
        .multipliedBy(dfEndInv * accrualFactorInv);
    PointSensitivityBuilder sensEnd = discountFactors.zeroRatePointSensitivity(deposit.getEndDate())
        .multipliedBy(-dfStart * dfEndInv * dfEndInv * accrualFactorInv);
    return sensStart.combinedWith(sensEnd).build();
  }

}
