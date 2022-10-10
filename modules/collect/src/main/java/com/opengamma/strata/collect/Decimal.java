/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

/**
 * A decimal number, similar to {@code BigDecimal}, but optimized for the needs of finance.
 * <p>
 * This class represents a decimal number using a {@code long} unscaled value and an {@code int} scale.
 * The scale is constrained to be from 0 to 18. The unscaled value limits the precision to 18 digits.
 * Given this, the class supports 18 decimal places for values between -1 and 1,
 * 17 decimal places for values between -10 and 10, and so on.
 * Fractional values never have trailing zeros, thus the comparator is compatible with equals.
 */
public final class Decimal implements Serializable, Comparable<Decimal> {

  /** The serialization version id. */
  private static final long serialVersionUID = 1L;
  /** Max precision. */
  private static final int MAX_PRECISION = 18;
  /** Max scale. */
  static final int MAX_SCALE = 18;
  /** Max unscaled value. */
  private static final long MAX_UNSCALED = 999_999_999_999_999_999L;
  /** Powers of ten. */
  private static final long[] POWERS = new long[MAX_SCALE + 1];
  static {
    POWERS[0] = 1;
    for (int i = 1; i < POWERS.length; i++) {
      POWERS[i] = POWERS[i - 1] * 10;
    }
  }
  /** Math context that truncates. */
  private static final MathContext MATH_CONTEXT = new MathContext(MAX_PRECISION, RoundingMode.DOWN);

  /**
   * A decimal value representing zero.
   */
  public static final Decimal ZERO = new Decimal(0, 0);
  /**
   * A decimal value representing the largest supported value.
   */
  public static final Decimal MAX_VALUE = new Decimal(MAX_UNSCALED, 0);
  /**
   * A decimal value representing the smallest supported value.
   */
  public static final Decimal MIN_VALUE = new Decimal(-MAX_UNSCALED, 0);

  /** The unscaled value. */
  private final long unscaled;
  /** The scale. */
  private final int scale;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a {@code long}.
   * 
   * @param value  the value
   * @return the equivalent decimal
   */
  public static Decimal of(long value) {
    if (value > MAX_UNSCALED || value < -MAX_UNSCALED) {
      throw new IllegalArgumentException("Decimal value must not exceed 18 digits of precision at scale 0: " + value);
    }
    return new Decimal(value, 0);
  }

  /**
   * Obtains an instance from a {@code double}.
   * <p>
   * This operates as though the {@code double} is converted to a {@code String} and then parsed using {@link #of(String)}.
   * 
   * @param value  the value
   * @return the equivalent decimal
   * @throws IllegalArgumentException if the value is not finite or is too large
   */
  public static Decimal of(double value) {
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Decimal value must be finite: " + value);
    }
    long longValue = (long) value;
    if (value == longValue) {
      return of(longValue);
    }
    return of(Double.toString(value));
  }

  /**
   * Obtains an instance from a {@code String}.
   * <p>
   * This uses a parser with the same semantics as constructing a {@link BigDecimal} with the string
   * and then converting using {@link #of(BigDecimal)}.
   * 
   * @param str  the string
   * @return the equivalent decimal
   * @throws NumberFormatException if the string cannot be parsed
   * @throws IllegalArgumentException if the value is too large
   */
  public static Decimal of(String str) {
    // pre-filter algorithm, based on performance testing
    int len = str.length();
    if (len == 0) {
      throw new NumberFormatException("Decimal string must not be empty");
    } else if (len == 1) {
      return parseShortString(str);
    } else {
      return parseString(str);
    }
  }

  private static Decimal parseString(String str) {
    char[] chs = str.toCharArray();
    if (chs.length > 256) {
      throw new NumberFormatException("Decimal string must not exceed 256 characters");
    }
    // extract sign
    int sign = 1;
    int startPos = 0;
    char first = chs[0];
    if (first == '-') {
      sign = -1;
      startPos = 1;
    } else if (first == '+') {
      startPos = 1;
    }
    // extract number
    long unscaled = 0;
    int precision = 0;
    int scale = 0;
    boolean afterDecimalPoint = false;
    for (int i = startPos; i < chs.length; i++) {
      char ch = chs[i];
      switch (ch) {
        case '+':
        case '-': {
          throw new NumberFormatException("Decimal string must not have sign in the middle: " + str);
        }
        case '.': {
          if (afterDecimalPoint) {
            throw new NumberFormatException("Decimal string must not have two decimal points: " + str);
          }
          afterDecimalPoint = true;
          break;
        }
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9': {
          // max precision has been reached, stop parsing but still look for errors
          if (precision >= MAX_PRECISION) {
            if (!afterDecimalPoint) {
              throw new NumberFormatException("Decimal string must not exceed 18 digits before decimal point: " + str);
            }
            break;
          }
          // max scale has been reached, stop parsing but still look for errors
          if (scale >= MAX_SCALE) {
            break;
          }
          int chValue = ch - 48;
          // increase scale if after decimal point
          if (afterDecimalPoint) {
            scale++;
          }
          // parse unscaled value, skip leading zeroes before decimal point
          if (precision > 0 || chValue > 0) {
            unscaled = unscaled * 10 + chValue;
            precision++;
          }
          break;
        }
        default:
          return of(new BigDecimal(str));  // handles exponents and unicode digits
      }
    }
    return ofScaled(unscaled * sign, scale);
  }

  private static Decimal parseShortString(String str) {
    char ch = str.charAt(0);
    if (ch >= '0' && ch <= '9') {
      return new Decimal(ch - 48, 0);
    }
    throw new NumberFormatException("Decimal string is invalid: " + ch);
  }

  /**
   * Obtains an instance from a {@code BigDecimal}.
   * <p>
   * The scale is adjusted to be in the range 0-18, with any smaller fractional part dropped by truncation.
   * 
   * @param value  the value
   * @return the equivalent decimal
   * @throws IllegalArgumentException if the value is too large
   */
  public static Decimal of(BigDecimal value) {
    return ofRounded(value.round(MATH_CONTEXT));
  }

  // creates from a pre-rounded value
  private static Decimal ofRounded(BigDecimal value) {
    BigDecimal adjusted = value.stripTrailingZeros();
    adjusted = adjusted.scale() < 0 ? adjusted.setScale(0) : adjusted;
    if (adjusted.precision() > MAX_PRECISION) {
      throw new IllegalArgumentException("Decimal value must not exceed 18 digits of precision at scale 0: " + value);
    }
    // longValueExact() used for extra safety, it should never actually throw
    return ofScaled(adjusted.unscaledValue().longValueExact(), adjusted.scale());
  }

  /**
   * Obtains an instance from an unscaled value and a scale.
   * <p>
   * The scale is adjusted to be in the range 0-18, with any smaller fractional part dropped by truncation.
   * The result is normalized to have no fractional trailing zeroes.
   * <p>
   * For example, {@code Decimal.ofScaled(1230, 2)} returns a decimal with the value '12.3'.
   * 
   * @param unscaled  the unscaled value
   * @param scale  the scale
   * @return the equivalent decimal
   * @throws IllegalArgumentException if the value is too large
   */
  public static Decimal ofScaled(long unscaled, int scale) {
    if (unscaled == 0) {
      return ZERO;
    }
    if (scale < 0 || scale > MAX_SCALE || unscaled > MAX_UNSCALED || unscaled < -MAX_UNSCALED) {
      return ofScaled0(unscaled, scale);
    }
    return create(unscaled, scale);
  }

  // create, removing any trailing zeroes
  private static Decimal create(long unscaled, int scale) {
    if (scale > 0 && ((unscaled % 10) == 0)) {
      return create(unscaled / 10, scale - 1);
    }
    return new Decimal(unscaled, scale);
  }

  // special cases, broken out to aid JVM inlining
  private static Decimal ofScaled0(long unscaled, int scale) {
    // any scale allowed for ZERO, max precision will truncate to zero at this scale
    if (scale >= 2 * MAX_SCALE) {
      return ZERO;
    }
    if (scale <= -MAX_SCALE) {
      throw new IllegalArgumentException("Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + "E" + -scale);
    }
    // reduce the precision by truncation
    if (unscaled > MAX_UNSCALED || unscaled < -MAX_UNSCALED) {
      if (scale == 0) {
        throw new IllegalArgumentException("Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + "E" + -scale);
      } else {
        // recurse to check scale (only needs to happen once as max precision of a long is 19)
        return ofScaled0(unscaled / 10, scale - 1);
      }
    }
    // set scale to zero if possible
    if (scale < 0) {
      try {
        long adjUnscaled = Math.multiplyExact(unscaled, POWERS[-scale]);
        return ofScaled0(adjUnscaled, 0);  // recurse to check if between MAX_UNSCALED and Long.MAX_VALUE
      } catch (ArithmeticException ex) {
        throw new IllegalArgumentException("Decimal value must not exceed 18 digits of precision: " + unscaled + "E" + -scale);
      }
    }
    // reduce the scale by truncation
    if (scale > MAX_SCALE) {
      long adjUnscaled = unscaled / POWERS[scale - MAX_SCALE];
      if (adjUnscaled == 0) {
        return ZERO;
      }
      return create(adjUnscaled, MAX_SCALE);
    }
    // scale >= 0, thus OK to call create()
    return create(unscaled, scale);
  }

  /**
   * Parses an instance from a {@code String}.
   * <p>
   * This uses a parser with the same semantics as constructing a {@link BigDecimal} with the string
   * and then converting using {@link #of(BigDecimal)}.
   * 
   * @param str  the string
   * @return the equivalent decimal
   * @throws NumberFormatException if the string cannot be parsed
   * @throws IllegalArgumentException if the value is too large
   */
  @FromString
  public static Decimal parse(String str) {
    // this method exists to provide a unique name for method references
    // and to allow parse() to diverge from of() in the future if desired
    return of(str);
  }

  // create an instance
  private Decimal(long unscaled, int scale) {
    this.unscaled = unscaled;
    this.scale = scale;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the unscaled part of the value.
   * 
   * @return the unscaled value
   */
  public long unscaledValue() {
    return unscaled;
  }

  /**
   * Returns the scale.
   * 
   * @return the scale, from 0 to 18
   */
  public int scale() {
    return scale;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value plus the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other decimal
   * @return the result of the addition
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal plus(Decimal other) {
    if (this.unscaled == 0) {
      return other;
    }
    if (other.unscaled == 0) {
      return this;
    }
    return plus0(this.unscaled, this.scale, other.unscaled, other.scale);
  }

  /**
   * Returns a decimal value that is equal to this value plus the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other decimal
   * @return the result of the addition
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal plus(long other) {
    if (other == 0) {
      return this;
    }
    if (other < -MAX_UNSCALED || other > MAX_UNSCALED) {
      try {
        return of(Math.addExact(longValue(), other));
      } catch (ArithmeticException ex) {
        throw new IllegalArgumentException("Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + " * " + other);
      }
    }
    return plus0(unscaled, scale, other, 0);
  }

  /**
   * Returns a decimal value that is equal to this value plus the specified value.
   * <p>
   * The {@code double} is converted to a {@code Decimal} before the calculation.
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other decimal
   * @return the result of the addition
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal plus(double other) {
    if (other == 0) {
      return this;
    }
    return plus(Decimal.of(other));
  }

  // sum
  private Decimal plus0(long unscaled1, int scale1, long unscaled2, int scale2) {
    if (scale1 == scale2) {
      long sum = unscaled1 + unscaled2;  // safe, as MAX_UNSCALED + MAX_UNSCALED fits in a long
      return ofScaled(sum, scale1);
    } else if (scale1 > scale2) {
      return plus0Sorted(unscaled1, scale1, unscaled2, scale2);
    } else {
      return plus0Sorted(unscaled2, scale2, unscaled1, scale1);
    }
  }

  // sum where scale1 > scale2
  private Decimal plus0Sorted(long unscaled1, int scale1, long unscaled2, int scale2) {
    int scaleDiff = scale1 - scale2;
    if (scaleDiff < MAX_SCALE && Math.abs(unscaled2) < POWERS[MAX_SCALE - scaleDiff - 1]) {
      long rescaled2 = unscaled2 * POWERS[scaleDiff];
      return ofScaled(unscaled1 + rescaled2, scale1);  // simple add is safe by analysis above
    }
    return of(BigDecimal.valueOf(unscaled1, scale1).add(BigDecimal.valueOf(unscaled2, scale2)));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value minus the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other decimal
   * @return the result of the subtraction
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal minus(Decimal other) {
    if (other.unscaled == 0) {
      return this;
    }
    return plus(other.negated());
  }

  /**
   * Returns a decimal value that is equal to this value minus the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other value
   * @return the result of the subtraction
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal minus(long other) {
    if (other == 0) {
      return this;
    }
    if (other < -MAX_UNSCALED || other > MAX_UNSCALED) {
      try {
        return of(Math.subtractExact(longValue(), other));
      } catch (ArithmeticException ex) {
        throw new IllegalArgumentException("Decimal value must not exceed 18 digits of precision at scale 0: " + unscaled + " * " + other);
      }
    }
    return plus0(unscaled, scale, -other, 0);
  }

  /**
   * Returns a decimal value that is equal to this value minus the specified value.
   * <p>
   * The {@code double} is converted to a {@code Decimal} before the calculation.
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other value
   * @return the result of the subtraction
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal minus(double other) {
    if (other == 0) {
      return this;
    }
    return minus(Decimal.of(other));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value multiplied by the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * 
   * @param other  the other value
   * @return the result of the multiplication
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal multipliedBy(Decimal other) {
    if (other.scale == 0) {
      return multipliedBy(other.unscaled);
    }
    return of(toBigDecimal().multiply(other.toBigDecimal()));
  }

  /**
   * Returns a decimal value that is equal to this value multiplied by the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * 
   * @param other  the other value
   * @return the result of the multiplication
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal multipliedBy(long other) {
    if (other == 0) {
      return ZERO;
    }
    try {
      return ofScaled(Math.multiplyExact(unscaled, other), scale);
    } catch (ArithmeticException ex) {
      return of(toBigDecimal().multiply(BigDecimal.valueOf(other)));
    }
  }

  /**
   * Returns a decimal value that is equal to this value multiplied by the specified value.
   * <p>
   * The {@code double} is converted to a {@code Decimal} before the calculation.
   * The result will have a scale in the range 0-18.
   * 
   * @param other  the other value
   * @return the result of the multiplication
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal multipliedBy(double other) {
    if (other == 0) {
      return ZERO;
    }
    return multipliedBy(Decimal.of(other));
  }

  /**
   * Returns a decimal value with the decimal point moved.
   * <p>
   * This can be used to multiply or divide by powers of ten.
   * Positive values move right (multiply), negative values move left (divide)
   * <p>
   * {@code Decimal.of(1.235d).movePoint(2)} returns a decimal with the value '123.5'.
   * <p>
   * {@code Decimal.of(1.235d).movePoint(-2)} returns a decimal with the value '0.01235'.
   * 
   * @param movement  the amount to move by, positive to move right (multiply), negative to move left (divide)
   * @return the result of the movement
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal movePoint(int movement) {
    if (movement == 0) {
      return this;
    }
    try {
      return ofScaled0(unscaled, Math.subtractExact(scale, movement));
    } catch (ArithmeticException ex) {
      return ZERO;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value that is equal to this value divided by the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other value
   * @return the result of the division
   * @throws ArithmeticException if dividing by zero
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal dividedBy(Decimal other) {
    return ofRounded(toBigDecimal().divide(other.toBigDecimal(), MATH_CONTEXT));
  }

  /**
   * Returns a decimal value that is equal to this value divided by the specified value, with a rounding mode.
   * <p>
   * The result will have a scale in the range 0-18.
   * 
   * @param other  the other value
   * @param roundingMode  the rounding mode
   * @return the result of the division
   * @throws ArithmeticException if dividing by zero
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal dividedBy(Decimal other, RoundingMode roundingMode) {
    return ofRounded(toBigDecimal().divide(other.toBigDecimal(), new MathContext(MAX_PRECISION, roundingMode)));
  }

  /**
   * Returns a decimal value that is equal to this value divided by the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other value
   * @return the result of the division
   * @throws ArithmeticException if dividing by zero
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal dividedBy(long other) {
    if (other == 1) {
      return this;
    }
    int pos = Arrays.binarySearch(POWERS, other);
    if (pos > 0) {
      return movePoint(-pos);
    }
    return ofRounded(toBigDecimal().divide(BigDecimal.valueOf(other), MATH_CONTEXT));
  }

  /**
   * Returns a decimal value that is equal to this value divided by the specified value.
   * <p>
   * The {@code double} is converted to a {@code Decimal} before the calculation.
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other value
   * @return the result of the multiplication
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal dividedBy(double other) {
    return dividedBy(Decimal.of(other));
  }

  /**
   * Returns the remainder when dividing this value by the specified value.
   * <p>
   * The result will have a scale in the range 0-18.
   * The result may be truncated (rounded down) if necessary.
   * 
   * @param other  the other value
   * @return the remainder of the division
   * @throws ArithmeticException if dividing by zero
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal remainder(Decimal other) {
    return ofRounded(toBigDecimal().remainder(other.toBigDecimal(), MATH_CONTEXT));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a decimal value rounded to the specified scale.
   * <p>
   * This ensures that the result has the specified scale or less, because trailing zeroes are removed.
   * Specifying a scale of 18 or greater will have no effect.
   * The result will have a scale in the range 0-18.
   * <p>
   * {@code Decimal.of(1.235d).roundToScale(2, HALF_UP)} returns a decimal with the value '1.24'.
   * <p>
   * {@code Decimal.of(1.201d).roundToScale(2, HALF_UP)} returns a decimal with the value '1.2'.
   * <p>
   * {@code Decimal.of(1235).roundToScale(-1, HALF_UP)} returns a decimal with the value '1240'.
   * 
   * @param desiredScale  the scale, positive for decimal places, negative to round the integer-part
   * @param roundingMode  the rounding mode
   * @return the result of the round
   * @throws IllegalArgumentException if the scale is -18 or less
   */
  public Decimal roundToScale(int desiredScale, RoundingMode roundingMode) {
    if (desiredScale >= scale) {
      return this;
    }
    // the scale -18 has tricky edge cases, avoid those problems via a blanket ban
    if (desiredScale <= -MAX_SCALE) {
      throw new IllegalArgumentException("Rounding scale must not be -18 or less: " + desiredScale);
    }
    // scaling zero always ends up with ZERO
    if (unscaled == 0) {
      return ZERO;
    }
    // scales above 18 will have no effect
    int adjScale = Math.min(desiredScale, 18);
    // optimize common rounding modes
    switch (roundingMode) {
      case DOWN:
        return roundDownToScale(adjScale);
      case HALF_UP:
        return roundHalfUpToScale(adjScale);
      case UP:
        return roundUpToScale(adjScale);
      case FLOOR:
        return unscaled > 0 ? roundDownToScale(adjScale) : roundUpToScale(adjScale);
      case CEILING:
        return unscaled > 0 ? roundUpToScale(adjScale) : roundDownToScale(adjScale);
      default:
        return of(toBigDecimal().setScale(adjScale, roundingMode));
    }
  }

  // round down is simple truncation
  private Decimal roundDownToScale(int adjScale) {
    int scaleDiff = scale - adjScale;
    if (scaleDiff <= MAX_SCALE) {
      long rescaled = unscaled / POWERS[scaleDiff];
      return ofScaled(rescaled, adjScale);
    }
    return ZERO;
  }

  // round half-up only requires looking at the next digit
  private Decimal roundHalfUpToScale(int adjScale) {
    int scaleDiff = scale - adjScale;
    if (scaleDiff < MAX_SCALE) {
      long rescaledPlusNext = unscaled / POWERS[scaleDiff - 1];
      long rescaled = rescaledPlusNext / 10;
      long nextDigit = rescaledPlusNext % 10;
      int bump = nextDigit >= 5 ? 1 : (nextDigit <= -5 ? -1 : 0);
      return ofScaled(rescaled + bump, adjScale);
    }
    return of(toBigDecimal().setScale(adjScale, RoundingMode.HALF_DOWN));
  }

  // round up using round down
  private Decimal roundUpToScale(int adjScale) {
    int scaleDiff = scale - adjScale;
    if (scaleDiff <= MAX_SCALE) {
      long rescaled = unscaled / POWERS[scaleDiff];
      if (unscaled == rescaled) {
        return this;
      }
      return Decimal.ofScaled(rescaled + Long.signum(unscaled), adjScale);
    }
    return of(toBigDecimal().setScale(adjScale, RoundingMode.UP));
  }

  /**
   * Returns a decimal value rounded to the specified precision.
   * <p>
   * This ensures that the result has no more than the specified precision.
   * Specifying a precision of 18 or greater will have no effect.
   * The result will have a scale in the range 0-18.
   * <p>
   * Note that the decimal 12,000 is considered to have a precision of 2 and scale -3 for the purpose of rounding.
   * In the result it will however be stored with a scale of 0, which could be viewed as a precision of 5.
   * 
   * @param precision  the precision, not negative
   * @param roundingMode  the rounding mode
   * @return the result of the round
   */
  public Decimal roundToPrecision(int precision, RoundingMode roundingMode) {
    ArgChecker.notNegative(precision, "precision");
    if (precision >= 18) {
      return this;
    }
    return ofRounded(toBigDecimal().round(new MathContext(precision, roundingMode)));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the decimal is zero.
   * 
   * @return true if zero
   */
  public boolean isZero() {
    return unscaled == 0;
  }

  /**
   * Returns a decimal value that is positive.
   * 
   * @return the positive absolute value
   */
  public Decimal abs() {
    return new Decimal(unscaled < 0 ? -unscaled : unscaled, scale);
  }

  /**
   * Returns a decimal value that is negated.
   * 
   * @return the negated value
   */
  public Decimal negated() {
    return new Decimal(-unscaled, scale);
  }

  /**
   * Returns the sign, -1 for negative, 0 for zero and 1 for positive.
   *
   * @return -1 for negative, 0 for zero and 1 for positive
   */
  public int signum() {
    return Long.signum(unscaled);
  }

  //-------------------------------------------------------------------------
  /**
   * Maps this decimal value using the maths operations of {@code double}.
   * <p>
   * Note that {@code double} maths operations can be imprecise.
   * 
   * @param fn  the function to apply
   * @return the result of the function
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal mapAsDouble(DoubleUnaryOperator fn) {
    return of(fn.applyAsDouble(doubleValue()));
  }

  /**
   * Maps this decimal value using the maths operations of {@code BigDecimal}.
   * 
   * @param fn  the function to apply
   * @return the result of the function
   * @throws IllegalArgumentException if the result is too large
   */
  public Decimal mapAsBigDecimal(UnaryOperator<BigDecimal> fn) {
    return of(fn.apply(toBigDecimal()));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the equivalent {@code double}.
   * 
   * @return the equivalent value
   */
  public double doubleValue() {
    if (scale == 0) {
      return unscaled;
    }
    if (Math.abs(unscaled) < 1L << 52) {
      return ((double) unscaled) / POWERS[scale];
    }
    return Double.parseDouble(toString());
  }

  /**
   * Returns the equivalent {@code long}.
   * <p>
   * This truncates any fractional part.
   * 
   * @return the equivalent value
   */
  public long longValue() {
    return unscaled / POWERS[scale];
  }

  /**
   * Returns the equivalent {@code BigDecimal}.
   * 
   * @return the equivalent value
   */
  public BigDecimal toBigDecimal() {
    return BigDecimal.valueOf(unscaled, scale);
  }

  /**
   * Returns the equivalent {@code FixedScaleDecimal}.
   * <p>
   * Callers should call {@link #roundToScale(int, RoundingMode)} first if scale is unknown.
   * 
   * @param fixedScale  the fixed scale
   * @return the fixed scale decimal
   * @throws IllegalArgumentException if the fixed scale is less than the scale of this decimal
   */
  public FixedScaleDecimal toFixedScale(int fixedScale) {
    return FixedScaleDecimal.of(this, fixedScale);
  }

  //-------------------------------------------------------------------------
  /**
   * Formats the decimal to at least the specified number of decimal places.
   * <p>
   * With a minimum decimal places of 2,
   * the decimal '12.1' will be formatted as '12.10', and
   * the decimal '12.123' will be formatted as '12.123'
   * <p>
   * Calling this method with '0' as the minimum decimal places is equivalent to using {@link #toString()}..
   * 
   * @param minDecimalPlaces  the minimum number of decimal places, from 0 to 18 inclusive
   * @return the formatted string
   */
  public String formatAtLeast(int minDecimalPlaces) {
    if (minDecimalPlaces < 0 || minDecimalPlaces > 18) {
      throw new IllegalArgumentException("Format requires decimal places between 0 and 18 inclusive");
    }
    return format0(Math.max(minDecimalPlaces, scale));
  }

  /**
   * Formats the decimal to exactly the specified number of decimal places, specifying the rounding mode.
   * <p>
   * With 2 decimal places and rounding mode HALF_UP,
   * the decimal '12.1' will be formatted as '12.10', and
   * the decimal '12.125' will be formatted as '12.13'
   * <p>
   * Use {@link RoundingMode#DOWN} to truncate at the specified number of decimal places.
   * 
   * @param decimalPlaces  the number of decimal places, from 0 to 18 inclusive
   * @param roundingMode  the rounding mode to use
   * @return the formatted string
   */
  public String format(int decimalPlaces, RoundingMode roundingMode) {
    if (decimalPlaces < 0 || decimalPlaces > 18) {
      throw new IllegalArgumentException("Format requires decimal places between 0 and 18 inclusive");
    }
    return roundToScale(decimalPlaces, roundingMode).format0(decimalPlaces);
  }

  /**
   * States if this decimal is greater than the other decimal.
   *
   * @param other the other decimal
   * @return true if this is greater than the other
   */
  public boolean isGreaterThan(Decimal other) {
    return compareTo(other) > 0;
  }

  /**
   * States if this decimal is greater than or equal to the other decimal.
   *
   * @param other the other decimal
   * @return true if this is greater than or equal to the other
   */
  public boolean isGreaterThanEqualTo(Decimal other) {
    return compareTo(other) >= 0;
  }

  /**
   * States if this decimal is less than the other decimal.
   *
   * @param other the other decimal
   * @return true if this is less than the other
   */
  public boolean isLessThan(Decimal other) {
    return compareTo(other) < 0;
  }

  /**
   * States if this decimal is less than or equal to the other decimal.
   *
   * @param other the other decimal
   * @return true if this is less than or equal to the other
   */
  public boolean isLessThanEqualTo(Decimal other) {
    return compareTo(other) <= 0;
  }

  // formats the string
  private String format0(int decimalPlaces) {
    // split 78345 into whole and fraction: 78 and 345
    // prefix the fraction 345, so there is an additional digit, thus 1345, dropping the sign
    // this ensures that leading zeroes in the fraction are retained
    long abs = Math.abs(unscaled);
    long power = POWERS[scale];
    long whole = abs / power;
    long prefixedFraction = (abs % power) + power;
    long paddedFraction = prefixedFraction * POWERS[decimalPlaces - scale];
    StringBuilder buf = new StringBuilder(40);
    if (unscaled < 0) {
      buf.append('-');
    }
    buf.append(whole);
    if (decimalPlaces > 0) {
      buf.append('.');
      // use knowledge of fixed length of long to output each character directly
      int pos = buf.length();
      buf.setLength(pos + decimalPlaces);
      for (int i = decimalPlaces + pos - 1; i >= pos; i--) {
        int value = (int) (paddedFraction % 10);
        buf.setCharAt(i, (char) (value + 48));
        paddedFraction = paddedFraction / 10;
      }
    }
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  @Override
  public int compareTo(Decimal other) {
    if (this.scale == other.scale) {
      return Long.compare(this.unscaled, other.unscaled);
    }
    long power1 = POWERS[this.scale];
    long power2 = POWERS[other.scale];
    long whole1 = this.unscaled / power1;
    long whole2 = other.unscaled / power2;
    if (whole1 < whole2) {
      return -1;
    } else if (whole1 > whole2) {
      return 1;
    }
    long fraction1 = (this.unscaled % power1) * POWERS[18 - this.scale];
    long fraction2 = (other.unscaled % power2) * POWERS[18 - other.scale];
    return Long.compare(fraction1, fraction2);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof Decimal) {
      Decimal other = (Decimal) obj;
      return this.unscaled == other.unscaled && this.scale == other.scale;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(unscaled) ^ scale;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formal string representation of the decimal.
   * <p>
   * This is equivalent to the "plain string" of {@code BigDecimal}.
   * 
   * @return the plain string
   */
  @Override
  @ToString
  public String toString() {
    return format0(scale);
  }

}
