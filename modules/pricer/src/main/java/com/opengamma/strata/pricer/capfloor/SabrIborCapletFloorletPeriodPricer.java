/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import static com.opengamma.strata.market.model.SabrParameterType.ALPHA;
import static com.opengamma.strata.market.model.SabrParameterType.BETA;
import static com.opengamma.strata.market.model.SabrParameterType.NU;
import static com.opengamma.strata.market.model.SabrParameterType.RHO;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;
import com.opengamma.strata.product.common.PutCall;

/**
 * Pricer for caplet/floorlet in SABR model.
 * <p>
 * The value of the caplet/floorlet after expiry is a fixed payoff amount. The value is zero if valuation date is 
 * after payment date of the caplet/floorlet.
 */
public class SabrIborCapletFloorletPeriodPricer
    extends VolatilityIborCapletFloorletPeriodPricer {

  /**
   * Default implementation.
   */
  public static final SabrIborCapletFloorletPeriodPricer DEFAULT = new SabrIborCapletFloorletPeriodPricer();

  @Override
  protected void validate(IborCapletFloorletVolatilities volatilities) {
    ArgChecker.isTrue(volatilities instanceof SabrIborCapletFloorletVolatilities, "volatilities must be SABR volatilities");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the Ibor caplet/floorlet to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the 
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential 
   * re-calibration of the model parameters to the raw market data.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyModel(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    Currency currency = period.getCurrency();
    if (ratesProvider.getValuationDate().isAfter(period.getPaymentDate())) {
      return PointSensitivityBuilder.none();
    }
    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    PutCall putCall = period.getPutCall();
    double strike = period.getStrike();
    double indexRate = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    PointSensitivityBuilder dfSensi = ratesProvider.discountFactors(currency).zeroRatePointSensitivity(period.getPaymentDate());
    double factor = period.getNotional() * period.getYearFraction();
    if (expiry < 0d) { // option expired already, but not yet paid
      double sign = putCall.isCall() ? 1d : -1d;
      double payoff = Math.max(sign * (indexRate - strike), 0d);
      return dfSensi.multipliedBy(payoff * factor);
    }
    ValueDerivatives volatilityAdj = volatilities.volatilityAdjoint(expiry, strike, indexRate);
    PointSensitivityBuilder indexRateSensiSensi =
        ratesProvider.iborIndexRates(period.getIndex()).ratePointSensitivity(period.getIborRate().getObservation());
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double fwdPv = factor * volatilities.price(expiry, putCall, strike, indexRate, volatilityAdj.getValue());
    double fwdDelta = factor * volatilities.priceDelta(expiry, putCall, strike, indexRate, volatilityAdj.getValue());
    double fwdVega = factor * volatilities.priceVega(expiry, putCall, strike, indexRate, volatilityAdj.getValue());

    return dfSensi.multipliedBy(fwdPv).combinedWith(
        indexRateSensiSensi.multipliedBy(fwdDelta * df + fwdVega * volatilityAdj.getDerivative(0) * df));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity to the SABR model parameters of the Ibor caplet/floorlet.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param volatilities  the volatilities
   * @return the point sensitivity to the SABR model parameters
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      IborCapletFloorletPeriod period,
      RatesProvider ratesProvider,
      SabrIborCapletFloorletVolatilities volatilities) {

    double expiry = volatilities.relativeTime(period.getFixingDateTime());
    if (expiry < 0d) { // option expired already
      return PointSensitivityBuilder.none();
    }
    Currency currency = period.getCurrency();
    PutCall putCall = period.getPutCall();
    double strike = period.getStrike();
    double indexRate = ratesProvider.iborIndexRates(period.getIndex()).rate(period.getIborRate().getObservation());
    double factor = period.getNotional() * period.getYearFraction();
    ValueDerivatives volatilityAdj = volatilities.volatilityAdjoint(expiry, strike, indexRate);
    DoubleArray derivative = volatilityAdj.getDerivatives();
    double df = ratesProvider.discountFactor(currency, period.getPaymentDate());
    double vega = df * factor * volatilities.priceVega(expiry, putCall, strike, indexRate, volatilityAdj.getValue());
    IborCapletFloorletVolatilitiesName name = volatilities.getName();

    return PointSensitivityBuilder.of(
        IborCapletFloorletSabrSensitivity.of(name, expiry, ALPHA, currency, vega * derivative.get(2)),
        IborCapletFloorletSabrSensitivity.of(name, expiry, BETA, currency, vega * derivative.get(3)),
        IborCapletFloorletSabrSensitivity.of(name, expiry, RHO, currency, vega * derivative.get(4)),
        IborCapletFloorletSabrSensitivity.of(name, expiry, NU, currency, vega * derivative.get(5)));
  }

}
