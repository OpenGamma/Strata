/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import org.assertj.core.api.Assertions;

import com.opengamma.strata.collect.result.Result;

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

}
