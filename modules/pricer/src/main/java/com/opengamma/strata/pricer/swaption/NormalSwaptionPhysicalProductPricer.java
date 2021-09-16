/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.impl.option.NormalFormulaRepository;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swaption.ResolvedSwaption;

/**
 * Pricer for swaption with physical settlement in a normal model on the swap rate.
 * <p>
 * The swap underlying the swaption must have a fixed leg on which the forward rate is computed.
 * The underlying swap must be single currency.
 * <p>
 * The volatility parameters are not adjusted for the underlying swap convention.
 * <p>
 * The value of the swaption after expiry is 0.
 * For a swaption which has already expired, a negative number is returned by
 * {@link SwaptionVolatilities#relativeTime(ZonedDateTime)}.
 */
public class NormalSwaptionPhysicalProductPricer
    extends VolatilitySwaptionPhysicalProductPricer {

  /**
   * Default implementation.
   */
  public static final NormalSwaptionPhysicalProductPricer DEFAULT =
      new NormalSwaptionPhysicalProductPricer(DiscountingSwapProductPricer.DEFAULT);

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link Swap}
   */
  public NormalSwaptionPhysicalProductPricer(DiscountingSwapProductPricer swapPricer) {
    super(swapPricer);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the implied normal volatility from the present value of a swaption.
   * <p>
   * The guess volatility for the start of the root-finding process is 1%.
   * 
   * @param swaption  the product
   * @param ratesProvider  the rates provider
   * @param dayCount  the day-count used to estimate the time between valuation date and swaption expiry
   * @param presentValue  the present value of the swaption product
   * @return the implied volatility associated with the present value
   */
  public double impliedVolatilityFromPresentValue(
      ResolvedSwaption swaption,
      RatesProvider ratesProvider,
      DayCount dayCount,
      double presentValue) {

    double sign = swaption.getLongShort().sign();
    ArgChecker.isTrue(presentValue * sign > 0, "Present value sign must be in line with the option Long/Short flag ");
    validateSwaption(swaption);
    LocalDate valuationDate = ratesProvider.getValuationDate();
    LocalDate expiryDate = swaption.getExpiryDate();
    ArgChecker.isTrue(expiryDate.isAfter(valuationDate),
        "Expiry must be after valuation date to compute an implied volatility");
    double expiry = dayCount.yearFraction(valuationDate, expiryDate);
    ResolvedSwap underlying = swaption.getUnderlying();
    ResolvedSwapLeg fixedLeg = fixedLeg(underlying);
    double forward = forwardRate(swaption, ratesProvider);
    double pvbp = getSwapPricer().getLegPricer().pvbp(fixedLeg, ratesProvider);
    double numeraire = Math.abs(pvbp);
    double strike = getSwapPricer().getLegPricer().couponEquivalent(fixedLeg, ratesProvider, pvbp);
    PutCall putCall = PutCall.ofPut(fixedLeg.getPayReceive().isReceive());
    return NormalFormulaRepository.impliedVolatility(
        Math.abs(presentValue), forward, strike, expiry, 0.01, numeraire, putCall);
  }

}
