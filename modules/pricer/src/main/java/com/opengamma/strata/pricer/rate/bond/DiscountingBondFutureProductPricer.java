/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.bond;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.Security;
import com.opengamma.strata.finance.rate.bond.BondFuture;
import com.opengamma.strata.finance.rate.bond.FixedCouponBond;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.CompoundedRateType;
import com.opengamma.strata.pricer.rate.LegalEntityDiscountingProvider;

/**
 * Pricer for for bond future products.
 * <p>
 * This function provides the ability to price a {@link BondFuture}.
 */
public final class DiscountingBondFutureProductPricer extends AbstractBondFutureProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingBondFutureProductPricer DEFAULT =
      new DiscountingBondFutureProductPricer(DiscountingFixedCouponBondProductPricer.DEFAULT);
  /**
   * Underlying pricer.
   */
  private final DiscountingFixedCouponBondProductPricer bondPricer;

  /**
   * Creates an instance. 
   * 
   * @param bondPricer  the pricer for {@link FixedCouponBond}.
   */
  public DiscountingBondFutureProductPricer(DiscountingFixedCouponBondProductPricer bondPricer) {
    this.bondPricer = ArgChecker.notNull(bondPricer, "bondPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the price of the bond future product.
   * <p>
   * The price of the product is the price on the valuation date.
   * 
   * @param future  the future to price
   * @param provider  the rates provider
   * @return the price of the product, in decimal form
   */
  public double price(BondFuture future, LegalEntityDiscountingProvider provider) {
    ImmutableList<Security<FixedCouponBond>> bondSecurity = future.getBondSecurityBasket();
    int size = bondSecurity.size();
    double[] priceBonds = new double[size];
    for (int i = 0; i < size; ++i) {
      Security<FixedCouponBond> bond = bondSecurity.get(i);
      double dirtyPrice = bondPricer.dirtyPriceFromCurves(bond, provider, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond.getProduct(), future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
    }
    final double priceFuture = Doubles.min(priceBonds);
    return priceFuture;
  }

  /**
   * Calculates the price of the bond future product with z-spread.
   * <p>
   * The price of the product is the price on the valuation date.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * 
   * @param future  the future to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the price of the product, in decimal form
   */
  public double priceWithZSpread(
      BondFuture future,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {
    ImmutableList<Security<FixedCouponBond>> bondSecurity = future.getBondSecurityBasket();
    int size = bondSecurity.size();
    double[] priceBonds = new double[size];
    for (int i = 0; i < size; ++i) {
      Security<FixedCouponBond> bond = bondSecurity.get(i);
      double dirtyPrice = bondPricer.dirtyPriceFromCurvesWithZSpread(
          bond, provider, zSpread, compoundedRateType, periodPerYear, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond.getProduct(), future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
    }
    double priceFuture = Doubles.min(priceBonds);
    return priceFuture;
  }

  /**
   * Calculates the price sensitivity of the bond future product.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * <p>
   * Note that the price sensitivity should be no currency. 
   * 
   * @param future  the future to price
   * @param provider  the rates provider
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivity(BondFuture future, LegalEntityDiscountingProvider provider) {
    ImmutableList<Security<FixedCouponBond>> bondSecurity = future.getBondSecurityBasket();
    int size = bondSecurity.size();
    double[] priceBonds = new double[size];
    int indexCTD = 0;
    double priceMin = 2d;
    for (int i = 0; i < size; i++) {
      Security<FixedCouponBond> bond = bondSecurity.get(i);
      double dirtyPrice = bondPricer.dirtyPriceFromCurves(bond, provider, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond.getProduct(), future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
      if (priceBonds[i] < priceMin) {
        priceMin = priceBonds[i];
        indexCTD = i;
      }
    }
    PointSensitivityBuilder pointSensi = bondPricer.dirtyPriceSensitivity(
        bondSecurity.get(indexCTD), provider, future.getLastDeliveryDate());
    return pointSensi.multipliedBy(1d / future.getConversionFactor().get(indexCTD)).build();
  }

  /**
   * Calculates the price sensitivity of the bond future product with z-spread.
   * <p>
   * The price sensitivity of the product is the sensitivity of the price to the underlying curves.
   * <p>
   * The z-spread is a parallel shift applied to continuously compounded rates or periodic compounded rates 
   * of the issuer discounting curve. 
   * <p>
   * Note that the price sensitivity should be no currency. 
   * 
   * @param future  the future to price
   * @param provider  the rates provider
   * @param zSpread  the z-spread
   * @param compoundedRateType  the compounded rate type
   * @param periodPerYear  the number of periods per year
   * @return the price curve sensitivity of the product
   */
  public PointSensitivities priceSensitivityWithZSpread(
      BondFuture future,
      LegalEntityDiscountingProvider provider,
      double zSpread,
      CompoundedRateType compoundedRateType,
      int periodPerYear) {
    ImmutableList<Security<FixedCouponBond>> bondSecurity = future.getBondSecurityBasket();
    int size = bondSecurity.size();
    double[] priceBonds = new double[size];
    int indexCTD = 0;
    double priceMin = 2d;
    for (int i = 0; i < size; i++) {
      Security<FixedCouponBond> bond = bondSecurity.get(i);
      double dirtyPrice = bondPricer.dirtyPriceFromCurvesWithZSpread(
          bond, provider, zSpread, compoundedRateType, periodPerYear, future.getLastDeliveryDate());
      priceBonds[i] = bondPricer.cleanPriceFromDirtyPrice(
          bond.getProduct(), future.getLastDeliveryDate(), dirtyPrice) / future.getConversionFactor().get(i);
      if (priceBonds[i] < priceMin) {
        priceMin = priceBonds[i];
        indexCTD = i;
      }
    }
    PointSensitivityBuilder pointSensi = bondPricer.dirtyPriceSensitivityWithZspread(
        bondSecurity.get(indexCTD), provider, zSpread, compoundedRateType, periodPerYear, future.getLastDeliveryDate());
    return pointSensi.multipliedBy(1d / future.getConversionFactor().get(indexCTD)).build();
  }
}
