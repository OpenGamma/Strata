/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.sensitivity.multicurve;

import java.time.LocalDate;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.index.RateIndex;
import com.opengamma.util.ArgumentChecker;

public class ForwardRateSensitivityLD implements Comparable<ForwardRateSensitivityLD>{
  //TODO: Transform into ImmutableBean
  /** The rate index for which the sensitivity is computed. Not null. */
  private final RateIndex index;
  /** The fixing date. Not null.*/
  private final LocalDate fixingDate; // TODO: for ON we may want to have start and end, not each daily sensitivity.
  /** The fixing period start date. If not null, override the standard date related to the index.*/
  private final LocalDate fixingPeriodStartDate; 
  /** The fixing period end date. If not null, override the standard date related to the index.*/
  private final LocalDate fixingPeriodEndDate; 
  /** The sensitivity value. */
  private final double value;
  /** The currency for the sensitivity. Not null. */
  private final Currency currency;
  
  public ForwardRateSensitivityLD(RateIndex index, LocalDate fixingDate, LocalDate fixingPeriodStartDate,
      LocalDate fixingPeriodEndDate, double value, Currency currency) {
    ArgumentChecker.notNull(index, "index");
    ArgumentChecker.notNull(fixingDate, "fixing date");
    ArgumentChecker.notNull(currency, "currency");
    this.index = index;
    this.fixingDate = fixingDate;
    this.fixingPeriodStartDate = fixingPeriodStartDate;
    this.fixingPeriodEndDate = fixingPeriodEndDate;
    this.value = value;
    this.currency = currency;
  }
  
  public ForwardRateSensitivityLD(RateIndex index, LocalDate fixingDate, double value, Currency currency) {
    ArgumentChecker.notNull(index, "index");
    ArgumentChecker.notNull(fixingDate, "fixing date");
    ArgumentChecker.notNull(currency, "currency");
    this.index = index;
    this.fixingDate = fixingDate;
    fixingPeriodStartDate = null;
    fixingPeriodEndDate = null;
    this.value = value;
    this.currency = currency;
  }

  public RateIndex getIndex() {
    return index;
  }

  public double getValue() {
    return value;
  }

  public Currency getCurrency() {
    return currency;
  }

  public LocalDate getFixingDate() {
    return fixingDate;
  }

  public LocalDate getFixingPeriodStartDate() {
    return fixingPeriodStartDate;
  }

  public LocalDate getFixingPeriodEndDate() {
    return fixingPeriodEndDate;
  }

  @Override
  public int compareTo(ForwardRateSensitivityLD o) {
    int cmpsCurveName = index.toString().compareTo(o.index.toString());
    if(cmpsCurveName != 0) {
      return cmpsCurveName;
    }
    int cmpsFixingDate = fixingDate.compareTo(o.fixingDate);
    if(cmpsFixingDate != 0) {
      return cmpsFixingDate;
    }
    int cmpsCurrency = currency.compareTo(o.currency);
    if(cmpsCurrency != 0) {
      return cmpsCurrency;
    }
    if(fixingPeriodStartDate == null && o.fixingPeriodStartDate != null) {
      return -1;
    }
    if(fixingPeriodStartDate != null && o.fixingPeriodStartDate == null) {
      return 1;
    }
    if(fixingPeriodStartDate != null && o.fixingPeriodStartDate != null) {
      int cmpsFixingStartDate = fixingPeriodStartDate.compareTo(o.fixingPeriodStartDate);
      if(cmpsFixingStartDate != 0) {
        return cmpsFixingStartDate;
      }
    }
    if(fixingPeriodEndDate == null && o.fixingPeriodEndDate != null) {
      return -1;
    }
    if(fixingPeriodEndDate != null && o.fixingPeriodEndDate == null) {
      return 1;
    }
    if(fixingPeriodEndDate != null && o.fixingPeriodEndDate != null) {
      int cmpsFixingEndDate = fixingPeriodEndDate.compareTo(o.fixingPeriodEndDate);
      if(cmpsFixingEndDate != 0) {
        return cmpsFixingEndDate;
      }
    }
    return 0;
  }
  
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append(index).append(" - ");
    buf.append(fixingDate).append(": ");
    buf.append(value).append(' ').append(currency);
    return buf.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + currency.hashCode();
    result = prime * result + fixingDate.hashCode();
    result = prime * result + index.hashCode();
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
    ForwardRateSensitivityLD other = (ForwardRateSensitivityLD) obj;
    if (currency == null) {
      if (other.currency != null)
        return false;
    } else if (!currency.equals(other.currency))
      return false;
    if (fixingDate == null) {
      if (other.fixingDate != null)
        return false;
    } else if (!fixingDate.equals(other.fixingDate))
      return false;
    if (index == null) {
      if (other.index != null)
        return false;
    } else if (!index.equals(other.index))
      return false;
    if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value))
      return false;
    return true;
  }

}
