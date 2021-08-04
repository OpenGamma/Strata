/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.ParameterizedData;

/**
 * A term structure of smile as used in Forex market.
 * <p>
 * The term structure defined here is composed of smile descriptions at different times.
 * The data of each smile contains delta and volatility in {@link SmileDeltaParameters}. 
 * The delta values must be common to all of the smiles.
 * <p>
 * The volatility and its sensitivities to data points are represented as a function of time, strike and forward.
 */
public interface SmileDeltaTermStructure
    extends ParameterizedData {

  /**
   * Gets the day count convention used for the expiry.
   * 
   * @return the day count
   */
  public abstract DayCount getDayCount();

  /**
   * Gets the number of smiles.
   * 
   * @return the number of smiles
   */
  public default int getSmileCount() {
    return getVolatilityTerm().size();
  }

  /**
   * Gets the number of strikes.
   * 
   * @return the number of strikes
   */
  public default int getStrikeCount() {
    return getVolatilityTerm().get(0).getVolatility().size();
  }

  /**
   * Gets delta values.
   * 
   * @return the delta values
   */
  public default DoubleArray getDelta() {
    return getVolatilityTerm().get(0).getDelta();
  }

  /**
   * Gets the volatility smiles from delta.
   * 
   * @return the volatility smiles
   */
  public abstract ImmutableList<SmileDeltaParameters> getVolatilityTerm();

  /**
   * Gets the expiries associated with the volatility term.
   * 
   * @return the set of expiry
   */
  public abstract DoubleArray getExpiries();

  /**
   * Gets the tenor associated with each expiry in the volatility term.
   * 
   * @return the list of tenors associated with the expiries, empty if not available
   */
  public default List<Optional<Tenor>> getExpiryTenors() {
    return Collections.nCopies(getExpiries().size(), Optional.empty());
  }

  /**
   * Computes full delta for all strikes including put delta absolute value.
   * <p>
   * The ATM is 0.50 delta and the x call are transformed in 1-x put.
   * 
   * @return the delta
   */
  public default DoubleArray getDeltaFull() {
    int nbDelta = getDelta().size();
    double[] result = new double[2 * nbDelta + 1];
    for (int loopdelta = 0; loopdelta < nbDelta; loopdelta++) {
      result[loopdelta] = getDelta().get(loopdelta);
      result[nbDelta + 1 + loopdelta] = 1.0 - getDelta().get(nbDelta - 1 - loopdelta);
    }
    result[nbDelta] = 0.50;
    return DoubleArray.ofUnsafe(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the volatility at a given time/strike/forward from the term structure.
   * 
   * @param expiry  the time to expiry
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility
   */
  public abstract double volatility(double expiry, double strike, double forward);

  /**
   * Calculates the volatility and the volatility sensitivity with respect to the volatility data points.
   * 
   * @param expiry  the time to expiry
   * @param strike  the strike
   * @param forward  the forward
   * @return the volatility
   */
  public abstract VolatilityAndBucketedSensitivities volatilityAndSensitivities(
      double expiry,
      double strike,
      double forward);

  /**
   * Computes the partial derivatives of the volatilities.
   * <p>
   * The first derivatives are {@code dVol/dExpiry and dVol/dStrike}.
   * The derivatives are in the following order:
   * <ul>
   * <li>[0] derivative with respect to expiry
   * <li>[1] derivative with respect to strike
   * </ul>
   *
   * @param expiry  the expiry at which the partial derivative is taken
   * @param strike  the strike at which the partial derivative is taken
   * @param forward  the forward rate
   * @return the z-value and it's partial first derivatives
   * @throws RuntimeException if the derivative cannot be calculated
   */
  public abstract ValueDerivatives partialFirstDerivatives(double expiry, double strike, double forward);

  /**
   * Calculates the smile at a given time.
   * 
   * @param expiry  the time to expiry
   * @return the smile
   */
  public abstract SmileDeltaParameters smileForExpiry(double expiry);

  /**
   * Calculates the smile at a given time and the sensitivities with respect to the volatility data points.
   * 
   * @param expiry  the time to expiry
   * @param volatilityAtTimeSensitivity  the sensitivity to the volatilities of the smile at the given time
   * @return the smile and sensitivities
   */
  public abstract SmileAndBucketedSensitivities smileAndSensitivitiesForExpiry(
      double expiry,
      DoubleArray volatilityAtTimeSensitivity);

  //-------------------------------------------------------------------------
  @Override
  public abstract SmileDeltaTermStructure withParameter(int parameterIndex, double newValue);

  @Override
  public abstract SmileDeltaTermStructure withPerturbation(ParameterPerturbation perturbation);

}
