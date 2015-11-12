/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.bond;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;
import com.opengamma.strata.product.rate.bond.BondFuture;
import com.opengamma.strata.product.rate.bond.BondFutureTrade;

/**
 * Pricer implementation for bond future trades.
 * <p>
 * This function provides the ability to price a {@link BondFutureTrade}.
 */
public final class DiscountingBondFutureTradePricer extends AbstractBondFutureTradePricer {

  /**
   * Default implementation.
   */
  public static final DiscountingBondFutureTradePricer DEFAULT = new DiscountingBondFutureTradePricer(
      DiscountingBondFutureProductPricer.DEFAULT);

  /**
   * Underlying pricer.
   */
  private final DiscountingBondFutureProductPricer productPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link BondFuture}
   */
  public DiscountingBondFutureTradePricer(DiscountingBondFutureProductPricer productPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
  }

  //-------------------------------------------------------------------------
  @Override
  protected DiscountingBondFutureProductPricer getProductPricer() {
    return productPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the price of the trade, in decimal form
   */
  public double price(BondFutureTrade trade, LegalEntityDiscountingProvider provider) {
    return productPricer.price(trade.getSecurity().getProduct(), provider);
  }

  /**
   * Calculates the price of the bond future trade with z-spread.
   * <p>
   * The price of the trade is the price on the valuation date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the price of the trade, in decimal form
   */
  public double priceWithZSpread(
      BondFutureTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    return productPricer.priceWithZSpread(trade.getSecurity().getProduct(), provider, zSpread, compoundedRateType, periodPerYear);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the present value
   */
  public CurrencyAmount presentValue(
      BondFutureTrade trade,
      LegalEntityDiscountingProvider provider,
      double referencePrice) {

    double price = price(trade, provider);
    return presentValue(trade, price, referencePrice);
  }

  /**
   * Calculates the present value of the bond future trade with z-spread.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the present value
   */
  public CurrencyAmount presentValueWithZSpread(
      BondFutureTrade trade,
      LegalEntityDiscountingProvider provider,
      double referencePrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    double price = priceWithZSpread(trade, provider, zSpread, compoundedRateType, periodPerYear);
    return presentValue(trade, price, referencePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bond future trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(BondFutureTrade trade, LegalEntityDiscountingProvider provider) {
    BondFuture product = trade.getSecurity().getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, provider);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  /**
   * Calculates the present value sensitivity of the bond future trade with z-spread.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      BondFutureTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    BondFuture product = trade.getSecurity().getProduct();
    PointSensitivities priceSensi =
        productPricer.priceSensitivityWithZSpread(product, provider, zSpread, compoundedRateType, periodPerYear);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread of the bond future trade.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @return the par spread.
   */
  public double parSpread(BondFutureTrade trade, LegalEntityDiscountingProvider provider, double referencePrice) {
    return price(trade, provider) - referencePrice;
  }

  /**
   * Calculates the par spread of the bond future trade with z-spread.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param referencePrice  the price with respect to which the margining should be done. The reference price is
   *   the trade date before any margining has taken place and the price used for the last margining otherwise.
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the par spread.
   */
  public double parSpreadWithZSpread(
      BondFutureTrade trade,
      LegalEntityDiscountingProvider provider,
      double referencePrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    return priceWithZSpread(trade, provider, zSpread, compoundedRateType, periodPerYear) - referencePrice;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread sensitivity of the bond future trade.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivity(BondFutureTrade trade, LegalEntityDiscountingProvider provider) {
    return productPricer.priceSensitivity(trade.getSecurity().getProduct(), provider);
  }

  /**
   * Calculates the par spread sensitivity of the bond future trade with z-spread.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * 
   * @param trade  the trade to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivityWithZSpread(
      BondFutureTrade trade,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    return productPricer.priceSensitivityWithZSpread(
        trade.getSecurity().getProduct(), provider, zSpread, compoundedRateType, periodPerYear);
  }

}
