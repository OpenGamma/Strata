/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.time.LocalDate;
import java.time.ZoneOffset;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.impl.rate.ForwardOvernightCompoundedRateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.rate.OvernightCompoundedRateComputation;

/**
 * Pricer for overnight in-arrears caplet/floorlet based on volatilities.
 * <p>
 * The pricing methodologies are defined in individual implementations of the volatilities, {@code IborCapletFloorletVolatilities}.
 * <p>
 * The volatilities are stored in {@code IborCapletFloorletVolatilities}, it should be understood as "TermRateCapletFloorletVolatilities".
 * <p>
 * The pricing is based on "interpolated volatilities" for the compounded in-arrears rates,
 * in particular Section 6.3 of the reference below.
 * Reference: A. Lyashenko and F. Mercurio. Looking forward to backward-looking rates: A modeling frame- 
 * work for term rates replacing LIBOR. SSRN Working Paper 3330240, March 2019.
 */
public class VolatilityOvernightInArrearsCapletFloorletPeriodPricer {

  /**
   * Default implementation.
   */
  public static final VolatilityOvernightInArrearsCapletFloorletPeriodPricer DEFAULT =
      new VolatilityOvernightInArrearsCapletFloorletPeriodPricer();

  /**
   * The function to compute overnight rates, including if necessary the past composition from time series.
   */
  private static final ForwardOvernightCompoundedRateComputationFn ON_FUNCT =
      ForwardOvernightCompoundedRateComputationFn.DEFAULT;

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the overnight in-arrears caplet/floorlet period.
   * <p>
   * The result is expressed using the currency of the period.
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      OvernightInArrearsCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    Currency currency = period.getCurrency();
    if (ratesProvider.getValuationDate().isAfter(period.getPaymentDate())) {
      return CurrencyAmount.of(currency, 0d);
    }
    OvernightCompoundedRateComputation onComputation = period.getOvernightRate();
    LocalDate startDate = onComputation.getStartDate();
    LocalDate endDate = onComputation.getEndDate();
    double startTime = volatilities.relativeTime(startDate.atStartOfDay(ZoneOffset.UTC)); // ON rates don't have an exact fixing time
    double endTime = volatilities.relativeTime(endDate.atStartOfDay(ZoneOffset.UTC));
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    PutCall putCall = period.getPutCall();
    double strike = period.getStrike();
    double forward = ON_FUNCT
        .rate(onComputation, onComputation.getStartDate(), onComputation.getEndDate(), ratesProvider);
    if (!ratesProvider.getValuationDate().isBefore(period.getEndDate())) { // Between end compounding and payment date
      double dfPayment = ratesProvider.discountFactor(currency, period.getPaymentDate());
      return period.payoff(forward).multipliedBy(dfPayment);
    }
    double volatility = volatilities.volatility(endTime, strike, forward);
    double adjustedVolatility = adjustedVolatility(startTime, endTime, volatility);
    double price =
        df * period.getYearFraction() * volatilities.price(endTime, putCall, strike, forward, adjustedVolatility);
    return CurrencyAmount.of(currency, price * period.getNotional());
  }

  /**
   * Computes the present value sensitivity to the rate with a volatility "sticky strike".
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value rate sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyStrike(
      OvernightInArrearsCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    Currency currency = period.getCurrency();
    if (ratesProvider.getValuationDate().isAfter(period.getPaymentDate())) {
      return PointSensitivityBuilder.none();
    }
    OvernightCompoundedRateComputation onComputation = period.getOvernightRate();
    LocalDate startDate = onComputation.getStartDate();
    LocalDate endDate = onComputation.getEndDate();
    double startTime = volatilities.relativeTime(startDate.atStartOfDay(ZoneOffset.UTC)); // ON rates don't have an exact fixing time
    double endTime = volatilities.relativeTime(endDate.atStartOfDay(ZoneOffset.UTC));
    double dfPayment = ratesProvider.discountFactor(currency, period.getPaymentDate());
    PutCall putCall = period.getPutCall();
    double strike = period.getStrike();
    double forward = ON_FUNCT
        .rate(onComputation, onComputation.getStartDate(), onComputation.getEndDate(), ratesProvider);
    if (!ratesProvider.getValuationDate().isBefore(period.getEndDate())) { // Between end compounding and payment date
      double pvForward = period.payoff(forward).getAmount();
      // Backward sweep
      double dfPaymentBar = pvForward;
      ZeroRateSensitivity ddfPaymentdr = ratesProvider
          .discountFactors(currency).zeroRatePointSensitivity(period.getPaymentDate());
      return ddfPaymentdr.multipliedBy(dfPaymentBar);
    }
    double volatility = volatilities.volatility(endTime, strike, forward);
    double adjustedVolatility = adjustedVolatility(startTime, endTime, volatility);
    double price = volatilities.price(endTime, putCall, strike, forward, adjustedVolatility);
    double pv = dfPayment * period.getYearFraction() * price * period.getNotional();
    // Backward sweep
    double pvBar = 1.0;
    double priceBar = dfPayment * period.getYearFraction() * period.getNotional() * pvBar;
    double dfPaymentBar = pv / dfPayment * pvBar;
    double priceDelta = volatilities.priceDelta(endTime, putCall, strike, forward, adjustedVolatility);
    double forwardBar = priceDelta * priceBar;
    PointSensitivityBuilder dforwarddr = ON_FUNCT
        .rateSensitivity(onComputation, onComputation.getStartDate(), onComputation.getEndDate(), ratesProvider);
    ZeroRateSensitivity ddfPaymentdr = ratesProvider
        .discountFactors(currency).zeroRatePointSensitivity(period.getPaymentDate());
    return ddfPaymentdr.multipliedBy(dfPaymentBar).combinedWith(dforwarddr.multipliedBy(forwardBar));
  }

  /**
   * Computes the present value sensitivity to the volatilities.
   * 
   * @param period  the caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the present value volatility sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsVolatility(
      OvernightInArrearsCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    Currency currency = period.getCurrency();
    if (!ratesProvider.getValuationDate().isBefore(period.getEndDate())) {
      return PointSensitivityBuilder.none();
    }
    OvernightCompoundedRateComputation onComputation = period.getOvernightRate();
    LocalDate startDate = onComputation.getStartDate();
    LocalDate endDate = onComputation.getEndDate();
    double startTime = volatilities.relativeTime(startDate.atStartOfDay(ZoneOffset.UTC)); // ON rates don't have an exact fixing time
    double endTime = volatilities.relativeTime(endDate.atStartOfDay(ZoneOffset.UTC));
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    PutCall putCall = period.getPutCall();
    double strike = period.getStrike();
    double forward = ON_FUNCT
        .rate(onComputation, onComputation.getStartDate(), onComputation.getEndDate(), ratesProvider);
    double volatility = volatilities.volatility(endTime, strike, forward);
    double adjustedVolatility = adjustedVolatility(startTime, endTime, volatility);
    double price = volatilities.price(endTime, putCall, strike, forward, adjustedVolatility);
    double pv = df * period.getYearFraction() * price * period.getNotional();
    // Backward sweep
    double pvBar = 1.0;
    double priceBar = pv / price * pvBar;
    double priceVega = volatilities.priceVega(endTime, putCall, strike, forward, adjustedVolatility);
    double adjustedVolatilityBar = priceVega * priceBar;
    double volatilityBar = adjustedVolatility / volatility * adjustedVolatilityBar;
    return IborCapletFloorletSensitivity.of(
        volatilities.getName(),
        endTime,
        strike,
        forward,
        currency,
        volatilityBar);
  }

  /**
   * Volatility adjusted for the decrease of forward rate volatility in the composition period.
   * 
   * @param startTime  the start time
   * @param endTime  the end time
   * @param volatility  the volatility
   * @return the adjusted volatility
   */
  public double adjustedVolatility(double startTime, double endTime, double volatility) {
    if (startTime > 0) {
      return volatility * Math.sqrt(1.0 / 3.0 + 2.0d / 3.0d * startTime / endTime);
    }
    return volatility * endTime / (endTime - startTime) / Math.sqrt(3.0d);
  }

}
