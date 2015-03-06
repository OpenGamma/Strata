/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import com.opengamma.basics.currency.Currency;
import com.opengamma.util.ArgumentChecker;

public class ForwardRateSensitivity implements Comparable<ForwardRateSensitivity>{
  //TODO: Transform into ImmutableBean
  /** The name of the curve for which the sensitivity is computed. Not null. */
  private final String curveName;
  /** The time between valuation date and the fixing date of the forward rate. */
  private final double fixingTime;
  /** The time between valuation date and the start date of the forward rate period. */
  private final double startTime;
  /** The time between valuation date and the end date of the forward rate period. */
  private final double endTime;
  /** Accrual factor associated to the forward rate, in the day count of the index.*/
  private final double accrualFactor;
  /** The sensitivity value. */
  private final double value;
  /** The currency for the sensitivity. Not null. */
  private final Currency currency;
  
  public ForwardRateSensitivity(String curveName, double fixingTime, double startTime, double endTime, 
      double accrualFactor, double value, Currency currency) {
    ArgumentChecker.notNull(curveName, "curve name");
    ArgumentChecker.notNull(currency, "currency");
    this.curveName = curveName;
    this.fixingTime = fixingTime;
    this.startTime = startTime;
    this.endTime = endTime;
    this.accrualFactor = accrualFactor;
    this.value = value;
    this.currency = currency;
  }

  public String getCurveName() {
    return curveName;
  }

  public double getFixingTime() {
    return fixingTime;
  }

  public double getStartTime() {
    return startTime;
  }

  public double getEndTime() {
    return endTime;
  }

  public double getAccrualFactor() {
    return accrualFactor;
  }

  public double getValue() {
    return value;
  }

  public Currency getCurrency() {
    return currency;
  }

  @Override
  public int compareTo(ForwardRateSensitivity o) {
    int cmpsCurveName = curveName.compareTo(o.curveName);
    if(cmpsCurveName != 0) {
      return cmpsCurveName;
    }
    double cmpsFixingTime = Math.signum(fixingTime - o.fixingTime);
    if(cmpsFixingTime != 0.0) {
      return (int) cmpsFixingTime;
    }
    double cmpsStartTime = Math.signum(startTime - o.startTime);
    if(cmpsStartTime != 0.0) {
      return (int) cmpsStartTime;
    }
    double cmpsEndTime = Math.signum(endTime - o.endTime);
    if(cmpsEndTime != 0.0) {
      return (int) cmpsEndTime;
    }
    int cmpsCurrency = currency.compareTo(o.currency);
    if(cmpsCurrency != 0) {
      return cmpsCurrency;
    }
    return 0;
  }
  
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append(curveName).append(": ");
    buf.append('[').append(fixingTime).append(", ").append(startTime).append(", ").append(endTime).append(", ").append(accrualFactor).append("] ");
    buf.append(value).append(' ').append(currency);
    return buf.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(accrualFactor);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    result = prime * result + currency.hashCode();
    result = prime * result + curveName.hashCode();
    temp = Double.doubleToLongBits(endTime);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(fixingTime);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(startTime);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(value);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ForwardRateSensitivity other = (ForwardRateSensitivity) obj;
    if (Double.doubleToLongBits(accrualFactor) != Double.doubleToLongBits(other.accrualFactor))
      return false;
    if (currency == null) {
      if (other.currency != null)
        return false;
    } else if (!currency.equals(other.currency))
      return false;
    if (curveName == null) {
      if (other.curveName != null)
        return false;
    } else if (!curveName.equals(other.curveName))
      return false;
    if (Double.doubleToLongBits(endTime) != Double.doubleToLongBits(other.endTime))
      return false;
    if (Double.doubleToLongBits(fixingTime) != Double.doubleToLongBits(other.fixingTime))
      return false;
    if (Double.doubleToLongBits(startTime) != Double.doubleToLongBits(other.startTime))
      return false;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
      return false;
    return true;
  }
  
  

}
