/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * Enum representing alternative ways to apply a shift which modifies the value of a piece of market data.
 */
public enum ShiftType implements NamedEnum {

  /**
   * A relative shift where the value is scaled by the shift amount.
   * <p>
   * The shift amount is interpreted as a decimal percentage. For example, a shift amount of 0.1 is a
   * shift of +10% which multiplies the value by 1.1. A shift amount of -0.2 is a shift of -20%
   * which multiplies the value by 0.8.
   * <p>
   * {@code shiftedValue = (value + value * shiftAmount)}
   * <p>
   * {@code shiftAmount} is well-defined for nonzero {@code value}.
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

    @Override
    public double computeShift(double baseValue, double shiftedValue) {
      return shiftedValue / baseValue - 1d;
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

    @Override
    public double computeShift(double baseValue, double shiftedValue) {
      return shiftedValue - baseValue;
    }
  },

  /**
   * A scaled shift where the value is multiplied by the shift.
   * <p>
   * {@code shiftedValue = (value * shiftAmount)}
   * <p>
   * {@code shiftAmount} is well-defined for nonzero {@code value}.
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

    @Override
    public double computeShift(double baseValue, double shiftedValue) {
      return shiftedValue / baseValue;
    }
  },

  /**
   * A fixed shift where the value becomes the shift amount.
   * <p>
   * {@code shiftedValue = shiftAmount}
   */
  FIXED {
    @Override
    public double applyShift(double value, double shiftAmount) {
      return shiftAmount;
    }

    @Override
    public ValueAdjustment toValueAdjustment(double shiftAmount) {
      return ValueAdjustment.ofReplace(shiftAmount);
    }

    @Override
    public double computeShift(double baseValue, double shiftedValue) {
      return shiftedValue;
    }
  };

  // helper for name conversions
  private static final EnumNames<ShiftType> NAMES = EnumNames.of(ShiftType.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static ShiftType of(String name) {
    return NAMES.parse(name);
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
   * Computes the shift amount using appropriate logic for the shift type.
   * 
   * @param baseValue  the base value
   * @param shiftedValue  the shifted value
   * @return the shift amount
   */
  public abstract double computeShift(double baseValue, double shiftedValue);

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

}
