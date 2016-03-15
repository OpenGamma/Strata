/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.interpolator.BoundCurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.interpolator.CurveInterpolator;

/**
 * An interpolated term structure of smile as used in Forex market.
 * <p>
 * The term structure defined here is composed of smile descriptions at different times. 
 * The data of each smile contains delta and volatility in {@link SmileDeltaParameters}. 
 * The delta values must be common to all of the smiles. 
 * <p>
 * This interface in particular defines interpolation in the time dimension given a set of interpolator and extrapolators. 
 * Smile construction methodology of each time slice is specified in a subclass.
 */
public interface InterpolatedSmileDeltaTermStructure extends SmileDeltaTermStructure {

  @Override
  public default SmileDeltaParameters smileForTime(double expiry) {
    int nbVol = getStrikeCount();
    int nbTime = getSmileCount();
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = getVolatilityTerm().get(looptime).getVolatility().get(loopvol);
      }
      BoundCurveInterpolator bound = getTimeInterpolator().bind(
          getTimeToExpiry(), DoubleArray.ofUnsafe(volDelta), getTimeLeftExtrapolator(), getTimeRightExtrapolator());
      volatilityT[loopvol] = bound.interpolate(expiry);
    }
    return SmileDeltaParameters.of(expiry, getDelta(), DoubleArray.ofUnsafe(volatilityT));
  }

  @Override
  public default SmileAndBucketedSensitivities smileAndSensitivitiesForTime(
      double expiry,
      DoubleArray volatilityAtTimeSensitivity) {

    int nbVol = getStrikeCount();
    ArgChecker.isTrue(volatilityAtTimeSensitivity.size() == nbVol, "Sensitivity with incorrect size");
    ArgChecker.isTrue(nbVol > 1, "Need more than one volatility value to perform interpolation");
    int nbTime = getSmileCount();
    ArgChecker.isTrue(nbTime > 1, "Need more than one time value to perform interpolation");
    double[] volatilityT = new double[nbVol];
    double[][] volatilitySensitivity = new double[nbTime][nbVol];
    for (int loopvol = 0; loopvol < nbVol; loopvol++) {
      double[] volDelta = new double[nbTime];
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volDelta[looptime] = getVolatilityTerm().get(looptime).getVolatility().get(loopvol);
      }
      BoundCurveInterpolator bound = getTimeInterpolator().bind(
          getTimeToExpiry(), DoubleArray.ofUnsafe(volDelta), getTimeLeftExtrapolator(), getTimeRightExtrapolator());
      DoubleArray volatilitySensitivityVol = bound.parameterSensitivity(expiry);
      for (int looptime = 0; looptime < nbTime; looptime++) {
        volatilitySensitivity[looptime][loopvol] =
            volatilitySensitivityVol.get(looptime) * volatilityAtTimeSensitivity.get(loopvol);
      }
      volatilityT[loopvol] = bound.interpolate(expiry);
    }
    SmileDeltaParameters smile = SmileDeltaParameters.of(expiry, getDelta(), DoubleArray.ofUnsafe(volatilityT));
    return SmileAndBucketedSensitivities.of(smile, DoubleMatrix.ofUnsafe(volatilitySensitivity));
  }

  /**
   * Gets the time left extrapolator. 
   * 
   * @return the time left extrapolator
   */
  public abstract CurveExtrapolator getTimeLeftExtrapolator();

  /**
   * Gets the time interpolator. 
   * 
   * @return the time interpolator
   */
  public abstract CurveInterpolator getTimeInterpolator();

  /**
   * Gets the time right extrapolator. 
   * 
   * @return the time right extrapolator
   */
  public abstract CurveExtrapolator getTimeRightExtrapolator();

}
