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
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.cms.CmsPeriodType;
import com.opengamma.strata.product.swap.ResolvedSwap;

/**
 *  Computes the price of a CMS coupon by simple forward estimation.
 *  <p>
 *  This is an overly simplistic approach to CMS coupon pricer. It is provided only for testing and comparison 
 *  purposes. It is not recommended to use this for valuation or risk management purposes.
 */
public class DiscountingCmsPeriodPricer {

  /**
   * Default implementation.
   */
  public static final DiscountingCmsPeriodPricer DEFAULT = new DiscountingCmsPeriodPricer(
      DiscountingSwapProductPricer.DEFAULT);

  /**
   * Pricer for the underlying swap.
   */
  private final DiscountingSwapProductPricer swapPricer;

  /**
   * Creates an instance.
   * 
   * @param swapPricer  the pricer for {@link ResolvedSwap}
   */
  public DiscountingCmsPeriodPricer(
      DiscountingSwapProductPricer swapPricer) {
    this.swapPricer = ArgChecker.notNull(swapPricer, "legPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value of CMS coupon by simple forward rate estimation.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @return the present value
   */
  public CurrencyAmount presentValue(
      CmsPeriod cmsPeriod,
      RatesProvider provider) {

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
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    double forward = swapPricer.parRate(swap, provider);
    return CurrencyAmount.of(ccy, forward * dfPayment * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
  }

  /**
   * Computes the forward rate associated to the swap underlying the CMS period.
   * <p>
   * Returns a value only if the period has not fixed yet. If the fixing date is on or before the valuation date,
   * an {@link IllegalArgumentException} is thrown.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @return the forward rate
   */
  public double forwardRate(
      CmsPeriod cmsPeriod,
      RatesProvider provider) {

    LocalDate fixingDate = cmsPeriod.getFixingDate();
    LocalDate valuationDate = provider.getValuationDate();
    if (!fixingDate.isAfter(valuationDate)) { // Using fixing
      throw new IllegalArgumentException("Forward rate is availaible only for valuation date after the fixing date");
    }
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    return swapPricer.parRate(swap, provider);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value curve sensitivity by simple forward rate estimation.
   * 
   * @param cmsPeriod  the CMS 
   * @param provider  the rates provider
   * @return the present value sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivity(
      CmsPeriod cmsPeriod,
      RatesProvider provider) {

    Currency ccy = cmsPeriod.getCurrency();
    LocalDate valuationDate = provider.getValuationDate();
    if (valuationDate.isAfter(cmsPeriod.getPaymentDate())) {
      return PointSensitivityBuilder.none();
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
        return provider.discountFactors(ccy).zeroRatePointSensitivity(
            cmsPeriod.getPaymentDate()).multipliedBy(payoff * cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
      } else if (fixingDate.isBefore(valuationDate)) {
        throw new IllegalArgumentException(Messages.format(
            "Unable to get fixing for {} on date {}, no time-series supplied", cmsPeriod.getIndex(), fixingDate));
      }
    }
    if (!cmsPeriod.getCmsPeriodType().equals(CmsPeriodType.COUPON)) {
      throw new IllegalArgumentException("Unable to price cap or floor in this pricer");
    }
    // Using forward
    ResolvedSwap swap = cmsPeriod.getUnderlyingSwap();
    ZeroRateSensitivity dfPaymentdr = provider.discountFactors(ccy).zeroRatePointSensitivity(cmsPeriod.getPaymentDate());
    double forward = swapPricer.parRate(swap, provider);
    PointSensitivityBuilder forwardSensi = swapPricer.parRateSensitivity(swap, provider);
    return forwardSensi.multipliedBy(dfPayment).combinedWith(dfPaymentdr.multipliedBy(forward))
        .multipliedBy(cmsPeriod.getNotional() * cmsPeriod.getYearFraction());
  }

}
