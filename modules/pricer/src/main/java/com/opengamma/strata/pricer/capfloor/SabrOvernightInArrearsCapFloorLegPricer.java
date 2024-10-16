/*
 * Copyright (C) 2024 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import java.time.ZoneOffset;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.MapStream;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;
import com.opengamma.strata.product.capfloor.ResolvedOvernightInArrearsCapFloorLeg;

/**
 * Pricer for overnight rate in arrears cap/floor legs in SABR model.
 */
public class SabrOvernightInArrearsCapFloorLegPricer {

  /**
   * Default implementation.
   */
  public static final SabrOvernightInArrearsCapFloorLegPricer DEFAULT =
      new SabrOvernightInArrearsCapFloorLegPricer(SabrOvernightInArrearsCapletFloorletPeriodPricer.DEFAULT);

  /**
   * The period pricer.
   */
  private final SabrOvernightInArrearsCapletFloorletPeriodPricer periodPricer;

  /**
   * Creates an instance.
   *
   * @param periodPricer the pricer for {@link OvernightInArrearsCapletFloorletPeriod}.
   */
  public SabrOvernightInArrearsCapFloorLegPricer(SabrOvernightInArrearsCapletFloorletPeriodPricer periodPricer) {
    this.periodPricer = ArgChecker.notNull(periodPricer, "periodPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains the underlying period pricer.
   *
   * @return the period pricer
   */
  public SabrOvernightInArrearsCapletFloorletPeriodPricer getPeriodPricer() {
    return periodPricer;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the overnight rate in arrears cap/floor leg.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * The result is returned using the payment currency of the leg.
   *
   * @param capFloorLeg the cap/floor leg
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ResolvedOvernightInArrearsCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods().stream()
        .map(period -> periodPricer.presentValue(period, ratesProvider, volatilities))
        .reduce(CurrencyAmount::plus)
        .orElse(CurrencyAmount.zero(capFloorLeg.getCurrency()));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value for each caplet/floorlet of the overnight rate in arrears cap/floor leg.
   * <p>
   * The present value of each caplet/floorlet is the value on the valuation date.
   * The result is returned using the payment currency of the leg.
   *
   * @param capFloorLeg the cap/floor leg
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the present values
   */
  public OvernightInArrearsCapletFloorletPeriodCurrencyAmounts presentValueCapletFloorletPeriods(
      ResolvedOvernightInArrearsCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    Map<OvernightInArrearsCapletFloorletPeriod, CurrencyAmount> periodPresentValues =
        MapStream.of(capFloorLeg.getCapletFloorletPeriods())
            .mapValues(period -> periodPricer.presentValue(period, ratesProvider, volatilities))
            .toMap();
    return OvernightInArrearsCapletFloorletPeriodCurrencyAmounts.of(periodPresentValues);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the current cash of the overnight in arrears cap/floor leg.
   *
   * @param capFloorLeg the cap/floor leg
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ResolvedOvernightInArrearsCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods().stream()
        .filter(period -> period.getPaymentDate().equals(ratesProvider.getValuationDate()))
        .map(period -> periodPricer.presentValue(period, ratesProvider, volatilities))
        .reduce(CurrencyAmount::plus)
        .orElse(CurrencyAmount.zero(capFloorLeg.getCurrency()));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the forward rates for each caplet/floorlet of the overnight rate in arrears cap/floor leg.
   *
   * @param capFloorLeg the cap/floor leg
   * @param ratesProvider the rates provider
   * @return the forward rates
   */
  public OvernightInArrearsCapletFloorletPeriodAmounts forwardRates(
      ResolvedOvernightInArrearsCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider) {

    Map<OvernightInArrearsCapletFloorletPeriod, Double> forwardRates =
        MapStream.of(capFloorLeg.getCapletFloorletPeriods())
            .filterKeys(period -> !ratesProvider.getValuationDate().isAfter(period.getOvernightRate().getEndDate()))
            .mapValues(period -> periodPricer.forwardRate(period, ratesProvider))
            .toMap();
    return OvernightInArrearsCapletFloorletPeriodAmounts.of(forwardRates);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the implied volatilities for each caplet/floorlet of the overnight rate in arrears cap/floor leg.
   *
   * @param capFloorLeg the cap/floor leg
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the implied volatilities
   */
  public OvernightInArrearsCapletFloorletPeriodAmounts impliedVolatilities(
      ResolvedOvernightInArrearsCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    ImmutableMap<OvernightInArrearsCapletFloorletPeriod, Double> impliedVolatilities =
        MapStream.of(capFloorLeg.getCapletFloorletPeriods())
            .filterKeys(period -> volatilities.relativeTime(
                period.getOvernightRate().getEndDate().atStartOfDay(ZoneOffset.UTC)) >= 0)
            .mapValues(period -> periodPricer.impliedVolatility(period, ratesProvider, volatilities))
            .toMap();
    return OvernightInArrearsCapletFloorletPeriodAmounts.of(impliedVolatilities);
  }

  /**
   * Calculates the present value sensitivity of the overnight in arrears cap/floor leg to the rate curves.
   * <p>
   * The present value sensitivity is computed in a "sticky model parameter" style, i.e. the sensitivity to the
   * curve nodes with the SABR model parameters unchanged. This sensitivity does not include a potential
   * re-calibration of the model parameters to the raw market data.
   *
   * @param capFloorLeg the cap/floor leg
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the point sensitivity to the rate curves
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyModel(
      ResolvedOvernightInArrearsCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods().stream()
        .map(period -> periodPricer.presentValueSensitivityRatesStickyModel(period, ratesProvider, volatilities))
        .reduce(PointSensitivityBuilder::combinedWith)
        .orElse(PointSensitivityBuilder.none());
  }

  /**
   * Calculates the present value sensitivity to the SABR model parameters of the overnight in arrears cap/floor.
   * <p>
   * The sensitivity of the present value to the SABR model parameters, alpha, beta, rho and nu.
   *
   * @param capFloorLeg the cap/floor leg
   * @param ratesProvider the rates provider
   * @param volatilities the volatilities
   * @return the point sensitivity to the SABR model parameters
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      ResolvedOvernightInArrearsCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods().stream()
        .map(period -> periodPricer.presentValueSensitivityModelParamsSabr(period, ratesProvider, volatilities))
        .reduce(PointSensitivityBuilder::combinedWith)
        .orElse(PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  private void validate(RatesProvider ratesProvider, IborCapletFloorletVolatilities volatilities) {
    ArgChecker.isTrue(volatilities.getValuationDate().equals(ratesProvider.getValuationDate()),
        "volatility and rate data must be for the same date");
  }

}
