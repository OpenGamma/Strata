/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Stream;

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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.function.IntObjConsumer;
import com.opengamma.strata.collect.function.IntObjFunction;

/**
 * An immutable array of {@code Object} values.
 * <p>
 * This provides a cut down version of {@link List} that implements {@link Array}.
 * <p>
 * It is intended that the array does not contain {@code null}, however this is not validated.
 * 
 * @param <T>  the type of the element
 */
public final class ObjArray<T>
    implements Array<T>, ImmutableBean, Serializable {

  /**
   * An empty array.
   */
  private static final ObjArray<Object> EMPTY = new ObjArray<Object>(new Object[0]);

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1L;
  static {
    JodaBeanUtils.registerMetaBean(Meta.META);
  }

  /**
   * The underlying array.
   */
  private final Object[] array;

  //-------------------------------------------------------------------------
  /**
   * Obtains an empty immutable array.
   * 
   * @param <R>  the type of the value in the array
   * @return the empty immutable array
   */
  @SuppressWarnings("unchecked")
  public static <R> ObjArray<R> of() {
    return (ObjArray<R>) EMPTY;
  }

  /**
   * Obtains an immutable array with a single value.
   * 
   * @param <R>  the type of the value in the array
   * @param value  the single value
   * @return an array containing the specified value
   */
  public static <R> ObjArray<R> of(R value) {
    return new ObjArray<R>(new Object[] {value});
  }

  /**
   * Obtains an immutable array with two values.
   * 
   * @param <R>  the type of the value in the array
   * @param value1  the first value
   * @param value2  the second value
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> of(R value1, R value2) {
    return new ObjArray<R>(new Object[] {value1, value2});
  }

  /**
   * Obtains an immutable array with three values.
   * 
   * @param <R>  the type of the value in the array
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> of(R value1, R value2, R value3) {
    return new ObjArray<R>(new Object[] {value1, value2, value3});
  }

  /**
   * Obtains an immutable array with four values.
   * 
   * @param <R>  the type of the value in the array
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> of(R value1, R value2, R value3, R value4) {
    return new ObjArray<R>(new Object[] {value1, value2, value3, value4});
  }

  /**
   * Obtains an immutable array with five values.
   * 
   * @param <R>  the type of the value in the array
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> of(
      R value1, R value2, R value3, R value4, R value5) {
    return new ObjArray<R>(new Object[] {value1, value2, value3, value4, value5});
  }

  /**
   * Obtains an immutable array with six values.
   * 
   * @param <R>  the type of the value in the array
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> of(
      R value1, R value2, R value3, R value4,
      R value5, R value6) {
    return new ObjArray<R>(new Object[] {value1, value2, value3, value4, value5, value6});
  }

  /**
   * Obtains an immutable array with seven values.
   * 
   * @param <R>  the type of the value in the array
   * @param value1  the first value
   * @param value2  the second value
   * @param value3  the third value
   * @param value4  the fourth value
   * @param value5  the fifth value
   * @param value6  the sixth value
   * @param value7  the seventh value
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> of(
      R value1, R value2, R value3, R value4,
      R value5, R value6, R value7) {
    return new ObjArray<R>(new Object[] {value1, value2, value3, value4, value5, value6, value7});
  }

  /**
   * Obtains an immutable array with eight values.
   * 
   * @param <R>  the type of the value in the array
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
  public static <R> ObjArray<R> of(
      R value1, R value2, R value3, R value4,
      R value5, R value6, R value7, R value8) {
    return new ObjArray<R>(new Object[] {value1, value2, value3, value4, value5, value6, value7, value8});
  }

  /**
   * Obtains an immutable array with more than eight values.
   * 
   * @param <R>  the type of the value in the array
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
  @SafeVarargs
  public static <R> ObjArray<R> of(
      R value1, R value2, R value3, R value4,
      R value5, R value6, R value7, R value8, R... otherValues) {
    Object[] base = new Object[otherValues.length + 8];
    base[0] = value1;
    base[1] = value2;
    base[2] = value3;
    base[3] = value4;
    base[4] = value5;
    base[5] = value6;
    base[6] = value7;
    base[7] = value8;
    System.arraycopy(otherValues, 0, base, 8, otherValues.length);
    return new ObjArray<R>(base);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with entries filled using a function.
   * <p>
   * The function is passed the array index and returns the value for that index.
   * 
   * @param <R>  the type of the value in the array
   * @param size  the number of elements
   * @param valueFunction  the function used to populate the value
   * @return an array initialized using the function
   */
  public static <R> ObjArray<R> of(int size, IntFunction<? extends R> valueFunction) {
    if (size == 0) {
      return of();
    }
    Object[] array = new Object[size];
    Arrays.setAll(array, valueFunction);
    return new ObjArray<R>(array);
  }

  /**
   * Obtains an instance by wrapping an array.
   * <p>
   * This method is inherently unsafe as it relies on good behavior by callers.
   * Callers must never make any changes to the passed in array after calling this method.
   * Doing so would violate the immutability of this class.
   * 
   * @param <R>  the type of the value in the array
   * @param array  the array to assign
   * @return an array instance wrapping the specified array
   */
  public static <R> ObjArray<R> ofUnsafe(Object[] array) {
    if (array.length == 0) {
      return of();
    }
    return new ObjArray<R>(array);
  }

  //-----------------------------------------------------------------------
  /**
   * Obtains an instance from a list.
   * 
   * @param <R>  the type of the value in the array
   * @param list  the list to initialize from
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> copyOf(List<? extends R> list) {
    if (list.size() == 0) {
      return of();
    }
    return new ObjArray<R>(list.toArray());
  }

  /**
   * Obtains an instance from an array.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param <R>  the type of the value in the array
   * @param array  the array to copy, cloned
   * @return an array containing the specified values
   */
  public static <R> ObjArray<R> copyOf(Object[] array) {
    if (array.length == 0) {
      return of();
    }
    return new ObjArray<R>(Arrays.copyOf(array, array.length, Object[].class));
  }

  /**
   * Obtains an instance by copying part of an array.
   * <p>
   * The input array is copied and not mutated.
   * 
   * @param <R>  the type of the value in the array
   * @param array  the array to copy
   * @param fromIndexInclusive  the start index of the input array to copy from
   * @param toIndexExclusive  the end index of the input array to copy to
   * @return an array containing the specified values
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  private static <R> ObjArray<R> copyOf(Object[] array, int fromIndexInclusive, int toIndexExclusive) {
    if (fromIndexInclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + fromIndexInclusive + " > " + array.length);
    }
    if (toIndexExclusive > array.length) {
      throw new IndexOutOfBoundsException("Array index out of bounds: " + toIndexExclusive + " > " + array.length);
    }
    if ((toIndexExclusive - fromIndexInclusive) == 0) {
      return of();
    }
    return new ObjArray<R>(Arrays.copyOfRange(array, fromIndexInclusive, toIndexExclusive, Object[].class));
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with all entries equal to the same value.
   * 
   * @param size  the number of elements
   * @param value  the value of all the elements
   * @return an array filled with the specified value
   */
  public static <R> ObjArray<R> filled(int size, R value) {
    if (size == 0) {
      return of();
    }
    Object[] array = new Object[size];
    Arrays.fill(array, value);
    return new ObjArray<R>(array);
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from an array.
   * 
   * @param array  the array to copy, cloned
   */
  private ObjArray(Object[] array) {
    this.array = array;
  }

  //-------------------------------------------------------------------------
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
  @Override
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
  @SuppressWarnings("unchecked")
  @Override
  public T get(int index) {
    return (T) array[index];
  }

  /**
   * Checks if this array contains the specified value.
   * <p>
   * The value is checked using {@code Objects.equal}.
   * This also allow this method to be used to find any occurrences of {@code null}
   * 
   * @param value  the value to find
   * @return true if the value is contained in this array
   */
  public boolean contains(Object value) {
    if (array.length > 0) {
      for (int i = 0; i < array.length; i++) {
        if (Objects.equal(array[i], value)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Find the index of the first occurrence of the specified value.
   * <p>
   * The value is checked using {@code Objects.equal}.
   * This also allow this method to be used to find any occurrences of {@code null}
   * 
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  public int indexOf(Object value) {
    if (array.length > 0) {
      for (int i = 0; i < array.length; i++) {
        if (Objects.equal(array[i], value)) {
          return i;
        }
      }
    }
    return -1;
  }

  /**
   * Find the index of the first occurrence of the specified value.
   * <p>
   * The value is checked using {@code Objects.equal}.
   * This also allow this method to be used to find any occurrences of {@code null}
   * 
   * @param value  the value to find
   * @return the index of the value, -1 if not found
   */
  public int lastIndexOf(Object value) {
    if (array.length > 0) {
      for (int i = array.length - 1; i >= 0; i--) {
        if (Objects.equal(array[i], value)) {
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
   * @param <R>  the type of the value in the array
   * @param destination  the array to copy into
   * @param offset  the offset in the destination array to start from
   * @throws IndexOutOfBoundsException if the destination array is not large enough
   *  or the offset is negative
   * @throws ArrayStoreException if the destination type cannot accept a value
   */
  public <R> void copyInto(R[] destination, int offset) {
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
  public ObjArray<T> subArray(int fromIndexInclusive) {
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
  public ObjArray<T> subArray(int fromIndexInclusive, int toIndexExclusive) {
    return copyOf(array, fromIndexInclusive, toIndexExclusive);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this instance to an independent array.
   * 
   * @return a copy of the underlying array
   */
  public Object[] toArray() {
    return Arrays.copyOf(array, array.length, Object[].class);
  }

  /**
   * Converts this instance to an independent array of the specified type.
   * 
   * @param <R>  the type of the result
   * @param generator  the array generator, taking the size of the desired array
   * @return a copy of the underlying array
   */
  public <R> R[] toArray(IntFunction<R[]> generator) {
    R[] result = generator.apply(array.length);
    System.arraycopy(array, 0, result, 0, array.length);
    return result;
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
  public Object[] toArrayUnsafe() {
    return array;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a list equivalent to this array.
   * 
   * @return a list wrapping this array
   */
  public List<T> toList() {
    return new ImmList<T>(this);
  }

  /**
   * Returns a stream over the array values.
   *
   * @return a stream over the values in the array
   */
  @SuppressWarnings("unchecked")
  @Override
  public Stream<T> stream() {
    return Stream.of((T[]) array);
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
  public void forEach(IntObjConsumer<T> action) {
    for (int i = 0; i < array.length; i++) {
      action.accept(i, get(i));
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
  public ObjArray<T> with(int index, T newValue) {
    if (Objects.equal(array[index], newValue)) {
      return this;
    }
    Object[] result = array.clone();
    result[index] = newValue;
    return new ObjArray<T>(result);
  }

  //-------------------------------------------------------------------------
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
   * @param operator  the operator to be applied
   * @return a copy of this array with the operator applied to the original values
   */
  public <R> ObjArray<R> map(Function<T, R> operator) {
    Object[] result = new Object[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = operator.apply(get(i));
    }
    return new ObjArray<R>(result);
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
  public <R> ObjArray<R> mapWithIndex(IntObjFunction<T, R> function) {
    Object[] result = new Object[array.length];
    for (int i = 0; i < array.length; i++) {
      result[i] = function.apply(i, get(i));
    }
    return new ObjArray<R>(result);
  }

  //-------------------------------------------------------------------------
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
  public <R> R reduce(R identity, BiFunction<R, ? super T, R> operator) {
    R result = identity;
    for (int i = 0; i < array.length; i++) {
      result = operator.apply(result, get(i));
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
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof ObjArray) {
      ObjArray<?> other = (ObjArray<?>) obj;
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
   * 
   * @param <T>  the type of the elements
   */
  static class ImmIterator<T> implements ListIterator<T> {
    private final ObjArray<T> underlying;
    private int index;

    public ImmIterator(ObjArray<T> underlying) {
      this.underlying = underlying;
    }

    @Override
    public boolean hasNext() {
      return index < underlying.size();
    }

    @Override
    public boolean hasPrevious() {
      return index > 0;
    }

    @Override
    public T next() {
      if (hasNext()) {
        return underlying.get(index++);
      }
      throw new NoSuchElementException("Iteration has reached the last element");
    }

    @Override
    public T previous() {
      if (hasPrevious()) {
        return underlying.get(--index);
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
      throw new UnsupportedOperationException("Unable to remove from ObjArray");
    }

    @Override
    public void set(T value) {
      throw new UnsupportedOperationException("Unable to set value in ObjArray");
    }

    @Override
    public void add(T value) {
      throw new UnsupportedOperationException("Unable to add value to ObjArray");
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Immutable {@code List} representation of the array.
   * 
   * @param <T>  the type of the elements
   */
  static class ImmList<T> extends AbstractList<T> implements RandomAccess, Serializable {
    private static final long serialVersionUID = 1L;

    private final ObjArray<T> underlying;

    ImmList(ObjArray<T> underlying) {
      this.underlying = underlying;
    }

    @Override
    public int size() {
      return underlying.size();
    }

    @Override
    public T get(int index) {
      return underlying.get(index);
    }

    @Override
    public boolean contains(Object obj) {
      return underlying.contains(obj);
    }

    @Override
    public int indexOf(Object obj) {
      return underlying.indexOf(obj);
    }

    @Override
    public int lastIndexOf(Object obj) {
      return underlying.lastIndexOf(obj);
    }

    @Override
    public ListIterator<T> iterator() {
      return listIterator();
    }

    @Override
    public ListIterator<T> listIterator() {
      return new ImmIterator<T>(underlying);
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
      throw new UnsupportedOperationException("Unable to remove range from ObjArray");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Meta bean.
   */
  static final class Meta extends BasicMetaBean {

    private static final MetaBean META = new Meta();
    private static final MetaProperty<Object[]> ARRAY = new BasicMetaProperty<Object[]>("array") {

      @Override
      public MetaBean metaBean() {
        return META;
      }

      @Override
      public Class<?> declaringType() {
        return ObjArray.class;
      }

      @Override
      public Class<Object[]> propertyType() {
        return Object[].class;
      }

      @Override
      public Type propertyGenericType() {
        return Object[].class;
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
      public Object[] get(Bean bean) {
        return ((ObjArray<?>) bean).toArray();
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
    @SuppressWarnings("rawtypes")
    public BeanBuilder<ObjArray> builder() {
      return new BasicImmutableBeanBuilder<ObjArray>(this) {
        private Object[] array = EMPTY.array;

        @Override
        public Object get(String propertyName) {
          if (propertyName.equals(ARRAY.name())) {
            return array.clone();
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
        }

        @Override
        public BeanBuilder<ObjArray> set(String propertyName, Object value) {
          if (propertyName.equals(ARRAY.name())) {
            this.array = ((Object[]) ArgChecker.notNull(value, "value")).clone();
          } else {
            throw new NoSuchElementException("Unknown property: " + propertyName);
          }
          return this;
        }

        @Override
        public ObjArray build() {
          return new ObjArray(array);
        }
      };
    }

    @Override
    public Class<? extends Bean> beanType() {
      return ObjArray.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return MAP;
    }
  }

}
