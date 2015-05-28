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
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.DiscountFactors;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * The methods associated to the pricing of term deposit by discounting.
 * <p>
 * This function provides the ability to price a {@link TermDeposit}.
 */
public class DiscountingTermDepositProductPricerBeta {

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
    double pv = (deposit.getNotional() + deposit.getInterest()) * dfEnd - initialAmount(deposit, provider) * dfStart;
    return CurrencyAmount.of(currency, pv);
  }

  // the initial amount is the same as the principal, but zero if the start date has passed
  // the caller must negate the result of this method if required
  private double initialAmount(ExpandedTermDeposit deposit, RatesProvider provider) {
    return provider.getValuationDate().isAfter(deposit.getStartDate()) ? 0d : deposit.getNotional();
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
    double dfEndBar = deposit.getNotional() + deposit.getInterest();
    double dfStartBar = -initialAmount(deposit, provider);
    // sensitivity
    DiscountFactors discountFactors = provider.discountFactors(currency);
    PointSensitivityBuilder sensStart = discountFactors.pointSensitivity(deposit.getStartDate())
        .multipliedBy(dfStartBar);
    PointSensitivityBuilder sensEnd = discountFactors.pointSensitivity(deposit.getEndDate())
        .multipliedBy(dfEndBar);
    return sensStart.combinedWith(sensEnd).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the deposit fair rate given the start and end time and the accrual factor.
   * <p>
   * When deposit has already start the number may not be meaningful as the remaining period
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
    return (dfStart / dfEnd - 1d) / accrualFactor;
  }

  /**
   * Calculates the spread to be added to the deposit rate to have a zero present value.
   * <p>
   * The calculation is based on both the initial and final payments. 
   * Thus the resulting number may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(TermDepositProduct product, RatesProvider provider) {
    ExpandedTermDeposit deposit = product.expand();
    double parRate = parRate(product, provider);
    return parRate - deposit.getRate();
  }

  /**
   * Calculates the par spread curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param product  the product to price
   * @param provider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(TermDepositProduct product, RatesProvider provider) {
    ExpandedTermDeposit deposit = product.expand();
    Currency currency = deposit.getCurrency();
    double accrualFactorInv = 1d / deposit.getYearFraction();
    double dfStart = provider.discountFactor(currency, deposit.getStartDate());
    double dfEndInv = 1d / provider.discountFactor(currency, deposit.getEndDate());
    DiscountFactors discountFactors = provider.discountFactors(currency);
    PointSensitivityBuilder sensStart = discountFactors.pointSensitivity(deposit.getStartDate())
        .multipliedBy(dfEndInv * accrualFactorInv);
    PointSensitivityBuilder sensEnd = discountFactors.pointSensitivity(deposit.getEndDate())
        .multipliedBy(-dfStart * dfEndInv * dfEndInv * accrualFactorInv);
    return sensStart.combinedWith(sensEnd).build();
  }

}
