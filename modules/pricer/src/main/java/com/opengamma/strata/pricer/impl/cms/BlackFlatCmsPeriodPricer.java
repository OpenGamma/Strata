/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.cms;

import java.time.LocalDate;
import java.util.OptionalDouble;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilities;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.CmsPeriodType;
import com.opengamma.strata.product.swap.RatePaymentPeriod;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapLegType;

/**
 *  Computes the price of a CMS coupon in a constant log-normal volatility set-up.
 *  <p>
 *  Reference: Brotherton-Ratcliffe, R. and Iben, B. (1997). Advanced Strategies in financial Risk Management, 
 *    Chapter Yield Curve Application of Swap Products. New York Institute of Finance.
 *  OpenGamma implementation note: Pricing of CMS by replication and other approaches, Version 2.1, May 2016.
 */
public final class BlackFlatCmsPeriodPricer {

  /**
   * Pricer for the underlying swap.
   */
  private final DiscountingSwapProductPricer swapPricer;

  /* Small parameter below which a value is regarded as 0. */
  static final double EPS = 1.0E-4;

  /**
   * Obtains the pricer.
   * 
   * @param swapPricer  the pricer for underlying swap
   * @return the pricer
   */
  public static BlackFlatCmsPeriodPricer of(DiscountingSwapProductPricer swapPricer) {
    return new BlackFlatCmsPeriodPricer(swapPricer);
  }

  private BlackFlatCmsPeriodPricer(DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value by replication in SABR framework with extrapolation on the right.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @param swaptionVolatilities  the swaption volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      CmsPeriod cmsPeriod,
      RatesProvider provider,
      SwaptionVolatilities swaptionVolatilities) {

    Currency ccy = cmsPeriod.getCurrency();
    LocalDate valuationDate = provider.getValuationDate();
    if (valuationDate.isAfter(cmsPeriod.getPaymentDate())) {
      return CurrencyAmount.zero(ccy);
    }
    LocalDate fixingDate = cmsPeriod.getFixingDate();
    double dfPayment = provider.discountFactor(ccy, cmsPeriod.getPaymentDate());
    if (!fixingDate.isAfter(valuationDate)) { // Using fixing
      OptionalDouble fixedRate = provider.timeSeries(cmsPeriod.getIndex()).get(fixingDate);
      if (fixedRate.isPresent()) {
        double payoff = 0d;
        switch (cmsPeriod.getCmsPeriodType()) {
          case CAPLET:
            payoff = Math.max(fixedRate.getAsDouble() - cmsPeriod.getStrike(), 0d);
            break;
          case FLOORLET:
            payoff = Math.max(cmsPeriod.getStrike() - fixedRate.getAsDouble(), 0d);
            break;
          case COUPON:
            payoff = fixedRate.getAsDouble();
            break;
          default:
            throw new IllegalArgumentException("unsupported CMS type");
        }
        return CurrencyAmount.of(ccy, payoff * dfPayment * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
      } else if (fixingDate.isBefore(valuationDate)) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    if (!cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON)) {
      throw new IllegalArgumentException("Unable to price cap or floor in this pricer");
    }
    // Using forward
    SwapIndex index = cmsPeriod.getIndex();
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    ResolvedSwapLeg fixedLeg = swap.getLegs(SwapLegType.FIXED).get(0);
    int nbFixedPaymentYear = (int) Math.round(1d /
        ((RatePaymentPeriod) fixedLeg.getPaymentPeriods().get(0)).getAccrualPeriods().get(0).getYearFraction());
    int nbFixedPeriod = fixedLeg.getPaymentPeriods().size();
    double forward = swapPricer.parRate(swap, provider);
    double tenor = swaptionVolatilities.tenor(swap.getStartDate(), swap.getEndDate());
    double expiryTime = swaptionVolatilities.relativeTime(
        fixingDate.atTime(index.getFixingTime()).atZone(index.getFixingZone()));
    double volatility = swaptionVolatilities.volatility(expiryTime, tenor, forward, forward);
    ValueDerivatives annuityDerivatives = swapPricer.getLegPricer().annuityCash2(nbFixedPaymentYear, nbFixedPeriod, volatility);
    double forwardAdjustment = -0.5 * forward * forward * volatility * volatility * expiryTime *
        annuityDerivatives.getDerivative(1) / annuityDerivatives.getDerivative(0);
    return CurrencyAmount.of(
        ccy,
        (forward + forwardAdjustment) * dfPayment * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
  }

}
