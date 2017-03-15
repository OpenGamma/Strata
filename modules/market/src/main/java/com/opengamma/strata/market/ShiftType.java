/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Enum representing alternative ways to apply a shift which modifies the value of a piece of market data.
 */
public enum ShiftType {

  /**
   * A relative shift where the value is scaled by the shift amount.
   * <p>
   * The shift amount is interpreted as a decimal percentage. For example, a shift amount of 0.1 is a
   * shift of +10% which multiplies the value by 1.1. A shift amount of -0.2 is a shift of -20%
   * which multiplies the value by 0.8.
   * <p>
   * {@code shiftedValue = (value + value * shiftAmount)}
   */
  RELATIVE {
    @Override
    public double applyShift(double value, double shiftAmount) {
      return value + value * shiftAmount;
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
  ABSOLUTE {
    @Override
    public double applyShift(double value, double shiftAmount) {
      return value + shiftAmount;
    }

    @Override
    public ValueAdjustment toValueAdjustment(double shiftAmount) {
      return ValueAdjustment.ofDeltaAmount(shiftAmount);
    }
  },

  /**
   * A scaled shift where the value is multiplied by the shift.
   * <p>
   * {@code shiftedValue = (value * shiftAmount)}
   */
  SCALED {
    @Override
    public double applyShift(double value, double shiftAmount) {
      return value * shiftAmount;
    }

    @Override
    public ValueAdjustment toValueAdjustment(double shiftAmount) {
      return ValueAdjustment.ofMultiplier(shiftAmount);
    }
  };

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static ShiftType of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
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

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
