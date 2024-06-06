/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.result.FailureReason.CALCULATION_FAILED;
import static com.opengamma.strata.collect.result.FailureReason.ERROR;
import static com.opengamma.strata.collect.result.FailureReason.INVALID;
import static com.opengamma.strata.collect.result.FailureReason.MISSING_DATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.TestHelper;

/**
 * Test.
 */
public class ResultTest {

  private static final Function<String, Integer> MAP_STRLEN = String::length;

  private static final Function<String, Result<Integer>> FUNCTION_STRLEN =
      input -> Result.success(input.length());
  private static final BiFunction<String, String, Result<String>> FUNCTION_MERGE =
      (t, u) -> Result.success(t + " " + u);

  //-------------------------------------------------------------------------
  @Test
  public void success() {
    Result<String> test = Result.success("success");
    assertThat(test.isSuccess()).isEqualTo(true);
    assertThat(test.isFailure()).isEqualTo(false);
    assertThat(test.get()).hasValue("success");
    assertThat(test.getValue()).isEqualTo("success");
    assertThat(test.getValueOrElse("blue")).isEqualTo("success");
    assertThat(test.getValueOrElse(null)).isEqualTo("success");
    assertThatIllegalArgumentException().isThrownBy(() -> test.getValueOrElseApply(null));
  }

  @Test
  public void ifSuccess() {
    Result<String> test = Result.success("success");
    test.ifSuccess(value -> assertThat(value).isEqualTo("success"));
  }

  @Test
  public void ifFailure() {
    Result<String> test = Result.failure(FailureReason.INVALID, "no success");
    test.ifFailure((failure) -> {
      assertThat(failure.getReason()).isEqualTo(FailureReason.INVALID);
      assertThat(failure.getMessage()).isEqualTo("no success");
    });
  }

  @Test
  public void success_getFailure() {
    Result<String> test = Result.success("success");
    assertThatIllegalStateException().isThrownBy(test::getFailure);
  }

  //-------------------------------------------------------------------------
  @Test
  public void success_map() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.map(MAP_STRLEN);
    assertThat(test.isSuccess()).isEqualTo(true);
    assertThat(test.getValue()).isEqualTo(Integer.valueOf(7));
  }

  @Test
  public void success_mapFailure() {
    Result<String> success = Result.success("success");
    Result<String> test = success.mapFailure(failure -> Failure.of(FailureReason.NOT_APPLICABLE, "Failure"));
    assertThat(test).isSameAs(success);
  }

  @Test
  public void success_mapFailureItems() {
    Result<String> success = Result.success("success");
    Result<String> test = success.mapFailureItems(item -> FailureItem.of(FailureReason.NOT_APPLICABLE, "Failure"));
    assertThat(test).isSameAs(success);
  }

  @Test
  public void success_flatMap() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.flatMap(FUNCTION_STRLEN);
    assertThat(test.isSuccess()).isEqualTo(true);
    assertThat(test.getValue()).isEqualTo(Integer.valueOf(7));
  }

  @Test
  public void success_combineWith_success() {
    Result<String> success1 = Result.success("Hello");
    Result<String> success2 = Result.success("World");
    Result<String> test = success1.combineWith(success2, FUNCTION_MERGE);
    assertThat(test.isSuccess()).isEqualTo(true);
    assertThat(test.getValue()).isEqualTo("Hello World");
  }

  @Test
  public void success_combineWith_failure() {
    Result<String> success = Result.success("Hello");
    Result<String> failure = Result.failure(new IllegalArgumentException());
    Result<String> test = success.combineWith(failure, FUNCTION_MERGE);
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
  }

  @Test
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

  @Test
  public void success_stream() {
    Result<String> success = Result.success("Hello");
    assertThat(success.stream().toArray()).containsExactly("Hello");
  }

  @Test
  public void success_map_throwing() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.map(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test)
        .isFailure(ERROR)
        .hasFailureMessageMatching("Big bad error");
  }

  @Test
  public void success_flatMap_throwing() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.flatMap(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test)
        .isFailure(ERROR)
        .hasFailureMessageMatching("Big bad error");
  }

  //-------------------------------------------------------------------------
  @Test
  public void failure() {
    IllegalArgumentException ex = new IllegalArgumentException("failure");
    Result<String> test = Result.failure(ex);
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.isFailure()).isEqualTo(true);
    assertThat(test.get()).isEmpty();
    assertThat(test.getValueOrElse("blue")).isEqualTo("blue");
    assertThat(test.getValueOrElseApply(f -> "blue")).isEqualTo("blue");
    assertThat(test.getValueOrElseApply(Failure::getMessage)).isEqualTo("failure");
    assertThat(test.getValueOrElse(null)).isNull();
    assertThatIllegalArgumentException().isThrownBy(() -> test.getValueOrElseApply(null));
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("failure");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getFirstItem();
    assertThat(item.getReason()).isEqualTo(ERROR);
    assertThat(item.getMessage()).isEqualTo("failure");
    assertThat(item.getCauseType().get()).isEqualTo(ex.getClass());
    assertThat(item.getStackTrace()).isEqualTo(Throwables.getStackTraceAsString(ex).replace(System.lineSeparator(), "\n"));
  }  

  @Test
  public void failure_error() {
    Error ex = new Error("failure");
    Result<String> test = Result.failure(ex);
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.isFailure()).isEqualTo(true);
    assertThat(test.get()).isEmpty();
    assertThat(test.getValueOrElse("blue")).isEqualTo("blue");
    assertThat(test.getValueOrElseApply(f -> "blue")).isEqualTo("blue");
    assertThat(test.getValueOrElseApply(Failure::getMessage)).isEqualTo("failure");
    assertThat(test.getValueOrElse(null)).isNull();
    assertThatIllegalArgumentException().isThrownBy(() -> test.getValueOrElseApply(null));
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("failure");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getFirstItem();
    assertThat(item.getReason()).isEqualTo(ERROR);
    assertThat(item.getMessage()).isEqualTo("failure");
    assertThat(item.getCauseType().get()).isEqualTo(ex.getClass());
    assertThat(item.getStackTrace()).isEqualTo(Throwables.getStackTraceAsString(ex).replace(System.lineSeparator(), "\n"));
  }

  @Test
  public void failure_mapFailure() {
    Result<String> base = Result.failure(new IllegalArgumentException("failure"));
    Failure testFailure = Failure.of(FailureReason.ERROR, new IllegalArgumentException("failure2"));
    Result<String> test = base.mapFailure(failure -> testFailure);
    assertThat(test.getFailure()).isSameAs(testFailure);
  }

  @Test
  public void failure_mapFailureItems() {
    Result<String> base = Result.failure(new IllegalArgumentException("failure"));
    FailureItem testFailureItem = FailureItem.of(FailureReason.ERROR, new IllegalArgumentException("failure2"));
    Result<String> test = base.mapFailureItems(failure -> testFailureItem);
    assertThat(test.getFailure().getMessage()).isEqualTo(base.getFailure().getMessage());
    assertThat(test.getFailure().getReason()).isEqualTo(base.getFailure().getReason());
    assertThat(test.getFailure().getItems()).allSatisfy(failureItem -> assertThat(failureItem).isSameAs(testFailureItem));
  }

  @Test
  public void failure_map_flatMap_ifSuccess() {
    Result<String> test = Result.failure(new IllegalArgumentException("failure"));
    Result<Integer> test1 = test.map(MAP_STRLEN);
    assertThat(test1).isSameAs(test);
    Result<Integer> test2 = test.flatMap(FUNCTION_STRLEN);
    assertThat(test2).isSameAs(test);
  }

  @Test
  public void failure_getValue() {
    Result<String> test = Result.failure(new IllegalArgumentException());
    assertThatIllegalStateException().isThrownBy(() -> test.getValue());
  }

  @Test
  public void failure_combineWith_success() {
    Result<String> failure = Result.failure(new IllegalArgumentException("failure"));
    Result<String> success = Result.success("World");
    Result<String> test = failure.combineWith(success, FUNCTION_MERGE);
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("failure");
  }

  @Test
  public void failure_combineWith_failure() {
    Result<String> failure1 = Result.failure(new IllegalArgumentException("failure"));
    Result<String> failure2 = Result.failure(new IllegalArgumentException("fail"));
    Result<String> test = failure1.combineWith(failure2, FUNCTION_MERGE);
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("failure, fail");
  }

  @Test
  public void failure_stream() {
    Result<String> failure = Result.failure(new IllegalArgumentException("failure"));
    assertThat(failure.stream().toArray()).isEmpty();
  }

  @Test
  public void failure_map_throwing() {
    Result<String> success = Result.failure(new IllegalArgumentException("failure"));
    Result<Integer> test = success.map(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("failure");
  }

  @Test
  public void failure_flatMap_throwing() {
    Result<String> success = Result.failure(new IllegalArgumentException("failure"));
    Result<Integer> test = success.flatMap(r -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("failure");
  }

  //-------------------------------------------------------------------------
  @Test
  public void failure_fromStatusMessageArgs_placeholdersMatchArgs1() {
    Result<String> failure = Result.failure(ERROR, "my {} failure", "blue");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my blue failure");
  }

  @Test
  public void failure_fromStatusMessageArgs_placeholdersMatchArgs2() {
    Result<String> failure = Result.failure(ERROR, "my {} {} failure", "blue", "rabbit");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my blue rabbit failure");
  }

  @Test
  public void failure_fromStatusMessageArgs_placeholdersExceedArgs() {
    Result<String> failure = Result.failure(ERROR, "my {} {} failure", "blue");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my blue {} failure");
  }

  @Test
  public void failure_fromStatusMessageArgs_placeholdersLessThanArgs1() {
    Result<String> failure = Result.failure(ERROR, "my {} failure", "blue", "rabbit");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my blue failure - [rabbit]");
  }

  @Test
  public void failure_fromStatusMessageArgs_placeholdersLessThanArgs2() {
    Result<String> failure = Result.failure(ERROR, "my {} failure", "blue", "rabbit", "carrot");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my blue failure - [rabbit, carrot]");
  }

  @Test
  public void failure_fromStatusMessageArgs_placeholdersLessThanArgs3() {
    Result<String> failure = Result.failure(ERROR, "my failure", "blue", "rabbit", "carrot");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my failure - [blue, rabbit, carrot]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void failure_fromResult_failure() {
    Result<String> failure = Result.failure(ERROR, "my failure");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my failure");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertThat(item.getReason()).isEqualTo(ERROR);
    assertThat(item.getMessage()).isEqualTo("my failure");
    assertThat(item.getCauseType().isPresent()).isEqualTo(false);
    assertThat(item.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(item.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(item.getStackTrace()).doesNotContain(".Result.failure(");
  }

  @Test
  public void failure_fromResult_success() {
    Result<String> success = Result.success("Hello");
    assertThatIllegalArgumentException().isThrownBy(() -> Result.failure(success));
  }

  //-------------------------------------------------------------------------
  @Test
  public void failure_fromFailure() {
    Failure failure = Failure.of(ERROR, "my failure");
    Result<Integer> test = Result.failure(failure);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my failure");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertThat(item.getReason()).isEqualTo(ERROR);
    assertThat(item.getMessage()).isEqualTo("my failure");
    assertThat(item.getCauseType().isPresent()).isEqualTo(false);
    assertThat(item.getStackTrace()).isNotNull();
  }

  //-------------------------------------------------------------------------
  @Test
  public void failure_fromFailureItem() {
    FailureItem inputItem = FailureItem.of(ERROR, "my failure");
    Result<Integer> test = Result.failure(inputItem);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("my failure");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertThat(item).isSameAs(inputItem);
  }

  //-------------------------------------------------------------------------
  @Test
  public void failure_fromFailureItemException() {
    FailureItem item1 = FailureItem.of(INVALID, "my failure");
    FailureItemException ex1 = new FailureItemException(item1);
    Failure fail1 = Failure.from(ex1);
    assertThat(fail1.getItems()).containsExactlyInAnyOrder(item1);

    Failure fail2 = Failure.from(new FailureException(fail1));
    assertThat(fail2).isSameAs(fail1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void ofNullable_nonNull() {
    Result<Integer> test = Result.ofNullable(6);
    assertThat(test.isFailure()).isFalse();
    assertThat(test.getValue().intValue()).isEqualTo(6);
  }

  @Test
  public void ofNullable_null() {
    Result<Integer> test = Result.ofNullable(null);
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("Found null where a value was expected");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertThat(item.getReason()).isEqualTo(MISSING_DATA);
    assertThat(item.getMessage()).isEqualTo("Found null where a value was expected");
    assertThat(item.getCauseType().isPresent()).isEqualTo(false);
    assertThat(item.getStackTrace()).isNotNull();
  }

  //-------------------------------------------------------------------------

  @Test
  public void ofOptional_nonEmpty() {
    Result<Integer> test = Result.ofOptional(Optional.of(6));
    assertThat(test.isFailure()).isFalse();
    assertThat(test.getValue().intValue()).isEqualTo(6);
  }

  @Test
  public void ofOptional_empty() {
    Result<Integer> test = Result.ofOptional(Optional.empty());
    assertThat(test.isFailure()).isTrue();
    assertThat(test.getFailure().getMessage()).isEqualTo("Found empty where a value was expected");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertThat(item.getReason()).isEqualTo(MISSING_DATA);
    assertThat(item.getMessage()).isEqualTo("Found empty where a value was expected");
    assertThat(item.getCauseType().isPresent()).isEqualTo(false);
    assertThat(item.getStackTrace()).isNotNull();
  }

  //-------------------------------------------------------------------------

  @Test
  public void of_with_success() {

    Result<String> test = Result.of(() -> "success");
    assertThat(test.isSuccess()).isEqualTo(true);
    assertThat(test.isFailure()).isEqualTo(false);
    assertThat(test.getValue()).isEqualTo("success");
  }

  @Test
  public void of_with_exception() {

    Result<String> test = Result.of(() -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.isFailure()).isEqualTo(true);
    assertThatIllegalStateException().isThrownBy(test::getValue);
  }

  @Test
  public void wrap_with_success() {
    Result<String> test = Result.wrap(() -> Result.success("success"));
    assertThat(test.isSuccess()).isEqualTo(true);
    assertThat(test.isFailure()).isEqualTo(false);
    assertThat(test.getValue()).isEqualTo("success");
  }

  @Test
  public void wrap_with_failure() {

    Result<String> test = Result.wrap(() -> Result.failure(ERROR, "Something failed"));
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.isFailure()).isEqualTo(true);
    assertThatIllegalStateException().isThrownBy(test::getValue);
  }

  @Test
  public void wrap_with_exception() {

    Result<String> test = Result.wrap(() -> {
      throw new IllegalArgumentException("Big bad error");
    });
    assertThat(test.isSuccess()).isEqualTo(false);
    assertThat(test.isFailure()).isEqualTo(true);
    assertThatIllegalStateException().isThrownBy(test::getValue);
  }

  //-------------------------------------------------------------------------
  @Test
  public void anyFailures_varargs() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertThat(Result.anyFailures(failure1, failure2)).isTrue();
    assertThat(Result.anyFailures(failure1, success1)).isTrue();
    assertThat(Result.anyFailures(success1, success2)).isFalse();
  }

  @Test
  public void anyFailures_collection() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertThat(Result.anyFailures(ImmutableList.of(failure1, failure2))).isTrue();
    assertThat(Result.anyFailures(ImmutableList.of(failure1, success1))).isTrue();
    assertThat(Result.anyFailures(ImmutableList.of(success1, success2))).isFalse();
  }

  @Test
  public void countFailures_varargs() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertThat(Result.countFailures(failure1, failure2)).isEqualTo(2);
    assertThat(Result.countFailures(failure1, success1)).isEqualTo(1);
    assertThat(Result.countFailures(success1, success2)).isEqualTo(0);
  }

  @Test
  public void countFailures_collection() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertThat(Result.countFailures(ImmutableList.of(failure1, failure2))).isEqualTo(2);
    assertThat(Result.countFailures(ImmutableList.of(failure1, success1))).isEqualTo(1);
    assertThat(Result.countFailures(ImmutableList.of(success1, success2))).isEqualTo(0);
  }

  @Test
  public void allSuccess_varargs() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertThat(Result.allSuccessful(failure1, failure2)).isFalse();
    assertThat(Result.allSuccessful(failure1, success1)).isFalse();
    assertThat(Result.allSuccessful(success1, success2)).isTrue();
  }

  @Test
  public void allSuccess_collection() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    assertThat(Result.allSuccessful(ImmutableList.of(failure1, failure2))).isFalse();
    assertThat(Result.allSuccessful(ImmutableList.of(failure1, success1))).isFalse();
    assertThat(Result.allSuccessful(ImmutableList.of(success1, success2))).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void combine_iterableWithFailures() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 2");
    Result<String> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<String> failure2 = Result.failure(ERROR, "failure 2");
    Set<Result<String>> results = ImmutableSet.of(success1, success2, failure1, failure2);

    assertThat(Result.combine(results, s -> s))
        .isFailure(FailureReason.MULTIPLE);
  }

  @Test
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

  @Test
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

  @Test
  public void flatCombine_iterableWithFailures() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 2");
    Result<String> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<String> failure2 = Result.failure(ERROR, "failure 2");
    Set<Result<String>> results = ImmutableSet.of(success1, success2, failure1, failure2);

    assertThat(Result.flatCombine(results, Result::success))
        .isFailure(FailureReason.MULTIPLE);
  }

  @Test
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

  @Test
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

  @Test
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

  @Test
  public void failure_fromResults_varargs1() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    Result<Object> test = Result.failure(success1, success2, failure1, failure2);
    Set<FailureItem> expected = new HashSet<>();
    expected.addAll(failure1.getFailure().getItems());
    expected.addAll(failure2.getFailure().getItems());
    assertThat(test.getFailure().getItems()).isEqualTo(expected);
  }

  @Test
  public void failure_fromResults_varargs2() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    Result<Object> failure1 = Result.failure(MISSING_DATA, "failure 1");
    Result<Object> failure2 = Result.failure(ERROR, "failure 2");
    Result<Object> test = Result.failure(success1, failure1, success2, failure2);
    Set<FailureItem> expected = new HashSet<>();
    expected.addAll(failure1.getFailure().getItems());
    expected.addAll(failure2.getFailure().getItems());
    assertThat(test.getFailure().getItems()).isEqualTo(expected);
  }

  @Test
  public void failure_fromResults_varargs_allSuccess() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    assertThatIllegalArgumentException().isThrownBy(() -> Result.failure(success1, success2));
  }

  @Test
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
    assertThat(test.getFailure().getItems()).isEqualTo(expected);
  }

  @Test
  public void failure_fromResults_collection_allSuccess() {
    Result<String> success1 = Result.success("success 1");
    Result<String> success2 = Result.success("success 1");
    assertThatIllegalArgumentException().isThrownBy(() -> Result.failure(Arrays.asList(success1, success2)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void generateFailureFromException() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(exception);
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("something went wrong");
  }

  @Test
  public void generateFailureFromExceptionWithMessage() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(exception, "my message");
    assertThat(test.getFailure().getReason()).isEqualTo(ERROR);
    assertThat(test.getFailure().getMessage()).isEqualTo("my message");
  }

  @Test
  public void generateFailureFromExceptionWithCustomStatus() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(CALCULATION_FAILED, exception);
    assertThat(test.getFailure().getReason()).isEqualTo(CALCULATION_FAILED);
    assertThat(test.getFailure().getMessage()).isEqualTo("something went wrong");
  }

  @Test
  public void generateFailureFromExceptionWithCustomStatusAndMessage() {
    Exception exception = new Exception("something went wrong");
    Result<Object> test = Result.failure(CALCULATION_FAILED, exception, "my message");
    assertThat(test.getFailure().getReason()).isEqualTo(CALCULATION_FAILED);
    assertThat(test.getFailure().getMessage()).isEqualTo("my message");
  }

  //-------------------------------------------------------------------------
  @Test
  public void failureDeduplicateFailure() {
    Result<Object> result = Result.failure(MISSING_DATA, "failure");
    FailureItem failure = result.getFailure().getItems().iterator().next();

    Result<Object> test = Result.failure(result, result);
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    assertThat(test.getFailure().getItems()).isEqualTo(ImmutableSet.of(failure));
    assertThat(test.getFailure().getMessage()).isEqualTo("failure");
  }

  @Test
  public void failureSameType() {
    Result<Object> failure1 = Result.failure(MISSING_DATA, "message 1");
    Result<Object> failure2 = Result.failure(MISSING_DATA, "message 2");
    Result<Object> failure3 = Result.failure(MISSING_DATA, "message 3");
    Result<?> composite = Result.failure(failure1, failure2, failure3);
    assertThat(composite.getFailure().getReason()).isEqualTo(MISSING_DATA);
    assertThat(composite.getFailure().getMessage()).isEqualTo("message 1, message 2, message 3");
  }

  @Test
  public void failureDifferentTypes() {
    Result<Object> failure1 = Result.failure(MISSING_DATA, "message 1");
    Result<Object> failure2 = Result.failure(CALCULATION_FAILED, "message 2");
    Result<Object> failure3 = Result.failure(ERROR, "message 3");
    Result<?> composite = Result.failure(failure1, failure2, failure3);
    assertThat(composite.getFailure().getReason()).isEqualTo(FailureReason.MULTIPLE);
    assertThat(composite.getFailure().getMessage()).isEqualTo("message 1, message 2, message 3");
  }

  //------------------------------------------------------------------------
  @Test
  public void createByBuilder_neitherValueNorFailure() {
    assertThatIllegalArgumentException().isThrownBy(() -> Result.meta().builder().build());
  }

  @Test
  public void createByBuilder_bothValueAndFailure() {
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> Result.meta().builder()
                .set("value", "A")
                .set("failure", Failure.of(CALCULATION_FAILED, "Fail"))
                .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void generatedStackTrace() {
    Result<Object> test = Result.failure(FailureReason.INVALID, "my {} {} failure", "big", "bad");
    assertThat(test.getFailure().getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getFailure().getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
    FailureItem item = test.getFailure().getItems().iterator().next();
    assertThat(item.getCauseType()).isEmpty();
    assertThat(item.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(item.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(item.getStackTrace()).startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure");
    assertThat(item.getStackTrace()).contains(".generatedStackTrace(");
    assertThat(item.toString()).isEqualTo("INVALID: my big bad failure");
  }

  //-------------------------------------------------------------------------
  @Test
  public void generatedStackTrace_Failure() {
    Failure test = Failure.of(FailureReason.INVALID, "my {} {} failure", "big", "bad");
    assertThat(test.getReason()).isEqualTo(FailureReason.INVALID);
    assertThat(test.getMessage()).isEqualTo("my big bad failure");
    assertThat(test.getItems().size()).isEqualTo(1);
    FailureItem item = test.getItems().iterator().next();
    assertThat(item.getCauseType()).isEmpty();
    assertThat(item.getStackTrace()).doesNotContain(".FailureItem.of(");
    assertThat(item.getStackTrace()).doesNotContain(".Failure.of(");
    assertThat(item.getStackTrace()).startsWith("com.opengamma.strata.collect.result.FailureItem: my big bad failure");
    assertThat(item.getStackTrace()).contains(".generatedStackTrace_Failure(");
    assertThat(item.toString()).isEqualTo("INVALID: my big bad failure");
  }

  //------------------------------------------------------------------------
  @Test
  public void equalsHashCode() {
    Exception ex = new Exception("Problem");
    Result<Object> a1 = Result.failure(MISSING_DATA, ex);
    Result<Object> a2 = Result.failure(MISSING_DATA, ex);
    Result<Object> b = Result.failure(ERROR, "message 2");
    Result<Object> c = Result.success("Foo");
    Result<Object> d = Result.success("Bar");

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
    assertThat(a1.equals(d)).isEqualTo(false);

    assertThat(b.equals(a1)).isEqualTo(false);
    assertThat(b.equals(a2)).isEqualTo(false);
    assertThat(b.equals(b)).isEqualTo(true);
    assertThat(b.equals(c)).isEqualTo(false);
    assertThat(b.equals(d)).isEqualTo(false);

    assertThat(c.equals(a1)).isEqualTo(false);
    assertThat(c.equals(a2)).isEqualTo(false);
    assertThat(c.equals(b)).isEqualTo(false);
    assertThat(c.equals(c)).isEqualTo(true);
    assertThat(c.equals(d)).isEqualTo(false);

    assertThat(d.equals(a1)).isEqualTo(false);
    assertThat(d.equals(a2)).isEqualTo(false);
    assertThat(d.equals(b)).isEqualTo(false);
    assertThat(d.equals(c)).isEqualTo(false);
    assertThat(d.equals(d)).isEqualTo(true);
  }

  // Following are tests for Result using the AssertJ assertions
  // Primarily a test of the assertions themselves

  @Test
  public void assert_success() {
    Result<String> test = Result.success("success");
    assertThat(test)
        .isSuccess()
        .hasValue("success");
  }

  // We can't use assertThrows as that rethrows AssertionError
  @Test
  public void assert_success_getFailure() {
    Result<String> test = Result.success("success");
    try {
      assertThat(test).isFailure();
      fail("Should have thrown AssertionError");
    } catch (AssertionError ex) {
      // expected
    }
  }

  @Test
  public void assert_success_map() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.map(MAP_STRLEN);
    assertThat(test).isSuccess().hasValue(7);
  }

  @Test
  public void assert_success_flatMap() {
    Result<String> success = Result.success("success");
    Result<Integer> test = success.flatMap(FUNCTION_STRLEN);
    assertThat(test).isSuccess().hasValue(7);
  }

  @Test
  public void assert_success_combineWith_success() {
    Result<String> success1 = Result.success("Hello");
    Result<String> success2 = Result.success("World");
    Result<String> test = success1.combineWith(success2, FUNCTION_MERGE);
    assertThat(test).isSuccess().hasValue("Hello World");
  }

  @Test
  public void assert_success_combineWith_failure() {
    Result<String> success = Result.success("Hello");
    Result<String> failure = Result.failure(new IllegalArgumentException());
    Result<String> test = success.combineWith(failure, FUNCTION_MERGE);
    assertThat(test).isFailure(ERROR);
    assertThat(test.getFailure().getItems().size()).isEqualTo(1);
  }

  @Test
  public void assert_failure() {
    Result<String> test = Result.failure(new IllegalArgumentException("failure"));
    assertThat(test)
        .isFailure(ERROR)
        .hasFailureMessageMatching("failure");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Result<Object> failure = Result.failure(MISSING_DATA, "message 1");
    TestHelper.coverImmutableBean(failure);
    TestHelper.coverImmutableBean(failure.getFailure());
    TestHelper.coverImmutableBean(failure.getFailure().getFirstItem());

    Result<String> success = Result.success("Hello");
    TestHelper.coverImmutableBean(success);

    TestHelper.coverEnum(FailureReason.class);
  }

}
