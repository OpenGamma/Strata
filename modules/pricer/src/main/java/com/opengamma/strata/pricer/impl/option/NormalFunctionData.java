/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

/**
 * A data bundle with the data require for the normal option model (Bachelier model).
 */
public final class NormalFunctionData {

  /**
   * The forward.
   */
  private final double forward;
  /**
   * The numeraire.
   */
  private final double numeraire;
  /**
   * The normal volatility.
   */
  private final double volatility;

  /**
   * Data bundle for pricing in a normal framework.
   * That is, the forward value of the underlying asset is a martingale in the chosen numeraire measure.
   * 
   * @param forward  the forward value of the underlying asset, such as forward value of a stock, or forward Libor rate
   * @param numeraire  the numeraire associated to the equation
   * @param sigma  the normal volatility
   */
  public NormalFunctionData(final double forward, final double numeraire, final double sigma) {
    this.forward = forward;
    this.numeraire = numeraire;
    this.volatility = sigma;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the forward value of the underlying asset.
   * 
   * @return the forward value of the underlying asset
   */
  public double getForward() {
    return forward;
  }

  /**
   * Gets the numeraire associated with the equation.
   * 
   * @return the numeraire associated with the equation
   */
  public double getNumeraire() {
    return numeraire;
  }

  /**
   * Gets the normal volatility.
   * 
   * @return the normal volatility
   */
  public double getNormalVolatility() {
    return volatility;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NormalFunctionData other = (NormalFunctionData) obj;
    if (Double.doubleToLongBits(numeraire) != Double.doubleToLongBits(other.numeraire)) {
      return false;
    }
    if (Double.doubleToLongBits(forward) != Double.doubleToLongBits(other.forward)) {
      return false;
    }
    return Double.doubleToLongBits(volatility) == Double.doubleToLongBits(other.volatility);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(numeraire);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(forward);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(volatility);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("NormalFunctionData[");
    sb.append("forward=");
    sb.append(forward);
    sb.append(", numeraire=");
    sb.append(numeraire);
    sb.append(", volatility=");
    sb.append(volatility);
    sb.append("]");
    return sb.toString();
  }

}
