/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.util.Objects;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Triple;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolatorFactory;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Defines a term structure of smiles as used in Forex market.
 * <p>
 * This contains smile from at-the-money, risk reversal and strangle.
 * The delta used is the delta with respect to forward.
 * Interpolation is used to find additional values.
 */
class SmileDeltaTermStructureParametersStrikeInterpolation
    extends SmileDeltaTermStructureParameters {
  // NOTE: This class is package scoped, as the Smile data provider API is effectively still in Beta

  /**
   * The interpolator/extrapolator used in the strike dimension.
   */
  private final Interpolator1D _strikeInterpolator;

  /**
   * The default interpolator: linear with flat extrapolation.
   */
  private static final Interpolator1D DEFAULT_INTERPOLATOR_STRIKE =
      CombinedInterpolatorExtrapolatorFactory.getInterpolator(
          Interpolator1DFactory.LINEAR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR,
          Interpolator1DFactory.FLAT_EXTRAPOLATOR);

  /**
   * Constructor from volatility term structure. 
   * <p>
   * The default interpolator is used to interpolate in the strike dimension. 
   * The default interpolator is linear with flat extrapolation.
   * 
   * @param volatilityTerm  the volatility description at the different expiration.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(SmileDeltaParameters[] volatilityTerm) {
    this(volatilityTerm, DEFAULT_INTERPOLATOR_STRIKE);
  }

  /**
   * Constructor from name and volatility term structure. 
   * <p>
   * The default interpolator is used to interpolate in the strike dimension. 
   * The default interpolator is linear with flat extrapolation.
   * 
   * @param name  the name of the smile parameter term structure
   * @param volatilityTerm  the volatility description at the different expiration.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(String name, SmileDeltaParameters[] volatilityTerm) {
    this(name, volatilityTerm, DEFAULT_INTERPOLATOR_STRIKE);
  }

  /**
   * Constructor from volatility term structure and strike interpolator. 
   * 
   * @param volatilityTerm  the volatility description at the different expiration.
   * @param strikeInterpolator  the interpolator used in the strike dimension.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      SmileDeltaParameters[] volatilityTerm,
      Interpolator1D strikeInterpolator) {
    super(volatilityTerm);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from name, volatility term structure, strike interpolator. 
   * 
   * @param name  the name of the smile parameter term structure
   * @param volatilityTerm  the volatility description at the different expiration.
   * @param strikeInterpolator  the interpolator used in the strike dimension.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      SmileDeltaParameters[] volatilityTerm,
      Interpolator1D strikeInterpolator) {
    super(name, volatilityTerm);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from volatility term structure, strike interpolator and time interpolator.
   * 
   * @param volatilityTerm  the volatility description at the different expiration.
   * @param strikeInterpolator  the interpolator used in the strike dimension.
   * @param timeInterpolator  the interpolator used in the time dimension.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      SmileDeltaParameters[] volatilityTerm,
      Interpolator1D strikeInterpolator,
      Interpolator1D timeInterpolator) {
    super(volatilityTerm, timeInterpolator);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from volatility term structure, strike interpolator and time interpolator.
   * 
   * @param name  the name of the smile parameter term structure
   * @param volatilityTerm  the volatility description at the different expiration.
   * @param strikeInterpolator  the interpolator used in the strike dimension.
   * @param timeInterpolator  the interpolator used in the time dimension.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      SmileDeltaParameters[] volatilityTerm,
      Interpolator1D strikeInterpolator,
      Interpolator1D timeInterpolator) {
    super(name, volatilityTerm, timeInterpolator);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from market data. 
   * <p>
   * The market date consists of time to expiration, delta and volatility.  
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiration. 
   * <p>
   * The default interpolator is used to interpolate in the strike dimension. 
   * The default interpolator is linear with flat extrapolation.
   * 
   * @param timeToExpiration  the time to expiration of each volatility smile.
   * @param delta  the delta at which the volatilities are given. 
   * @param volatility  the volatilities at each delta.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      double[] timeToExpiration,
      double[] delta,
      double[][] volatility) {
    this(timeToExpiration, delta, volatility, DEFAULT_INTERPOLATOR_STRIKE);
  }

  /**
   * Constructor from name and market data. 
   * <p>
   * The market date consists of time to expiration, delta and volatility.  
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiration. 
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiration} 
   * and {@code m} is the length of {@code delta}.
   * <p>
   * The default interpolator is used to interpolate in the strike dimension. 
   * The default interpolator is linear with flat extrapolation.
   * 
   * @param name  the name of the smile parameter term structure
   * @param timeToExpiration  the time to expiration of each volatility smile.
   * @param delta  the delta at which the volatilities are given. 
   * @param volatility  the volatilities at each delta.
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      double[] timeToExpiration,
      double[] delta,
      double[][] volatility) {
    this(name, timeToExpiration, delta, volatility, DEFAULT_INTERPOLATOR_STRIKE);
  }

  /**
   * Constructor from market data and strike interpolator. 
   * <p>
   * The market date consists of time to expiration, delta and volatility.  
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiration. 
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiration} 
   * and {@code m} is the length of {@code delta}.
   * 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param volatility  the volatilities at each delta 
   * @param strikeInterpolator  the interpolator used in the strike dimension 
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      double[] timeToExpiration,
      double[] delta,
      double[][] volatility,
      Interpolator1D strikeInterpolator) {
    super(timeToExpiration, delta, volatility);
    ArgChecker.notNull(strikeInterpolator, "strike interpolator");
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from name, market data and strike interpolator. 
   * <p>
   * The market date consists of time to expiration, delta and volatility.  
   * The delta must be positive and sorted in ascending order.
   * The range of delta is common to all time to expiration. 
   * <p>
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiration} 
   * and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name of smile parameter term structure 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param volatility  the volatilities at each delta 
   * @param strikeInterpolator  the interpolator used in the strike dimension 
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      double[] timeToExpiration,
      double[] delta,
      double[][] volatility,
      Interpolator1D strikeInterpolator) {
    super(name, timeToExpiration, delta, volatility);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from market data.
   * <p>
   * The market data consists of time to expiration, delta, ATM volatilities, risk reversal figures and 
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiration. 
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiration} and {@code m} is the length of {@code delta}. 
   * <p>
   * The default interpolator is used to interpolate in the strike dimension. 
   * The default interpolator is linear with flat extrapolation.
   * 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiration
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      double[] timeToExpiration,
      double[] delta, double[] atm,
      double[][] riskReversal,
      double[][] strangle) {
    this(timeToExpiration, delta, atm, riskReversal, strangle, DEFAULT_INTERPOLATOR_STRIKE);
  }

  /**
   * Constructor from name and market data.
   * <p>
   * The market data consists of time to expiration, delta, ATM volatilities, risk reversal figures and 
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiration. 
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiration} and {@code m} is the length of {@code delta}. 
   * <p>
   * The default interpolator is used to interpolate in the strike dimension. 
   * The default interpolator is linear with flat extrapolation.
   * 
   * @param name  the name of smile parameter term structure 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiration
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      double[] timeToExpiration,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle) {
    this(name, timeToExpiration, delta, atm, riskReversal, strangle, DEFAULT_INTERPOLATOR_STRIKE);
  }

  /**
   * Constructor from market data and strike interpolator.
   * <p>
   * The market data consists of time to expiration, delta, ATM volatilities, risk reversal figures and 
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiration. 
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiration} and {@code m} is the length of {@code delta}. 
   * 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiration
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   * @param strikeInterpolator  the interpolator to be used in the strike dimension
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      double[] timeToExpiration,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle,
      Interpolator1D strikeInterpolator) {
    super(timeToExpiration, delta, atm, riskReversal, strangle);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from name, market data and strike interpolator.
   * <p>
   * The market data consists of time to expiration, delta, ATM volatilities, risk reversal figures and 
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiration. 
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiration} and {@code m} is the length of {@code delta}. 
   * 
   * @param name  the name of smile parameter term structure 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiration
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   * @param strikeInterpolator  the interpolator to be used in the strike dimension
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      double[] timeToExpiration,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle,
      Interpolator1D strikeInterpolator) {
    super(name, timeToExpiration, delta, atm, riskReversal, strangle);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from market data, strike interpolator and time interpolator.
   * <p>
   * The market data consists of time to expiration, delta, ATM volatilities, risk reversal figures and 
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiration. 
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiration} and {@code m} is the length of {@code delta}. 
   * 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiration
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   * @param strikeInterpolator  the interpolator to be used in the strike dimension
   * @param timeInterpolator  the interpolator to be used in the time dimension
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      double[] timeToExpiration,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle,
      Interpolator1D strikeInterpolator,
      Interpolator1D timeInterpolator) {
    super(timeToExpiration, delta, atm, riskReversal, strangle, timeInterpolator);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  /**
   * Constructor from name, market data, strike interpolator and time interpolator.
   * <p>
   * The market data consists of time to expoiration, delta, ATM volatilities, risk reversal figures and 
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiration. 
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiration} and {@code m} is the length of {@code delta}. 
   * 
   * @param name  the name of the smile parameter term structure 
   * @param timeToExpiration  the time to expiration of each volatility smile 
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiration
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   * @param strikeInterpolator  the interpolator to be used in the strike dimension
   * @param timeInterpolator  the interpolator to be used in the time dimension
   */
  public SmileDeltaTermStructureParametersStrikeInterpolation(
      String name,
      double[] timeToExpiration,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle,
      Interpolator1D strikeInterpolator,
      Interpolator1D timeInterpolator) {
    super(name, timeToExpiration, delta, atm, riskReversal, strangle, timeInterpolator);
    _strikeInterpolator = ArgChecker.notNull(strikeInterpolator, "strikeInterpolator");
  }

  //-------------------------------------------------------------------------
  /**
   * Create a copy of the bundle
   * @return A copy of the bundle
   */
  @Override
  public SmileDeltaTermStructureParametersStrikeInterpolation copy() {
    return new SmileDeltaTermStructureParametersStrikeInterpolation(
        getName(), getVolatilityTerm(), getStrikeInterpolator(), getTimeInterpolator());
  }

  /**
   * Get the volatility at a given time/strike/forward from the term structure. The volatility at a given delta are interpolated linearly on the total variance (s^2*t) and extrapolated flat.
   * The volatility are then linearly interpolated in the strike dimension and extrapolated flat.
   * @param time The time to expiry.
   * @param strike The strike.
   * @param forward The forward.
   * @return The volatility.
   */
  public double getVolatility(double time, double strike, double forward) {
    ArgChecker.isTrue(time >= 0, "Positive time");
    SmileDeltaParameters smile = getSmileForTime(time);
    double[] strikes = smile.getStrike(forward);
    Interpolator1DDataBundle volatilityInterpolation =
        _strikeInterpolator.getDataBundle(strikes, smile.getVolatility().toArray());
    double volatility = _strikeInterpolator.interpolate(volatilityInterpolation, strike);
    return volatility;
  }

  /**
   * Computes the volatility and the volatility sensitivity with respect to the volatility data points.
   * @param time The time to expiration.
   * @param strike The strike.
   * @param forward The forward.
   * After the methods, it contains the volatility sensitivity to the data points.
   * Only the lines of impacted dates are changed. The input data on the other lines will not be changed.
   * @return The volatility.
   */
  public VolatilityAndBucketedSensitivities getVolatilityAndSensitivities(double time, double strike, double forward) {
    ArgChecker.isTrue(time >= 0, "Positive time");
    SmileDeltaParameters smile = getSmileForTime(time);
    double[] strikes = smile.getStrike(forward);
    Interpolator1DDataBundle volatilityInterpolation =
        _strikeInterpolator.getDataBundle(strikes, smile.getVolatility().toArray());
    double volatility = _strikeInterpolator.interpolate(volatilityInterpolation, strike);
    // Backward sweep
    double[] smileVolatilityBar = _strikeInterpolator.getNodeSensitivitiesForValue(volatilityInterpolation, strike);
    SmileAndBucketedSensitivities smileAndSensitivities = getSmileAndSensitivitiesForTime(time, smileVolatilityBar);
    return VolatilityAndBucketedSensitivities.of(volatility, smileAndSensitivities.getSensitivities());
  }

  /**
   * Get the volatility from a triple.
   * @param tsf The Time, Strike, Forward triple, not null
   * @return The volatility.
   */
  public Double getVolatility(Triple<Double, Double, Double> tsf) {
    ArgChecker.notNull(tsf, "time/strike/forward triple");
    return getVolatility(tsf.getFirst(), tsf.getSecond(), tsf.getThird());
  }

  public VolatilityAndBucketedSensitivities getVolatilityAndSensitivities(Triple<Double, Double, Double> tsf) {
    ArgChecker.notNull(tsf, "time/strike/forward triple");
    return getVolatilityAndSensitivities(tsf.getFirst(), tsf.getSecond(), tsf.getThird());
  }

  /**
   * Gets the interpolator
   * @return The interpolator
   */
  public Interpolator1D getStrikeInterpolator() {
    return _strikeInterpolator;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = super.hashCode();
    result = prime * result + _strikeInterpolator.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SmileDeltaTermStructureParametersStrikeInterpolation other = (SmileDeltaTermStructureParametersStrikeInterpolation) obj;
    if (!Objects.equals(_strikeInterpolator, other._strikeInterpolator)) {
      return false;
    }
    return true;
  }

}
