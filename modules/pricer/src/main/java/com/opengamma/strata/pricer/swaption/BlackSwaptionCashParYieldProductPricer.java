/**
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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.rate.FixedRateObservation;
import com.opengamma.strata.product.rate.RateObservation;
import com.opengamma.strata.product.swap.ExpandedSwap;
import com.opengamma.strata.product.swap.ExpandedSwapLeg;
import com.opengamma.strata.product.swap.PaymentPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.SwapProduct;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.ExpandedSwaption;
import com.opengamma.strata.product.swaption.SettlementType;
import com.opengamma.strata.product.swaption.SwaptionProduct;

/**
 * Pricer for swaption with par yield curve method of cash settlement in a log-normal or Black model on the swap rate.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed. The underlying swap
 * must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * The volatilities from the provider are taken as such.
 * <p>
 * The value of the swaption after expiry is 0. For a swaption which already expired, negative number is returned by 
 * the method, {@link BlackSwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class BlackSwaptionCashParYieldProductPricer {

  /**
   * Default implementation.
   */
  public static final BlackSwaptionCashParYieldProductPricer DEFAULT =
      new BlackSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /** 
   * Pricer for {@link SwapProduct}. 
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public BlackSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer swapPricer) {
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
      BlackSwaptionVolatilities swaptionVolatilities) {

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
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    double price = annuityCash * discountSettle * BlackFormulaRepository.price(forward, strike, expiry, volatility, isCall);
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
      BlackSwaptionVolatilities swaptionVolatilities) {

    return MultiCurrencyAmount.of(presentValue(swaption, ratesProvider, swaptionVolatilities));
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied Black volatility of the swaption.
   * 
   * @param swaption  the product to price
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the Black implied volatility associated to the swaption
   */
  public double impliedVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      BlackSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    ArgChecker.isTrue(expiry >= 0d, "Option must be before expiry to compute an implied volatility");
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    return swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value delta of the swaption product.
   * <p>
   * The present value delta is given by {@code discountFactor * annuityCash * blackDelta} where {@code blackDelta} 
   * is the first derivative of Black price with respect to forward. 
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
      BlackSwaptionVolatilities swaptionVolatilities) {

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
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    double delta = annuityCash * discountSettle * BlackFormulaRepository.delta(forward, strike, expiry, volatility, isCall);
    return CurrencyAmount.of(fixedLeg.getCurrency(), delta * expanded.getLongShort().sign());
  }

  /**
   * Calculates the present value gamma of the swaption product.
   * <p>
   * The present value gamma is given by {@code discountFactor * annuityCash * blackGamma} where {@code blackGamma}
   * is the second derivative of Black price with respect to forward. 
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
      BlackSwaptionVolatilities swaptionVolatilities) {

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
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    double gamma = annuityCash * discountSettle * BlackFormulaRepository.gamma(forward, strike, expiry, volatility);
    return CurrencyAmount.of(fixedLeg.getCurrency(), gamma * expanded.getLongShort().sign());
  }

  /**
   * Calculates the present value theta of the swaption product.
   * <p>
   * The present value theta is given by {@code discountFactor * annuityCash * blackTheta} where {@code blackTheta} 
   * is the minus of the Black price sensitivity to {@code timeToExpiry}. 
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
      BlackSwaptionVolatilities swaptionVolatilities) {

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
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    double theta = annuityCash * discountSettle
        * BlackFormulaRepository.driftlessTheta(forward, strike, expiry, volatility);
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
      BlackSwaptionVolatilities swaptionVolatilities) {

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
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double annuityCashDr = swapPricer.getLegPricer().annuityCashDerivative(fixedLeg, forward);
    LocalDate settlementDate = ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate();
    double discountSettle = ratesProvider.discountFactor(fixedLeg.getCurrency(), settlementDate);
    double strike = getStrike(fixedLeg);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    double price = BlackFormulaRepository.price(forward, strike, expiry, volatility, isCall);
    double delta = BlackFormulaRepository.delta(forward, strike, expiry, volatility, isCall);
    PointSensitivityBuilder forwardSensi = swapPricer.parRateSensitivity(underlying, ratesProvider);
    PointSensitivityBuilder discountSettleSensi =
        ratesProvider.discountFactors(fixedLeg.getCurrency()).zeroRatePointSensitivity(settlementDate);
    double sign = expanded.getLongShort().sign();
    return forwardSensi.multipliedBy(sign * discountSettle * (annuityCash * delta + annuityCashDr * price))
        .combinedWith(discountSettleSensi.multipliedBy(sign * annuityCash * price));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the implied volatility of the swaption product.
   * <p>
   * The sensitivity to the Black volatility is also called Black vega.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the Black volatility
   */
  public SwaptionSensitivity presentValueSensitivityBlackVolatility(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      BlackSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double strike = getStrike(fixedLeg);
    if (expiry < 0d) { // Option has expired already
      return SwaptionSensitivity.of(
          swaptionVolatilities.getConvention(), expiryDateTime, tenor, strike, 0d, fixedLeg.getCurrency(), 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    double vegaUnsigned = annuityCash * discountSettle * BlackFormulaRepository.vega(forward, strike, expiry, volatility);
    double vega = vegaUnsigned * expanded.getLongShort().sign();
    return SwaptionSensitivity.of(
        swaptionVolatilities.getConvention(), expiryDateTime, tenor, strike, forward, fixedLeg.getCurrency(), vega);
  }

  //-------------------------------------------------------------------------
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

  // get fixed rate 
  private double getStrike(ExpandedSwapLeg fixedLeg) {
    PaymentPeriod paymentPeriod = fixedLeg.getPaymentPeriods().get(0);
    ArgChecker.isTrue(paymentPeriod instanceof RatePaymentPeriod, "Payment period must be RatePaymentPeriod");
    RatePaymentPeriod ratePaymentPeriod = (RatePaymentPeriod) paymentPeriod;
    // compounding is caught when par rate is computed
    RateObservation rateObservation = ratePaymentPeriod.getAccrualPeriods().get(0).getRateObservation();
    ArgChecker.isTrue(rateObservation instanceof FixedRateObservation, "Swap leg must be fixed leg");
    return ((FixedRateObservation) rateObservation).getRate();
  }

  // validate that the rates and volatilities providers are coherent
  private void validate(
      RatesProvider ratesProvider,
      ExpandedSwaption swaption,
      BlackSwaptionVolatilities swaptionVolatilities) {

    ArgChecker.isTrue(swaptionVolatilities.getValuationDateTime().toLocalDate().equals(ratesProvider.getValuationDate()),
        "Volatility and rate data must be for the same date");
    ArgChecker.isFalse(swaption.getUnderlying().isCrossCurrency(), "Underlying swap must be single currency");
    ArgChecker.isTrue(swaption.getSwaptionSettlement().getSettlementType().equals(SettlementType.CASH),
        "Swaption must be cash settlement");
    CashSettlement cashSettle = (CashSettlement) swaption.getSwaptionSettlement();
    ArgChecker.isTrue(cashSettle.getCashSettlementMethod().equals(CashSettlementMethod.PAR_YIELD),
        "Cash settlement method must be par yield");
  }

}
