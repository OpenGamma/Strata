/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.deposit;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.rate.deposit.ExpandedTermDeposit;
import com.opengamma.strata.finance.rate.deposit.TermDeposit;
import com.opengamma.strata.finance.rate.deposit.TermDepositProduct;
import com.opengamma.strata.pricer.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Pricer for for foreign exchange transaction products.
 * <p>
 * This function provides the ability to price a {@link TermDeposit}.
 */
public class DiscountingTermDepositProductPricerBeta {
  // copied/modified from CashDiscountingMethod
  // TODO: when valuation date after end date?

  /**
   * Default implementation.
   */
  public static final DiscountingTermDepositProductPricerBeta DEFAULT = new DiscountingTermDepositProductPricerBeta();

  /**
   * Creates an instance.
   */
  public DiscountingTermDepositProductPricerBeta() {
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value
   */
  public CurrencyAmount presentValue(TermDepositProduct product, RatesProvider provider) {
    ExpandedTermDeposit deposit = product.expand();
    Currency currency = deposit.getCurrency();
    double dfStart = provider.discountFactor(currency, deposit.getStartDate());
    double dfEnd = provider.discountFactor(currency, deposit.getEndDate());
    double pv = (deposit.getPrincipal() + deposit.getInterest()) * dfEnd - initialAmount(deposit, provider) * dfStart;
    return CurrencyAmount.of(currency, pv);
  }

  // the initial amount is the same as the principal, but zero if the start date has passed
  // the caller must negate the result of this method if required
  private double initialAmount(ExpandedTermDeposit deposit, RatesProvider provider) {
    return provider.getValuationDate().isAfter(deposit.getStartDate()) ? 0d : deposit.getPrincipal();
  }

  /**
   * Calculates the present value sensitivity by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(TermDepositProduct product, RatesProvider provider) {
    ExpandedTermDeposit deposit = product.expand();
    Currency currency = deposit.getCurrency();
    // backward sweep
    double dfEndBar = deposit.getPrincipal() + deposit.getInterest();
    double dfStartBar = -initialAmount(deposit, provider);
    // sensitivity
    PointSensitivityBuilder sensStart = provider.discountFactorZeroRateSensitivity(currency, deposit.getStartDate())
        .multipliedBy(dfStartBar);
    PointSensitivityBuilder sensEnd = provider.discountFactorZeroRateSensitivity(currency, deposit.getEndDate())
        .multipliedBy(dfEndBar);
    return sensStart.combinedWith(sensEnd).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the deposit fair rate given the start and end time and the accrual factor.
   * <p>
   * When deposit has already start the number may not be meaning full as the remaining period
   * is not in line with the accrual factor.
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(TermDepositProduct product, RatesProvider provider) {
    ExpandedTermDeposit deposit = product.expand();
    Currency currency = deposit.getCurrency();
    double dfStart = provider.discountFactor(currency, deposit.getStartDate());
    double dfEnd = provider.discountFactor(currency, deposit.getEndDate());
    double accrualFactor = deposit.getYearFraction();
    return (dfStart / dfEnd - 1) / accrualFactor;
  }

  /**
   * Calculates the spread to be added to the deposit rate to have a zero present value.
   * <p>
   * When deposit has already started the number may not be meaningful as only the final
   * payment remains (no initial payment).
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(TermDepositProduct product, RatesProvider provider) {
    ExpandedTermDeposit deposit = product.expand();
    Currency currency = deposit.getCurrency();
    double dfStart = provider.discountFactor(currency, deposit.getStartDate());
    double dfEnd = provider.discountFactor(currency, deposit.getEndDate());
    return (initialAmount(deposit, provider) * dfStart - (deposit.getPrincipal() + deposit.getInterest()) * dfEnd) /
        (deposit.getPrincipal() * deposit.getYearFraction() * dfEnd);
  }

  /**
   * Calculates the par spread curve sensitivity.
   * <p>
   * When deposit has already started the number may not be meaningful as only the final
   * payment remains (no initial payment).
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(TermDepositProduct product, RatesProvider provider) {
    ExpandedTermDeposit deposit = product.expand();
    Currency currency = deposit.getCurrency();
    double dfStart = provider.discountFactor(currency, deposit.getStartDate());
    double dfEnd = provider.discountFactor(currency, deposit.getEndDate());
    // backward sweep
    double accrualFactorPrincipal = deposit.getPrincipal() * deposit.getYearFraction();
    double dfStartBar = (initialAmount(deposit, provider) / dfEnd) / accrualFactorPrincipal;
    double dfEndBar = -(initialAmount(deposit, provider) * dfStart / (dfEnd * dfEnd)) / accrualFactorPrincipal;
    // sensitivity
    PointSensitivityBuilder sensStart = provider.discountFactorZeroRateSensitivity(currency, deposit.getStartDate())
        .multipliedBy(dfStartBar);
    PointSensitivityBuilder sensEnd = provider.discountFactorZeroRateSensitivity(currency, deposit.getEndDate())
        .multipliedBy(dfEndBar);
    return sensStart.combinedWith(sensEnd).build();
  }

}
