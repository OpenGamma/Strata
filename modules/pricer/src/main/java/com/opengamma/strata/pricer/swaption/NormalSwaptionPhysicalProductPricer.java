/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;

import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.PayReceive;
import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.pricer.impl.option.EuropeanVanillaOption;
import com.opengamma.strata.pricer.impl.option.NormalFunctionData;
import com.opengamma.strata.pricer.impl.option.NormalImpliedVolatilityFormula;
import com.opengamma.strata.pricer.impl.option.NormalPriceFunction;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapProduct;
import com.opengamma.strata.product.swaption.ExpandedSwaption;
import com.opengamma.strata.product.swaption.SettlementType;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionProduct;

/**
 * Pricer for swaption with physical settlement in a normal model on the swap rate.
 * <p>
 * The swap underlying the swaption should have a fixed leg on which the forward rate is computed. The underlying swap
 * should be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap conventions. The volatilities from the provider
 * are taken as such.
 * <p>
 * The value of the swaption after expiry is 0. For a swaption which already expired, negative number is returned by 
 * the method, {@link NormalVolatilitySwaptionProvider#relativeTime(ZonedDateTime)}.
 */
public class NormalSwaptionPhysicalProductPricer {

  /**
   * Default implementation.
   */
  public static final NormalSwaptionPhysicalProductPricer DEFAULT = new NormalSwaptionPhysicalProductPricer(
      DiscountingSwapProductPricer.DEFAULT);
  /** 
   * Pricer for {@link SwapProduct}. 
   */
  private final DiscountingSwapProductPricer swapPricer;
  /** 
   * Normal model pricing function. 
   */
  public static final NormalPriceFunction NORMAL = new NormalPriceFunction();
  /** The formula to infer the normal implied volatility from a price/present value. */
  private static final NormalImpliedVolatilityFormula FORMULA_IMPLIED = NormalImpliedVolatilityFormula.DEFAULT;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public NormalSwaptionPhysicalProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swap pricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value of the swaption product
   */
  public CurrencyAmount presentValue(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0.0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    // option required to pass the strike (in case the swap has non-constant coupon).
    Function<NormalFunctionData, Double> func = NORMAL.getPriceFunction(option);
    double pv = func.apply(normalData) * ((expanded.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return CurrencyAmount.of(fixedLeg.getCurrency(), pv);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption product.
   * 
   * @param swaption  the swaption to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value of the swaption product
   */
  public MultiCurrencyAmount currencyExposure(
      Swaption swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, volatilityProvider));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied Normal volatility of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value of the swap product
   */
  public double impliedVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0.0d, "option should be before expiry to compute an implied volatility");
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
  }

  /**
   * Computes the implied normal volatility from the present value of a swaption.
   * <p>
   * The guess volatility for the start of the root-finding process is 1%.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param dayCount  the day-count used to estimate the time between valuation date and swaption expiry
   * @param presentValue  the present value of the swaption product
   * @return the implied volatility associated to the present value
   */
  public double impliedVolatilityFromPresentValue(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      DayCount dayCount,
      double presentValue) {

    ExpandedSwaption expanded = swaption.expand();
    double sign = expanded.getLongShort() == LongShort.LONG ? 1.0d : -1.0;
    ArgChecker.isTrue(presentValue * sign > 0, "present value sign must be in ligne with the option Long/Short flag ");
    validateSwaption(ratesProvider, expanded);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    LocalDate expiryDate = expanded.getExpiryDate();
    ArgChecker.isTrue(expiryDate.isAfter(valuationDate),
        "expiry should be after valuation date to compute an implied volatility");
    double expiry = dayCount.yearFraction(valuationDate, expiryDate);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), 0.01);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    return FORMULA_IMPLIED.impliedVolatility(normalData, option, Math.abs(presentValue));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the swaption product.
   * <p>
   * The present value delta is given by {@code pvbp * normalDelta} where {@code normalDelta} is the first derivative 
   * of normal price with respect to forward. 
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value delta of the swaption product
   */
  public CurrencyAmount presentValueDelta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0.0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    double delta = NORMAL.getDelta(option, normalData) * ((expanded.getLongShort() == LongShort.LONG) ? 1d : -1d);
    return CurrencyAmount.of(fixedLeg.getCurrency(), delta);
  }

  /**
   * Calculates the present value gamma of the swaption product.
   * <p>
   * The present value gamma is given by {@code pvbp * normalGamma} where {@code normalGamma} is the second derivative 
   * of normal price with respect to forward. 
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value gamma of the swaption product
   */
  public CurrencyAmount presentValueGamma(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0.0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    // option required to pass the strike (in case the swap has non-constant coupon).
    double gamma = NORMAL.getGamma(option, normalData) * ((expanded.getLongShort() == LongShort.LONG) ? 1d : -1d);
    return CurrencyAmount.of(fixedLeg.getCurrency(), gamma);
  }

  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The present value theta is given by {@code pvbp * normalTheta} where {@code normalTheta} is the minus of 
   * the normal price sensitivity to {@code timeToExpiry}. 
   * <p>
   * The result is expressed using the currency of the swapion.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value theta of the swaption product
   */
  public CurrencyAmount presentValueTheta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0.0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    // option required to pass the strike (in case the swap has non-constant coupon).
    double theta = NORMAL.getTheta(option, normalData) * ((expanded.getLongShort() == LongShort.LONG) ? 1d : -1d);
    return CurrencyAmount.of(fixedLeg.getCurrency(), theta);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product.
   * <p>
   * The present value sensitivity of the product is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0.0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, 1.0d, volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    // option required to pass the strike (in case the swap has non-constant coupon).
    // Backward sweep
    PointSensitivityBuilder pvbpDr = swapPricer.getLegPricer().pvbpSensitivity(fixedLeg, ratesProvider);
    PointSensitivityBuilder forwardDr = swapPricer.parRateSensitivity(underlying, ratesProvider);
    ValueDerivatives pv = NORMAL.getPriceAdjoint(option, normalData);
    double sign = (expanded.getLongShort() == LongShort.LONG) ? 1.0 : -1.0;
    return pvbpDr.multipliedBy(pv.getValue() * sign * Math.signum(pvbp))
        .combinedWith(forwardDr.multipliedBy(pv.getDerivative(0) * Math.abs(pvbp) * sign));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption product.
   * <p>
   * The sensitivity to the implied normal volatility is also called normal vega.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param volatilityProvider  the normal volatility provider
   * @return the point sensitivity to the normal volatility
   */
  public SwaptionSensitivity presentValueSensitivityNormalVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalVolatilitySwaptionProvider volatilityProvider) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, volatilityProvider);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = volatilityProvider.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = volatilityProvider.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    if (expiry < 0.0d) { // Option has expired already
      return SwaptionSensitivity.of(volatilityProvider.getConvention(), expiryDateTime, tenor, strike, 0.0d,
          fixedLeg.getCurrency(), 0.0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double volatility = volatilityProvider.getVolatility(expiryDateTime, tenor, strike, forward);
    NormalFunctionData normalData = NormalFunctionData.of(forward, Math.abs(pvbp), volatility);
    boolean isCall = (fixedLeg.getPayReceive() == PayReceive.PAY);
    // Payer at strike is exercise when rate > strike, i.e. call on rate
    EuropeanVanillaOption option = EuropeanVanillaOption.of(strike, expiry, isCall ? PutCall.CALL : PutCall.PUT);
    // option required to pass the strike (in case the swap has non-constant coupon).
    // Backward sweep
    double vega = NORMAL.getVega(option, normalData) * ((expanded.getLongShort() == LongShort.LONG) ? 1.0 : -1.0);
    return SwaptionSensitivity.of(volatilityProvider.getConvention(), expiryDateTime, tenor, strike, forward,
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
  private void validate(RatesProvider ratesProvider, ExpandedSwaption swaption,
      NormalVolatilitySwaptionProvider volatilityProvider) {
    ArgChecker.isTrue(volatilityProvider.getValuationDateTime().toLocalDate().equals(ratesProvider.getValuationDate()),
        "volatility and rate data should be for the same date");
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "underlying swap should be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.PHYSICAL),
        "swaption should be physical settlement");
  }

  // validate that the rates and volatilities providers are coherent
  private void validateSwaption(RatesProvider ratesProvider, ExpandedSwaption swaption) {
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "underlying swap should be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.PHYSICAL),
        "swaption should be physical settlement");
  }

}
