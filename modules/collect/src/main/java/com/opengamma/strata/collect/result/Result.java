/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.Messages;

/**
 * The result of an operation, either success or failure.
 * <p>
 * This provides a functional approach to error handling, that can be used instead of exceptions.
 * A success result contains a non-null result value.
 * A failure result contains details of the {@linkplain Failure failure} that occurred.
 * <p>
 * Methods using this approach to error handling are expected to return {@code Result<T>}
 * and not throw exceptions. The factory method {@link #of(Supplier)} and related methods
 * can be used to capture exceptions and convert them to failure results.
 * <p>
 * Application code using a result should also operate in a functional style.
 * Use {@link #map(Function)} and {@link #flatMap(Function)} in preference to
 * {@link #isSuccess()} and {@link #getValue()}.
 * <pre>
 *  Result{@literal <Foo>} intermediateResult = calculateIntermediateResult();
 *  return intermediateResult.flatMap(foo -&gt; calculateFinalResult(foo, ...));
 * </pre>
 * <p>
 * Results can be generated using the factory methods on this class.
 *
 * @param <T> the type of the underlying result for a successful invocation
 */
@BeanDefinition(builderScope = "private")
public final class Result<T>
    implements ImmutableBean, Serializable {
  // two properties are used where one might do to reduce serialized data size

  /**
   * The value.
   * This is only present if the result is a success.
   */
  @PropertyDefinition(get = "field")
  private final T value;
  /**
   * The failure.
   * This is only present if the result is a failure.
   */
  @PropertyDefinition(get = "field")
  private final Failure failure;

  //-------------------------------------------------------------------------
  /**
   * Creates a successful result wrapping a value.
   * <p>
   * This returns a successful result object for the non-null value.
   * <p>
   * Note that passing an instance of {@code Failure} to this method would
   * be a programming error.
   *
   * @param <R> the type of the value
   * @param value  the result value
   * @return a successful result wrapping the value
   */
  public static <R> Result<R> success(R value) {
    return new Result<>(ArgChecker.notNull(value, "value"));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a failed result specifying the failure reason.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   *
   * @param <R> the expected type of the result
   * @param reason  the result reason
   * @param message  a message explaining the failure, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  public static <R> Result<R> failure(FailureReason reason, String message, Object... messageArgs) {
    String msg = Messages.format(message, messageArgs);
    return new Result<>(Failure.of(FailureItem.ofAutoStackTrace(reason, msg, 1)));
  }

  /**
   * Creates a success {@code Result} wrapping the value produced by the
   * supplier.
   * <p>
   * Note that if the supplier throws an exception, this will be caught
   * and converted to a failure {@code Result}.
   * This recognizes and handles {@link FailureException} and {@link FailureItemProvider} exceptions.
   *
   * @param <T> the type of the value
   * @param supplier  supplier of the result value
   * @return a success {@code Result} wrapping the value produced by the supplier
   */
  public static <T> Result<T> of(Supplier<T> supplier) {
    try {
      return success(supplier.get());
    } catch (Exception ex) {
      return failure(ex);
    }
  }

  /**
   * Creates a {@code Result} wrapping the result produced by the supplier.
   * <p>
   * Note that if the supplier throws an exception, this will be caught
   * and converted to a failure {@code Result}.
   * This recognizes and handles {@link FailureException} and {@link FailureItemProvider} exceptions.
   *
   * @param <T> the type of the result value
   * @param supplier  supplier of the result
   * @return a {@code Result} produced by the supplier
   */
  public static <T> Result<T> wrap(Supplier<Result<T>> supplier) {
    try {
      return supplier.get();
    } catch (Exception ex) {
      return failure(ex);
    }
  }

  /**
   * Creates a failed result caused by a throwable.
   * <p>
   * This recognizes and handles {@link FailureException} and {@link FailureItemProvider} exceptions.
   * If the exception type is not recognized, the failure will have a reason of {@code ERROR}.
   *
   * @param <R> the expected type of the result
   * @param cause  the cause of the failure
   * @return a failure result
   */
  public static <R> Result<R> failure(Throwable cause) {
    return failure(Failure.from(cause));
  }

  /**
   * Creates a failed result caused by an exception.
   * <p>
   * This recognizes and handles {@link FailureException} and {@link FailureItemProvider} exceptions.
   * If the exception type is not recognized, the failure will have a reason of {@code ERROR}.
   *
   * @param <R> the expected type of the result
   * @param cause  the cause of the failure
   * @return a failure result
   */
  public static <R> Result<R> failure(Exception cause) {
    // this method is retained to ensure binary compatibility
    return failure(Failure.from(cause));
  }

  /**
   * Creates a failed result caused by a throwable.
   * <p>
   * The failure will have a reason of {@code ERROR}.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   *
   * @param <R> the expected type of the result
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  public static <R> Result<R> failure(Throwable cause, String message, Object... messageArgs) {
    return new Result<>(Failure.of(FailureReason.ERROR, cause, message, messageArgs));
  }

  /**
   * Creates a failed result caused by an exception.
   * <p>
   * The failure will have a reason of {@code ERROR}.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   *
   * @param <R> the expected type of the result
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  public static <R> Result<R> failure(Exception cause, String message, Object... messageArgs) {
    // this method is retained to ensure binary compatibility
    return new Result<>(Failure.of(FailureReason.ERROR, cause, message, messageArgs));
  }

  /**
   * Creates a failed result caused by a throwable with a specified reason.
   *
   * @param <R> the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @return a failure result
   */
  public static <R> Result<R> failure(FailureReason reason, Throwable cause) {
    return new Result<>(Failure.of(reason, cause));
  }

  /**
   * Creates a failed result caused by an exception with a specified reason.
   *
   * @param <R> the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @return a failure result
   */
  public static <R> Result<R> failure(FailureReason reason, Exception cause) {
    // this method is retained to ensure binary compatibility
    return new Result<>(Failure.of(reason, cause));
  }

  /**
   * Creates a failed result caused by a throwable with a specified reason and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   *
   * @param <R> the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  public static <R> Result<R> failure(
      FailureReason reason,
      Throwable cause,
      String message,
      Object... messageArgs) {

    return new Result<>(Failure.of(reason, cause, message, messageArgs));
  }

  /**
   * Creates a failed result caused by an exception with a specified reason and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   *
   * @param <R> the expected type of the result
   * @param reason  the result reason
   * @param cause  the cause of the failure
   * @param message  a message explaining the failure, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return a failure result
   */
  public static <R> Result<R> failure(
      FailureReason reason,
      Exception cause,
      String message,
      Object... messageArgs) {

    // this method is retained to ensure binary compatibility
    return new Result<>(Failure.of(reason, cause, message, messageArgs));
  }

  /**
   * Returns a failed result from another failed result.
   * <p>
   * This method ensures the result type matches the expected type.
   * If the specified result is a successful result then an exception is thrown.
   *
   * @param <R> the expected result type
   * @param failureResult  a failure result
   * @return a failure result of the expected type
   * @throws IllegalArgumentException if the result is a success
   */
  @SuppressWarnings("unchecked")
  public static <R> Result<R> failure(Result<?> failureResult) {
    if (failureResult.isSuccess()) {
      throw new IllegalArgumentException("Result must be a failure");
    }
    return (Result<R>) failureResult;
  }

  /**
   * Creates a failed result combining multiple failed results.
   * <p>
   * The input results can be successes or failures, only the failures will be included in the created result.
   * Intended to be used with {@link #anyFailures(Result...)}.
   * <blockquote><pre>
   *   if (Result.anyFailures(result1, result2, result3) {
   *     return Result.failure(result1, result2, result3);
   *   }
   * </pre></blockquote>
   *
   * @param <R> the expected type of the result
   * @param result1  the first result
   * @param result2  the second result
   * @param results  the rest of the results
   * @return a failed result wrapping multiple other failed results
   * @throws IllegalArgumentException if all of the results are successes
   */
  public static <R> Result<R> failure(Result<?> result1, Result<?> result2, Result<?>... results) {
    ArgChecker.notNull(result1, "result1");
    ArgChecker.notNull(result2, "result2");
    ArgChecker.notNull(results, "results");
    ImmutableList<Result<?>> list = ImmutableList.<Result<?>>builder()
        .add(result1)
        .add(result2)
        .addAll(Arrays.asList(results))
        .build();
    return failure(list);
  }

  /**
   * Creates a failed result combining multiple failed results.
   * <p>
   * The input results can be successes or failures, only the failures will be included in the created result.
   * Intended to be used with {@link #anyFailures(Iterable)}.
   * <blockquote><pre>
   *   if (Result.anyFailures(results) {
   *     return Result.failure(results);
   *   }
   * </pre></blockquote>
   *
   * @param <R> the expected type of the result
   * @param results  multiple results, of which at least one must be a failure, not empty
   * @return a failed result wrapping multiple other failed results
   * @throws IllegalArgumentException if results is empty or contains nothing but successes
   */
  public static <R> Result<R> failure(Iterable<? extends Result<?>> results) {
    ArgChecker.notEmpty(results, "results");
    ImmutableSet<FailureItem> items = Guavate.stream(results)
        .filter(Result::isFailure)
        .map(Result::getFailure)
        .flatMap(f -> f.getItems().stream())
        .collect(Guavate.toImmutableSet());
    if (items.isEmpty()) {
      throw new IllegalArgumentException("All results were successes");
    }
    return new Result<>(Failure.of(items));
  }

  /**
   * Creates a failed result containing a failure.
   * <p>
   * This is useful for converting an existing {@code Failure} instance to a result.
   *
   * @param <R> the expected type of the result
   * @param failure  details of the failure
   * @return a failed result containing the specified failure
   */
  public static <R> Result<R> failure(Failure failure) {
    return new Result<>(failure);
  }

  /**
   * Creates a failed result containing a failure item.
   * <p>
   * This is useful for converting an existing {@code FailureItem} instance to a result.
   *
   * @param <R> the expected type of the result
   * @param failureItem  details of the failure
   * @return a failed result containing the specified failure
   */
  public static <R> Result<R> failure(FailureItem failureItem) {
    return new Result<>(Failure.of(failureItem));
  }

  /**
   * Returns a success result containing the value if it is non-null, else returns a failure result
   * with the specified reason and message.
   * <p>
   * This is useful for interoperability with APIs that return {@code null}, for example {@code Map.get()}, where
   * a missing value should be treated as a failure.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   *
   * @param <R> the expected type of the result
   * @param value  the potentially null value
   * @param reason  the reason for the failure
   * @param message  a message explaining the failure, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return a success result if the value is non-null, else a failure result
   */
  public static <R> Result<R> ofNullable(
      R value,
      FailureReason reason,
      String message,
      Object... messageArgs) {

    if (value != null) {
      return success(value);
    } else {
      return failure(reason, message, messageArgs);
    }
  }

  /**
   * Returns a success result containing the value if it is non-null, else returns a failure result
   * with a reason of {@link FailureReason#MISSING_DATA} and message to say an unexpected null was found.
   * <p>
   * This is useful for interoperability with APIs that can return {@code null} but where null is not expected.
   *
   * @param <R> the expected type of the result
   * @param value  the potentially null value
   * @return a success result if the value is non-null, else a failure result
   */
  public static <R> Result<R> ofNullable(R value) {
    return ofNullable(value, FailureReason.MISSING_DATA, "Found null where a value was expected");
  }

  /**
   * Returns a success result containing the value if present, else returns a failure result
   * with the specified reason and message.
   * <p>
   * The message is produced using a template that contains zero to many "{}" placeholders.
   * Each placeholder is replaced by the next available argument.
   * If there are too few arguments, then the message will be left with placeholders.
   * If there are too many arguments, then the excess arguments are appended to the
   * end of the message. No attempt is made to format the arguments.
   * See {@link Messages#format(String, Object...)} for more details.
   *
   * @param <R> the expected type of the result
   * @param value  the potentially null value
   * @param reason  the reason for the failure
   * @param message  a message explaining the failure, uses "{}" for inserting {@code messageArgs}
   * @param messageArgs  the arguments for the message
   * @return a success result if the value is non-null, else a failure result
   */
  public static <R> Result<R> ofOptional(
      Optional<R> value,
      FailureReason reason,
      String message,
      Object... messageArgs) {

    return value
        .map(Result::success)
        .orElse(Result.failure(reason, message, messageArgs));
  }

  /**
   * Returns a success result containing the value if present, else returns a failure result
   * with a reason of {@link FailureReason#MISSING_DATA} and message to say an unexpected empty value was found.
   *
   * @param <R> the expected type of the result
   * @param value  the potentially null value
   * @return a success result if the value is non-null, else a failure result
   */
  public static <R> Result<R> ofOptional(Optional<R> value) {
    return ofOptional(value, FailureReason.MISSING_DATA, "Found empty where a value was expected");
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if all the results are successful.
   * 
   * @param results  the results to check
   * @return true if all of the results are successes
   */
  public static boolean allSuccessful(Result<?>... results) {
    return Stream.of(results).allMatch(Result::isSuccess);
  }

  /**
   * Checks if all the results are successful.
   * 
   * @param results  the results to check
   * @return true if all of the results are successes
   */
  public static boolean allSuccessful(Iterable<? extends Result<?>> results) {
    return Guavate.stream(results).allMatch(Result::isSuccess);
  }

  /**
   * Checks if any of the results are failures.
   * 
   * @param results  the results to check
   * @return true if any of the results are failures
   */
  public static boolean anyFailures(Result<?>... results) {
    return Stream.of(results).anyMatch(Result::isFailure);
  }

  /**
   * Checks if any of the results are failures.
   * 
   * @param results  the results to check
   * @return true if any of the results are failures
   */
  public static boolean anyFailures(Iterable<? extends Result<?>> results) {
    return Guavate.stream(results).anyMatch(Result::isFailure);
  }

  /**
   * Counts how many of the results are failures.
   *
   * @param results  the results to check
   * @return the number of results that are failures
   */
  public static long countFailures(Result<?>... results) {
    return Stream.of(results).filter(Result::isFailure).count();
  }

  /**
   * Counts how many of the results are failures.
   *
   * @param results  the results to check
   * @return the number of results that are failures
   */
  public static long countFailures(Iterable<? extends Result<?>> results) {
    return Guavate.stream(results).filter(Result::isFailure).count();
  }

  /**
   * Takes a collection of results, checks if all of them are successes
   * and then applies the supplied function to the successes wrapping
   * the result in a success result. If any of the initial results was
   * a failure, then a failure result reflecting the failures in the
   * initial results is returned.
   * <p>
   * If an exception is thrown when the function is applied, this will
   * be caught and a failure {@code Result} returned.
   * <p>
   * The following code shows where this method can be used. The code:
   * <blockquote><pre>
   *   Set&lt;Result&lt;MyData&gt;&gt; results = goAndGatherData();
   *   if (Result.anyFailures(results)) {
   *     return Result.failure(results);
   *   } else {
   *     Set&lt;FooData&gt; combined =
   *         results.stream()
   *             .map(Result::getValue)
   *             .map(MyData::transformToFoo)
   *             .collect(toSet());
   *     return Result.success(combined);
   *   }
   * </pre></blockquote>
   * can be replaced with:
   * <blockquote><pre>
   *   Set&lt;Result&lt;MyData&gt;&gt; results = goAndGatherData();
   *   return Result.combine(results, myDataStream -&gt;
   *       myDataStream
   *           .map(MyData::transformToFoo)
   *           .collect(toSet())
   *   );
   * </pre></blockquote>
   *
   * @param results  the results to be transformed if they are all successes
   * @param function  the function to apply to the stream of results if they were all successes
   * @param <T>  the type of the values held in the input results
   * @param <R>  the type of the values held in the transformed results
   * @return a success result holding the result of applying the function to the
   *   input results if they were all successes, a failure otherwise
   */
  public static <T, R> Result<R> combine(
      Iterable<? extends Result<T>> results,
      Function<Stream<T>, R> function) {

    try {
      return allSuccessful(results) ?
          success(function.apply(extractSuccesses(results))) :
          failure(results);

    } catch (Exception ex) {
      return failure(ex);
    }
  }

  /**
   * Takes a collection of results, checks if all of them are successes
   * and then applies the supplied function to the successes. If any of
   * the initial results was a failure, then a failure result reflecting
   * the failures in the initial results is returned.
   * <p>
   * If an exception is thrown when the function is applied, this will
   * be caught and a failure {@code Result} returned.
   * <p>
   * The following code shows where this method can be used. The code:
   * <blockquote><pre>
   *   Set&lt;Result&lt;MyData&gt;&gt; results = goAndGatherData();
   *   if (Result.anyFailures(results)) {
   *     return Result.failure(results);
   *   } else {
   *     Set&lt;FooData&gt; combined =
   *         results.stream()
   *             .map(Result::getValue)
   *             .map(MyData::transformToFoo)
   *             .collect(toSet());
   *     return doSomethingReturningResult(combined); // this could fail
   *   }
   * </pre></blockquote>
   * can be replaced with:
   * <blockquote><pre>
   *   Set&lt;Result&lt;MyData&gt;&gt; results = goAndGatherData();
   *   return Result.flatCombine(results, myDataStream -&gt; {
   *     Set&lt;CombinedData&gt; combined =
   *         myDataStream
   *             .map(MyData::transformToFoo)
   *             .collect(toSet());
   *     return doSomethingReturningResult(combined); // this could fail
   *   });
   * </pre></blockquote>
   *
   * @param results  the results to be transformed if they are all successes
   * @param function  the function to apply to the stream of results if they were all successes
   * @param <T>  the type of the values held in the input results
   * @param <R>  the type of the values held in the transformed results
   * @return a result holding the result of applying the function to the
   *   input results if they were all successes, a failure otherwise
   */
  public static <T, R> Result<R> flatCombine(
      Iterable<? extends Result<T>> results,
      Function<Stream<T>, Result<R>> function) {

    try {
      return allSuccessful(results) ?
          function.apply(extractSuccesses(results)) :
          failure(results);

    } catch (Exception ex) {
      return failure(ex);
    }
  }

  private static <T> Stream<T> extractSuccesses(Iterable<? extends Result<T>> results) {
    return Guavate.stream(results).map(Result::getValue);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * 
   * @param value  the value to create from
   */
  private Result(T value) {
    this.value = value;
    this.failure = null;
  }

  /**
   * Creates an instance.
   * 
   * @param failure  the failure to create from
   */
  private Result(Failure failure) {
    this.value = null;
    this.failure = failure;
  }

  @ImmutableValidator
  private void validate() {
    if (value == null && failure == null) {
      throw new IllegalArgumentException("Both value and failure are null");
    }
    if (value != null && failure != null) {
      throw new IllegalArgumentException("Both value and failure are non-null");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Indicates if this result represents a successful call and has a result available.
   * <p>
   * This is the opposite of {@link #isFailure()}.
   *
   * @return true if the result represents a success and a value is available
   */
  public boolean isSuccess() {
    return value != null;
  }

  /**
   * Executes the given consumer if the result represents a successful call and has a result available.
   *
   * @param consumer the consumer to be decorated
   */
  public void ifSuccess(Consumer<? super T> consumer) {
    if (value != null) {
      consumer.accept(value);
    }
  }

  /**
   * Indicates if this result represents a failure.
   * <p>
   * This is the opposite of {@link #isSuccess()}.
   *
   * @return true if the result represents a failure
   */
  public boolean isFailure() {
    return failure != null;
  }

  /**
   * Executes the given consumer if the result represents a failure.
   *
   * @param consumer the consumer to be decorated
   */
  public void ifFailure(Consumer<Failure> consumer) {
    if (failure != null) {
      consumer.accept(failure);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the result value if calculated successfully, empty if a failure occurred.
   *
   * @return the result value if success, empty if failure
   */
  public Optional<T> get() {
    return Optional.ofNullable(value);
  }

  /**
   * Returns the actual result value if calculated successfully, throwing an
   * exception if a failure occurred.
   * <p>
   * If this result is a failure then an {@code IllegalStateException} will be thrown.
   * To avoid this, call {@link #isSuccess()} or {@link #isFailure()} first.
   * <p>
   * Application code is recommended to use {@link #map(Function)} and
   * {@link #flatMap(Function)} in preference to this method.
   *
   * @return the result value, only available if calculated successfully
   * @throws IllegalStateException if called on a failure result
   */
  public T getValue() {
    if (isFailure()) {
      throw new IllegalStateException("Unable to get a value from a failure result: " + getFailure().getMessage());
    }
    return value;
  }

  /**
   * Returns the actual result value if calculated successfully, or the specified
   * default value if a failure occurred.
   * <p>
   * If this result is a success then the result value is returned.
   * If this result is a failure then the default value is returned.
   * The default value may be null.
   * <p>
   * Application code is recommended to use {@link #map(Function)} and
   * {@link #flatMap(Function)} in preference to this method.
   *
   * @param defaultValue  the default value to return if the result is a failure
   * @return either the result value or the default value
   */
  public T getValueOrElse(T defaultValue) {
    return (isSuccess() ? value : defaultValue);
  }

  /**
   * Returns the actual result value if calculated successfully, else the
   * specified function is applied to the {@code Failure} that occurred.
   * <p>
   * If this result is a success then the result value is returned.
   * If this result is a failure then the function is applied to the failure.
   * The function must not be null.
   * <p>
   * This method can be used in preference to {@link #getValueOrElse(Object)}
   * when the default value is expensive to create. In such cases, the
   * default value will get created on each call, even though it will be
   * immediately discarded if the result is a success.
   *
   * @param mapper  function used to generate a default value. The function
   *   has no obligation to use the input {@code Failure} (in other words it can
   *   behave as a {@code Supplier<T>} if desired).
   * @return either the result value or the result of the function
   */
  public T getValueOrElseApply(Function<Failure, T> mapper) {
    ArgChecker.notNull(mapper, "mapper");
    return (isSuccess() ? value : mapper.apply(failure));
  }

  /**
   * Returns the failure instance indicating the reason why the calculation failed.
   * <p>
   * If this result is a success then an IllegalStateException will be thrown.
   * To avoid this, call {@link #isSuccess()} or {@link #isFailure()} first.
   *
   * @return the details of the failure, only available if calculation failed
   * @throws IllegalStateException if called on a success result
   */
  public Failure getFailure() {
    if (isSuccess()) {
      throw new IllegalStateException("Unable to get a failure from a success result");
    }
    return failure;
  }

  //-------------------------------------------------------------------------
  /**
   * Processes a successful result by applying a function that alters the value.
   * <p>
   * This operation allows post-processing of a result value.
   * The specified function represents a conversion to be performed on the value.
   * <p>
   * If this result is a success, then the specified function is invoked.
   * The return value of the specified function is returned to the caller
   * wrapped in a success result. If an exception is thrown when the function
   * is invoked, this will be caught and a failure {@code Result} returned.
   * <p>
   * If this result is a failure, then {@code this} is returned.
   * The specified function is not invoked.
   * <p>
   * For example, it allows a {@code double} to be converted to a string:
   * <blockquote><pre>
   *   result = ...
   *   return result.map(value -&gt; Double.toString(value));
   * </pre></blockquote>
   *
   * @param <R>  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the new result
   */
  public <R> Result<R> map(Function<? super T, ? extends R> function) {
    if (isSuccess()) {
      try {
        return success(function.apply(value));
      } catch (Exception ex) {
        return failure(ex);
      }
    } else {
      return failure(this);
    }
  }

  /**
   * Processes a failed result by applying a function that alters the failure.
   * <p>
   * This operation allows post-processing of a result failure.
   * The specified function represents a conversion to be performed on the failure.
   * <p>
   * If this result is a failure, then the specified function is invoked.
   * The return value of the specified function is returned to the caller
   * wrapped in a failure result. If an exception is thrown when the function
   * is invoked, this will be caught and a failure {@code Result} returned.
   * <p>
   * If this result is a success, then {@code this} is returned.
   * The specified function is not invoked.
   *
   * @param function  the function to transform the failure with
   * @return the new result
   */
  public Result<T> mapFailure(Function<Failure, Failure> function) {
    if (isFailure()) {
      try {
        return failure(function.apply(failure));
      } catch (Exception ex) {
        return failure(ex);
      }
    } else {
      return this;
    }
  }

  /**
   * Processes a failed result by applying a function that alters the failure items.
   * <p>
   * This operation allows post-processing of a result failure.
   * The specified function represents a conversion to be performed on the failure.
   * <p>
   * If this result is a failure, then the specified function is invoked.
   * The return values of the specified function is returned to the caller
   * wrapped in a failure result. If an exception is thrown when the function
   * is invoked, this will be caught and a failure {@code Result} returned.
   * <p>
   * If this result is a success, then {@code this} is returned.
   * The specified function is not invoked.
   *
   * @param function  the function to transform the failure with
   * @return the new result
   */
  public Result<T> mapFailureItems(Function<FailureItem, FailureItem> function) {
    if (isFailure()) {
      try {
        return failure(failure.mapItems(function));
      } catch (Exception ex) {
        return failure(ex);
      }
    } else {
      return this;
    }
  }

  /**
   * Processes a successful result by applying a function that returns another result.
   * <p>
   * This operation allows chaining of function calls that produce a result.
   * The specified function will typically call another method that returns a result.
   * <p>
   * If this result is a success, then the specified function is invoked.
   * The return value of the specified function is returned to the caller and may be
   * a success or failure. If an exception is thrown when the function
   * is invoked, this will be caught and a failure {@code Result} returned.
   * <p>
   * If this result is a failure, then an equivalent failure is returned.
   * The specified function is not invoked.
   * <p>
   * For example,
   * <blockquote><pre>
   *   result = ...
   *   return result.flatMap(value -&gt; doSomething(value));
   * </pre></blockquote>
   *
   * @param <R>  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the new result
   */
  public <R> Result<R> flatMap(Function<? super T, Result<R>> function) {
    if (isSuccess()) {
      try {
        return Objects.requireNonNull(function.apply(value));
      } catch (Exception ex) {
        return failure(ex);
      }
    } else {
      return failure(this);
    }
  }

  /**
   * Combines this result with another result.
   * <p>
   * This operation allows two results to be combined handling succeess and failure.
   * <p>
   * If both results are a success, then the specified function is invoked to combine them.
   * The return value of the specified function is returned to the caller and may be
   * a success or failure.
   * <p>
   * If either result is a failure, then a combination failure is returned.
   * The specified function is not invoked.
   * <blockquote><pre>
   *   result1 = ...
   *   result2 = ...
   *   return result1.combineWith(result2, (value1, value2) -&gt; doSomething(value1, value2));
   * </pre></blockquote>
   *
   * @param other  another result
   * @param function  a function for combining values from two results
   * @param <U>  the type of the value in the other result
   * @param <R>  the type of the value in the returned result
   * @return a the result of combining the result values or a failure if either result is a failure
   */
  public <U, R> Result<R> combineWith(Result<U> other, BiFunction<T, U, Result<R>> function) {
    ArgChecker.notNull(other, "other");
    ArgChecker.notNull(function, "function");
    if (isSuccess() && other.isSuccess()) {
      try {
        return Objects.requireNonNull(function.apply(value, other.value));
      } catch (Exception ex) {
        return failure(ex);
      }
    } else {
      return failure(this, other);
    }
  }

  /**
   * Converts this result to a stream.
   * <p>
   * If this result is a success then a single element stream containing the result value is returned.
   * If this result is a failure then an empty stream is returned.
   *
   * @return a stream of size one or zero
   */
  public Stream<T> stream() {
    return (isSuccess() ? Stream.of(value) : Stream.empty());
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code Result}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static Result.Meta meta() {
    return Result.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code Result}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R> Result.Meta<R> metaResult(Class<R> cls) {
    return Result.Meta.INSTANCE;
  }

  static {
    MetaBean.register(Result.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private Result(
      T value,
      Failure failure) {
    this.value = value;
    this.failure = failure;
    validate();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Result.Meta<T> metaBean() {
    return Result.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Result<?> other = (Result<?>) obj;
      return JodaBeanUtils.equal(value, other.value) &&
          JodaBeanUtils.equal(failure, other.failure);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(value);
    hash = hash * 31 + JodaBeanUtils.hashCode(failure);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("Result{");
    buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
    buf.append("failure").append('=').append(JodaBeanUtils.toString(failure));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Result}.
   * @param <T>  the type
   */
  public static final class Meta<T> extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    @SuppressWarnings("rawtypes")
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code value} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<T> value = (DirectMetaProperty) DirectMetaProperty.ofImmutable(
        this, "value", Result.class, Object.class);
    /**
     * The meta-property for the {@code failure} property.
     */
    private final MetaProperty<Failure> failure = DirectMetaProperty.ofImmutable(
        this, "failure", Result.class, Failure.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "value",
        "failure");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return value;
        case -1086574198:  // failure
          return failure;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends Result<T>> builder() {
      return new Result.Builder<>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends Result<T>> beanType() {
      return (Class) Result.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code value} property.
     * @return the meta-property, not null
     */
    public MetaProperty<T> value() {
      return value;
    }

    /**
     * The meta-property for the {@code failure} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Failure> failure() {
      return failure;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return ((Result<?>) bean).value;
        case -1086574198:  // failure
          return ((Result<?>) bean).failure;
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code Result}.
   * @param <T>  the type
   */
  private static final class Builder<T> extends DirectPrivateBeanBuilder<Result<T>> {

    private T value;
    private Failure failure;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return value;
        case -1086574198:  // failure
          return failure;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<T> set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          this.value = (T) newValue;
          break;
        case -1086574198:  // failure
          this.failure = (Failure) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Result<T> build() {
      return new Result<>(
          value,
          failure);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("Result.Builder{");
      buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
      buf.append("failure").append('=').append(JodaBeanUtils.toString(failure));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
