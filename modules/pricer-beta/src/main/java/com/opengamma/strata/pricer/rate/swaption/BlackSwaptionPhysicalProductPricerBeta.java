/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate.swaption;

import java.time.ZonedDateTime;
import java.util.List;

import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.BlackFunctionData;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.BlackPriceFunction;
import com.opengamma.analytics.financial.model.option.pricing.analytic.formula.EuropeanVanillaOption;
import com.opengamma.analytics.financial.model.volatility.BlackFormulaRepository;
import com.opengamma.analytics.math.function.Function1D;
import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.finance.rate.swap.ExpandedSwap;
import com.opengamma.strata.finance.rate.swap.ExpandedSwapLeg;
import com.opengamma.strata.finance.rate.swap.Swap;
import com.opengamma.strata.finance.rate.swap.SwapLegType;
import com.opengamma.strata.finance.rate.swap.SwapProduct;
import com.opengamma.strata.finance.rate.swaption.Swaption;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.provider.BlackVolatilitySwaptionProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.rate.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.sensitivity.SwaptionSensitivity;

/**
 * Pricer for swaption with physical settlement in a log-normal or Black model on the swap rate.
 * <p>
 * The swap underlying the swaption should have a fixed leg on which the forward rate is computed. The underlying swap
 * should be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap conventions. The volatilities from the provider
 * are taken as such.
 * <p>
 * The value of the swaption after expiry is 0. The fact that the option is after expiry is equivalent to a negative
 * number returned by the {@link BlackVolatilitySwaptionProvider#relativeYearFraction(ZonedDateTime)}.
 */
public class BlackSwaptionPhysicalProductPricerBeta {

  /**
   * Default implementation.
   */
  public static final BlackSwaptionPhysicalProductPricerBeta DEFAULT = new BlackSwaptionPhysicalProductPricerBeta(
      DiscountingSwapProductPricer.DEFAULT);

  /** Pricer for {@link SwapProduct}. */
  private final DiscountingSwapProductPricer swapPricer;

  /** Black model pricing function. */
  public static final BlackPriceFunction BLACK = new BlackPriceFunction();
  
  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public BlackSwaptionPhysicalProductPricerBeta(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swap pricer");
  }
  
  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product to price
   * @param rates  the rates provider
   * @param volatilities  the Black volatility parameters
   * @return the present value of the swaption product
   */
  public CurrencyAmount presentValue(
      Swaption swaption, 
      RatesProvider rates, 
      BlackVolatilitySwaptionProvider volatilities) {
    validate(rates, swaption, volatilities);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double expiry = volatilities.relativeYearFraction(expiryDateTime);
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if(expiry < 0.0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, rates);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, rates);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, rates, pvbp);
    double tenor = volatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilities.getVolatility(expiryDateTime, tenor, strike, forward);
    BlackFunctionData blackData = new BlackFunctionData(forward, Math.abs(pvbp), volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiry, isCall);
    // option required to pass the strike (in case the swap has non-constant coupon).
    Function1D<BlackFunctionData, Double> func = BLACK.getPriceFunction(option);
    double pv = func.evaluate(blackData) * ((swaption.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return CurrencyAmount.of(fixedLeg.getCurrency(), pv);
  }  

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption product.
   * 
   * @param swaption  the swaption to price
   * @param rates  the rates provider
   * @param volatilities  the Black volatility parameters
   * @return the present value of the swaption product
   */
  public MultiCurrencyAmount currencyExposure(
      Swaption swaption, 
      RatesProvider rates, 
      BlackVolatilitySwaptionProvider volatilities) {
    return MultiCurrencyAmount.of(presentValue(swaption, rates, volatilities));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied Normal volatility of the swaption.
   * 
   * @param swaption  the product to price
   * @param rates  the rates provider
   * @param volatilities  the Black volatility parameters
   * @return the Black implied volatility associated to the swaption
   */
  public double impliedVolatility(
      Swaption swaption, 
      RatesProvider rates, 
      BlackVolatilitySwaptionProvider volatilities) {
    validate(rates, swaption, volatilities);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double expiry = volatilities.relativeYearFraction(expiryDateTime);
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0.0d, "option should be before expiry to compute an implied volatility");
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
   * @param volatilities  the Black volatility parameters
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      Swaption swaption, 
      RatesProvider rates, 
      BlackVolatilitySwaptionProvider volatilities) {
    validate(rates, swaption, volatilities);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double expiry = volatilities.relativeYearFraction(expiryDateTime);
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if(expiry < 0.0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = swapPricer.parRate(underlying, rates);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, rates);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, rates, pvbp);
    double tenor = volatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilities.getVolatility(expiryDateTime, tenor, strike, forward);
    BlackFunctionData blackData = new BlackFunctionData(forward, 1.0d, volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = new EuropeanVanillaOption(strike, expiry, isCall);
    // option required to pass the strike (in case the swap has non-constant coupon).
    // Backward sweep
    PointSensitivityBuilder pvbpDr = swapPricer.getLegPricer().pvbpSensitivity(fixedLeg, rates);
    PointSensitivityBuilder forwardDr = swapPricer.parRateSensitivity(underlying, rates);
    double[] pvAd = BLACK.getPriceAdjoint(option, blackData);
    double sign =  (swaption.getLongShort() == LongShort.LONG) ? 1.0 : -1.0;
    return pvbpDr.multipliedBy(pvAd[0] * sign * Math.signum(pvbp))
        .combinedWith(forwardDr.multipliedBy(pvAd[1] * Math.abs(pvbp) * sign));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption product.
   * <p>
   * The sensitivity to the implied normal volatility is also called normal vega.
   * 
   * @param swaption  the swaption product
   * @param rates  the rates provider
   * @param volatilities  the Black volatility parameters
   * @return the point sensitivity to the normal volatility
   */
  public SwaptionSensitivity presentValueSensitivityBlackVolatility(
      Swaption swaption, 
      RatesProvider rates, 
      BlackVolatilitySwaptionProvider volatilities) {
    validate(rates, swaption, volatilities);
    ZonedDateTime expiryDateTime = swaption.getExpiryDateTime();
    double expiry = volatilities.relativeYearFraction(expiryDateTime);
    ExpandedSwap underlying = swaption.expand().getUnderlying().expand();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = volatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, rates);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, rates, pvbp);
    if(expiry < 0.0d) { // Option has expired already
      return SwaptionSensitivity.of(volatilities.getConvention(), expiryDateTime, tenor, strike, 0.0d, 
        fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, rates);
    double volatility = volatilities.getVolatility(expiryDateTime, tenor, strike, forward);
    // option required to pass the strike (in case the swap has non-constant coupon).
    // Backward sweep
    double vega = Math.abs(pvbp) * BlackFormulaRepository.vega(forward, strike, expiry, volatility) 
        * ((swaption.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return SwaptionSensitivity.of(volatilities.getConvention(), expiryDateTime, tenor, strike, forward, 
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
  private void validate(RatesProvider rates, Swaption swaption, BlackVolatilitySwaptionProvider volatility) {
    ArgChecker.isTrue(volatility.getValuationDate().equals(rates.getValuationDate()), 
        "volatility and rate data should be for the same date");
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "underlying swap should be single currency");
    ArgChecker.isFalse(swaption.isCashSettled(), "swaption should be physical settlement");
  }

}
