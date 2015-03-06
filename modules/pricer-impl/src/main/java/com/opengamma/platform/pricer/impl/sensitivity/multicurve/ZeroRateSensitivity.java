/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import com.opengamma.basics.currency.Currency;
import com.opengamma.util.ArgumentChecker;

public class ZeroRateSensitivity implements Comparable<ZeroRateSensitivity>{
  //TODO: Transform into ImmutableBean?
  /** The name of the curve for which the sensitivity is computed. Not null. */
  private final String curveName;
  /** The time between valuation date and the date for which the sensitivity is computed. */
  private final double time;
  /** The sensitivity value. */
  private final double value;
  /** The currency for the sensitivity. Not null.*/
  private final Currency currency;
  
  public ZeroRateSensitivity(String curveName, double time, double value, Currency currency) {
    ArgumentChecker.notNull(curveName, "curve name");
    ArgumentChecker.notNull(currency, "currency");
    this.curveName = curveName;
    this.time = time;
    this.value = value;
    this.currency = currency;
  }

  public String getCurveName() {
    return curveName;
  }

  public double getTime() {
    return time;
  }

  public double getValue() {
    return value;
  }

  public Currency getCurrency() {
    return currency;
  }

  @Override
  public int compareTo(ZeroRateSensitivity o) {
    int cmpsCurveName = curveName.compareTo(o.curveName);
    if(cmpsCurveName != 0) {
      return cmpsCurveName;
    }
    if(time < o.time) {
      return -1;
    }
    if(time > o.time) {
      return 1;
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
    buf.append(time).append(", ");
    buf.append(value).append(' ').append(currency);
    return buf.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + currency.hashCode();
    result = prime * result +  curveName.hashCode();
    long temp;
    temp = Double.doubleToLongBits(time);
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
    ZeroRateSensitivity other = (ZeroRateSensitivity) obj;
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
    if (Double.doubleToLongBits(time) != Double.doubleToLongBits(other.time))
      return false;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
      return false;
    return true;
  }

}
