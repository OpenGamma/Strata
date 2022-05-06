/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.capfloor;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletBinaryPeriod;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod;
import com.opengamma.strata.product.capfloor.OvernightInArrearsCapletFloorletPeriod.Builder;
import com.opengamma.strata.product.common.PutCall;

/**
 * Pricer for binary caplet/floorlet based on volatilities.
 * <p>
 * The pricing methodologies is based on 'call spread' approach.
 * <p>
 * The value of the caplet/floorlet after expiry is a fixed payoff amount. The value is zero if valuation date is 
 * after payment date of the caplet/floorlet.
 */
public class VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricer {

  /** The default spread between the approximating options strikes. */
  private static final double DEFAULT_SPREAD = 1.0e-4;

  /**
   * Default implementation.
   */
  public static final VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricer DEFAULT =
      new VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricer(
          SabrOvernightInArrearsCapletFloorletPeriodPricer.DEFAULT,
          DEFAULT_SPREAD);

  /** Vanilla option pricer for approximating vanilla options. */
  private final SabrOvernightInArrearsCapletFloorletPeriodPricer capletPricer;

  /** The spread between the approximating options. */
  private final double spread;

  /**
   * Creates an instance.
   * 
   * @param capletPricer  the pricer for {@link OvernightInArrearsCapletFloorletPeriod}
   * @param spread  the spread between the approximating options strikes
   */
  public VerticalSpreadSabrOvernightInArrearsCapletFloorletBinaryPeriodPricer(
      SabrOvernightInArrearsCapletFloorletPeriodPricer capletPricer,
      double spread) {

    this.capletPricer = ArgChecker.notNull(capletPricer, "capletPricer");
    this.spread = ArgChecker.notNegativeOrZero(spread, "spread");
  }
  //-------------------------------------------------------------------------

  /**
   * Returns the pricer used to price the approximating vanilla caplet/floorlet pricer.
   *
   * @return the pricer
   */
  SabrOvernightInArrearsCapletFloorletPeriodPricer getVanillaOptionProductPricer() {
    return capletPricer;
  }

  /**
   * Returns the spread between the approximating FX vanilla options.
   *
   * @return the spread
   */
  double getSpread() {
    return spread;
  }

  /**
   * Calculates the present value of the binary caplet/floorlet period.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param sabrVolatilities  the SABR parameters
   * @return the present value
   */
  public CurrencyAmount presentValue(
      OvernightInArrearsCapletFloorletBinaryPeriod period,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities sabrVolatilities) {

    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> callSpread =
        vanillaOptionVerticalSpreadPair(period);
    CurrencyAmount firstOptionPv =
        capletPricer.presentValue(callSpread.getFirst(), ratesProvider, sabrVolatilities);
    CurrencyAmount secondOptionPv =
        capletPricer.presentValue(callSpread.getSecond(), ratesProvider, sabrVolatilities);
    return firstOptionPv.plus(secondOptionPv).multipliedBy(Math.signum(period.getAmount()));
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value rates sensitivity of the binary caplet/floorlet period.
   * <p>
   * The present value rates sensitivity of the caplet/floorlet is the sensitivity
   * of the present value to the underlying curves.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param sabrVolatilities  the SABR parameters
   * @return the present value curve sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityRatesStickyModel(
      OvernightInArrearsCapletFloorletBinaryPeriod period,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities sabrVolatilities) {

    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> callSpread =
        vanillaOptionVerticalSpreadPair(period);
    PointSensitivityBuilder firstOptionPts =
        capletPricer.presentValueSensitivityRatesStickyModel(callSpread.getFirst(), ratesProvider, sabrVolatilities);
    PointSensitivityBuilder secondOptionPts =
        capletPricer.presentValueSensitivityRatesStickyModel(callSpread.getSecond(), ratesProvider, sabrVolatilities);
    return (firstOptionPts.combinedWith(secondOptionPts)).multipliedBy(Math.signum(period.getAmount()));
  }

  /**
   * Calculates the present value sensitivity to model parameters of the binary caplet/floorlet period.
   * <p>
   * The present value rates sensitivity of the caplet/floorlet is the sensitivity
   * of the present value to the underlying curves.
   * 
   * @param period  the Ibor caplet/floorlet period
   * @param ratesProvider  the rates provider
   * @param sabrVolatilities  the SABR parameters
   * @return the present value curve sensitivity
   */
  public PointSensitivityBuilder presentValueSensitivityModelParamsSabr(
      OvernightInArrearsCapletFloorletBinaryPeriod period,
      RatesProvider ratesProvider,
      SabrParametersIborCapletFloorletVolatilities sabrVolatilities) {

    Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod> callSpread =
        vanillaOptionVerticalSpreadPair(period);
    PointSensitivityBuilder firstOptionPts =
        capletPricer.presentValueSensitivityModelParamsSabr(callSpread.getFirst(), ratesProvider, sabrVolatilities);
    PointSensitivityBuilder secondOptionPts =
        capletPricer.presentValueSensitivityModelParamsSabr(callSpread.getSecond(), ratesProvider, sabrVolatilities);
    return (firstOptionPts.combinedWith(secondOptionPts)).multipliedBy(Math.signum(period.getAmount()));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates pair of vanilla caplet for binary caplet/floorlet pricing by call spread.
   * 
   * @param binary  the binary caplet/floorlet
   * @return the call spread
   */
  public Pair<OvernightInArrearsCapletFloorletPeriod, OvernightInArrearsCapletFloorletPeriod>
      vanillaOptionVerticalSpreadPair(OvernightInArrearsCapletFloorletBinaryPeriod binary) {

    double capFloorInd = (binary.getPutCall().equals(PutCall.CALL) ? 1d : -1d);
    double adjStrikeLow = binary.getStrike() - spread;
    double adjStrikeHigh = binary.getStrike() + spread;
    double amount = Math.abs(binary.getAmount());
    double rescaledNotional = amount / (2.0d * spread) * capFloorInd; // To obtain the amount over the strike length 2 * spread
    Builder optionLong = OvernightInArrearsCapletFloorletPeriod.builder()
        .currency(binary.getCurrency())
        .notional(rescaledNotional)
        .startDate(binary.getStartDate())
        .endDate(binary.getEndDate())
        .yearFraction(binary.getYearFraction())
        .paymentDate(binary.getPaymentDate())
        .overnightRate(binary.getOvernightRate());
    Builder optionShort = OvernightInArrearsCapletFloorletPeriod.builder()
        .currency(binary.getCurrency())
        .notional(-rescaledNotional)
        .startDate(binary.getStartDate())
        .endDate(binary.getEndDate())
        .yearFraction(binary.getYearFraction())
        .paymentDate(binary.getPaymentDate())
        .overnightRate(binary.getOvernightRate());
    if (binary.getPutCall().equals(PutCall.CALL)) { // Cap
      return Pair.of(optionLong.caplet(adjStrikeLow).build(), optionShort.caplet(adjStrikeHigh).build());
    }
    // Floor
    return Pair.of(optionLong.floorlet(adjStrikeLow).build(), optionShort.floorlet(adjStrikeHigh).build());
  }

}
