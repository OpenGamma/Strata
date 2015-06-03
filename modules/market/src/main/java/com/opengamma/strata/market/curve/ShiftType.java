/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import com.opengamma.strata.basics.value.ValueAdjustment;

/**
 * Enum representing alternative ways to apply a shift which modifies the value of a piece of market data.
 */
public enum ShiftType {

  /**
   * A relative shift where the value is scaled by the shift amount.
   * <p>
   * The shift amount is interpreted as a decimal percentage. For example, a shift amount of 0.1 is a
   * shift of +10% which multiplies the value by 1.1. A shift amount of -0.2 is a shift of -20%
   * which multiplies the value by 0.8
   * <p>
   * {@code shiftedValue = (value x (1 + shiftAmount))}
   */
  RELATIVE("Relative") {
    @Override
    public double applyShift(double value, double shiftAmount) {
      return value * (1 + shiftAmount);
    }

    @Override
    public ValueAdjustment toValueAdjustment(double shiftAmount) {
      return ValueAdjustment.ofDeltaMultiplier(shiftAmount);
    }
  },

  /**
   * An absolute shift where the shift amount is added to the value.
   * <p>
   * {@code shiftedValue = (value + shiftAmount)}
   */
  ABSOLUTE("Absolute") {
    @Override
    public double applyShift(double value, double shiftAmount) {
      return value + shiftAmount;
    }

    @Override
    public ValueAdjustment toValueAdjustment(double shiftAmount) {
      return ValueAdjustment.ofDeltaAmount(shiftAmount);
    }
  };

  /**
   * Applies the shift to the value using appropriate logic for the shift type.
   *
   * @param value the value to shift
   * @param shiftAmount the shift to apply
   * @return the shifted value
   */
  public abstract double applyShift(double value, double shiftAmount);

  /**
   * Returns a value adjustment that applies the shift amount using appropriate logic for the shift type.
   *
   * @param shiftAmount  the shift to apply
   * @return a value adjustment that applies the shift amount using appropriate logic for the shift type
   */
  public abstract ValueAdjustment toValueAdjustment(double shiftAmount);

  /** The name of the shift type. */
  private String name;

  /**
   * Creates a new instance.
   *
   * @param name  the name of the value
   */
  ShiftType(final String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
