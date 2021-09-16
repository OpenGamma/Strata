/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.market.model.SabrParameterType.ALPHA;
import static com.opengamma.strata.market.model.SabrParameterType.BETA;
import static com.opengamma.strata.market.model.SabrParameterType.NU;
import static com.opengamma.strata.market.model.SabrParameterType.RHO;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.CashSwaptionSettlement;
import com.opengamma.strata.product.swaption.ResolvedSwaption;

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
public class SabrSwaptionCashParYieldProductPricer
    extends VolatilitySwaptionCashParYieldProductPricer {

  /**
   * Default implementation.
   */
  public static final SabrSwaptionCashParYieldProductPricer DEFAULT =
      new SabrSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public SabrSwaptionCashParYieldProductPricer(DiscountingSwapProductPricer swapPricer) {
    super(swapPricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the swaption product to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the 
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential 
   * re-calibration of the model parameters to the raw market data.
   * 
   * @param swaption  the swaption product
   * @param ratesProvider  the rates provider
   * @param swaptionVolatilities  the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyModel(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    ZonedDateTime expiryDateTime = swaption.getExpiry();
    double expiry = swaptionVolatilities.relativeTime(expiryDateTime);
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
    double shift = swaptionVolatilities.shift(expiry, tenor);
    ValueDerivatives volatilityAdj = swaptionVolatilities.volatilityAdjoint(expiry, tenor, strike, forward);
    boolean isCall = fixedLeg.getPayReceive().isPay();
    double shiftedForward = forward + shift;
    double shiftedStrike = strike + shift;
    double price = BlackFormulaRepository.price(shiftedForward, shiftedStrike, expiry, volatilityAdj.getValue(), isCall);
    double delta = BlackFormulaRepository.delta(shiftedForward, shiftedStrike, expiry, volatilityAdj.getValue(), isCall);
    double vega = BlackFormulaRepository.vega(shiftedForward, shiftedStrike, expiry, volatilityAdj.getValue());
    PointSensitivityBuilder forwardSensi = getSwapPricer().parRateSensitivity(underlying, ratesProvider);
    PointSensitivityBuilder discountSettleSensi =
        ratesProvider.discountFactors(fixedLeg.getCurrency()).zeroRatePointSensitivity(settlementDate);
    double sign = swaption.getLongShort().sign();
    return forwardSensi.multipliedBy(
        sign * discountSettle * (annuityCash * (delta + vega * volatilityAdj.getDerivative(0)) + annuityCashDr * price))
        .combinedWith(discountSettleSensi.multipliedBy(sign * annuityCash * price));
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
   * @return the point sensitivity to the SABR model parameters
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      SabrSwaptionVolatilities swaptionVolatilities) {

    validate(swaption, ratesProvider, swaptionVolatilities);
    double expiry = swaptionVolatilities.relativeTime(swaption.getExpiry());
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    double tenor = swaptionVolatilities.tenor(fixedLeg.getStartDate(), fixedLeg.getEndDate());
    double shift = swaptionVolatilities.shift(expiry, tenor);
    double strike = calculateStrike(fixedLeg);
    if (expiry < 0d) { // Option has expired already
      return PointSensitivityBuilder.none();
    }
    double forward = forwardRate(swaption, ratesProvider);
    double volatility = swaptionVolatilities.volatility(expiry, tenor, strike, forward);
    double numeraire = calculateNumeraire(swaption, fixedLeg, forward, ratesProvider);
    DoubleArray derivative = swaptionVolatilities.volatilityAdjoint(expiry, tenor, strike, forward).getDerivatives();
    double vega = numeraire * swaption.getLongShort().sign() *
        BlackFormulaRepository.vega(forward + shift, strike + shift, expiry, volatility);
    // sensitivities
    Currency ccy = fixedLeg.getCurrency();
    SwaptionVolatilitiesName name = swaptionVolatilities.getName();
    return PointSensitivityBuilder.of(
        SwaptionSabrSensitivity.of(name, expiry, tenor, ALPHA, ccy, vega * derivative.get(2)),
        SwaptionSabrSensitivity.of(name, expiry, tenor, BETA, ccy, vega * derivative.get(3)),
        SwaptionSabrSensitivity.of(name, expiry, tenor, RHO, ccy, vega * derivative.get(4)),
        SwaptionSabrSensitivity.of(name, expiry, tenor, NU, ccy, vega * derivative.get(5)));
  }

}
