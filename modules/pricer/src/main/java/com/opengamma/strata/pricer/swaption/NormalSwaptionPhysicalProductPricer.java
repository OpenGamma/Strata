/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapProduct;
import com.opengamma.strata.product.swaption.ExpandedSwaption;
import com.opengamma.strata.product.swaption.SettlementType;
import com.opengamma.strata.product.swaption.SwaptionProduct;

/**
 * Pricer for swaption with physical settlement in a normal model on the swap rate.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * The volatilities from the provider are taken as such.
 * <p>
 * The value of the swaption after expiry is 0. For a swaption which already expired, negative number is returned by 
 * the method, {@link NormalSwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class NormalSwaptionPhysicalProductPricer {

  /**
   * Default implementation.
   */
  public static final NormalSwaptionPhysicalProductPricer DEFAULT =
      new NormalSwaptionPhysicalProductPricer(DiscountingSwapProductPricer.DEFAULT);
  /** 
   * Pricer for {@link SwapProduct}. 
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public NormalSwaptionPhysicalProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption product
   */
  public CurrencyAmount presentValue(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double numeraire = Math.abs(pvbp);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double price = numeraire * NormalFormulaRepository.price(forward, strike, expiry, volatility, putCall);
    return CurrencyAmount.of(fixedLeg.getCurrency(), price * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption product.
   * 
   * @param swaption  the swaption to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value of the swaption product
   */
  public MultiCurrencyAmount currencyExposure(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, swaptionVolatilities));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied Normal volatility of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the implied volatility associated to the swaption
   */
  public double impliedVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0d, "Option must be before expiry to compute an implied volatility");
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
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
    double sign = expanded.getLongShort().sign();
    ArgChecker.isTrue(presentValue * sign > 0, "Present value sign must be in line with the option Long/Short flag ");
    validateSwaption(ratesProvider, expanded);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    LocalDate expiryDate = expanded.getExpiryDate();
    ArgChecker.isTrue(expiryDate.isAfter(valuationDate),
        "Expiry must be after valuation date to compute an implied volatility");
    double expiry = dayCount.yearFraction(valuationDate, expiryDate);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double numeraire = Math.abs(pvbp);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    return NormalFormulaRepository.impliedVolatility(
        Math.abs(presentValue), forward, strike, expiry, 0.01, numeraire, putCall);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the swaption product.
   * <p>
   * The present value delta is given by {@code pvbp * normalDelta} where {@code normalDelta} is the first derivative 
   * of normal price with respect to forward.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value delta of the swaption product
   */
  public CurrencyAmount presentValueDelta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double numeraire = Math.abs(pvbp);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double delta = numeraire * NormalFormulaRepository.delta(forward, strike, expiry, volatility, putCall);
    return CurrencyAmount.of(fixedLeg.getCurrency(), delta * expanded.getLongShort().sign());
  }

  /**
   * Calculates the present value gamma of the swaption product.
   * <p>
   * The present value gamma is given by {@code pvbp * normalGamma} where {@code normalGamma} is the second derivative 
   * of normal price with respect to forward.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value gamma of the swaption product
   */
  public CurrencyAmount presentValueGamma(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double numeraire = Math.abs(pvbp);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double gamma = numeraire * NormalFormulaRepository.gamma(forward, strike, expiry, volatility, putCall);
    return CurrencyAmount.of(fixedLeg.getCurrency(), gamma * expanded.getLongShort().sign());
  }

  /**
   * Calculates the present value of the swaption product.
   * <p>
   * The present value theta is given by {@code pvbp * normalTheta} where {@code normalTheta} is the minus of the
   * normal price sensitivity to {@code timeToExpiry}.
   * <p>
   * The result is expressed using the currency of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value theta of the swaption product
   */
  public CurrencyAmount presentValueTheta(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double numeraire = Math.abs(pvbp);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double theta = numeraire * NormalFormulaRepository.theta(forward, strike, expiry, volatility, putCall);
    return CurrencyAmount.of(fixedLeg.getCurrency(), theta * expanded.getLongShort().sign());
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
   * @param swaptionVolatilities  the volatilities
   * @return the present value curve sensitivity of the swap product
   */
  public PointSensitivityBuilder presentValueSensitivityStickyStrike(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    ValueDerivatives priceAdj = NormalFormulaRepository.priceAdjoint(forward, strike, expiry, volatility, 1d, putCall);
    double price = priceAdj.getValue();
    double delta = priceAdj.getDerivative(0);
    // Backward sweep
    PointSensitivityBuilder pvbpDr = swapPricer.getLegPricer().pvbpSensitivity(fixedLeg, ratesProvider);
    PointSensitivityBuilder forwardDr = swapPricer.parRateSensitivity(underlying, ratesProvider);
    double sign = expanded.getLongShort().sign();
    return pvbpDr.multipliedBy(price * sign * Math.signum(pvbp))
        .combinedWith(forwardDr.multipliedBy(delta * Math.abs(pvbp) * sign));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption product.
   * <p>
   * The sensitivity to the implied normal volatility is also called normal vega.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the volatility
   */
  public SwaptionSensitivity presentValueSensitivityNormalVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double pvbp = swapPricer.getLegPricer().pvbp(fixedLeg, ratesProvider);
    double strike = calculateStrike(fixedLeg, ratesProvider, pvbp);
    if (expiry < 0d) { // Option has expired already
      return SwaptionSensitivity.of(
          swaptionVolatilities.getConvention(), expiryDateTime, tenor, strike, 0d, fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double numeraire = Math.abs(pvbp);
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double vega = numeraire * NormalFormulaRepository.vega(forward, strike, expiry, volatility, putCall);
    return SwaptionSensitivity.of(
        swaptionVolatilities.getConvention(),
        expiryDateTime,
        tenor,
        strike,
        forward,
        fixedLeg.getCurrency(),
        vega * expanded.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  // calculates the strike
  private double calculateStrike(ExpandedSwapLeg fixedLeg, RatesProvider ratesProvider, double pvbp) {
    return swapPricer.getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
  }

  // check that one leg is fixed and return it
  private ExpandedSwapLeg fixedLeg(ExpandedSwap swap) {
    ArgChecker.isFalse(swap.isCrossCurrency(), "Swap must be single currency");
    // find fixed leg
    List<ExpandedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    return fixedLegs.get(0);
  }

  // validate that the rates and volatilities providers are coherent
  private void validate(
      RatesProvider ratesProvider,
      ExpandedSwaption swaption,
      NormalSwaptionVolatilities swaptionVolatilities) {

    ArgChecker.isTrue(swaptionVolatilities.getValuationDateTime().toLocalDate().equals(ratesProvider.getValuationDate()),
        "Volatility and rate data must be for the same date");
    validateSwaption(ratesProvider, swaption);
  }

  // validate that the rates and volatilities providers are coherent
  private void validateSwaption(RatesProvider ratesProvider, ExpandedSwaption swaption) {
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "Underlying swap must be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.PHYSICAL),
        "Swaption must be physical settlement");
  }

}
