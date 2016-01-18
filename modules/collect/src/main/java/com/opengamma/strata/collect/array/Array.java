/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.function.IntObjConsumer;
import com.opengamma.strata.collect.function.IntObjFunction;

/**
 * Base interface for all array types.
 * <p>
 * This provides an abstraction over data structures that represent an array accessed by index.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 * 
 * @param <T>  the type of the element, the boxed type if primitive
 */
public interface Array<T> {

  /**
   * Obtains an empty immutable array.
   * 
   * @param <R>  the type of the value in the array
   * @return the empty immutable array
   */
  @SuppressWarnings("unchecked")
  public static <R> Array<R> empty() {
    return ObjectArray.empty();
  }

  /**
   * Obtains an instance from a list.
   * 
   * @param <R>  the type of the value in the array
   * @param values  the values
   * @return an array containing the specified values
   */
  public static <R> Array<R> of(List<? extends R> values) {
    if (values.size() == 0) {
      return empty();
    }
    return ObjectArray.of(ImmutableList.copyOf(values));
  }

  /**
   * Obtains an immutable array from the specified values.
   * 
   * @param <R>  the type of the value in the array
   * @param values  the values
   * @return an array containing the specified values
   */
  @SafeVarargs
  public static <R> Array<R> of(R... values) {
    return ObjectArray.of(ImmutableList.copyOf(values));
  }

  /**
   * Obtains an immutable array of the specified size where the same value is used for each element.
   * 
   * @param <R>  the type of the value in the array
   * @param size  the size
   * @param value  the value applicable for all elements of the array
   * @return an array of the specified size based on the value
   */
  public static <R> Array<R> filled(int size, R value) {
    return FilledObjectArray.of(size, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the size of the array.
   * <p>
   * This is the total number of elements in the array.
   * 
   * @return the size of the array
   */
  public abstract int size();

  /**
   * Checks if this array is empty.
   * 
   * @return true if empty
   */
  public default boolean isEmpty() {
    return size() == 0;
  }

  /**
   * Gets the element at the specified index in this array.
   * <p>
   * The index must be valid, between zero (inclusive) and {@code size()} (exclusive).
   * 
   * @param index  the zero-based index to retrieve
   * @return the element at the index
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public abstract T get(int index);

  //-------------------------------------------------------------------------
  /**
   * Returns a list equivalent to this array.
   * <p>
   * The resulting list will be immutable.
   * 
   * @return an immutable list wrapping this array
   */
  public default List<T> toList() {
    // return type allows implementations to return a type that is not Guava's ImmutableList
    return stream().collect(toImmutableList());
  }

  /**
   * Returns a stream over the elements.
   * <p>
   * The stream returns an ordered view of the values in the array.
   *
   * @return a stream over the elements in the array
   */
  public abstract Stream<T> stream();

  //-------------------------------------------------------------------------
  /**
   * Applies an action to each value in the array.
   * <p>
   * This is used to perform an action on the contents of this array.
   * The action receives both the index and the value.
   * For example, the action could print out the array.
   * <pre>
   *   base.forEach((index, value) -> System.out.println(index + ": " + value));
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method. 
   *
   * @param action  the action to be applied
   */
  public default void forEach(IntObjConsumer<T> action) {
    for (int i = 0; i < size(); i++) {
      action.accept(i, get(i));
    }
  }

  /**
   * Returns an instance with an operation applied to each value in the array.
   * <p>
   * This is used to perform an operation on the contents of this array, returning a new array.
   * The operator only receives the value.
   * For example, the operator could take the inverse of each element.
   * <pre>
   *   result = base.map(value -> value.toString());
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method. 
   *
   * @param <R>  the type of the element in the resulting array
   * @param function  the function to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  public default <R> Array<R> map(Function<T, R> function) {
    ImmutableList.Builder<R> result = ImmutableList.builder();
    for (int i = 0; i < size(); i++) {
      result.add(function.apply(get(i)));
    }
    return ObjectArray.of(result.build());
  }

  /**
   * Returns an instance with an operation applied to each indexed value in the array.
   * <p>
   * This is used to perform an operation on the contents of this array, returning a new array.
   * The function receives both the index and the value.
   * For example, the operator could multiply the value by the index.
   * <pre>
   *   result = base.mapWithIndex((index, value) -> index + ":" + value);
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method. 
   *
   * @param <R>  the type of the element in the resulting array
   * @param function  the function to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  public default <R> Array<R> mapWithIndex(IntObjFunction<T, R> function) {
    ImmutableList.Builder<R> result = ImmutableList.builder();
    for (int i = 0; i < size(); i++) {
      result.add(function.apply(i, get(i)));
    }
    return ObjectArray.of(result.build());
  }

  /**
   * Reduces this array returning a single value.
   * <p>
   * This is used to reduce the values in this array to a single value.
   * The operator is called once for each element in the arrays.
   * The first argument to the operator is the running total of the reduction, starting from zero.
   * The second argument to the operator is the element.
   * <p>
   * This instance is immutable and unaffected by this method. 
   * 
   * @param <R>  the type of the result
   * @param identity  the identity value to start from
   * @param operator  the operator used to combine the value with the current total
   * @return the result of the reduction
   */
  public default <R> R reduce(R identity, BiFunction<R, ? super T, R> operator) {
    R result = identity;
    for (int i = 0; i < size(); i++) {
      result = operator.apply(result, get(i));
    }
    return result;
  }

}
