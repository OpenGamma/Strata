/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.payment;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.BaseProvider;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.product.payment.ResolvedBulletPaymentTrade;

/**
 * Pricer for for bullet payment trades.
 * <p>
 * This provides the ability to price {@link ResolvedBulletPaymentTrade}.
 * The trade is priced by discounting the underlying payment.
 */
public class DiscountingBulletPaymentTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingBulletPaymentTradePricer DEFAULT = new DiscountingBulletPaymentTradePricer(
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public DiscountingBulletPaymentTradePricer(
      DiscountingPaymentPricer paymentPricer) {
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying payment pricer.
   * 
   * @return the payment pricer
   */
  public DiscountingPaymentPricer getPaymentPricer() {
    return paymentPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bullet payment trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is the discounted forecast value.
   * 
   * @param trade  the trade
   * @param provider  the provider
   * @return the present value of the trade
   */
  public CurrencyAmount presentValue(ResolvedBulletPaymentTrade trade, BaseProvider provider) {
    return paymentPricer.presentValue(trade.getProduct().getPayment(), provider);
  }

  /**
   * Explains the present value of the bullet payment product.
   * <p>
   * This returns explanatory information about the calculation.
   * 
   * @param trade  the trade
   * @param provider  the provider
   * @return the explanatory information
   */
  public ExplainMap explainPresentValue(ResolvedBulletPaymentTrade trade, BaseProvider provider) {
    return paymentPricer.explainPresentValue(trade.getProduct().getPayment(), provider);
  }

  /**
   * Calculates the present value sensitivity of the bullet payment trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedBulletPaymentTrade trade, BaseProvider provider) {
    return paymentPricer.presentValueSensitivity(trade.getProduct().getPayment(), provider).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flow of the bullet payment trade.
   * <p>
   * There is only one cash flow on the payment date for the bullet payment trade.
   * 
   * @param trade  the trade
   * @param provider  the provider
   * @return the cash flows
   */
  public CashFlows cashFlows(ResolvedBulletPaymentTrade trade, BaseProvider provider) {
    return paymentPricer.cashFlows(trade.getProduct().getPayment(), provider);
  }

  /**
   * Calculates the currency exposure of the bullet payment trade.
   * 
   * @param trade  the trade
   * @param provider  the provider
   * @return the currency exposure
   */
  public CurrencyAmount currencyExposure(ResolvedBulletPaymentTrade trade, BaseProvider provider) {
    return paymentPricer.presentValue(trade.getProduct().getPayment(), provider);
  }

  /**
   * Calculates the current cash of the bullet payment trade.
   * 
   * @param trade  the trade
   * @param provider  the provider
   * @return the current cash
   */
  public CurrencyAmount currentCash(ResolvedBulletPaymentTrade trade, BaseProvider provider) {
    return paymentPricer.currentCash(trade.getProduct().getPayment(), provider);
  }

}
