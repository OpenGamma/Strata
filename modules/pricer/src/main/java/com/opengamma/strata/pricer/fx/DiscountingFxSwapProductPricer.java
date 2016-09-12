/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import com.opengamma.strata.product.fx.ResolvedFxSwap;

/**
 * Pricer for foreign exchange swap transaction products.
 * <p>
 * This provides the ability to price an {@link ResolvedFxSwap}.
 */
public class DiscountingFxSwapProductPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingFxSwapProductPricer DEFAULT =
      new DiscountingFxSwapProductPricer(DiscountingFxSingleProductPricer.DEFAULT);

  /**
   * Underlying single FX pricer.
   */
  private final DiscountingFxSingleProductPricer fxPricer;

  /**
   * Creates an instance.
   * 
   * @param fxPricer  the pricer for {@link ResolvedFxSingle}
   */
  public DiscountingFxSwapProductPricer(
      DiscountingFxSingleProductPricer fxPricer) {
    this.fxPricer = ArgChecker.notNull(fxPricer, "fxPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX swap product.
   * <p>
   * This discounts each payment on each leg in its own currency.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the present value in the two natural currencies
   */
  public MultiCurrencyAmount presentValue(ResolvedFxSwap swap, RatesProvider provider) {
    MultiCurrencyAmount farPv = fxPricer.presentValue(swap.getFarLeg(), provider);
    MultiCurrencyAmount nearPv = fxPricer.presentValue(swap.getNearLeg(), provider);
    return nearPv.plus(farPv);
  }

  /**
   * Calculates the present value sensitivity of the FX swap product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivity(ResolvedFxSwap swap, RatesProvider provider) {
    PointSensitivities nearSens = fxPricer.presentValueSensitivity(swap.getNearLeg(), provider);
    PointSensitivities farSens = fxPricer.presentValueSensitivity(swap.getFarLeg(), provider);
    return nearSens.combinedWith(farSens);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the par spread.
   * <p>
   * The par spread is the spread that should be added to the FX forward points to have a zero value.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the spread
   */
  public double parSpread(ResolvedFxSwap swap, RatesProvider provider) {
    Payment counterPaymentNear = swap.getNearLeg().getCounterCurrencyPayment();
    MultiCurrencyAmount pv = presentValue(swap, provider);
    double pvCounterCcy = pv.convertedTo(counterPaymentNear.getCurrency(), provider).getAmount();
    double dfEnd = provider.discountFactor(counterPaymentNear.getCurrency(), swap.getFarLeg().getPaymentDate());
    double notionalBaseCcy = swap.getNearLeg().getBaseCurrencyPayment().getAmount();
    return -pvCounterCcy / (notionalBaseCcy * dfEnd);
  }

  /**
   * Calculates the par spread sensitivity to the curves.
   * <p>
   * The sensitivity is reported in the counter currency of the product, but is actually dimensionless.
   * 
   * @param swap  the product
   * @param provider  the rates provider
   * @return the spread curve sensitivity
   */
  public PointSensitivities parSpreadSensitivity(ResolvedFxSwap swap, RatesProvider provider) {
    Payment counterPaymentNear = swap.getNearLeg().getCounterCurrencyPayment();
    MultiCurrencyAmount pv = presentValue(swap, provider);
    double pvCounterCcy = pv.convertedTo(counterPaymentNear.getCurrency(), provider).getAmount();
    double dfEnd = provider.discountFactor(counterPaymentNear.getCurrency(), swap.getFarLeg().getPaymentDate());
    double notionalBaseCcy = swap.getNearLeg().getBaseCurrencyPayment().getAmount();
    double ps = -pvCounterCcy / (notionalBaseCcy * dfEnd);
    // backward sweep
    double psBar = 1d;
    double pvCounterCcyBar = -1d / (notionalBaseCcy * dfEnd) * psBar;
    double dfEndBar = -ps / dfEnd * psBar;
    ZeroRateSensitivity ddfEnddr = provider.discountFactors(counterPaymentNear.getCurrency())
        .zeroRatePointSensitivity(swap.getFarLeg().getPaymentDate());
    PointSensitivities result = ddfEnddr.multipliedBy(dfEndBar).build();
    PointSensitivities dpvdr = presentValueSensitivity(swap, provider);
    PointSensitivities dpvdrConverted = dpvdr.convertedTo(counterPaymentNear.getCurrency(), provider);
    return result.combinedWith(dpvdrConverted.multipliedBy(pvCounterCcyBar));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX swap product.
   * <p>
   * This discounts each payment on each leg in its own currency.
   * 
   * @param product  the product
   * @param provider  the rates provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(ResolvedFxSwap product, RatesProvider provider) {
    return presentValue(product, provider);
  }

  /**
   * Calculates the current cash of the FX swap product.
   * 
   * @param swap  the product
   * @param valuationDate  the valuation date
   * @return the current cash
   */
  public MultiCurrencyAmount currentCash(ResolvedFxSwap swap, LocalDate valuationDate) {
    MultiCurrencyAmount farPv = fxPricer.currentCash(swap.getFarLeg(), valuationDate);
    MultiCurrencyAmount nearPv = fxPricer.currentCash(swap.getNearLeg(), valuationDate);
    return nearPv.plus(farPv);
  }

}
