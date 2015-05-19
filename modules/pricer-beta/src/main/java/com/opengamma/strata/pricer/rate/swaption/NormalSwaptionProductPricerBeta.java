/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import java.time.ZonedDateTime;
import java.util.List;

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.NormalPriceFunction;
import com.opengamma.analytics.math.function.Function1D;
import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.finance.rate.swaption.Swaption;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.provider.NormalVolatilitySwaptionProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.sensitivity.SwaptionSensitivity;

/**
 * Pricer for swaption in a normal model on the swap rate.
 * <p>
 * The swap underlying the swaption should have a fixed leg on which the forward rate is computed. The underlying swap
 * should be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap conventions. The volatilities from the provider
 * are taken as such.
 */
public class NormalSwaptionProductPricerBeta {

  /**
   * Default implementation.
   */
  public static final NormalSwaptionProductPricerBeta DEFAULT = new NormalSwaptionProductPricerBeta(
      DiscountingSwapProductPricer.DEFAULT);

  /** Pricer for {@link SwapProduct}. */
  private final DiscountingSwapProductPricer swapPricer;

  /** Normal model pricing function. */
  public static final NormalPriceFunction NORMAL = new NormalPriceFunction();
  
  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public NormalSwaptionProductPricerBeta(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swap pricer");
  }
  
  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product to price
   * @param rates  the rates provider
   * @param volatilities  the normal volatility parameters
   * @return the present value of the swap product
   */
  public CurrencyAmount presentValue(Swaption swaption, RatesProvider rates, 
      NormalVolatilitySwaptionProvider volatilities) {
    validate(rates, swaption, volatilities);
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double forward = swapPricer.parRate(underlying, rates);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, rates);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, rates, pvbp);
    double tenor = volatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilities.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = new NormalFunctionData(forward, Math.abs(pvbp), volatility);
    double expiry = volatilities.relativeTime(expiryDateTime);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiry, isCall);
    // option required to pass the strike (in case the swap has non-constant coupon).
    Function1D<NormalFunctionData, Double> func = NORMAL.getPriceFunction(option);
    double pv = func.evaluate(normalData) * ((swaption.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return CurrencyAmount.of(fixedLeg.getCurrency(), pv);
  }  

  /**
   * Computes the implied Normal volatility of the swaption.
   * 
   * @param swaption  the product to price
   * @param rates  the rates provider
   * @param volatilities  the normal volatility parameters
   * @return the present value of the swap product
   */
  public double impliedVolatility(Swaption swaption, RatesProvider rates, 
      NormalVolatilitySwaptionProvider volatilities) {
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double forward = swapPricer.parRate(underlying, rates);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, rates);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, rates, pvbp);
    double tenor = volatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return volatilities.getVolatility(expiryDateTime, tenor, strike, forward);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swaption  the swaption product
   * @param rates  the rates provider
   * @param volatilities  the normal volatility provider
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(Swaption swaption, RatesProvider rates, 
      NormalVolatilitySwaptionProvider volatilities) {
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double forward = swapPricer.parRate(underlying, rates);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, rates);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, rates, pvbp);
    double tenor = volatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilities.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = new NormalFunctionData(forward, 1.0d, volatility);
    double expiry = volatilities.relativeTime(expiryDateTime);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiry, isCall);
    // option required to pass the strike (in case the swap has non-constant coupon).
    // Backward sweep
    PointSensitivityBuilder pvbpDr = swapPricer.getLegPricer().pvbpSensitivity(fixedLeg, rates);
    PointSensitivityBuilder forwardDr = swapPricer.parRateSensitivity(underlying, rates);
    double[] ad = new double[3];
    double pv = NORMAL.getPriceAdjoint(option, normalData, ad);
    double sign =  (swaption.getLongShort() == LongShort.LONG) ? 1.0 : -1.0;
    return pvbpDr.multipliedBy(pv * sign * Math.signum(pvbp)).combinedWith(forwardDr.multipliedBy(ad[0] * Math.abs(pvbp) * sign));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption product.
   * <p>
   * The sensitivity to the implied normal volatility is also called normal vega.
   * 
   * @param swaption  the swaption product
   * @param rates  the rates provider
   * @param volatilities  the normal volatility provider
   * @return the point sensitivity to the normal volatility
   */
  public SwaptionSensitivity presentValueSensitivityNormalVolatility(Swaption swaption, RatesProvider rates, 
      NormalVolatilitySwaptionProvider volatilities) {
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double forward = swapPricer.parRate(underlying, rates);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, rates);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, rates, pvbp);
    double tenor = volatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilities.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = new NormalFunctionData(forward, Math.abs(pvbp), volatility);
    double expiry = volatilities.relativeTime(expiryDateTime);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiry, isCall);
    // option required to pass the strike (in case the swap has non-constant coupon).
    // Backward sweep
    double vega = NORMAL.getVega(option, normalData) * ((swaption.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return SwaptionSensitivity.of(volatilities.getIndex(), expiryDateTime, tenor, strike, forward, 
        fixedLeg.getCurrency(), vega);
  }

  // check that one leg is fixed and return it
  private ExpandedSwapLeg fixedLeg(ExpandedSwap swap) {
  ArgChecker.isFalse(swap.isCrossCurrency(), "swap should be single currency");
  // find fixed leg
  List<ExpandedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
  if (fixedLegs.isEmpty()) {
    throw new IllegalArgumentException("Swap must contain a fixed leg");
  }
  return fixedLegs.get(0);
  }
  
  // validate that the rates and volatilities providers are coherent
  private void validate(RatesProvider rates, Swaption swaption, NormalVolatilitySwaptionProvider volatility) {
    ArgChecker.isTrue(volatility.getValuationDate().equals(rates.getValuationDate()), 
        "volatility and rate data should be for the same date");
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "underlying swap should be single currency");
  }

}
