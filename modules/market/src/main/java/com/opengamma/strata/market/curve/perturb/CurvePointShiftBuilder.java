/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.perturb;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Mutable builder for building instances of {@link CurvePointShift}.
 * <p>
 * This is created via {@link CurvePointShift#builder(ShiftType)}.
 */
public final class CurvePointShiftBuilder {

  /**
   * The type of shift to apply to the rates.
   */
  private final ShiftType shiftType;
  /**
   * The shift amounts, keyed by the identifier of the node to which they should be applied.
   */
  private final Map<Object, Double> shifts = new HashMap<>();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor used by {@link CurvePointShift#builder}.
   *
   * @param shiftType  the type of shift to apply to the rates
   */
  CurvePointShiftBuilder(ShiftType shiftType) {
    this.shiftType = ArgChecker.notNull(shiftType, "shiftType");
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a shift for a curve node to the builder.
   *
   * @param nodeIdentifier  the identifier of the node to which the shift should be applied
   * @param shiftAmount  the size of the shift
   * @return this builder
   */
  public CurvePointShiftBuilder addShift(Object nodeIdentifier, double shiftAmount) {
    ArgChecker.notNull(nodeIdentifier, "nodeIdentifier");
    shifts.put(nodeIdentifier, shiftAmount);
    return this;
  }

  /**
   * Adds multiple shifts to the builder.
   *
   * @param shifts  the shift amounts, keyed by the identifier of the node to which they should be applied
   * @return this builder
   */
  public CurvePointShiftBuilder addShifts(Map<? extends Object, Double> shifts) {
    ArgChecker.notNull(shifts, "shifts");
    this.shifts.putAll(shifts);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance of {@link CurvePointShift} built from the data in this builder.
   *
   * @return an instance of {@link CurvePointShift} built from the data in this builder
   */
  public CurvePointShift build() {
    return new CurvePointShift(shiftType, shifts);
  }

}
