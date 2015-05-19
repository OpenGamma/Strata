/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import org.assertj.core.api.AbstractAssert;

import com.opengamma.strata.collect.result.Failure;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;

/**
 * An assert helper that provides useful AssertJ assertion
 * methods for {@link Result} instances.
 * <p>
 * These allow {code Result} instances to be inspected in tests in the
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
   * Create an {@code Assert} instance for the supplied {@code Result}.
   *
   * @param result  the result instance to wrap
   * @return an instance of {@code ResultAssert}
   */
  public static ResultAssert assertThat(Result<?> result) {
    return new ResultAssert(result);
  }

  /**
   * Private constructor, use {@link #assertThat(Result)} to construct an instance.
   *
   * @param actual  the instance of {@code Result} to create an {@code Assert} for
   */
  private ResultAssert(Result<?> actual) {
    super(actual, ResultAssert.class);
  }

  //-------------------------------------------------------------------------
  /**
   * Assert that the {@code Result} is a Success.
   *
   * @return this, if the wrapped object is a success
   * @throws AssertionError if the wrapped object is a failure
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
   * Assert that the {@code Result} is a success and contains the specified value.
   *
   * @param value  the value the {@code Result} is expected to contain
   * @return this, if the wrapped object is a success and has the specified value
   * @throws AssertionError if the wrapped object is a failure, or does not have the specified value
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
   * @return this, if the wrapped object is a failure
   * @throws AssertionError if the wrapped object is a success
   */
  public ResultAssert isFailure() {
    isNotNull();

    if (!actual.isFailure()) {
      failWithMessage("Expected Failure but was Success with value: <%s>", actual.getValue());
    }
    return this;
  }

  /**
   * Assert that the {@code Result} is a failure with the specified reason.
   *
   * @param expected  the expected failure reason
   * @return this, if the wrapped object is a failure with the specified reason
   * @throws AssertionError if the wrapped object is a success, or does not have the expected reason
   */
  public ResultAssert isFailure(FailureReason expected) {
    isFailure();

    FailureReason actualReason = actual.getFailure().getReason();
    if (actualReason != expected) {
      failWithMessage("Expected Failure with reason: <%s> but was Failure with reason: <%s>",
          expected, actualReason);
    }
    return this;
  }

  /**
   * Assert that the {@code Result} is a failure with the specified message.
   *
   * @param regex  the regex that the failure message is expected to match
   * @return this, if the wrapped object is a failure with the specified message
   * @throws AssertionError if the wrapped object is a success, or does not have the expected message
   */
  public ResultAssert hasFailureMessageMatching(String regex) {
    isFailure();

    String message = actual.getFailure().getMessage();
    if (!message.matches(regex)) {
      failWithMessage("Expected Failure with message matching: <%s> but was Failure with message: <%s>",
          regex, message);
    }
    return this;
  }

}
