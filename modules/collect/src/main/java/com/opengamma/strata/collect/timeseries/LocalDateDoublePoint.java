/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import java.time.LocalDate;

import org.joda.beans.JodaBeanUtils;

import com.google.common.collect.ComparisonChain;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Immutable representation of a single point in a {@code LocalDateDoubleTimeSeries}.
 * <p>
 * This implementation uses arrays internally.
 */
public final class LocalDateDoublePoint
    implements Comparable<LocalDateDoublePoint> {

  /**
   * The date.
   */
  private final LocalDate date;
  /**
   * The value.
   */
  private final double value;

  //-------------------------------------------------------------------------
  /**
   * Obtains a point from date and value.
   *
   * @param date  the date
   * @param value  the value
   * @return the point
   */
  public static LocalDateDoublePoint of(LocalDate date, double value) {
    return new LocalDateDoublePoint(date, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   *
   * @param date  the date
   * @param value  the value
   */
  private LocalDateDoublePoint(LocalDate date, double value) {
    this.date = ArgChecker.notNull(date, "date");
    this.value = value;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the date.
   *
   * @return the date
   */
  public LocalDate getDate() {
    return date;
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  public double getValue() {
    return value;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this point with another date.
   * 
   * @param date  the date to change the point to
   * @return a point based on this point with the date changed
   */
  public LocalDateDoublePoint withDate(LocalDate date) {
    return LocalDateDoublePoint.of(date, value);
  }

  /**
   * Returns a copy of this point with another value.
   * 
   * @param value  the value to change the point to
   * @return a point based on this point with the value changed
   */
  public LocalDateDoublePoint withValue(double value) {
    return LocalDateDoublePoint.of(date, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this point to another.
   * <p>
   * The sort order is by date, then by double.
   * This is compatible with equals.
   * 
   * @param other  the other point
   * @return negative if this is less than, zero if equal, positive if greater than
   */
  @Override
  public int compareTo(LocalDateDoublePoint other) {
    return ComparisonChain.start()
        .compare(date, other.date)
        .compare(value, other.value)
        .result();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this point is equal to another point.
   *
   * @param obj  the object to check, null returns false
   * @return true if this is equal to the other point
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof LocalDateDoublePoint) {
      LocalDateDoublePoint other = (LocalDateDoublePoint) obj;
      return date.equals(other.date) && JodaBeanUtils.equal(value, other.value);
    }
    return false;
  }

  /**
   * A hash code for this point.
   *
   * @return a suitable hash code
   */
  @Override
  public int hashCode() {
    return date.hashCode() ^ JodaBeanUtils.hashCode(value);
  }

  /**
   * Returns a string representation of the point.
   * 
   * @return the string
   */
  @Override
  public String toString() {
    return new StringBuilder(24)
        .append('(')
        .append(date)
        .append('=')
        .append(value)
        .append(')')
        .toString();
  }

}
