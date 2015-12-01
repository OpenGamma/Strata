/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.math.impl.interpolation.CombinedInterpolatorExtrapolator;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Defines a term structure of smiles as used in Forex market.
 * <p>
 * This contains smile from at-the-money, risk reversal and strangle.
 * The delta used is the delta with respect to forward.
 */
class SmileDeltaTermStructureParameters {
  // NOTE: This class is package scoped, as the Smile data provider API is effectively still in Beta

  /**
   * Atomic used to generate a name.
   */
  private static final AtomicLong ATOMIC = new AtomicLong();
  /**
   * The name of smile parameter term structure.
   */
  private final String _name;
  /**
   * The time to expiry in the term structure.
   */
  private final double[] _timeToExpiry;
  /**
   * The smile description at the different time to expiry. All item should have the same deltas.
   */
  private final SmileDeltaParameters[] _volatilityTerm;
  /**
   * The interpolator/extrapolator used in the expiry dimension.
   */
  private final Interpolator1D _timeInterpolator;

  /**
   * The default interpolator: time square (total variance) with flat extrapolation.
   */
  private static final Interpolator1D DEFAULT_INTERPOLATOR_EXPIRY = CombinedInterpolatorExtrapolator.of(
      CurveInterpolators.TIME_SQUARE.getName(),
      CurveExtrapolators.FLAT.getName(),
      CurveExtrapolators.FLAT.getName());

  /**
   * Constructor from volatility term structure.
   * 
   * @param volatilityTerm The volatility description at the different expiry.
   */
  public SmileDeltaTermStructureParameters(SmileDeltaParameters[] volatilityTerm) {
    this(Long.toString(ATOMIC.getAndIncrement()), volatilityTerm, DEFAULT_INTERPOLATOR_EXPIRY);
  }

  /**
   * Constructor from name and volatility term structure.
   * <p>
   * The default interpolator is used to interpolate in the time dimension. 
   * 
   * @param name  the name of the smile parameter term structure
   * @param volatilityTerm The volatility description at the different expiry.
   */
  public SmileDeltaTermStructureParameters(String name, SmileDeltaParameters[] volatilityTerm) {
    this(name, volatilityTerm, DEFAULT_INTERPOLATOR_EXPIRY);
  }

  /**
   * Constructor from volatility term structure and time interpolator.
   * <p>
   * The default interpolator is used to interpolate in the time dimension. 
   * 
   * @param volatilityTerm  the volatility description at the different expiry.
   * @param interpolator  the time interpolator
   */
  public SmileDeltaTermStructureParameters(SmileDeltaParameters[] volatilityTerm, Interpolator1D interpolator) {
    this(Long.toString(ATOMIC.getAndIncrement()), volatilityTerm, interpolator);
  }

  /**
   * Constructor from name, volatility term structure and time interpolator.
   * 
   * @param name  the name of the smile parameter term structure
   * @param volatilityTerm  the volatility description at the different expiry
   * @param interpolator  the time interpolator
   */
  public SmileDeltaTermStructureParameters(
      String name,
      SmileDeltaParameters[] volatilityTerm,
      Interpolator1D interpolator) {
    _timeInterpolator = ArgChecker.notNull(interpolator, "interpolator");
    _volatilityTerm = ArgChecker.notNull(volatilityTerm, "volatilityTerm");
    _name = ArgChecker.notNull(name, "name");
    int nbExp = volatilityTerm.length;
    _timeToExpiry = new double[nbExp];
    for (int loopexp = 0; loopexp < nbExp; loopexp++) {
      _timeToExpiry[loopexp] = _volatilityTerm[loopexp].getTimeToExpiry();
    }
  }

  /**
   * Constructor from market data.
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiry}
   * and {@code m} is the length of {@code delta}.
   * <p>
   * The default interpolator is used to interpolate in the time dimension. 
   * 
   * @param timeToExpiry  the time to expiry of each volatility smile
   * @param delta  the delta at which the volatilities are given 
   * @param volatility  the volatilities at each delta 
   */
  public SmileDeltaTermStructureParameters(double[] timeToExpiry, double[] delta, double[][] volatility) {
    this(Long.toString(ATOMIC.getAndIncrement()), timeToExpiry, delta, volatility);
  }

  /**
   * Constructor from name and market data.
   * <p>
   * The market date consists of time to expiry, delta and volatility.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code volatility} should be {@code n * (2 * m + 1)}, where {@code n} is the length of {@code timeToExpiry}
   * and {@code m} is the length of {@code delta}.
   * <p>
   * The default interpolator is used to interpolate in the time dimension. 
   * 
   * @param name  the name of smile parameter term structure 
   * @param timeToExpiry  the time to expiry of each volatility smile
   * @param delta  the delta at which the volatilities are given
   * @param volatility  the volatilities at each delta 
   */
  public SmileDeltaTermStructureParameters(String name, double[] timeToExpiry, double[] delta, double[][] volatility) {
    ArgChecker.notNull(timeToExpiry, "time to expiry");
    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(volatility, "volatility");
    _name = ArgChecker.notNull(name, "name");
    _timeToExpiry = ArgChecker.notNull(timeToExpiry, "timeToExpiry");
    int nbExp = timeToExpiry.length;
    ArgChecker.isTrue(volatility.length == nbExp,
        "Volatility array length {} should be equal to the number of expiries {}", volatility.length, nbExp);
    ArgChecker.isTrue(volatility[0].length == 2 * delta.length + 1,
        "Volatility array {} should be equal to (2 * number of deltas) + 1, have {}",
        volatility[0].length, 2 * delta.length + 1);
    _volatilityTerm = new SmileDeltaParameters[nbExp];
    for (int loopexp = 0; loopexp < nbExp; loopexp++) {
      _volatilityTerm[loopexp] = SmileDeltaParameters.of(
          timeToExpiry[loopexp], DoubleArray.copyOf(delta), DoubleArray.copyOf(volatility[loopexp]));
    }
    _timeInterpolator = DEFAULT_INTERPOLATOR_EXPIRY;
    ArgChecker.isTrue(_volatilityTerm[0].getVolatility().size() > 1,
        "Need more than one volatility value to perform interpolation");
  }

  /**
   * Constructor from market data.
   * <p>
   * The market data consists of time to expiry, delta, ATM volatilities, risk reversal figures and
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiry} and {@code m} is the length of {@code delta}.
   * <p>
   * The default interpolator is used to interpolate in the time dimension. 
   * 
   * @param timeToExpiry  the time to expiry of each volatility smile
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiry
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   */
  public SmileDeltaTermStructureParameters(
      double[] timeToExpiry,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle) {
    this(Long.toString(ATOMIC.getAndIncrement()), timeToExpiry, delta, atm, riskReversal, strangle);
  }

  /**
   * Constructor from name and market data.
   * <p>
   * The market data consists of time to expiry, delta, ATM volatilities, risk reversal figures and
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiry} and {@code m} is the length of {@code delta}.
   * <p>
   * The default interpolator is used to interpolate in the time dimension. 
   * 
   * @param name  the name of smile parameter term structure 
   * @param timeToExpiry  the time to expiry of each volatility smile
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiry
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   */
  public SmileDeltaTermStructureParameters(
      String name,
      double[] timeToExpiry,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle) {
    this(name, timeToExpiry, delta, atm, riskReversal, strangle, DEFAULT_INTERPOLATOR_EXPIRY);
  }

  /**
   * Constructor from market data and time interpolator.
   * <p>
   * The market data consists of time to expiry, delta, ATM volatilities, risk reversal figures and
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiry} and {@code m} is the length of {@code delta}.
   * 
   * @param timeToExpiry  the time to expiry of each volatility smile
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiry
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   * @param timeInterpolator  the interpolator to be used in the time dimension
   */
  public SmileDeltaTermStructureParameters(
      double[] timeToExpiry,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle,
      Interpolator1D timeInterpolator) {
    this(Long.toString(ATOMIC.getAndIncrement()), timeToExpiry, delta, atm, riskReversal, strangle,
        timeInterpolator);
  }

  /**
   * Constructor from name, market data and time interpolator.
   * <p>
   * The market data consists of time to expoiration, delta, ATM volatilities, risk reversal figures and 
   * strangle figures.
   * <p>
   * The range of delta is common to all time to expiry.
   * {@code riskReversal} and {@code strangle} should be {@code n * m}, and the length of {@code atm} should {@code n}, 
   * where {@code n} is the length of {@code timeToExpiry} and {@code m} is the length of {@code delta}.
   * 
   * @param name  the name of the smile parameter term structure 
   * @param timeToExpiry  the time to expiry of each volatility smile
   * @param delta  the delta at which the volatilities are given 
   * @param atm  the ATM volatilities for each time to expiry
   * @param riskReversal  the risk reversal figures
   * @param strangle  the strangle figure
   * @param timeInterpolator  the interpolator to be used in the time dimension
   */
  public SmileDeltaTermStructureParameters(
      String name,
      double[] timeToExpiry,
      double[] delta,
      double[] atm,
      double[][] riskReversal,
      double[][] strangle,
      Interpolator1D timeInterpolator) {
    ArgChecker.notNull(delta, "delta");
    ArgChecker.notNull(atm, "ATM");
    ArgChecker.notNull(riskReversal, "risk reversal");
    ArgChecker.notNull(strangle, "strangle");
    _timeInterpolator = ArgChecker.notNull(timeInterpolator, "timeInterpolator");
    _name = ArgChecker.notNull(name, "name");
    _timeToExpiry = ArgChecker.notNull(timeToExpiry, "timeToExpiry");
    int nbExp = timeToExpiry.length;
    ArgChecker.isTrue(atm.length == nbExp, "ATM length should be coherent with time to expiry length");
    ArgChecker.isTrue(riskReversal.length == nbExp,
        "Risk reversal length should be coherent with time to expiry length");
    ArgChecker.isTrue(strangle.length == nbExp, "Strangle length should be coherent with time to expiry length");
    ArgChecker.isTrue(riskReversal[0].length == delta.length,
        "Risk reversal size should be coherent with time to delta length");
    ArgChecker.isTrue(strangle[0].length == delta.length, "Strangle size should be coherent with time to delta length");
    _volatilityTerm = new SmileDeltaParameters[nbExp];
    for (int loopexp = 0; loopexp < nbExp; loopexp++) {
      _volatilityTerm[loopexp] = SmileDeltaParameters.of(
          timeToExpiry[loopexp],
          atm[loopexp],
          DoubleArray.copyOf(delta),
          DoubleArray.copyOf(riskReversal[loopexp]),
          DoubleArray.copyOf(strangle[loopexp]));
    }
    ArgChecker.isTrue(_volatilityTerm[0].getVolatility().size() > 1,
        "Need more than one volatility value to perform interpolation");
  }

  /**
   * Obtains a copy of this {@link SmileDeltaTermStructureParameters}. 
   * 
   * @return the copy
   */
  public SmileDeltaTermStructureParameters copy() {
    return new SmileDeltaTermStructureParameters(getName(), getVolatilityTerm(), getTimeInterpolator());
  }

  /**
   * Get smile at a given time. The smile is described by the volatilities at a given delta. The smile is obtained from the data by the given interpolator.
   * @param time The time to expiry.
   * @return The smile.
   */
  public SmileDeltaParameters getSmileForTime(double time) {
    int nbVol = _volatilityTerm[0].getVolatility().size();
    int nbTime = _timeToExpiry.length;
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = _volatilityTerm[looptime].getVolatility().get(loopvol);
      }
      Interpolator1DDataBundle interpData =
          _timeInterpolator.getDataBundleFromSortedArrays(_timeToExpiry, volDelta);
      volatilityT[loopvol] = _timeInterpolator.interpolate(interpData, time);
    }
    SmileDeltaParameters smile = SmileDeltaParameters.of(
        time, _volatilityTerm[0].getDelta(), DoubleArray.copyOf(volatilityT));
    return smile;
  }

  /**
   * Get the smile at a given time and the sensitivities with respect to the volatilities.
   * @param time The time to expiry.
   * @param volatilityAtTimeSensitivity The sensitivity to the volatilities of the smile at the given time.
   * After the methods, it contains the volatility sensitivity to the data points.
   * @return The smile
   */
  public SmileAndBucketedSensitivities getSmileAndSensitivitiesForTime(double time, double[] volatilityAtTimeSensitivity) {
    int nbVol = _volatilityTerm[0].getVolatility().size();
    ArgChecker.isTrue(volatilityAtTimeSensitivity.length == nbVol, "Sensitivity with incorrect size");
    ArgChecker.isTrue(nbVol > 1, "Need more than one volatility value to perform interpolation");
    int nbTime = _timeToExpiry.length;
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    double[][] volatilitySensitivity = new double[nbTime][nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = _volatilityTerm[looptime].getVolatility().get(loopvol);
      }
      Interpolator1DDataBundle interpData =
          _timeInterpolator.getDataBundleFromSortedArrays(_timeToExpiry, volDelta);
      double[] volatilitySensitivityVol = _timeInterpolator.getNodeSensitivitiesForValue(interpData, time);
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volatilitySensitivity[looptime][loopvol] = volatilitySensitivityVol[looptime] * volatilityAtTimeSensitivity[loopvol];
      }
      volatilityT[loopvol] = _timeInterpolator.interpolate(interpData, time);
    }
    SmileDeltaParameters smile = SmileDeltaParameters.of(time, _volatilityTerm[0].getDelta(), DoubleArray.copyOf(volatilityT));
    return SmileAndBucketedSensitivities.of(smile, DoubleMatrix.copyOf(volatilitySensitivity));
  }

  /**
   * Gets the name of smile parameter term structure.
   * 
   * @return the name
   */
  public String getName() {
    return _name;
  }

  /**
   * Gets the times to expiry.
   * @return The times.
   */
  public double[] getTimeToExpiry() {
    return _timeToExpiry;
  }

  /**
   * Gets the number of expirys.
   * @return The number of expirys.
   */
  public int getNumberExpiry() {
    return _timeToExpiry.length;
  }

  /**
   * Gets the time interpolator
   * @return The time interpolator
   */
  public Interpolator1D getTimeInterpolator() {
    return _timeInterpolator;
  }

  /**
   * Gets the volatility smiles from delta.
   * @return The volatility smiles.
   */
  public SmileDeltaParameters[] getVolatilityTerm() {
    return _volatilityTerm;
  }

  /**
   * Gets the number of strikes (common to all dates).
   * @return The number of strikes.
   */
  public int getNumberStrike() {
    return _volatilityTerm[0].getVolatility().size();
  }

  /**
   * Gets delta (common to all time to expiry).
   * @return The delta.
   */
  public double[] getDelta() {
    return _volatilityTerm[0].getDelta().toArray();
  }

  /**
   * Gets put delta absolute value for all strikes. The ATM is 0.50 delta and the x call are transformed in 1-x put.
   * @return The delta.
   */
  public double[] getDeltaFull() {
    int nbDelta = _volatilityTerm[0].getDelta().size();
    double[] result = new double[2 * nbDelta + 1];
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      result[loopdelta] = _volatilityTerm[0].getDelta().get(loopdelta);
      result[nbDelta + 1 + loopdelta] = 1.0 - _volatilityTerm[0].getDelta().get(nbDelta - 1 - loopdelta);
    }
    result[nbDelta] = 0.50;
    return result;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + _name.hashCode();
    result = prime * result + Arrays.hashCode(_timeToExpiry);
    result = prime * result + Arrays.hashCode(_volatilityTerm);
    result = prime * result + _timeInterpolator.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    SmileDeltaTermStructureParameters other = (SmileDeltaTermStructureParameters) obj;
    if (!Objects.equals(_name, other._name)) {
      return false;
    }
    if (!Arrays.equals(_timeToExpiry, other._timeToExpiry)) {
      return false;
    }
    if (!Arrays.equals(_volatilityTerm, other._volatilityTerm)) {
      return false;
    }
    if (!Objects.equals(_timeInterpolator, other._timeInterpolator)) {
      return false;
    }
    return true;
  }

}
