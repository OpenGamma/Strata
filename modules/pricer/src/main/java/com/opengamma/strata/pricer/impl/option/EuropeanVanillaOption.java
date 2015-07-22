/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Simple representation of a European-style vanilla option.
 */
public final class EuropeanVanillaOption {

  private final double strike;
  private final double timeToExpiry;
  private final boolean isCall;

  public EuropeanVanillaOption(double strike, double timeToExpiry, boolean isCall) {
    ArgChecker.isTrue(timeToExpiry >= 0.0, "timeToExpiry must be >= 0.0");
    this.strike = strike;
    this.timeToExpiry = timeToExpiry;
    this.isCall = isCall;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the flag for call vs put.
   * 
   * @return true if call, false if put
   */
  public boolean isCall() {
    return isCall;
  }

  /**
   * Gets the time to expiry.
   * 
   * @return the time to expiry
   */
  public double getTimeToExpiry() {
    return timeToExpiry;
  }

  /**
   * Gets the strike.
   * 
   * @return the strike
   */
  public double getStrike() {
    return strike;
  }

  /**
   * Computes the pay-off for a spot price at expiry.
   * 
   * @param spot  the spot price
   * @return the pay-off
   */
  public double getPayoff(double spot) {
    return isCall() ? Math.max(0, spot - strike) : Math.max(0, strike - spot);
  }

  public EuropeanVanillaOption withStrike(double strike) {
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }

  public EuropeanVanillaOption withTimeToExpiry(double timeToExpiry) {
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }

  public EuropeanVanillaOption withIsCall(boolean isCall) {
    return new EuropeanVanillaOption(strike, timeToExpiry, isCall);
  }

  //-------------------------------------------------------------------------
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
    EuropeanVanillaOption other = (EuropeanVanillaOption) obj;
    if (isCall != other.isCall) {
      return false;
    }
    if (Double.doubleToLongBits(strike) != Double.doubleToLongBits(other.strike)) {
      return false;
    }
    if (Double.doubleToLongBits(timeToExpiry) != Double.doubleToLongBits(other.timeToExpiry)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + (isCall ? 1231 : 1237);
    long temp;
    temp = Double.doubleToLongBits(strike);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(timeToExpiry);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

}
