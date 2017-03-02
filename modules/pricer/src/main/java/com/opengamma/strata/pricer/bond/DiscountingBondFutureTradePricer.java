/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.bond;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.CompoundedRateType;
import com.opengamma.strata.product.bond.BondFuture;
import com.opengamma.strata.product.bond.BondFutureTrade;
import com.opengamma.strata.product.bond.FixedCouponBond;
import com.opengamma.strata.product.bond.ResolvedBondFuture;
import com.opengamma.strata.product.bond.ResolvedBondFutureTrade;

/**
 * Pricer implementation for bond future trades.
 * <p>
 * This function provides the ability to price a {@link BondFutureTrade}.
 * 
 * <h4>Price</h4>
 * Strata uses <i>decimal prices</i> for bond futures in the trade model, pricers and market data.
 * This is coherent with the pricing of {@link FixedCouponBond}. The bond futures delivery is a bond
 * for an amount computed from the bond future price, a conversion factor and the accrued interest.
 */
public final class DiscountingBondFutureTradePricer {

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
  /**
   * Calculates the present value of the bond future trade from the current price.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The calculation is performed against a reference price. The reference price
   * must be the last settlement price used for margining, except on the trade date,
   * when it must be the trade price.
   * 
   * @param trade  the trade
   * @param currentPrice  the price on the valuation date
   * @param referencePrice  the price with respect to which the margining should be done
   * @return the present value
   */
  CurrencyAmount presentValue(ResolvedBondFutureTrade trade, double currentPrice, double referencePrice) {
    ResolvedBondFuture future = trade.getProduct();
    double priceIndex = productPricer.marginIndex(future, currentPrice);
    double referenceIndex = productPricer.marginIndex(future, referencePrice);
    double pv = (priceIndex - referenceIndex) * trade.getQuantity();
    return CurrencyAmount.of(future.getCurrency(), pv);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future trade.
   * <p>
   * The price of the trade is the price on the valuation date.
   * <p>
   * Strata uses <i>decimal prices</i> for bond futures. This is coherent with the pricing of {@link FixedCouponBond}.
   * For example, a price of 99.32% is represented in Strata by 0.9932.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @return the price of the trade, in decimal form
   */
  public double price(ResolvedBondFutureTrade trade, LegalEntityDiscountingProvider discountingProvider) {
    return productPricer.price(trade.getProduct(), discountingProvider);
  }

  /**
   * Calculates the price of the bond future trade with z-spread.
   * <p>
   * The price of the trade is the price on the valuation date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the price of the trade, in decimal form
   */
  public double priceWithZSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    return productPricer.priceWithZSpread(trade.getProduct(), discountingProvider, zSpread, compoundedRateType, periodPerYear);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the bond future trade.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double lastSettlementPrice) {

    double price = price(trade, discountingProvider);
    double referencePrice = referencePrice(trade, discountingProvider.getValuationDate(), lastSettlementPrice);
    return presentValue(trade, price, referencePrice);
  }

  /**
   * Calculates the present value of the bond future trade with z-spread.
   * <p>
   * The present value of the product is the value on the valuation date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the present value
   */
  public CurrencyAmount presentValueWithZSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double lastSettlementPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    double price = priceWithZSpread(trade, discountingProvider, zSpread, compoundedRateType, periodPerYear);
    double referencePrice = referencePrice(trade, discountingProvider.getValuationDate(), lastSettlementPrice);
    return presentValue(trade, price, referencePrice);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the bond future trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivity(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    ResolvedBondFuture product = trade.getProduct();
    PointSensitivities priceSensi = productPricer.priceSensitivity(product, discountingProvider);
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
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityWithZSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    ResolvedBondFuture product = trade.getProduct();
    PointSensitivities priceSensi =
        productPricer.priceSensitivityWithZSpread(product, discountingProvider, zSpread, compoundedRateType, periodPerYear);
    PointSensitivities marginIndexSensi = productPricer.marginIndexSensitivity(product, priceSensi);
    return marginIndexSensi.multipliedBy(trade.getQuantity());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread of the bond future trade.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the par spread
   */
  public double parSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double lastSettlementPrice) {

    double referencePrice = referencePrice(trade, discountingProvider.getValuationDate(), lastSettlementPrice);
    return price(trade, discountingProvider) - referencePrice;
  }

  /**
   * Calculates the par spread of the bond future trade with z-spread.
   * <p>
   * The par spread is defined in the following way. When the reference price (or market quote)
   * is increased by the par spread, the present value of the trade is zero.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the par spread
   */
  public double parSpreadWithZSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double lastSettlementPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    double referencePrice = referencePrice(trade, discountingProvider.getValuationDate(), lastSettlementPrice);
    return priceWithZSpread(trade, discountingProvider, zSpread, compoundedRateType, periodPerYear) - referencePrice;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread sensitivity of the bond future trade.
   * <p>
   * The par spread sensitivity of the trade is the sensitivity of the par spread to
   * the underlying curves.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivity(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider) {

    return productPricer.priceSensitivity(trade.getProduct(), discountingProvider);
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
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the par spread curve sensitivity of the trade
   */
  public PointSensitivities parSpreadSensitivityWithZSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    return productPricer.priceSensitivityWithZSpread(
        trade.getProduct(), discountingProvider, zSpread, compoundedRateType, periodPerYear);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the bond future trade.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the currency exposure of the bond future trade
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double lastSettlementPrice) {

    double price = price(trade, discountingProvider);
    double referencePrice = referencePrice(trade, discountingProvider.getValuationDate(), lastSettlementPrice);
    return MultiCurrencyAmount.of(presentValue(trade, price, referencePrice));
  }

  /**
   * Calculates the currency exposure of the bond future trade with z-spread.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve.
   * <p>
   * This method calculates based on the difference between the model price and the
   * last settlement price, or the trade price if traded on the valuation date.
   * 
   * @param trade  the trade
   * @param discountingProvider  the discounting provider
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the currency exposure of the bond future trade
   */
  public MultiCurrencyAmount currencyExposureWithZSpread(
      ResolvedBondFutureTrade trade,
      LegalEntityDiscountingProvider discountingProvider,
      double lastSettlementPrice,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {

    double price = priceWithZSpread(trade, discountingProvider, zSpread, compoundedRateType, periodPerYear);
    double referencePrice = referencePrice(trade, discountingProvider.getValuationDate(), lastSettlementPrice);
    return MultiCurrencyAmount.of(presentValue(trade, price, referencePrice));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the reference price for the trade.
   * <p>
   * If the valuation date equals the trade date, then the reference price is the trade price.
   * Otherwise, the reference price is the last settlement price used for margining.
   * 
   * @param trade  the trade
   * @param valuationDate  the date for which the reference price should be calculated
   * @param lastSettlementPrice  the last settlement price used for margining, in decimal form
   * @return the reference price, in decimal form
   */
  private double referencePrice(ResolvedBondFutureTrade trade, LocalDate valuationDate, double lastSettlementPrice) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    return (trade.getTradeDate().equals(valuationDate) ? trade.getPrice() : lastSettlementPrice);
  }

}
