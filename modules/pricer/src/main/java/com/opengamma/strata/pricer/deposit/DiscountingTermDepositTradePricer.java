/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.ResolvedTermDeposit;
import com.opengamma.strata.product.deposit.ResolvedTermDepositTrade;

/**
 * The methods associated to the pricing of term deposit by discounting.
 * <p>
 * This provides the ability to price {@link ResolvedTermDeposit}.
 */
public class DiscountingTermDepositTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingTermDepositTradePricer DEFAULT =
      new DiscountingTermDepositTradePricer(DiscountingTermDepositProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedTermDeposit}.
   */
  private final DiscountingTermDepositProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedTermDeposit}
   */
  public DiscountingTermDepositTradePricer(DiscountingTermDepositProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(ResolvedTermDepositTrade trade, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value sensitivity by discounting the final cash flow (nominal + interest)
   * and the initial payment (initial amount).
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedTermDepositTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the deposit fair rate given the start and end time and the accrual factor.
   * <p>
   * When the deposit has already started the number may not be meaningful as the remaining period
   * is not in line with the accrual factor.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(ResolvedTermDepositTrade trade, RatesProvider provider) {
    return productPricer.parRate(trade.getProduct(), provider);
  }

  /**
   * Calculates the par rate curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate curve sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedTermDepositTrade trade, RatesProvider provider) {
    return productPricer.parRateSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the spread to be added to the deposit rate to have a zero present value.
   * <p>
   * The calculation is based on both the initial and final payments.
   * Thus the resulting number may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(ResolvedTermDepositTrade trade, RatesProvider provider) {
    return productPricer.parSpread(trade.getProduct(), provider);
  }

  /**
   * Calculates the par spread curve sensitivity.
   * <p>
   * The calculation is based on both of initial and final payments.
   * Thus the number resulting may not be meaningful when deposit has already started and only the final
   * payment remains (no initial payment).
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedTermDepositTrade trade, RatesProvider provider) {
    return productPricer.parSpreadSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedTermDepositTrade trade, RatesProvider provider) {
    return MultiCurrencyAmount.of(presentValue(trade, provider));
  }

  /**
   * Calculates the current cash.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the current cash
   */
  public CurrencyAmount currentCash(ResolvedTermDepositTrade trade, RatesProvider provider) {
    ResolvedTermDeposit product = trade.getProduct();
    if (product.getStartDate().isEqual(provider.getValuationDate())) {
      return CurrencyAmount.of(product.getCurrency(), -product.getNotional());
    }
    if (product.getEndDate().isEqual(provider.getValuationDate())) {
      return CurrencyAmount.of(product.getCurrency(), product.getNotional() + product.getInterest());
    }
    return CurrencyAmount.zero(product.getCurrency());
  }

}
