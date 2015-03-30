/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect;

import java.util.Optional;
import java.util.OptionalDouble;

import org.assertj.core.api.Assertions;

import com.opengamma.collect.result.Result;

/**
 * Helper class to allow custom AssertJ assertions to be
 * accessible via the same static import as the standard
 * assertions.
 * <p>
 * Prefer to statically import {@link #assertThat(Result)}
 * from this class rather than {@link ResultAssert#assertThat(Result)}.
 */
public class CollectProjectAssertions extends Assertions {

  /**
   * Create an {@code Assert} instance that enables
   * assertions on {@code Result} objects.
   *
   * @param result  the result to create an {@code Assert} for
   * @return an {@code Assert} instance
   */
  public static ResultAssert assertThat(Result<?> result) {
    return ResultAssert.assertThat(result);
  }

  /**
   * Create an {@code Assert} instance that enables
   * assertions on {@code Optional} objects.
   *
   * @param optional  the optional to create an {@code Assert} for
   * @return an {@code Assert} instance
   */
  public static <T> OptionalAssert<T> assertThat(Optional<T> optional) {
    return OptionalAssert.assertThat(optional);
  }

  /**
   * Create an {@code Assert} instance that enables
   * assertions on {@code OptionalDouble} objects.
   *
   * @param optional  the optional to create an {@code Assert} for
   * @return an {@code Assert} instance
   */
  public static OptionalDoubleAssert assertThat(OptionalDouble optional) {
    return OptionalDoubleAssert.assertThat(optional);
  }
}
