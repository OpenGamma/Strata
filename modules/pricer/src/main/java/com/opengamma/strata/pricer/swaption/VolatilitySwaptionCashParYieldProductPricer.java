/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.common.SettlementType;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.RateComputation;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapPaymentPeriod;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.CashSwaptionSettlementMethod;
import com.opengamma.strata.product.swaption.ResolvedSwaption;

/**
 * Pricer for swaption with par yield curve method of cash settlement based on volatilities.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * <p>
 * The value of the swaption after expiry is 0.
 * For a swaption which already expired, negative number is returned by
 * {@link SwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class VolatilitySwaptionCashParYieldProductPricer {

  /**
   * Default implementation.
   */
  public static final VolatilitySwaptionCashParYieldProductPricer DEFAULT =
      new VolatilitySwaptionCashParYieldProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedSwap}.
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   *
   * @param swapPricer  the pricer for {@link Swap}
   */
  public VolatilitySwaptionCashParYieldProductPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the swap pricer.
   *
   * @return the swap pricer
   */
  protected DiscountingSwapProductPricer getSwapPricer() {
    return swapPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption.
   * <p>
   * The result is expressed using the currency of the swaption.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = forwardRate(swaption, ratesProvider);
    double numeraire = calculateNumeraire(swaption, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double price = numeraire * swaptionVolatilities.price(expiry, tenor, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), price * swaption.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the currency exposure of the swaption.
   * <p>
   * This is equivalent to the present value of the swaption.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, swaptionVolatilities));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied volatility of the swaption.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the implied volatility associated with the swaption
   */
  public double impliedVolatility(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0d, "Option must be before expiry to compute an implied volatility");
    double forward = forwardRate(swaption, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return swaptionVolatilities.volatility(expiry, tenor, strike, forward);
  }

  //-------------------------------------------------------------------------
  /**
   * Provides the forward rate.
   * <p>
   * This is the par rate for the forward starting swap that is the underlying of the swaption.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @return the forward rate
   */
  public double forwardRate(ResolvedSwaption swaption, RatesProvider ratesProvider) {
    return swapPricer.parRate(swaption.getUnderlying(), ratesProvider);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the swaption.
   * <p>
   * The present value delta is given by {@code pvbp * priceDelta} where {@code priceDelta}
   * is the first derivative of the price with respect to forward.
   * <p>
   * The result is expressed using the currency of the swaption.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value delta of the swaption
   */
  public CurrencyAmount presentValueDelta(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = forwardRate(swaption, ratesProvider);
    double numeraire = calculateNumeraire(swaption, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double delta = numeraire * swaptionVolatilities.priceDelta(expiry, tenor, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), delta * swaption.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value gamma of the swaption.
   * <p>
   * The present value gamma is given by {@code pvbp * priceGamma} where {@code priceGamma}
   * is the second derivative of the price with respect to forward.
   * <p>
   * The result is expressed using the currency of the swaption.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value gamma of the swaption
   */
  public CurrencyAmount presentValueGamma(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = forwardRate(swaption, ratesProvider);
    double numeraire = calculateNumeraire(swaption, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double gamma = numeraire * swaptionVolatilities.priceGamma(expiry, tenor, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), gamma * swaption.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the swaption.
   * <p>
   * The present value theta is given by {@code pvbp * priceTheta} where {@code priceTheta}
   * is the minus of the price sensitivity to {@code timeToExpiry}.
   * <p>
   * The result is expressed using the currency of the swaption.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the present value theta of the swaption
   */
  public CurrencyAmount presentValueTheta(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return CurrencyAmount.of(fixedLeg.getCurrency(), 0d);
    }
    double forward = forwardRate(swaption, ratesProvider);
    double numeraire = calculateNumeraire(swaption, fixedLeg, forward, ratesProvider);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double theta = numeraire * swaptionVolatilities.priceTheta(expiry, tenor, putCall, strike, forward, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), theta * swaption.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky strike" style, i.e. the sensitivity to the
   * curve nodes with the volatility at the swaption strike unchanged. This sensitivity does not include a potential
   * change of volatility due to the implicit change of forward rate or moneyness.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyStrike(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    if (expiry < 0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = forwardRate(swaption, ratesProvider);
    ValueDerivatives annuityDerivative = getSwapPricer().getLegPricer().annuityCashDerivative(fixedLeg, forward);
    double annuityCash = annuityDerivative.getValue();
    double annuityCashDr = annuityDerivative.getDerivative(0);
    LocalDate settlementDate = ((CashSwaptionSettlement) swaption.getSwaptionSettlement()).getSettlementDate();
    double discountSettle = ratesProvider.discountFactor(fixedLeg.getCurrency(), settlementDate);
    double strike = calculateStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double price = swaptionVolatilities.price(expiry, tenor, putCall, strike, forward, volatility);
    double delta = swaptionVolatilities.priceDelta(expiry, tenor, putCall, strike, forward, volatility);
    // Backward sweep
    PointSensitivityBuilder forwardSensi = getSwapPricer().parRateSensitivity(underlying, ratesProvider);
    PointSensitivityBuilder discountSettleSensi =
        ratesProvider.discountFactors(fixedLeg.getCurrency()).zeroRatePointSensitivity(settlementDate);
    double sign = swaption.getLongShort().sign();
    return forwardSensi.multipliedBy(sign * discountSettle * (annuityCash * delta + annuityCashDr * price))
        .combinedWith(discountSettleSensi.multipliedBy(sign * annuityCash * price));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption.
   * <p>
   * The sensitivity to the implied volatility is also called vega.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the volatility
   */
  public SwaptionSensitivity presentValueSensitivityModelParamsVolatility(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double strike = calculateStrike(fixedLeg);
    if (expiry < 0d) { // Option has expired already
      return SwaptionSensitivity.of(
          swaptionVolatilities.getName(), expiry, tenor, strike, 0d, fixedLeg.getCurrency(), 0d);
    }
    double forward = forwardRate(swaption, ratesProvider);
    double numeraire = calculateNumeraire(swaption, fixedLeg, forward, ratesProvider);
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    double vega = numeraire * swaptionVolatilities.priceVega(expiry, tenor, putCall, strike, forward, volatility);
    return SwaptionSensitivity.of(
        swaptionVolatilities.getName(),
        expiry,
        tenor,
        strike,
        forward,
        fixedLeg.getCurrency(),
        vega * swaption.getLongShort().sign());
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the numeraire, used to multiply the results.
   *
   * @param swaption  the swap
   * @param fixedLeg  the fixed leg
   * @param forward  the forward rate
   * @param ratesProvider  the rates provider
   * @return the numeraire
   */
  protected double calculateNumeraire(
      ResolvedSwaption swaption,
      ResolvedSwapLeg fixedLeg,
      double forward,
      RatesProvider ratesProvider) {

    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    CashSwaptionSettlement cashSettlement = (CashSwaptionSettlement) swaption.getSwaptionSettlement();
    double discountSettle = ratesProvider.discountFactor(fixedLeg.getCurrency(), cashSettlement.getSettlementDate());
    return Math.abs(annuityCash * discountSettle);
  }

  /**
   * Calculates the strike.
   *
   * @param fixedLeg  the fixed leg
   * @return the strike
   */
  protected double calculateStrike(ResolvedSwapLeg fixedLeg) {
    SwapPaymentPeriod paymentPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "Payment period must be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    // compounding is caught when par rate is computed
    RateComputation rateComputation = ratePaymentPeriod.getAccrualPeriods().get(0).getRateComputation();
    ArgChecker.isTrue(rateComputation instanceof FixedRateComputation, "Swap leg must be fixed leg");
    return ((FixedRateComputation) rateComputation).getRate();
  }

  /**
   * Checks that there is exactly one fixed leg and returns it.
   *
   * @param swap  the swap
   * @return the fixed leg
   */
  protected ResolvedSwapLeg fixedLeg(ResolvedSwap swap) {
    ArgChecker.isFalse(swap.isCrossCurrency(), "Swap must be single currency");
    // find fixed leg
    List<ResolvedSwapLeg> fixedLegs = swap.getLegs(SwapLegType.FIXED);
    if (fixedLegs.isEmpty()) {
      throw new IllegalArgumentException("Swap must contain a fixed leg");
    }
    return fixedLegs.get(0);
  }

  /**
   * Validates that the rates and volatilities providers are coherent
   * and that the swaption is single currency cash par-yield.
   *
   * @param swaption  the swaption
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   */
  protected void validate(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SwaptionVolatilities swaptionVolatilities) {

    ArgChecker.isTrue(swaptionVolatilities.getValuationDate().equals(ratesProvider.getValuationDate()),
        "Volatility and rate data must be for the same date");
    validateSwaption(swaption);
  }

  /**
   * Validates that the swaption is single currency cash par-yield.
   *
   * @param swaption  the swaption
   */
  protected void validateSwaption(ResolvedSwaption swaption) {
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "Underlying swap must be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH),
        "Swaption must be cash settlement");
    CashSwaptionSettlement cashSettle = (CashSwaptionSettlement) swaption.getSwaptionSettlement();
    ArgChecker.isTrue(cashSettle.getMethod().equals(CashSwaptionSettlementMethod.PAR_YIELD),
        "Cash settlement method must be par yield");
  }

}
