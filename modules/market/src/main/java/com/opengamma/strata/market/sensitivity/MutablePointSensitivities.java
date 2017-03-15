/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Mutable builder for sensitivity to a group of curves.
 * <p>
 * Contains a mutable list of {@linkplain PointSensitivity point sensitivity} objects, each
 * referring to a specific point on a curve that was queried.
 * The order of the list has no specific meaning, but does allow duplicates.
 * <p>
 * This is a mutable builder that is not intended for use in multiple threads.
 * It is intended to be used to create an immutable {@link PointSensitivities} instance.
 * Note that each individual point sensitivity implementation is immutable.
 */
public final class MutablePointSensitivities
    implements PointSensitivityBuilder {

  /**
   * The point sensitivities.
   * <p>
   * Each entry includes details of the curve it relates to.
   */
  private final List<PointSensitivity> sensitivities = new ArrayList<>();

  /**
   * Creates an empty instance.
   */
  public MutablePointSensitivities() {
  }

  /**
   * Creates an instance with the specified sensitivity.
   * 
   * @param sensitivity  the sensitivity to add
   */
  public MutablePointSensitivities(PointSensitivity sensitivity) {
    ArgChecker.notNull(sensitivity, "sensitivity");
    this.sensitivities.add(sensitivity);
  }

  /**
   * Creates an instance with the specified sensitivities.
   * 
   * @param sensitivities  the list of sensitivities, which is copied
   */
  public MutablePointSensitivities(List<? extends PointSensitivity> sensitivities) {
    ArgChecker.notNull(sensitivities, "sensitivities");
    this.sensitivities.addAll(sensitivities);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of sensitivity entries.
   * 
   * @return the size of the internal list of point sensitivities
   */
  public int size() {
    return sensitivities.size();
  }

  /**
   * Gets the immutable list of point sensitivities.
   * <p>
   * Each entry includes details of the curve it relates to.
   * 
   * @return the immutable list of sensitivities
   */
  public ImmutableList<PointSensitivity> getSensitivities() {
    return ImmutableList.copyOf(sensitivities);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a point sensitivity, mutating the internal list.
   * <p>
   * This instance will be mutated, with the new sensitivity added at the end of the list.
   * 
   * @param sensitivity  the sensitivity to add
   * @return {@code this}, for method chaining
   */
  public MutablePointSensitivities add(PointSensitivity sensitivity) {
    ArgChecker.notNull(sensitivity, "sensitivity");
    this.sensitivities.add(sensitivity);
    return this;
  }

  /**
   * Adds a list of point sensitivities, mutating the internal list.
   * <p>
   * This instance will be mutated, with the new sensitivities added at the end of the list.
   * 
   * @param sensitivities  the sensitivities to add
   * @return {@code this}, for method chaining
   */
  public MutablePointSensitivities addAll(List<PointSensitivity> sensitivities) {
    ArgChecker.notNull(sensitivities, "sensitivities");
    this.sensitivities.addAll(sensitivities);
    return this;
  }

  /**
   * Merges the list of point sensitivities from another instance, mutating the internal list.
   * <p>
   * This instance will be mutated, with the new sensitivities added at the end of the list.
   * 
   * @param other  the other sensitivity to add
   * @return {@code this}, for method chaining
   */
  public MutablePointSensitivities addAll(MutablePointSensitivities other) {
    this.sensitivities.addAll(other.sensitivities);
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public MutablePointSensitivities withCurrency(Currency currency) {
    sensitivities.replaceAll(ps -> ps.withCurrency(currency));
    return this;
  }

  @Override
  public MutablePointSensitivities multipliedBy(double factor) {
    return mapSensitivity(s -> s * factor);
  }

  @Override
  public MutablePointSensitivities mapSensitivity(DoubleUnaryOperator operator) {
    sensitivities.replaceAll(cs -> cs.withSensitivity(operator.applyAsDouble(cs.getSensitivity())));
    return this;
  }

  //-------------------------------------------------------------------------
  @Override
  public MutablePointSensitivities combinedWith(PointSensitivityBuilder other) {
    return other.buildInto(this);
  }

  @Override
  public MutablePointSensitivities buildInto(MutablePointSensitivities combination) {
    return (combination == this ? combination : combination.addAll(this));
  }

  @Override
  public PointSensitivities build() {
    return toImmutable();
  }

  @Override
  public MutablePointSensitivities cloned() {
    return new MutablePointSensitivities(new ArrayList<>(sensitivities));
  }

  //-------------------------------------------------------------------------
  /**
   * Sorts the mutable list of point sensitivities.
   * <p>
   * Sorts the point sensitivities in this instance.
   * 
   * @return {@code this}, for method chaining
   */
  public MutablePointSensitivities sort() {
    sensitivities.sort(PointSensitivity::compareKey);
    return this;
  }

  /**
   * Normalizes the point sensitivities by sorting and merging, mutating the internal list.
   * <p>
   * The list of sensitivities is sorted and then merged.
   * Any two entries that represent the same curve query are merged.
   * For example, if there are two point sensitivities that were created based on the same curve,
   * currency and fixing date, then the entries are combined, summing the sensitivity value.
   * <p>
   * The intention is that normalization occurs after gathering all the point sensitivities.
   * 
   * @return {@code this}, for method chaining
   */
  @Override
  public MutablePointSensitivities normalize() {
    sensitivities.sort(PointSensitivity::compareKey);
    PointSensitivity previous = sensitivities.get(0);
    for (int i = 1; i < sensitivities.size(); i++) {
      PointSensitivity current = sensitivities.get(i);
      if (current.compareKey(previous) == 0) {
        sensitivities.set(i - 1, previous.withSensitivity(previous.getSensitivity() + current.getSensitivity()));
        sensitivities.remove(i);
        i--;
      }
      previous = current;
    }
    return this;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns an immutable version of this object.
   * <p>
   * The result is an instance of the immutable {@link PointSensitivities}.
   * It will contain the same individual point sensitivities.
   * 
   * @return the immutable sensitivity instance, not null
   */
  public PointSensitivities toImmutable() {
    return PointSensitivities.of(sensitivities);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof MutablePointSensitivities) {
      MutablePointSensitivities other = (MutablePointSensitivities) obj;
      return sensitivities.equals(other.sensitivities);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return sensitivities.hashCode();
  }

  @Override
  public String toString() {
    return new StringBuilder(64)
        .append("MutablePointSensitivities{sensitivities=")
        .append(sensitivities)
        .append('}')
        .toString();
  }

}
