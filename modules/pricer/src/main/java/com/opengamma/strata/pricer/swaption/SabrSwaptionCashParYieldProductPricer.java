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
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
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
 * Pricer for swaption with par yield curve method of cash settlement in SABR model.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * The volatilities from the provider are taken as such.
 * <p>
 * The value of the swaption after expiry is 0. For a swaption which already expired, negative number is returned by 
 * the method, {@link SabrSwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class SabrSwaptionCashParYieldProductPricer {

  /**
   * Default implementation.
   */
  public static final SabrSwaptionCashParYieldProductPricer DEFAULT =
      new SabrSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer.DEFAULT);
  /** 
   * Pricer for {@link SwapProduct}. 
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public SabrSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer swapPricer) {
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
      SabrSwaptionVolatilities swaptionVolatilities) {

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
    double shift = swaptionVolatilities.getParameters().getShift(DoublesPair.of(expiry, tenor));
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    double price = annuityCash * discountSettle *
        BlackFormulaRepository.price(forward + shift, strike + shift, expiry, volatility, isCall);
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
      SabrSwaptionVolatilities swaptionVolatilities) {

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
      SabrSwaptionVolatilities swaptionVolatilities) {

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
  public PointSensitivityBuilder presentValueSensitivity(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

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
    double shift = swaptionVolatilities.getParameters().getShift(DoublesPair.of(expiry, tenor));
    ValueDerivatives volatilityAdj = swaptionVolatilities.getParameters().getVolatilityAdjoint(expiry, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    double shiftedForward = forward + shift;
    double shiftedStrike = strike + shift;
    double price = BlackFormulaRepository
        .price(shiftedForward, shiftedStrike, expiry, volatilityAdj.getValue(), isCall);
    double delta = BlackFormulaRepository
        .delta(shiftedForward, shiftedStrike, expiry, volatilityAdj.getValue(), isCall);
    double vega = BlackFormulaRepository.vega(shiftedForward, shiftedStrike, expiry, volatilityAdj.getValue());
    PointSensitivityBuilder forwardSensi = swapPricer.parRateSensitivity(underlying, ratesProvider);
    PointSensitivityBuilder discountSettleSensi =
        ratesProvider.discountFactors(fixedLeg.getCurrency()).zeroRatePointSensitivity(settlementDate);
    double sign = expanded.getLongShort().sign();
    return forwardSensi.multipliedBy(
        sign * discountSettle * (annuityCash * (delta + vega * volatilityAdj.getDerivative(0))
            + annuityCashDr * price)).combinedWith(discountSettleSensi.multipliedBy(sign * annuityCash * price));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the SABR model parameters of the swaption product.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the Black volatility
   */
  public SwaptionSabrSensitivity presentValueSabrParameterSensitivity(
      SwaptionProduct swaption,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    ExpandedSwaption expanded = swaption.expand();
    validate(ratesProvider, expanded, swaptionVolatilities);
    ZonedDateTime expiryDateTime = expanded.getExpiryDateTime();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
    ExpandedSwap underlying = expanded.getUnderlying();
    ExpandedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double shift = swaptionVolatilities.getParameters().getShift(DoublesPair.of(expiry, tenor));
    double strike = getStrike(fixedLeg);
    if (expiry < 0d) { // Option has expired already
      return SwaptionSabrSensitivity.of(
          swaptionVolatilities.getConvention(), expiryDateTime, tenor, strike, 0d, fixedLeg.getCurrency(), 0d, 0d, 0d, 0d);
    }
    double forward = swapPricer.parRate(underlying, ratesProvider);
    double volatility = swaptionVolatilities.volatility(expiryDateTime, tenor, strike, forward);
    double annuityCash = swapPricer.getLegPricer().annuityCash(fixedLeg, forward);
    double discountSettle = ratesProvider.discountFactor(
        fixedLeg.getCurrency(), ((CashSettlement) expanded.getSwaptionSettlement()).getSettlementDate());
    DoubleArray derivative =
        swaptionVolatilities.getParameters().getVolatilityAdjoint(expiry, tenor, strike, forward).getDerivatives();
    double vega = annuityCash * discountSettle * expanded.getLongShort().sign() *
        BlackFormulaRepository.vega(forward + shift, strike + shift, expiry, volatility);
    return SwaptionSabrSensitivity.of(
        swaptionVolatilities.getConvention(),
        expiryDateTime,
        tenor,
        strike,
        forward,
        fixedLeg.getCurrency(),
        vega * derivative.get(2),
        vega * derivative.get(3),
        vega * derivative.get(4),
        vega * derivative.get(5));
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
      SabrSwaptionVolatilities swaptionVolatilities) {

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
