/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect;

import org.assertj.core.api.AbstractAssert;

import com.opengamma.collect.result.Failure;
import com.opengamma.collect.result.FailureReason;
import com.opengamma.collect.result.Result;

/**
 * An assert helper that provides useful AssertJ assertion
 * methods for {@link Result} instances.
 * <p>
 * These allow {code Result}s to be inspected in tests in the
 * same fluent style as other basic classes.
 * <p>
 * So the following:
 * <pre>
 *   Result{@literal <SomeType>} result = someMethodCall();
 *   assertTrue(result.isSuccess());
 *   assertEquals(result.getValue(), SomeType.EXPECTED);
 * </pre>
 * can be replaced with:
 * <pre>
 *   Result{@literal <SomeType>} result = someMethodCall();
 *   assertThat(result)
 *     .isSuccess()
 *     .hasValue(SomeType.EXPECTED);
 * </pre>
 * The advantage of the latter is that if the result was not
 * a success, the error message produced will detail the
 * failure result. In the former, the only information is that
 * the result was not a success. Note that the {@link #isSuccess()}
 * call in the latter is unnecessary as it will be checked by the
 * {@link #hasValue(Object)} method as well.
 * <p>
 * In order to be able to use a statically imported assertThat()
 * method for both {@code Result} and other types, statically
 * import {@link CollectProjectAssertions#assertThat(Result)}
 * rather than this class.
 */
public class ResultAssert extends AbstractAssert<ResultAssert, Result<?>> {

  /**
   * Private constructor, use {@link #assertThat(Result)} to
   * construct an instance.
   *
   * @param actual the result to create an {@code Assert} for
   */
  private ResultAssert(Result<?> actual) {
    super(actual, ResultAssert.class);
  }

  /**
   * Create an {@code Assert} instance for the supplied {@code Result}.
   *
   * @param result  the result to create an {@code Assert} for
   * @return an {@code Assert} instance
   */
  public static ResultAssert assertThat(Result<?> result) {
    return new ResultAssert(result);
  }

  /**
   * Assert that the {@code Result} is a Success.
   *
   * @return this if a failure, else throw an {@code AssertionError}
   */
  public ResultAssert isSuccess() {
    isNotNull();

    if (!actual.isSuccess()) {
      Failure failure = actual.getFailure();
      failWithMessage("Expected Success but was Failure with reason: <%s> and message: <%s>",
          failure.getReason(), failure.getMessage());
    }
    return this;
  }

  /**
   * Assert that the {@code Result} is a success and contains the
   * specified value.
   *
   * @param value  the value the {@code Result} is expected to contain
   * @return this if a success with the specified value, else
   *   throw an {@code AssertionError}
   */
  public ResultAssert hasValue(Object value) {
    isSuccess();

    if (!actual.getValue().equals(value)) {
      failWithMessage("Expected Success with value: <%s> but was: <%s>",
          value, actual.getValue());
    }
    return this;
  }

  /**
   * Assert that the {@code Result} is a Failure.
   *
   * @return this if a failure, else throw an {@code AssertionError}
   */
  public ResultAssert isFailure() {
    isNotNull();

    if (!actual.isFailure()) {
      failWithMessage("Expected Failure but was Success with value: <%s>", actual.getValue());
    }
    return this;
  }

  /**
   * Assert that the {@code Result} is a failure with the
   * specified {@link FailureReason}.
   *
   * @return this if a failure with the specified reason, else
   *   throw an {@code AssertionError}
   */
  public ResultAssert isFailure(FailureReason expected) {
    isNotNull();

    FailureReason actualReason = actual.getFailure().getReason();
    if (actualReason != expected) {
      failWithMessage("Expected Failure with reason: <%s> but was Failure with reason: <%s>",
          expected, actualReason);
    }
    return this;
  }

  /**
   * Assert that the {@code Result} is a failure with the
   * specified message.
   *
   * @param regex  the regex that the failure message is expected to match
   */
  public void hasFailureMessageMatching(String regex) {
    isFailure();

    String message = actual.getFailure().getMessage();
    if (!message.matches(regex)) {
      failWithMessage("Expected Failure with message matching: <%s> but was Failure with message: <%s>",
          regex, message);
    }
  }
}
