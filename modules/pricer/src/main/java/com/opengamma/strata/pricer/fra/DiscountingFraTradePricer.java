/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fra;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.amount.CashFlows;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fra.ResolvedFra;
import com.opengamma.strata.product.fra.ResolvedFraTrade;

/**
 * Pricer for for forward rate agreement (FRA) trades.
 * <p>
 * This provides the ability to price {@link ResolvedFraTrade}.
 * The trade is priced by pricing the underlying product using a forward curve for the index.
 */
public class DiscountingFraTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFraTradePricer DEFAULT = new DiscountingFraTradePricer(
      DiscountingFraProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFra}.
   */
  private final DiscountingFraProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFra}
   */
  public DiscountingFraTradePricer(
      DiscountingFraProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying product pricer.
   * 
   * @return the product pricer
   */
  public DiscountingFraProductPricer getProductPricer() {
    return productPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FRA trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * This is the discounted forecast value.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the present value of the trade
   */
  public CurrencyAmount presentValue(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.presentValue(trade.getProduct(), provider);
  }

  /**
   * Explains the present value of the FRA product.
   * <p>
   * This returns explanatory information about the calculation.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the explanatory information
   */
  public ExplainMap explainPresentValue(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.explainPresentValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the present value sensitivity of the FRA trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the point sensitivity of the present value
   */
  public PointSensitivities presentValueSensitivity(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.presentValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forecast value of the FRA trade.
   * <p>
   * The forecast value of the trade is the value on the valuation date without present value discounting.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the forecast value of the trade
   */
  public CurrencyAmount forecastValue(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.forecastValue(trade.getProduct(), provider);
  }

  /**
   * Calculates the forecast value sensitivity of the FRA trade.
   * <p>
   * The forecast value sensitivity of the product is the sensitivity of the forecast value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the point sensitivity of the forecast value
   */
  public PointSensitivities forecastValueSensitivity(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.forecastValueSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par rate of the FRA trade.
   * <p>
   * The par rate is the rate for which the FRA present value is 0.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate
   */
  public double parRate(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.parRate(trade.getProduct(), provider);
  }

  /**
   * Calculates the par rate curve sensitivity of the FRA trade.
   * <p>
   * The par rate curve sensitivity of the product is the sensitivity of the par rate to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par rate sensitivity
   */
  public PointSensitivities parRateSensitivity(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.parRateSensitivity(trade.getProduct(), provider);
  }

  /**
   * Calculates the par spread of the FRA trade.
   * <p>
   * This is spread to be added to the fixed rate to have a present value of 0.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread
   */
  public double parSpread(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.parSpread(trade.getProduct(), provider);
  }

  /**
   * Calculates the par spread curve sensitivity of the FRA trade.
   * <p>
   * The par spread curve sensitivity of the product is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the par spread sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.parSpreadSensitivity(trade.getProduct(), provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the future cash flow of the FRA trade.
   * <p>
   * There is only one cash flow on the payment date for the FRA trade.
   * The expected currency amount of the cash flow is the same as {@link #forecastValue(ResolvedFraTrade, RatesProvider)}.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the cash flows
   */
  public CashFlows cashFlows(ResolvedFraTrade trade, RatesProvider provider) {
    return productPricer.cashFlows(trade.getProduct(), provider);
  }

  /**
   * Calculates the currency exposure of the FRA trade.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFraTrade trade, RatesProvider provider) {
    return MultiCurrencyAmount.of(presentValue(trade, provider));
  }

  /**
   * Calculates the current cash of the FRA trade.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return the current cash
   */
  public CurrencyAmount currentCash(ResolvedFraTrade trade, RatesProvider provider) {
    ResolvedFra fra = trade.getProduct();
    if (fra.getPaymentDate().isEqual(provider.getValuationDate())) {
      return productPricer.presentValue(fra, provider);
    }
    return CurrencyAmount.zero(fra.getCurrency());
  }

}
