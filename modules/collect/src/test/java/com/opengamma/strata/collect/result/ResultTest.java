/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.result.FailureReason.CALCULATION_FAILED;
import static com.opengamma.strata.collect.result.FailureReason.ERROR;
import static com.opengamma.strata.collect.result.FailureReason.MISSING_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.TestHelper;

/**
 * Test.
 */
@Test
public class ResultTest {

  private static final Function<String, Integer> MAP_STRLEN = String::length;

  private static final Function<String, Result<Integer>> FUNCTION_STRLEN =
      input -> Result.success(input.length());
  private static final BiFunction<String, String, Result<String>> FUNCTION_MERGE =
      (t, u) -> Result.success(t + " " + u);

  //-------------------------------------------------------------------------
  public void success() {
    Result<String> test = Result.success("success");
    assertEquals(test.isSuccess(), true);
    assertEquals(test.isFailure(), false);
    assertEquals(test.getValue(), "success");
    assertEquals(test.getValueOrElse("blue"), "success");
    assertThrowsIllegalArg(() -> test.getValueOrElse(null));
    assertThrowsIllegalArg(() -> test.getValueOrElseApply(null));
  }

  public void success_getFailure() {
    Result<String> test = Result.success("success");
    assertThrows(test::getFailure, IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void success_map() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.map(MAP_STRLEN);
    assertEquals(test.isSuccess(), true);
    assertEquals(test.getValue(), Integer.valueOf(7));
  }

  public void success_flatMap() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.flatMap(FUNCTION_STRLEN);
    assertEquals(test.isSuccess(), true);
    assertEquals(test.getValue(), Integer.valueOf(7));
  }

  public void success_combineWith_success() {
    Result<String> success1 = Result.success("Hello");
    Result<String> success2 = Result.success("World");
    Result<String> test = success1.combineWith(success2, FUNCTION_MERGE);
    assertEquals(test.isSuccess(), true);
    assertEquals(test.getValue(), "Hello World");
  }

  public void success_combineWith_failure() {
    Result<String> success = Result.success("Hello");
    Result<String> failure = Result.failure(new IllegalArgumentException());
    Result<String> test = success.combineWith(failure, FUNCTION_MERGE);
    assertEquals(test.isSuccess(), false);
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getItems().size(), 1);
  }

  public void success_combineWith_success_throws() {
    Result<String> success1 = Result.success("Hello");
    Result<String> success2 = Result.success("World");
    Result<String> test = success1.combineWith(success2, (s1, s2) -> {
      throw new IllegalArgumentException("Ooops");
    });
    assertThat(test)
        .isFailure()
        .hasFailureMessageMatching("Ooops");
  }

  public void success_stream() {
    Result<String> success = Result.success("Hello");
    assertThat(success.stream().toArray()).containsExactly("Hello");
  }

  public void success_map_throwing() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.map(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertEquals(test.isSuccess(), false);
    assertThat(test)
        .isFailure(ERROR)
        .hasFailureMessageMatching("Big bad error");
  }

  public void success_flatMap_throwing() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.flatMap(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertEquals(test.isSuccess(), false);
    assertThat(test)
        .isFailure(ERROR)
        .hasFailureMessageMatching("Big bad error");
  }

  //-------------------------------------------------------------------------
  public void failure() {
    IllegalArgumentException ex = new IllegalArgumentException("failure");
    Result<String> test = Result.failure(ex);
    assertEquals(test.isSuccess(), false);
    assertEquals(test.isFailure(), true);
    assertEquals(test.getValueOrElse("blue"), "blue");
    assertEquals(test.getValueOrElseApply(f -> "blue"), "blue");
    assertEquals(test.getValueOrElseApply(Failure::getMessage), "failure");
    assertThrowsIllegalArg(() -> test.getValueOrElse(null));
    assertThrowsIllegalArg(() -> test.getValueOrElseApply(null));
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getMessage(), "failure");
    assertEquals(test.getFailure().getItems().size(), 1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertEquals(item.getReason(), ERROR);
    assertEquals(item.getMessage(), "failure");
    assertEquals(item.getCauseType().get(), ex.getClass());
    assertEquals(item.getStackTrace(), Throwables.getStackTraceAsString(ex));
  }

  public void failure_map_flatMap_ifSuccess() {
    Result<String> test = Result.failure(new IllegalArgumentException("failure"));
    Result<Integer> test1 = test.map(MAP_STRLEN);
    assertSame(test1, test);
    Result<Integer> test2 = test.flatMap(FUNCTION_STRLEN);
    assertSame(test2, test);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void failure_getValue() {
    Result<String> test = Result.failure(new IllegalArgumentException());
    test.getValue();
  }

  public void failure_combineWith_success() {
    Result<String> failure = Result.failure(new IllegalArgumentException("failure"));
    Result<String> success = Result.success("World");
    Result<String> test = failure.combineWith(success, FUNCTION_MERGE);
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getMessage(), "failure");
  }

  public void failure_combineWith_failure() {
    Result<String> failure1 = Result.failure(new IllegalArgumentException("failure"));
    Result<String> failure2 = Result.failure(new IllegalArgumentException("fail"));
    Result<String> test = failure1.combineWith(failure2, FUNCTION_MERGE);
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getMessage(), "failure, fail");
  }

  public void failure_stream() {
    Result<String> failure = Result.failure(new IllegalArgumentException("failure"));
    assertThat(failure.stream().toArray()).isEmpty();
  }

  public void failure_map_throwing() {
    Result<String> success = Result.failure(new IllegalArgumentException("failure"));
    Result<Integer> test = success.map(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertEquals(test.isSuccess(), false);
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getMessage(), "failure");
  }

  public void failure_flatMap_throwing() {
    Result<String> success = Result.failure(new IllegalArgumentException("failure"));
    Result<Integer> test = success.flatMap(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertEquals(test.isSuccess(), false);
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getMessage(), "failure");
  }

  //-------------------------------------------------------------------------
  public void failure_fromStatusMessageArgs_placeholdersMatchArgs1() {
    Result<String> failure = Result.failure(ERROR, "my {} failure", "blue");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my blue failure");
  }

  public void failure_fromStatusMessageArgs_placeholdersMatchArgs2() {
    Result<String> failure = Result.failure(ERROR, "my {} {} failure", "blue", "rabbit");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my blue rabbit failure");
  }

  public void failure_fromStatusMessageArgs_placeholdersExceedArgs() {
    Result<String> failure = Result.failure(ERROR, "my {} {} failure", "blue");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my blue {} failure");
  }

  public void failure_fromStatusMessageArgs_placeholdersLessThanArgs1() {
    Result<String> failure = Result.failure(ERROR, "my {} failure", "blue", "rabbit");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my blue failure - [rabbit]");
  }

  public void failure_fromStatusMessageArgs_placeholdersLessThanArgs2() {
    Result<String> failure = Result.failure(ERROR, "my {} failure", "blue", "rabbit", "carrot");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my blue failure - [rabbit, carrot]");
  }

  public void failure_fromStatusMessageArgs_placeholdersLessThanArgs3() {
    Result<String> failure = Result.failure(ERROR, "my failure", "blue", "rabbit", "carrot");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my failure - [blue, rabbit, carrot]");
  }

  //-------------------------------------------------------------------------
  public void failure_fromResult_failure() {
    Result<String> failure = Result.failure(ERROR, "my failure");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my failure");
    assertEquals(test.getFailure().getItems().size(), 1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertEquals(item.getReason(), ERROR);
    assertEquals(item.getMessage(), "my failure");
    assertEquals(item.getCauseType().isPresent(), false);
    assertEquals(item.getStackTrace().contains(".FailureItem.of("), false);
    assertEquals(item.getStackTrace().contains(".Failure.of("), false);
    assertEquals(item.getStackTrace().contains(".Result.failure("), false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void failure_fromResult_success() {
    Result<String> success = Result.success("Hello");
    Result.failure(success);
  }

  //-------------------------------------------------------------------------
  public void failure_fromFailure() {
    Failure failure = Failure.of(ERROR, "my failure");
    Result<Integer> test = Result.failure(failure);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "my failure");
    assertEquals(test.getFailure().getItems().size(), 1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertEquals(item.getReason(), ERROR);
    assertEquals(item.getMessage(), "my failure");
    assertEquals(item.getCauseType().isPresent(), false);
    assertTrue(item.getStackTrace() != null);
  }

  //-------------------------------------------------------------------------
  public void ofNullable_nonNull() {
    Result<Integer> test = Result.ofNullable(6);
    assertFalse(test.isFailure());
    assertEquals(test.getValue().intValue(), 6);
  }

  public void ofNullable_null() {
    Result<Integer> test = Result.ofNullable(null);
    assertTrue(test.isFailure());
    assertEquals(test.getFailure().getMessage(), "Found null where a value was expected");
    assertEquals(test.getFailure().getItems().size(), 1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertEquals(item.getReason(), MISSING_DATA);
    assertEquals(item.getMessage(), "Found null where a value was expected");
    assertEquals(item.getCauseType().isPresent(), false);
    assertTrue(item.getStackTrace() != null);
  }

  //-------------------------------------------------------------------------
  public void of_with_success() {

    Result<String> test = Result.of(() -> "success");
    assertEquals(test.isSuccess(), true);
    assertEquals(test.isFailure(), false);
    assertEquals(test.getValue(), "success");
  }

  public void of_with_exception() {

    Result<String> test = Result.of(() -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertEquals(test.isSuccess(), false);
    assertEquals(test.isFailure(), true);
    assertThrows(test::getValue, IllegalStateException.class);
  }

  public void wrap_with_success() {
    Result<String> test = Result.wrap(() -> Result.success("success"));
    assertEquals(test.isSuccess(), true);
    assertEquals(test.isFailure(), false);
    assertEquals(test.getValue(), "success");
  }

  public void wrap_with_failure() {

    Result<String> test = Result.wrap(() -> Result.failure(ERROR, "Something failed"));
    assertEquals(test.isSuccess(), false);
    assertEquals(test.isFailure(), true);
    assertThrows(test::getValue, IllegalStateException.class);
  }

  public void wrap_with_exception() {

    Result<String> test = Result.wrap(() -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertEquals(test.isSuccess(), false);
    assertEquals(test.isFailure(), true);
    assertThrows(test::getValue, IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void anyFailures_varargs() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertTrue(Result.anyFailures(failure1, failure2));
    assertTrue(Result.anyFailures(failure1, success1));
    assertFalse(Result.anyFailures(success1, success2));
  }

  public void anyFailures_collection() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertTrue(Result.anyFailures(ImmutableList.of(failure1, failure2)));
    assertTrue(Result.anyFailures(ImmutableList.of(failure1, success1)));
    assertFalse(Result.anyFailures(ImmutableList.of(success1, success2)));
  }

  public void countFailures_varargs() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertEquals(Result.countFailures(failure1, failure2), 2);
    assertEquals(Result.countFailures(failure1, success1), 1);
    assertEquals(Result.countFailures(success1, success2), 0);
  }

  public void countFailures_collection() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertEquals(Result.countFailures(ImmutableList.of(failure1, failure2)), 2);
    assertEquals(Result.countFailures(ImmutableList.of(failure1, success1)), 1);
    assertEquals(Result.countFailures(ImmutableList.of(success1, success2)), 0);
  }

  public void allSuccess_varargs() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertFalse(Result.allSuccessful(failure1, failure2));
    assertFalse(Result.allSuccessful(failure1, success1));
    assertTrue(Result.allSuccessful(success1, success2));
  }

  public void allSuccess_collection() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertFalse(Result.allSuccessful(ImmutableList.of(failure1, failure2)));
    assertFalse(Result.allSuccessful(ImmutableList.of(failure1, success1)));
    assertTrue(Result.allSuccessful(ImmutableList.of(success1, success2)));
  }

  //-------------------------------------------------------------------------
  public void combine_iterableWithFailures() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 2");
    Result<String> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<String> failure2 = Result.failure(ERROR, "failure 2");
    Set<Result<String>> results = ImmutableSet.of(success1, success2, failure1, failure2);

    assertThat(Result.combine(results, s -> s))
        .isFailure(FailureReason.MULTIPLE);
  }

  public void combine_iterableWithSuccesses() {
    Result<Integer> success1 = Result.success(1);
    Result<Integer> success2 = Result.success(2);
    Result<Integer> success3 = Result.success(3);
    Result<Integer> success4 = Result.success(4);
    Set<Result<Integer>> results = ImmutableSet.of(success1, success2, success3, success4);

    Result<String> combined = Result.combine(
        results,
        s -> "res" + s.reduce(1, (i1, i2) -> i1 * i2));
    assertThat(combined)
        .isSuccess()
        .hasValue("res24");
  }

  public void combine_iterableWithSuccesses_throws() {
    Result<Integer> success1 = Result.success(1);
    Result<Integer> success2 = Result.success(2);
    Result<Integer> success3 = Result.success(3);
    Result<Integer> success4 = Result.success(4);
    Set<Result<Integer>> results = ImmutableSet.of(success1, success2, success3, success4);

    Result<String> combined = Result.combine(
        results,
        s -> {
          throw new IllegalArgumentException("Ooops");
        });

    assertThat(combined)
        .isFailure(ERROR)
        .hasFailureMessageMatching("Ooops");
  }

  //-------------------------------------------------------------------------

  public void flatCombine_iterableWithFailures() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 2");
    Result<String> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<String> failure2 = Result.failure(ERROR, "failure 2");
    Set<Result<String>> results = ImmutableSet.of(success1, success2, failure1, failure2);

    assertThat(Result.flatCombine(results, Result::success))
        .isFailure(FailureReason.MULTIPLE);
  }

  public void flatCombine_iterableWithSuccesses_combineFails() {
    Result<Integer> success1 = Result.success(1);
    Result<Integer> success2 = Result.success(2);
    Result<Integer> success3 = Result.success(3);
    Result<Integer> success4 = Result.success(4);
    Set<Result<Integer>> results = ImmutableSet.of(success1, success2, success3, success4);

    Result<String> combined = Result.flatCombine(
        results,
        s -> Result.failure(CALCULATION_FAILED, "Could not do it"));

    assertThat(combined)
        .isFailure(CALCULATION_FAILED);
  }

  public void flatCombine_iterableWithSuccesses_combineSucceeds() {
    Result<Integer> success1 = Result.success(1);
    Result<Integer> success2 = Result.success(2);
    Result<Integer> success3 = Result.success(3);
    Result<Integer> success4 = Result.success(4);
    Set<Result<Integer>> results = ImmutableSet.of(success1, success2, success3, success4);

    Result<String> combined = Result.flatCombine(
        results,
        s -> Result.success("res" + s.reduce(1, (i1, i2) -> i1 * i2)));

    assertThat(combined)
        .isSuccess()
        .hasValue("res24");
  }

  public void flatCombine_iterableWithSuccesses_combineThrows() {
    Result<Integer> success1 = Result.success(1);
    Result<Integer> success2 = Result.success(2);
    Result<Integer> success3 = Result.success(3);
    Result<Integer> success4 = Result.success(4);
    Set<Result<Integer>> results = ImmutableSet.of(success1, success2, success3, success4);

    Result<String> combined = Result.flatCombine(
        results,
        s -> {
          throw new IllegalArgumentException("Ooops");
        });

    assertThat(combined)
        .isFailure(ERROR)
        .hasFailureMessageMatching("Ooops");
  }

  //-------------------------------------------------------------------------

  public void failure_fromResults_varargs1() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    Result<Object> test = Result.failure(success1, success2, failure1, failure2);
    Set<FailureItem> expected = new HashSet<>();
    expected.addAll(failure1.getFailure().getItems());
    expected.addAll(failure2.getFailure().getItems());
    assertEquals(test.getFailure().getItems(), expected);
  }

  public void failure_fromResults_varargs2() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    Result<Object> test = Result.failure(success1, failure1, success2, failure2);
    Set<FailureItem> expected = new HashSet<>();
    expected.addAll(failure1.getFailure().getItems());
    expected.addAll(failure2.getFailure().getItems());
    assertEquals(test.getFailure().getItems(), expected);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void failure_fromResults_varargs_allSuccess() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result.failure(success1, success2);
  }

  public void failure_fromResults_collection() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<String> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<String> failure2 = Result.failure(ERROR, "failure 2");

    // Exposing collection explicitly shows why signature of failure is as it is
    List<Result<String>> results = Arrays.asList(success1, success2, failure1, failure2);
    Result<String> test = Result.failure(results);
    Set<FailureItem> expected = new HashSet<>();
    expected.addAll(failure1.getFailure().getItems());
    expected.addAll(failure2.getFailure().getItems());
    assertEquals(test.getFailure().getItems(), expected);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void failure_fromResults_collection_allSuccess() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result.failure(Arrays.asList(success1, success2));
  }

  //-------------------------------------------------------------------------
  public void generateFailureFromException() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(exception);
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getMessage(), "something went wrong");
  }

  public void generateFailureFromExceptionWithMessage() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(exception, "my message");
    assertEquals(test.getFailure().getReason(), ERROR);
    assertEquals(test.getFailure().getMessage(), "my message");
  }

  public void generateFailureFromExceptionWithCustomStatus() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(CALCULATION_FAILED, exception);
    assertEquals(test.getFailure().getReason(), CALCULATION_FAILED);
    assertEquals(test.getFailure().getMessage(), "something went wrong");
  }

  public void generateFailureFromExceptionWithCustomStatusAndMessage() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(CALCULATION_FAILED, exception, "my message");
    assertEquals(test.getFailure().getReason(), CALCULATION_FAILED);
    assertEquals(test.getFailure().getMessage(), "my message");
  }

  //-------------------------------------------------------------------------
  public void failureDeduplicateFailure() {
    Result<Object> result = Result.failure(MISSING_DATA, "failure");
    FailureItem failure = result.getFailure().getItems().iterator().next();

    Result<Object> test = Result.failure(result, result);
    assertEquals(test.getFailure().getItems().size(), 1);
    assertEquals(test.getFailure().getItems(), ImmutableSet.of(failure));
    assertEquals(test.getFailure().getMessage(), "failure");
  }

  public void failureSameType() {
    Result<Object> failure1 = Result.failure(MISSING_DATA, "message 1");
    Result<Object> failure2 = Result.failure(MISSING_DATA, "message 2");
    Result<Object> failure3 = Result.failure(MISSING_DATA, "message 3");
    Result<?> composite = Result.failure(failure1, failure2, failure3);
    assertEquals(composite.getFailure().getReason(), MISSING_DATA);
    assertEquals(composite.getFailure().getMessage(), "message 1, message 2, message 3");
  }

  public void failureDifferentTypes() {
    Result<Object> failure1 = Result.failure(MISSING_DATA, "message 1");
    Result<Object> failure2 = Result.failure(CALCULATION_FAILED, "message 2");
    Result<Object> failure3 = Result.failure(ERROR, "message 3");
    Result<?> composite = Result.failure(failure1, failure2, failure3);
    assertEquals(composite.getFailure().getReason(), FailureReason.MULTIPLE);
    assertEquals(composite.getFailure().getMessage(), "message 1, message 2, message 3");
  }

  //------------------------------------------------------------------------
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void createByBuilder_neitherValueNorFailure() {
    Result.meta().builder().build();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void createByBuilder_bothValueAndFailure() {
    Result.meta().builder()
        .set("value", "A")
        .set("failure", Failure.of(CALCULATION_FAILED, "Fail"))
        .build();
  }

  //------------------------------------------------------------------------
  public void equalsHashCode() {
    Exception ex = new Exception("Problem");
    Result<Object> a1 = Result.failure(MISSING_DATA, ex);
    Result<Object> a2 = Result.failure(MISSING_DATA, ex);
    Result<Object> b = Result.failure(ERROR, "message 2");
    Result<Object> c = Result.success("Foo");
    Result<Object> d = Result.success("Bar");

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.equals(d), false);

    assertEquals(b.equals(a1), false);
    assertEquals(b.equals(a2), false);
    assertEquals(b.equals(b), true);
    assertEquals(b.equals(c), false);
    assertEquals(b.equals(d), false);

    assertEquals(c.equals(a1), false);
    assertEquals(c.equals(a2), false);
    assertEquals(c.equals(b), false);
    assertEquals(c.equals(c), true);
    assertEquals(c.equals(d), false);

    assertEquals(d.equals(a1), false);
    assertEquals(d.equals(a2), false);
    assertEquals(d.equals(b), false);
    assertEquals(d.equals(c), false);
    assertEquals(d.equals(d), true);
  }

  // Following are tests for Result using the AssertJ assertions
  // Primarily a test of the assertions themselves

  public void assert_success() {
    Result<String> test = Result.success("success");
    assertThat(test)
        .isSuccess()
        .hasValue("success");
  }

  // We can't use assertThrows as that rethrows AssertionError
  @Test(expectedExceptions = AssertionError.class)
  public void assert_success_getFailure() {
    Result<String> test = Result.success("success");
    assertThat(test).isFailure();
  }

  public void assert_success_map() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.map(MAP_STRLEN);
    assertThat(test).isSuccess().hasValue(7);
  }

  public void assert_success_flatMap() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.flatMap(FUNCTION_STRLEN);
    assertThat(test).isSuccess().hasValue(7);
  }

  public void assert_success_combineWith_success() {
    Result<String> success1 = Result.success("Hello");
    Result<String> success2 = Result.success("World");
    Result<String> test = success1.combineWith(success2, FUNCTION_MERGE);
    assertThat(test).isSuccess().hasValue("Hello World");
  }

  public void assert_success_combineWith_failure() {
    Result<String> success = Result.success("Hello");
    Result<String> failure = Result.failure(new IllegalArgumentException());
    Result<String> test = success.combineWith(failure, FUNCTION_MERGE);
    assertThat(test).isFailure(ERROR);
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
  }

  public void assert_failure() {
    Result<String> test = Result.failure(new IllegalArgumentException("failure"));
    assertThat(test)
        .isFailure(ERROR)
        .hasFailureMessageMatching("failure");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Result<Object> failure = Result.failure(MISSING_DATA, "message 1");
    TestHelper.coverImmutableBean(failure);
    TestHelper.coverImmutableBean(failure.getFailure());
    TestHelper.coverImmutableBean(failure.getFailure().getItems().iterator().next());

    Result<String> success = Result.success("Hello");
    TestHelper.coverImmutableBean(success);

    TestHelper.coverEnum(FailureReason.class);
  }

}
