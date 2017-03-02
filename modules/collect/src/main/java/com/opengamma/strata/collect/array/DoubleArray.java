/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyStyle;
import org.joda.beans.impl.BasicImmutableBeanBuilder;
import org.joda.beans.impl.BasicMetaBean;
import org.joda.beans.impl.BasicMetaProperty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.function.DoubleTernaryOperator;
import com.opengamma.strata.collect.function.IntDoubleConsumer;
import com.opengamma.strata.collect.function.IntDoubleToDoubleFunction;

/**
 * An immutable array of {@code double} values.
 * <p>
 * This provides functionality similar to {@link List} but for {@code double[]}.
 * <p>
 * In mathematical terms, this is a vector, or one-dimensional matrix.
 */
public final class DoubleArray
    implements Matrix, ImmutableBean, Serializable {

  /**
   * An empty array.
   */
  public static final DoubleArray EMPTY = new DoubleArray(new double[0]);

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  static {
    JodaBeanUtils.registerMetaBean(Meta.META);
  }

  /**
   * The underlying array of doubles.
   */
  private final double[] array;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty immutable array.
   * 
   * @return the empty immutable array
   */
  public static DoubleArray of() {
    return EMPTY;
  }

  /**
   * Obtains an immutable array with a single value.
   * 
   * @param value  the single value
   * @return an array containing the specified value
   */
  public static DoubleArray of(double value) {
    return new DoubleArray(new double[] {value});
  }

  /**
   * Obtains an immutable array with two values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @return an array containing the specified values
   */
  public static DoubleArray of(double value1, double value2) {
    return new DoubleArray(new double[] {value1, value2});
  }

  /**
   * Obtains an immutable array with three values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @return an array containing the specified values
   */
  public static DoubleArray of(double value1, double value2, double value3) {
    return new DoubleArray(new double[] {value1, value2, value3});
  }

  /**
   * Obtains an immutable array with four values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @return an array containing the specified values
   */
  public static DoubleArray of(double value1, double value2, double value3, double value4) {
    return new DoubleArray(new double[] {value1, value2, value3, value4});
  }

  /**
   * Obtains an immutable array with five values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @return an array containing the specified values
   */
  public static DoubleArray of(
      double value1, double value2, double value3, double value4, double value5) {
    return new DoubleArray(new double[] {value1, value2, value3, value4, value5});
  }

  /**
   * Obtains an immutable array with six values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @return an array containing the specified values
   */
  public static DoubleArray of(
      double value1, double value2, double value3, double value4,
      double value5, double value6) {
    return new DoubleArray(new double[] {value1, value2, value3, value4, value5, value6});
  }

  /**
   * Obtains an immutable array with seven values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @param value7  the seventh value
   * @return an array containing the specified values
   */
  public static DoubleArray of(
      double value1, double value2, double value3, double value4,
      double value5, double value6, double value7) {
    return new DoubleArray(new double[] {value1, value2, value3, value4, value5, value6, value7});
  }

  /**
   * Obtains an immutable array with eight values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @param value7  the seventh value
   * @param value8  the eighth value
   * @return an array containing the specified values
   */
  public static DoubleArray of(
      double value1, double value2, double value3, double value4,
      double value5, double value6, double value7, double value8) {
    return new DoubleArray(new double[] {value1, value2, value3, value4, value5, value6, value7, value8});
  }

  /**
   * Obtains an immutable array with more than eight values.
   * 
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @param value7  the seventh value
   * @param value8  the eighth value
   * @param otherValues  the other values
   * @return an array containing the specified values
   */
  public static DoubleArray of(
      double value1, double value2, double value3, double value4,
      double value5, double value6, double value7, double value8, double... otherValues) {
    double[] base = new double[otherValues.length + 8];
    base[0] = value1;
    base[1] = value2;
    base[2] = value3;
    base[3] = value4;
    base[4] = value5;
    base[5] = value6;
    base[6] = value7;
    base[7] = value8;
    System.arraycopy(otherValues, 0, base, 8, otherValues.length);
    return new DoubleArray(base);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with entries filled using a function.
   * <p>
   * The function is passed the array index and returns the value for that index.
   * 
   * @param size  the number of elements
   * @param valueFunction  the function used to populate the value
   * @return an array initialized using the function
   */
  public static DoubleArray of(int size, IntToDoubleFunction valueFunction) {
    if (size == 0) {
      return EMPTY;
    }
    double[] array = new double[size];
    Arrays.setAll(array, valueFunction);
    return new DoubleArray(array);
  }

  /**
   * Obtains an instance by wrapping an array.
   * <p>
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the passed in array after calling this method.
   * Doing so would violate the immutability of this class.
   * 
   * @param array  the array to assign
   * @return an array instance wrapping the specified array
   */
  public static DoubleArray ofUnsafe(double[] array) {
    if (array.length == 0) {
      return EMPTY;
    }
    return new DoubleArray(array);
  }

  //-----------------------------------------------------------------------
  /**
   * Obtains an instance from a collection of {@code Double}.
   * <p>
   * The order of the values in the returned array is the order in which elements are returned
   * from the iterator of the collection.
   * 
   * @param collection  the collection to initialize from
   * @return an array containing the values from the collection in iteration order
   */
  public static DoubleArray copyOf(Collection<Double> collection) {
    if (collection.size() == 0) {
      return EMPTY;
    }
    if (collection instanceof ImmList) {
      return ((ImmList) collection).underlying;
    }
    return new DoubleArray(Doubles.toArray(collection));
  }

  /**
   * Obtains an instance from an array of {@code double}.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param array  the array to copy, cloned
   * @return an array containing the specified values
   */
  public static DoubleArray copyOf(double[] array) {
    if (array.length == 0) {
      return EMPTY;
    }
    return new DoubleArray(array.clone());
  }

  /**
   * Obtains an instance by copying part of an array.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param array  the array to copy
   * @param fromIndex  the offset from the start of the array
   * @return an array containing the specified values
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public static DoubleArray copyOf(double[] array, int fromIndex) {
    return copyOf(array, fromIndex, array.length);
  }

  /**
   * Obtains an instance by copying part of an array.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param array  the array to copy
   * @param fromIndexInclusive  the start index of the input array to copy from
   * @param toIndexExclusive  the end index of the input array to copy to
   * @return an array containing the specified values
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public static DoubleArray copyOf(double[] array, int fromIndexInclusive, int toIndexExclusive) {
    if (fromIndexInclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + fromIndexInclusive + " > " + array.length);
    }
    if (toIndexExclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + toIndexExclusive + " > " + array.length);
    }
    if ((toIndexExclusive - fromIndexInclusive) == 0) {
      return EMPTY;
    }
    return new DoubleArray(Arrays.copyOfRange(array, fromIndexInclusive, toIndexExclusive));
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with all entries equal to the zero.
   * 
   * @param size  the number of elements
   * @return an array filled with zeroes
   */
  public static DoubleArray filled(int size) {
    if (size == 0) {
      return EMPTY;
    }
    return new DoubleArray(new double[size]);
  }

  /**
   * Obtains an instance with all entries equal to the same value.
   * 
   * @param size  the number of elements
   * @param value  the value of all the elements
   * @return an array filled with the specified value
   */
  public static DoubleArray filled(int size, double value) {
    if (size == 0) {
      return EMPTY;
    }
    double[] array = new double[size];
    Arrays.fill(array, value);
    return new DoubleArray(array);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance from a {@code double[}.
   * 
   * @param array  the array, assigned not cloned
   */
  private DoubleArray(double[] array) {
    this.array = array;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the number of dimensions of this array.
   * 
   * @return one
   */
  @Override
  public int dimensions() {
    return 1;
  }

  /**
   * Gets the size of this array.
   * 
   * @return the array size, zero or greater
   */
  @Override
  public int size() {
    return array.length;
  }

  /**
   * Checks if this array is empty.
   * 
   * @return true if empty
   */
  public boolean isEmpty() {
    return array.length == 0;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value at the specified index in this array.
   * 
   * @param index  the zero-based index to retrieve
   * @return the value at the index
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public double get(int index) {
    return array[index];
  }

  /**
   * Checks if this array contains the specified value.
   * <p>
   * The value is checked using {@code Double.doubleToLongBits} in order to match {@code equals}.
   * This also allow this method to be used to find any occurrences of NaN.
   * 
   * @param value  the value to find
   * @return true if the value is contained in this array
   */
  public boolean contains(double value) {
    if (array.length > 0) {
      long bits = Double.doubleToLongBits(value);
      for (int i = 0; i < array.length; i++) {
        if (Double.doubleToLongBits(array[i]) == bits) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Find the index of the first occurrence of the specified value.
   * <p>
   * The value is checked using {@code Double.doubleToLongBits} in order to match {@code equals}.
   * This also allow this method to be used to find any occurrences of NaN.
   * 
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  public int indexOf(double value) {
    if (array.length > 0) {
      long bits = Double.doubleToLongBits(value);
      for (int i = 0; i < array.length; i++) {
        if (Double.doubleToLongBits(array[i]) == bits) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Find the index of the first occurrence of the specified value.
   * <p>
   * The value is checked using {@code Double.doubleToLongBits} in order to match {@code equals}.
   * This also allow this method to be used to find any occurrences of NaN.
   * 
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  public int lastIndexOf(double value) {
    if (array.length > 0) {
      long bits = Double.doubleToLongBits(value);
      for (int i = array.length - 1; i >= 0; i--) {
        if (Double.doubleToLongBits(array[i]) == bits) {
          return i;
        }
      }
    }
    return -1;
  }

  //-------------------------------------------------------------------------
  /**
   * Copies this array into the specified array.
   * <p>
   * The specified array must be at least as large as this array.
   * If it is larger, then the remainder of the array will be untouched.
   * 
   * @param destination  the array to copy into
   * @param offset  the offset in the destination array to start from
   * @throws IndexOutOfBoundsException if the destination array is not large enough
   *  or the offset is negative
   */
  public void copyInto(double[] destination, int offset) {
    if (destination.length < array.length + offset) {
      throw new IndexOutOfBoundsException("Destination array is not large enough");
    }
    System.arraycopy(array, 0, destination, offset, array.length);
  }

  /**
   * Returns an array holding the values from the specified index onwards.
   * 
   * @param fromIndexInclusive  the start index of the array to copy from
   * @return an array instance with the specified bounds
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public DoubleArray subArray(int fromIndexInclusive) {
    return subArray(fromIndexInclusive, array.length);
  }

  /**
   * Returns an array holding the values between the specified from and to indices.
   * 
   * @param fromIndexInclusive  the start index of the array to copy from
   * @param toIndexExclusive  the end index of the array to copy to
   * @return an array instance with the specified bounds
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public DoubleArray subArray(int fromIndexInclusive, int toIndexExclusive) {
    return copyOf(array, fromIndexInclusive, toIndexExclusive);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to an independent {@code double[]}.
   * 
   * @return a copy of the underlying array
   */
  public double[] toArray() {
    return array.clone();
  }

  /**
   * Returns the underlying array.
   * <p>
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the array returned by this method.
   * Doing so would violate the immutability of this class.
   * 
   * @return the raw array
   */
  public double[] toArrayUnsafe() {
    return array;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a list equivalent to this array.
   * 
   * @return a list wrapping this array
   */
  public List<Double> toList() {
    return new ImmList(this);
  }

  /**
   * Returns a stream over the array values.
   *
   * @return a stream over the values in the array
   */
  public DoubleStream stream() {
    return DoubleStream.of(array);
  }

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
  public void forEach(IntDoubleConsumer action) {
    for (int i = 0; i < array.length; i++) {
      action.accept(i, array[i]);
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns an instance with the value at the specified index changed.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param index  the zero-based index to set
   * @param newValue  the new value to store
   * @return a copy of this array with the value at the index changed
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public DoubleArray with(int index, double newValue) {
    if (Double.doubleToLongBits(array[index]) == Double.doubleToLongBits(newValue)) {
      return this;
    }
    double[] result = array.clone();
    result[index] = newValue;
    return new DoubleArray(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance with the specified amount added to each value.
   * <p>
   * This is used to add to the contents of this array, returning a new array.
   * <p>
   * This is a special case of {@link #map(DoubleUnaryOperator)}.
   * This instance is immutable and unaffected by this method.
   * 
   * @param amount  the amount to add, may be negative
   * @return a copy of this array with the amount added to each value
   */
  public DoubleArray plus(double amount) {
    if (amount == 0d) {
      return this;
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] + amount;
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance with the specified amount subtracted from each value.
   * <p>
   * This is used to subtract from the contents of this array, returning a new array.
   * <p>
   * This is a special case of {@link #map(DoubleUnaryOperator)}.
   * This instance is immutable and unaffected by this method.
   * 
   * @param amount  the amount to subtract, may be negative
   * @return a copy of this array with the amount subtracted from each value
   */
  public DoubleArray minus(double amount) {
    if (amount == 0d) {
      return this;
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] - amount;
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance with each value multiplied by the specified factor.
   * <p>
   * This is used to multiply the contents of this array, returning a new array.
   * <p>
   * This is a special case of {@link #map(DoubleUnaryOperator)}.
   * This instance is immutable and unaffected by this method.
   * 
   * @param factor  the multiplicative factor
   * @return a copy of this array with the each value multiplied by the factor
   */
  public DoubleArray multipliedBy(double factor) {
    if (factor == 1d) {
      return this;
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] * factor;
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance with each value divided by the specified divisor.
   * <p>
   * This is used to divide the contents of this array, returning a new array.
   * <p>
   * This is a special case of {@link #map(DoubleUnaryOperator)}.
   * This instance is immutable and unaffected by this method.
   *
   * @param divisor  the value by which the array values are divided
   * @return a copy of this array with the each value divided by the divisor
   */
  public DoubleArray dividedBy(double divisor) {
    if (divisor == 1d) {
      return this;
    }
    // multiplication is cheaper than division so it is more efficient to do the division once and multiply each element
    double factor = 1 / divisor;
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] * factor;
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance with an operation applied to each value in the array.
   * <p>
   * This is used to perform an operation on the contents of this array, returning a new array.
   * The operator only receives the value.
   * For example, the operator could take the inverse of each element.
   * <pre>
   *   result = base.map(value -> 1 / value);
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method.
   *
   * @param operator  the operator to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  public DoubleArray map(DoubleUnaryOperator operator) {
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = operator.applyAsDouble(array[i]);
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance with an operation applied to each indexed value in the array.
   * <p>
   * This is used to perform an operation on the contents of this array, returning a new array.
   * The function receives both the index and the value.
   * For example, the operator could multiply the value by the index.
   * <pre>
   *   result = base.mapWithIndex((index, value) -> index * value);
   * </pre>
   * <p>
   * This instance is immutable and unaffected by this method.
   *
   * @param function  the function to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  public DoubleArray mapWithIndex(IntDoubleToDoubleFunction function) {
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = function.applyAsDouble(i, array[i]);
    }
    return new DoubleArray(result);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an instance where each element is the sum of the matching values
   * in this array and the other array.
   * <p>
   * This is used to add two arrays, returning a new array.
   * Element {@code n} in the resulting array is equal to element {@code n} in this array
   * plus element {@code n} in the other array.
   * The arrays must be of the same size.
   * <p>
   * This is a special case of {@link #combine(DoubleArray, DoubleBinaryOperator)}.
   * This instance is immutable and unaffected by this method.
   * 
   * @param other  the other array
   * @return a copy of this array with matching elements added
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public DoubleArray plus(DoubleArray other) {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes");
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] + other.array[i];
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance where each element is equal to the difference between the
   * matching values in this array and the other array.
   * <p>
   * This is used to subtract the second array from the first, returning a new array.
   * Element {@code n} in the resulting array is equal to element {@code n} in this array
   * minus element {@code n} in the other array.
   * The arrays must be of the same size.
   * <p>
   * This is a special case of {@link #combine(DoubleArray, DoubleBinaryOperator)}.
   * This instance is immutable and unaffected by this method.
   * 
   * @param other  the other array
   * @return a copy of this array with matching elements subtracted
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public DoubleArray minus(DoubleArray other) {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes");
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] - other.array[i];
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance where each element is equal to the product of the
   * matching values in this array and the other array.
   * <p>
   * This is used to multiply each value in this array by the corresponding value in the other array,
   * returning a new array.
   * <p>
   * Element {@code n} in the resulting array is equal to element {@code n} in this array
   * multiplied by element {@code n} in the other array.
   * The arrays must be of the same size.
   * <p>
   * This is a special case of {@link #combine(DoubleArray, DoubleBinaryOperator)}.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements multiplied
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public DoubleArray multipliedBy(DoubleArray other) {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes");
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] * other.array[i];
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance where each element is calculated by dividing values in this array by values in the other array.
   * <p>
   * This is used to divide each value in this array by the corresponding value in the other array,
   * returning a new array.
   * <p>
   * Element {@code n} in the resulting array is equal to element {@code n} in this array
   * divided by element {@code n} in the other array.
   * The arrays must be of the same size.
   * <p>
   * This is a special case of {@link #combine(DoubleArray, DoubleBinaryOperator)}.
   * This instance is immutable and unaffected by this method.
   *
   * @param other  the other array
   * @return a copy of this array with matching elements divided
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public DoubleArray dividedBy(DoubleArray other) {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes");
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = array[i] / other.array[i];
    }
    return new DoubleArray(result);
  }

  /**
   * Returns an instance where each element is formed by some combination of the matching
   * values in this array and the other array.
   * <p>
   * This is used to combine two arrays, returning a new array.
   * Element {@code n} in the resulting array is equal to the result of the operator
   * when applied to element {@code n} in this array and element {@code n} in the other array.
   * The arrays must be of the same size.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param other  the other array
   * @param operator  the operator used to combine each pair of values
   * @return a copy of this array combined with the specified array
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public DoubleArray combine(DoubleArray other, DoubleBinaryOperator operator) {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes");
    }
    double[] result = new double[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = operator.applyAsDouble(array[i], other.array[i]);
    }
    return new DoubleArray(result);
  }

  /**
   * Combines this array and the other array returning a reduced value.
   * <p>
   * This is used to combine two arrays, returning a single reduced value.
   * The operator is called once for each element in the arrays.
   * The arrays must be of the same size.
   * <p>
   * The first argument to the operator is the running total of the reduction, starting from zero.
   * The second argument to the operator is the element from this array.
   * The third argument to the operator is the element from the other array.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param other  the other array
   * @param operator  the operator used to combine each pair of values with the current total
   * @return the result of the reduction
   * @throws IllegalArgumentException if the arrays have different sizes
   */
  public double combineReduce(DoubleArray other, DoubleTernaryOperator operator) {
    if (array.length != other.array.length) {
      throw new IllegalArgumentException("Arrays have different sizes");
    }
    double result = 0;
    for (int i = 0; i < array.length; i++) {
      result = operator.applyAsDouble(result, array[i], other.array[i]);
    }
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an array that combines this array and the specified array.
   * <p>
   * The result will have a length equal to {@code this.size() + arrayToConcat.length}.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param arrayToConcat  the array to add to the end of this array
   * @return a copy of this array with the specified array added at the end
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public DoubleArray concat(double... arrayToConcat) {
    if (array.length == 0) {
      return copyOf(arrayToConcat);
    }
    if (arrayToConcat.length == 0) {
      return this;
    }
    double[] result = new double[array.length + arrayToConcat.length];
    System.arraycopy(array, 0, result, 0, array.length);
    System.arraycopy(arrayToConcat, 0, result, array.length, arrayToConcat.length);
    return new DoubleArray(result);
  }

  /**
   * Returns an array that combines this array and the specified array.
   * <p>
   * The result will have a length equal to {@code this.size() + newArray.length}.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param arrayToConcat  the new array to add to the end of this array
   * @return a copy of this array with the specified array added at the end
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  public DoubleArray concat(DoubleArray arrayToConcat) {
    if (array.length == 0) {
      return arrayToConcat;
    }
    if (arrayToConcat.array.length == 0) {
      return this;
    }
    return concat(arrayToConcat.array);
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a sorted copy of this array.
   * <p>
   * This uses {@link Arrays#sort(double[])}.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return a sorted copy of this array
   */
  public DoubleArray sorted() {
    if (array.length < 2) {
      return this;
    }
    double[] result = array.clone();
    Arrays.sort(result);
    return new DoubleArray(result);
  }

  /**
   * Returns the minimum value held in the array.
   * <p>
   * If the array is empty, then an exception is thrown.
   * If the array contains NaN, then the result is NaN.
   * 
   * @return the minimum value
   * @throws IllegalStateException if the array is empty
   */
  public double min() {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find minimum of an empty array");
    }
    if (array.length == 1) {
      return array[0];
    }
    double min = Double.POSITIVE_INFINITY;
    for (int i = 0; i < array.length; i++) {
      min = Math.min(min, array[i]);
    }
    return min;
  }

  /**
   * Returns the minimum value held in the array.
   * <p>
   * If the array is empty, then an exception is thrown.
   * If the array contains NaN, then the result is NaN.
   * 
   * @return the maximum value
   * @throws IllegalStateException if the array is empty
   */
  public double max() {
    if (array.length == 0) {
      throw new IllegalStateException("Unable to find maximum of an empty array");
    }
    if (array.length == 1) {
      return array[0];
    }
    double max = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < array.length; i++) {
      max = Math.max(max, array[i]);
    }
    return max;
  }

  /**
   * Returns the sum of all the values in the array.
   * <p>
   * This is a special case of {@link #reduce(double, DoubleBinaryOperator)}.
   * 
   * @return the total of all the values
   */
  public double sum() {
    double total = 0;
    for (int i = 0; i < array.length; i++) {
      total += array[i];
    }
    return total;
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
   * @param identity  the identity value to start from
   * @param operator  the operator used to combine the value with the current total
   * @return the result of the reduction
   */
  public double reduce(double identity, DoubleBinaryOperator operator) {
    double result = identity;
    for (int i = 0; i < array.length; i++) {
      result = operator.applyAsDouble(result, array[i]);
    }
    return result;
  }

  //-------------------------------------------------------------------------
  @Override
  public MetaBean metaBean() {
    return Meta.META;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this array equals another within the specified tolerance.
   * <p>
   * This returns true if the two instances have {@code double} values that are
   * equal within the specified tolerance.
   * 
   * @param other  the other array
   * @param tolerance  the tolerance
   * @return true if equal up to the tolerance
   */
  public boolean equalWithTolerance(DoubleArray other, double tolerance) {
    return DoubleArrayMath.fuzzyEquals(array, other.array, tolerance);
  }

  /**
   * Checks if this array equals zero within the specified tolerance.
   * <p>
   * This returns true if all the {@code double} values equal zero within the specified tolerance.
   * 
   * @param tolerance  the tolerance
   * @return true if equal up to the tolerance
   */
  public boolean equalZeroWithTolerance(double tolerance) {
    return DoubleArrayMath.fuzzyEqualsZero(array, tolerance);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof DoubleArray) {
      DoubleArray other = (DoubleArray) obj;
      return Arrays.equals(array, other.array);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(array);
  }

  @Override
  public String toString() {
    return Arrays.toString(array);
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable {@code Iterator} representation of the array.
   */
  static class ImmIterator implements ListIterator<Double> {
    private final double[] array;
    private int index;

    public ImmIterator(double[] array) {
      this.array = array;
    }

    @Override
    public boolean hasNext() {
      return index < array.length;
    }

    @Override
    public boolean hasPrevious() {
      return index > 0;
    }

    @Override
    public Double next() {
      if (hasNext()) {
        return array[index++];
      }
      throw new NoSuchElementException("Iteration has reached the last element");
    }

    @Override
    public Double previous() {
      if (hasPrevious()) {
        return array[--index];
      }
      throw new NoSuchElementException("Iteration has reached the first element");
    }

    @Override
    public int nextIndex() {
      return index;
    }

    @Override
    public int previousIndex() {
      return index - 1;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Unable to remove from DoubleArray");
    }

    @Override
    public void set(Double value) {
      throw new UnsupportedOperationException("Unable to set value in DoubleArray");
    }

    @Override
    public void add(Double value) {
      throw new UnsupportedOperationException("Unable to add value to DoubleArray");
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable {@code List} representation of the array.
   */
  static class ImmList extends AbstractList<Double> implements RandomAccess, Serializable {
    private static final long serialVersionUID = 1L;

    private final DoubleArray underlying;

    ImmList(DoubleArray underlying) {
      this.underlying = underlying;
    }

    @Override
    public int size() {
      return underlying.size();
    }

    @Override
    public Double get(int index) {
      return underlying.get(index);
    }

    @Override
    public boolean contains(Object obj) {
      return (obj instanceof Double ? underlying.contains((Double) obj) : false);
    }

    @Override
    public int indexOf(Object obj) {
      return (obj instanceof Double ? underlying.indexOf((Double) obj) : -1);
    }

    @Override
    public int lastIndexOf(Object obj) {
      return (obj instanceof Double ? underlying.lastIndexOf((Double) obj) : -1);
    }

    @Override
    public ListIterator<Double> iterator() {
      return listIterator();
    }

    @Override
    public ListIterator<Double> listIterator() {
      return new ImmIterator(underlying.array);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
      throw new UnsupportedOperationException("Unable to remove range from DoubleArray");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  static final class Meta extends BasicMetaBean {

    private static final MetaBean META = new Meta();
    private static final MetaProperty<double[]> ARRAY = new BasicMetaProperty<double[]>("array") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return DoubleArray.class;
      }

      @Override
      public Class<double[]> propertyType() {
        return double[].class;
      }

      @Override
      public Type propertyGenericType() {
        return double[].class;
      }

      @Override
      public PropertyStyle style() {
        return PropertyStyle.IMMUTABLE;
      }

      @Override
      public List<Annotation> annotations() {
        return ImmutableList.of();
      }

      @Override
      public double[] get(Bean bean) {
        return ((DoubleArray) bean).toArray();
      }

      @Override
      public void set(Bean bean, Object value) {
        throw new UnsupportedOperationException("Property cannot be written: " + name());
      }
    };
    private static final ImmutableMap<String, MetaProperty<?>> MAP = ImmutableMap.of("array", ARRAY);

    private Meta() {
    }

    @Override
    public BeanBuilder<DoubleArray> builder() {
      return new BasicImmutableBeanBuilder<DoubleArray>(this) {
        private double[] array = DoubleArrayMath.EMPTY_DOUBLE_ARRAY;

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(ARRAY.name())) {
            return array.clone();
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<DoubleArray> set(String propertyName, Object value) {
          if (propertyName.equals(ARRAY.name())) {
            this.array = ((double[]) ArgChecker.notNull(value, "value")).clone();
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public DoubleArray build() {
          return new DoubleArray(array);
        }
      };
    }

    @Override
    public Class<? extends Bean> beanType() {
      return DoubleArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return MAP;
    }
  }

}
