/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import com.google.common.collect.ImmutableList;
import com.opengamma.collect.ArgChecker;

/**
 * Mutable builder for sensitivity to a group of curves.
 * <p>
 * Contains a mutable list of {@linkplain CurveSensitivity point sensitivity} objects, each
 * referring to a specific point on a curve that was queried.
 * <p>
 * This is a mutable builder that is not intended for use in multiple threads.
 * It is intended to be used to create an immutable {@link CurveGroupSensitivity} instance.
 * Note that each individual point sensitivity implementation is immutable.
 */
public final class MutableCurveGroupSensitivity {

  /**
   * The point sensitivities.
   * <p>
   * Each entry includes details of the curve it relates to.
   */
  private final List<CurveSensitivity> sensitivities = new ArrayList<>();

  /**
   * Creates an empty instance.
   */
  public MutableCurveGroupSensitivity() {
  }

  /**
   * Creates an instance with the specified sensitivities.
   * 
   * @param sensitivities  the list of sensitivities
   */
  public MutableCurveGroupSensitivity(List<? extends CurveSensitivity> sensitivities) {
    ArgChecker.notNull(sensitivities, "sensitivities");
    this.sensitivities.addAll(sensitivities);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of point sensitivities.
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
  public ImmutableList<CurveSensitivity> getSensitivities() {
    return ImmutableList.copyOf(sensitivities);
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a point sensitivity, mutating the internal list.
   * <p>
   * This instance will be mutated, with the new sensitivity added at the end of the list.
   * 
   * @param sensitivity  the sensitivity to add
   */
  public void add(CurveSensitivity sensitivity) {
    ArgChecker.notNull(sensitivity, "sensitivity");
    this.sensitivities.add(sensitivity);
  }

  /**
   * Adds a list of point sensitivities, mutating the internal list.
   * <p>
   * This instance will be mutated, with the new sensitivities added at the end of the list.
   * 
   * @param sensitivities  the sensitivities to add
   */
  public void addAll(List<CurveSensitivity> sensitivities) {
    ArgChecker.notNull(sensitivities, "sensitivities");
    this.sensitivities.addAll(sensitivities);
  }

  /**
   * Merges the list of point sensitivities from another instance, mutating the internal list.
   * <p>
   * This instance will be mutated, with the new sensitivities added at the end of the list.
   * 
   * @param other  the group sensitivity to add
   */
  public void merge(MutableCurveGroupSensitivity other) {
    this.sensitivities.addAll(other.sensitivities);
  }

  //-------------------------------------------------------------------------
  /**
   * Multiplies the point sensitivities by the specified factor, mutating the internal list.
   * <p>
   * This instance will be mutated, with each existing sensitivity multiplied.
   * 
   * @param factor  the multiplicative factor
   */
  public void multipliedBy(double factor) {
    mapSensitivities(s -> s * factor);
  }

  /**
   * Applies an operation to the point sensitivities, mutating the internal list.
   * <p>
   * This instance will be mutated, with the operator applied to each existing sensitivity.
   * <p>
   * This is used to apply a mathematical operation to the sensitivities.
   * For example, the operator could multiply the sensitivities by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.mapAmount(value -> value * 3);
   * </pre>
   *
   * @param operator  the operator to be applied to the sensitivities
   */
  public void mapSensitivities(DoubleUnaryOperator operator) {
    sensitivities.replaceAll(cs -> cs.withSensitivity(operator.applyAsDouble(cs.getSensitivity())));
  }

  /**
   * Sorts the mutable list of point sensitivities.
   * <p>
   * Sorts the point sensitivities in this instance.
   */
  public void sort() {
    sensitivities.sort(CurveSensitivity::compareExcludingSensitivity);
  }

  /**
   * Cleans the point sensitivities, mutating the internal list.
   * <p>
   * The list of sensitivities is sorted and then merged.
   */
  public void clean() {
    sensitivities.sort(CurveSensitivity::compareExcludingSensitivity);
    CurveSensitivity last = sensitivities.get(0);
    for (int i = 1; i < sensitivities.size(); i++) {
      CurveSensitivity current = sensitivities.get(i);
      if (current.compareExcludingSensitivity(last) == 0) {
        sensitivities.set(i - 1, last.withSensitivity(last.getSensitivity() + current.getSensitivity()));
        sensitivities.remove(i);
        i--;
      }
      last = current;
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns an immutable version of this object.
   * <p>
   * The result is the immutable {@link CurveGroupSensitivity} class.
   * It will contain the same individual point sensitivities.
   * 
   * @return the immutable sensitivity instance, not null
   */
  public CurveGroupSensitivity toImmutable() {
    return CurveGroupSensitivity.of(sensitivities);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof MutableCurveGroupSensitivity) {
      MutableCurveGroupSensitivity other = (MutableCurveGroupSensitivity) obj;
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
        .append("MutableCurveGroupSensitivity{sensitivities=")
        .append(sensitivities)
        .append('}')
        .toString();
  }

}
