/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.OptionalDouble;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.data.Offset;

/**
 * An assert helper that provides useful AssertJ assertion
 * methods for {@link OptionalDouble} instances.
 * <p>
 * These allow {@code OptionalDouble} instances to be inspected in tests in the
 * same fluent style as other basic classes.
 * <p>
 * So the following:
 * <pre>
 *   OptionalDouble optional = someMethodCall();
 *   assertTrue(optional.isPresent());
 *   assertEquals(optional.getAsDouble(), 12.345, TOLERANCE);
 * </pre>
 * can be replaced with:
 * <pre>
 *   OptionalDouble optional = someMethodCall();
 *   assertThat(optional)
 *     .isPresent()
 *     .hasValue(12.345, TOLERANCE);
 * </pre>
 * In order to be able to use a statically imported assertThat()
 * method for both {@code OptionalDouble} and other types, statically
 * import {@link CollectProjectAssertions#assertThat(OptionalDouble)}
 * rather than this class.
 */
public class OptionalDoubleAssert extends AbstractAssert<OptionalDoubleAssert, OptionalDouble> {

  /**
   * Create an {@code Assert} instance for the supplied {@code OptionalDouble}.
   *
   * @param optional  the optional instance to wrap
   * @return an instance of {@code OptionalDoubleAssert}
   */
  public static OptionalDoubleAssert assertThat(OptionalDouble optional) {
    return new OptionalDoubleAssert(optional);
  }

  /**
   * Private constructor, use {@link #assertThat(OptionalDouble)} to construct an instance.
   *
   * @param actual  the instance of {@code OptionalDouble} to create an {@code Assert} for
   */
  private OptionalDoubleAssert(OptionalDouble actual) {
    super(actual, OptionalDoubleAssert.class);
  }

  //-------------------------------------------------------------------------
  /**
   * Assert that the {@code OptionalDouble} contains a value.
   *
   * @return this, if the wrapped object contains a value
   * @throws AssertionError if the wrapped object does not contain a value
   */
  public OptionalDoubleAssert isPresent() {
    isNotNull();

    if (!actual.isPresent()) {
      failWithMessage("Expected a value but OptionalDouble was empty");
    }
    return this;
  }

  /**
   * Assert that the {@code OptionalDouble} is empty.
   *
   * @return this, if the wrapped object does not contain a value
   * @throws AssertionError if the wrapped object contains a value
   */
  public OptionalDoubleAssert isEmpty() {
    isNotNull();

    if (actual.isPresent()) {
      failWithMessage("Expected empty OptionalDouble but found value <%s>", actual.getAsDouble());
    }
    return this;
  }

  /**
   * Assert that the {@code OptionalDouble} contains a particular value.
   *
   * @param expected  the value to check
   * @return this, if the wrapped object contains the specified value
   * @throws AssertionError if the wrapped object does not contain a value, or the value is not the specified value
   */
  public OptionalDoubleAssert hasValue(double expected) {
    isPresent();

    if (actual.getAsDouble() != expected) {
      failWithMessage("Expected OptionalDouble with value: <%s> but was <%s>", expected, actual.getAsDouble());
    }
    return this;
  }

  /**
   * Assert that the {@code OptionalDouble} contains a particular value
   * within the specified tolerance.
   *
   * @param expected  the value to check
   * @param tolerance  the tolerance to use
   * @return this, if the wrapped object contains the specified value
   * @throws AssertionError if the wrapped object does not contain a value,
   * or the value is not within tolerance of the specified value
   */
  public OptionalDoubleAssert hasValue(double expected, Offset<Double> tolerance) {
    isPresent();

    if (Math.abs(expected - actual.getAsDouble()) > tolerance.value) {
      failWithMessage(
          "Expected OptionalDouble within <%s> of value: <%s> but was <%s>", tolerance, expected, actual.getAsDouble());
    }
    return this;
  }

  /**
   * Assert that the {@code OptionalDouble} contains a particular value
   * within the specified tolerance.
   *
   * @param expected  the value to check
   * @param tolerance  the tolerance to use
   * @return this, if the wrapped object contains the specified value
   * @throws AssertionError if the wrapped object does not contain a value,
   * or the value is not within tolerance of the specified value
   */
  public OptionalDoubleAssert hasValue(double expected, double tolerance) {
    return hasValue(expected, Offset.offset(tolerance));
  }

}
