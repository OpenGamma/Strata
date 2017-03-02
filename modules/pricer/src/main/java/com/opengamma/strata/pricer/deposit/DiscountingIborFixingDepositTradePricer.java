/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.deposit;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDeposit;
import com.opengamma.strata.product.deposit.ResolvedIborFixingDepositTrade;

/**
 * The methods associated to the pricing of Ibor fixing deposit trades by discounting.
 * <p>
 * This provides the ability to price {@link ResolvedIborFixingDepositTrade}.
 * These trades are synthetic trades which are used for curve calibration purposes.
 * They should not be used as actual trades.
 */
public class DiscountingIborFixingDepositTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingIborFixingDepositTradePricer DEFAULT =
      new DiscountingIborFixingDepositTradePricer(DiscountingIborFixingDepositProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedIborFixingDeposit}.
   */
  private final DiscountingIborFixingDepositProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedIborFixingDeposit}
   */
  public DiscountingIborFixingDepositTradePricer(DiscountingIborFixingDepositProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the Ibor fixing deposit trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value of the product
   */
  public CurrencyAmount presentValue(ResolvedIborFixingDepositTrade trade, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value sensitivity of the Ibor fixing deposit trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedIborFixingDepositTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the deposit fair rate given the start and end time and the accrual factor.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(ResolvedIborFixingDepositTrade trade, RatesProvider provider) {
    return productPricer.parRate(trade.getProduct(), provider);
  }

  /**
   * Calculates the deposit fair rate sensitivity to the curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate curve sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedIborFixingDepositTrade trade, RatesProvider provider) {
    return productPricer.parRateSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the spread to be added to the deposit rate to have a zero present value.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(ResolvedIborFixingDepositTrade trade, RatesProvider provider) {
    return productPricer.parSpread(trade.getProduct(), provider);
  }

  /**
   * Calculates the par spread curve sensitivity.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedIborFixingDepositTrade trade, RatesProvider provider) {
    return productPricer.parSpreadSensitivity(trade.getProduct(), provider);
  }

}
