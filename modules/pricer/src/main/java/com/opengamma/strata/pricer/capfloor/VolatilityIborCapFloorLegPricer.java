/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.view.IborCapletFloorletVolatilities;
import com.opengamma.strata.pricer.impl.capfloor.VolatilityIborCapletFloorletPeriodPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.ExpandedIborCapFloorLeg;
import com.opengamma.strata.product.capfloor.IborCapFloorLeg;
import com.opengamma.strata.product.capfloor.IborCapletFloorletPeriod;

/**
 * Pricer for cap/floor legs based on volatilities.
 * <p>
 * This function provides the ability to price {@link ExpandedIborCapFloorLeg}. 
 * One must apply {@code expand()} in order to price {@link IborCapFloorLeg}. 
 * <p>
 * The pricing methodologies are defined in individual implementations of the volatilities, {@link IborCapletFloorletVolatilities}. 
 */
public class VolatilityIborCapFloorLegPricer {

  /**
   * Default implementation. 
   */
  public static final VolatilityIborCapFloorLegPricer DEFAULT =
      new VolatilityIborCapFloorLegPricer(VolatilityIborCapletFloorletPeriodPricer.DEFAULT);

  /**
   * Pricer for {@link IborCapletFloorletPeriod}.
   */
  private final VolatilityIborCapletFloorletPeriodPricer periodPricer;

  /**
   * Creates an instance. 
   * 
   * @param periodPricer  the pricer for {@link IborCapletFloorletPeriod}.
   */
  public VolatilityIborCapFloorLegPricer(VolatilityIborCapletFloorletPeriodPricer periodPricer) {
    this.periodPricer = ArgChecker.notNull(periodPricer, "periodPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the cap/floor leg.
   * <p>
   * The present value of the leg is the value on the valuation date.
   * The result is returned using the payment currency of the leg.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param ratesProvider  the rates provider 
   * @param volatilities  the volatilities
   * @return the present value
   */
  public CurrencyAmount presentValue(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValue(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  /**
   * Calculates the present value delta of the cap/floor leg.
   * <p>
   * The present value delta of the leg is the sensitivity value on the valuation date.
   * The result is returned using the payment currency of the leg.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param ratesProvider  the rates provider 
   * @param volatilities  the volatilities
   * @return the present value delta
   */
  public CurrencyAmount presentValueDelta(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueDelta(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  /**
   * Calculates the present value gamma of the cap/floor leg.
   * <p>
   * The present value gamma of the leg is the sensitivity value on the valuation date.
   * The result is returned using the payment currency of the leg.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param ratesProvider  the rates provider 
   * @param volatilities  the volatilities
   * @return the present value gamma
   */
  public CurrencyAmount presentValueGamma(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueGamma(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  /**
   * Calculates the present value theta of the cap/floor leg.
   * <p>
   * The present value theta of the leg is the sensitivity value on the valuation date.
   * The result is returned using the payment currency of the leg.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param ratesProvider  the rates provider 
   * @param volatilities  the volatilities
   * @return the present value theta
   */
  public CurrencyAmount presentValueTheta(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueTheta(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .get();
  }

  /**
   * Calculates the present value curve sensitivity of the cap/floor leg.
   * <p>
   * The present value sensitivity of the leg is the sensitivity of the present value to
   * the underlying curves.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param ratesProvider  the rates provider 
   * @param volatilities  the volatilities
   * @return the present value curve sensitivity 
   */
  public PointSensitivityBuilder presentValueSensitivity(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueSensitivity(period, ratesProvider, volatilities))
        .reduce((p1, p2) -> p1.combinedWith(p2))
        .get();
  }

  /**
   * Calculates the present value volatility sensitivity of the cap/floor leg.
   * <p>
   * The present value volatility sensitivity of the leg is the sensitivity of the present value to the volatility 
   * values.
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param ratesProvider  the rates provider 
   * @param volatilities  the volatilities
   * @return the present value volatility sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityVolatility(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .map(period -> periodPricer.presentValueSensitivityVolatility(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.combinedWith(c2))
        .get();
  }

  /**
   * Calculates the current cash of the cap/floor leg. 
   * 
   * @param capFloorLeg  the cap/floor leg
   * @param ratesProvider  the rates provider 
   * @param volatilities  the volatilities
   * @return the current cash
   */
  public CurrencyAmount currentCash(
      ExpandedIborCapFloorLeg capFloorLeg,
      RatesProvider ratesProvider,
      IborCapletFloorletVolatilities volatilities) {

    validate(ratesProvider, volatilities);
    return capFloorLeg.getCapletFloorletPeriods()
        .stream()
        .filter(period -> period.getPaymentDate().equals(ratesProvider.getValuationDate()))
        .map(period -> periodPricer.presentValue(period, ratesProvider, volatilities))
        .reduce((c1, c2) -> c1.plus(c2))
        .orElse(CurrencyAmount.zero(capFloorLeg.getCurrency()));
  }

  //-------------------------------------------------------------------------
  private void validate(RatesProvider ratesProvider, IborCapletFloorletVolatilities volatilities) {
    ArgChecker.isTrue(volatilities.getValuationDate().equals(ratesProvider.getValuationDate()),
        "volatility and rate data must be for the same date");
  }

}
