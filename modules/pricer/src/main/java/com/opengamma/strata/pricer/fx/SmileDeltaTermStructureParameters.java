/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.interpolator.CurveInterpolator;

/**
 * A term structure of smile as used in Forex market.
 * <p>
 * The term structure defined here is composed of smile descriptions at different times. 
 * The data of each smile contains delta and volatility in {@link SmileDeltaParameters}. 
 * The delta values must be common to all of the smiles. 
 * <p>
 * This class in particular computes interpolation in the time dimension given a set of interpolator and extrapolators. 
 * The interpolation for the other dimension (strike, delta, or other equivalent quantities) is performed in a subclass. 
 * <p>
 * The volatility and its sensitivities to data points are represented as a function of time, strike and forward. 
 */
public interface SmileDeltaTermStructureParameters {

  /**
   * Calculates the volatility at a given time/strike/forward from the term structure. 
   * 
   * @param expiry  the time to expiry
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility
   */
  public double getVolatility(double expiry, double strike, double forward);

  /**
   * Calculates the volatility and the volatility sensitivity with respect to the volatility data points.
   * 
   * @param expiry  the time to expiry
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility
   */
  public VolatilityAndBucketedSensitivities getVolatilityAndSensitivities(double expiry, double strike, double forward);

  /**
   * Creates a copy of the smile parameters term structure bundle. 
   * 
   * @return the copy
   */
  public SmileDeltaTermStructureParameters copy();

  /**
   * Obtains smile at a given time. 
   * <p>
   * The smile for the specified time is created by applying time interpolator and extrapolator. 
   * The smile is obtained from the data by the given interpolator.
   * 
   * @param expiry  the time to expiry
   * @return the smile
   */
  default public SmileDeltaParameters getSmileForTime(double expiry) {
    int nbVol = getNumberStrike();
    int nbTime = getNumberSmiles();
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = getVolatilityTerm().get(looptime).getVolatility().get(loopvol);
      }
      BoundCurveInterpolator bound = getTimeInterpolator().bind(
          getTimeToExpiry(), DoubleArray.copyOf(volDelta), getTimeLeftExtrapolator(), getTimeRightExtrapolator());
      volatilityT[loopvol] = bound.interpolate(expiry);
    }
    return SmileDeltaParameters.of(expiry, getDelta(), DoubleArray.copyOf(volatilityT));
  }

  /**
   * Obtains the smile at a given time and the sensitivities with respect to the volatility data points.
   * 
   * @param expiry  the time to expiry
   * @param volatilityAtTimeSensitivity  the sensitivity to the volatilities of the smile at the given time
   * @return the smile and sensitivities
   */
  default public SmileAndBucketedSensitivities getSmileAndSensitivitiesForTime(
      double expiry,
      DoubleArray volatilityAtTimeSensitivity) {

    int nbVol = getNumberStrike();
    ArgChecker.isTrue(volatilityAtTimeSensitivity.size() == nbVol, "Sensitivity with incorrect size");
    ArgChecker.isTrue(nbVol > 1, "Need more than one volatility value to perform interpolation");
    int nbTime = getNumberSmiles();
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    double[][] volatilitySensitivity = new double[nbTime][nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = getVolatilityTerm().get(looptime).getVolatility().get(loopvol);
      }
      BoundCurveInterpolator bound = getTimeInterpolator().bind(
          getTimeToExpiry(), DoubleArray.copyOf(volDelta), getTimeLeftExtrapolator(), getTimeRightExtrapolator());
      double[] volatilitySensitivityVol = bound.parameterSensitivity(expiry).toArray();
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volatilitySensitivity[looptime][loopvol] =
            volatilitySensitivityVol[looptime] * volatilityAtTimeSensitivity.get(loopvol);
      }
      volatilityT[loopvol] = bound.interpolate(expiry);
    }
    SmileDeltaParameters smile = SmileDeltaParameters.of(expiry, getDelta(), DoubleArray.copyOf(volatilityT));
    return SmileAndBucketedSensitivities.of(smile, DoubleMatrix.copyOf(volatilitySensitivity));
  }

  /**
   * Gets the number of smiles.
   * 
   * @return the number of smiles
   */
  default int getNumberSmiles() {
    return getVolatilityTerm().size();
  }

  /**
   * Gets the number of strikes. 
   * 
   * @return the number of strikes
   */
  default int getNumberStrike() {
    return getVolatilityTerm().get(0).getVolatility().size();
  }

  /**
   * Gets delta values.
   * 
   * @return the delta values
   */
  default DoubleArray getDelta() {
    return getVolatilityTerm().get(0).getDelta();
  }

  /**
   * Gets a set of expiry for smiles.
   * 
   * @return the set of expiry
   */
  public DoubleArray getTimeToExpiry();

  /**
   * Gets the time left extrapolator. 
   * 
   * @return the time left extrapolator
   */
  public CurveExtrapolator getTimeLeftExtrapolator();

  /**
   * Gets the time interpolator. 
   * 
   * @return the time interpolator
   */
  public CurveInterpolator getTimeInterpolator();

  /**
   * Gets the time right extrapolator. 
   * 
   * @return the time right extrapolator
   */
  public CurveExtrapolator getTimeRightExtrapolator();

  /**
   * Gets the volatility smiles from delta.
   * 
   * @return the volatility smiles
   */
  public ImmutableList<SmileDeltaParameters> getVolatilityTerm();

  /**
   * Gets the name.
   * 
   * @return the name
   */
  public String getName();

  /**
   * Computes full delta for all strikes including put delta absolute value. 
   * <p>
   * The ATM is 0.50 delta and the x call are transformed in 1-x put.
   * 
   * @return the delta
   */
  default public double[] getDeltaFull() {
    int nbDelta = getDelta().size();
    double[] result = new double[2 * nbDelta + 1];
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      result[loopdelta] = getDelta().get(loopdelta);
      result[nbDelta + 1 + loopdelta] = 1.0 - getDelta().get(nbDelta - 1 - loopdelta);
    }
    result[nbDelta] = 0.50;
    return result;
  }

}
