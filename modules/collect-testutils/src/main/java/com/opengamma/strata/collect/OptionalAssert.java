/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.Optional;

import org.assertj.core.api.AbstractAssert;

/**
 * An assert helper that provides useful AssertJ assertion
 * methods for {@link Optional} instances.
 * <p>
 * These allow {@code Optional} instances to be inspected in tests in the
 * same fluent style as other basic classes.
 * <p>
 * So the following:
 * <pre>
 *   Optional{@literal <SomeType>} optional = someMethodCall();
 *   assertTrue(optional.isPresent());
 *   assertEquals(optional.get(), SomeType.EXPECTED);
 * </pre>
 * can be replaced with:
 * <pre>
 *   Optional{@literal <SomeType>} optional = someMethodCall();
 *   assertThat(optional)
 *     .isPresent()
 *     .hasValue(SomeType.EXPECTED);
 * </pre>
 * In order to be able to use a statically imported assertThat()
 * method for both {@code Optional} and other types, statically
 * import {@link CollectProjectAssertions#assertThat(Optional)}
 * rather than this class.
 *
 * @param <T> the type of the value in the {@code Optional}.
 */
public class OptionalAssert<T> extends AbstractAssert<OptionalAssert<T>, Optional<T>> {

  /**
   * Create an {@code Assert} instance for the supplied {@code Optional}.
   *
   * @param optional  the optional instance to wrap
   * @param <T>  the type of the optional value
   * @return an instance of {@code OptionalAssert}
   */
  public static <T> OptionalAssert<T> assertThat(Optional<T> optional) {
    return new OptionalAssert<>(optional);
  }

  /**
   * Private constructor, use {@link #assertThat(Optional)} to construct an instance.
   *
   * @param actual  the instance of {@code Optional} to create an {@code Assert} for
   */
  private OptionalAssert(Optional<T> actual) {
    super(actual, OptionalAssert.class);
  }

  //-------------------------------------------------------------------------
  /**
   * Assert that the {@code Optional} contains a value.
   *
   * @return this, if the wrapped object contains a value
   * @throws AssertionError if the wrapped object does not contain a value
   */
  public OptionalAssert<T> isPresent() {
    isNotNull();

    if (!actual.isPresent()) {
      failWithMessage("Expected a value but Optional was empty");
    }
    return this;
  }

  /**
   * Assert that the {@code Optional} is empty.
   *
   * @return this, if the wrapped object does not contain a value
   * @throws AssertionError if the wrapped object contains a value
   */
  public OptionalAssert<T> isEmpty() {
    isNotNull();

    if (actual.isPresent()) {
      failWithMessage("Expected empty Optional but found value <%s>", actual.get());
    }
    return this;
  }

  /**
   * Assert that the {@code Optional} contains a particular value.
   *
   * @param value  the value to check
   * @return this, if the wrapped object contains the specified value
   * @throws AssertionError if the wrapped object does not contain a value, or the value is not the specified value
   */
  public OptionalAssert<T> hasValue(T value) {
    isPresent();

    if (!actual.get().equals(value)) {
      failWithMessage("Expected Optional with value: <%s> but was <%s>", value, actual.get());
    }
    return this;
  }

}
