/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import java.time.LocalDate;

import com.opengamma.basics.currency.Currency;
import com.opengamma.util.ArgumentChecker;

public class ZeroRateSensitivityLD implements Comparable<ZeroRateSensitivityLD> {
  //TODO: Transform into ImmutableBean?
  /** The name of the curve for which the sensitivity is computed. Not null. */
  private final Currency ccyDiscount; // TODO: do we need two currencies?
  /** The payment date. */
  private final LocalDate date;
  /** The sensitivity value. */
  private final double value;
  /** The currency for the sensitivity. Not null.*/
  private final Currency ccySensitivity;
  
  public ZeroRateSensitivityLD(Currency ccyDiscount, LocalDate date, double value, Currency ccySensitivity) {
    ArgumentChecker.notNull(ccyDiscount, "currency of the discounting");
    ArgumentChecker.notNull(ccySensitivity, "currency of the sensitiivty");
    this.ccyDiscount = ccyDiscount;
    this.date = date;
    this.value = value;
    this.ccySensitivity = ccySensitivity;
  }

  public Currency getCurrencyDiscount() {
    return ccyDiscount;
  }

  public LocalDate getDate() {
    return date;
  }

  public double getValue() {
    return value;
  }

  public Currency getCurrencySensitivity() {
    return ccySensitivity;
  }

  @Override
  public int compareTo(ZeroRateSensitivityLD o) {
    int cmpsCurveName = ccyDiscount.compareTo(o.ccyDiscount);
    if(cmpsCurveName != 0) {
      return cmpsCurveName;
    }
    int cmpsDate = date.compareTo(o.date);
    if(cmpsDate != 0) {
      return cmpsDate;
    }
    int cmpsCurrency = ccySensitivity.compareTo(o.ccySensitivity);
    if(cmpsCurrency != 0) {
      return cmpsCurrency;
    }
    return 0;
  }
  
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append(ccyDiscount).append(" - ");
    buf.append(date).append(": ");
    buf.append(value).append(' ').append(ccySensitivity);
    return buf.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((ccyDiscount == null) ? 0 : ccyDiscount.hashCode());
    result = prime * result + ((ccySensitivity == null) ? 0 : ccySensitivity.hashCode());
    result = prime * result + ((date == null) ? 0 : date.hashCode());
    long temp;
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
    ZeroRateSensitivityLD other = (ZeroRateSensitivityLD) obj;
    if (ccyDiscount == null) {
      if (other.ccyDiscount != null)
        return false;
    } else if (!ccyDiscount.equals(other.ccyDiscount))
      return false;
    if (ccySensitivity == null) {
      if (other.ccySensitivity != null)
        return false;
    } else if (!ccySensitivity.equals(other.ccySensitivity))
      return false;
    if (date == null) {
      if (other.date != null)
        return false;
    } else if (!date.equals(other.date))
      return false;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
      return false;
    return true;
  }

}
