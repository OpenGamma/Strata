/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static com.opengamma.strata.collect.Guavate.concatToList;
import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A value with associated failures.
 * <p>
 * This captures a common use case where an operation can tolerate some failure.
 * This is often referred to as partial success or partial failure.
 * The class stores the value, of any object type, and a list of failures that may be empty.
 * <p>
 * The success value must be able to handle the case where everything fails.
 * In most cases, the success value will be a collection type, such as {@link List}
 * or {@link Map}, which can be empty if the operation failed completely.
 * <p>
 * The classic example is loading rows from a file, when some rows are valid and some are invalid.
 * The partial result would contain the successful rows, with the list of failures containing an
 * entry for each row that failed to parse.
 *
 * @param <T> the type of the underlying success value, typically a collection type
 */
@BeanDefinition(builderScope = "private")
public final class ValueWithFailures<T>
    implements ImmutableBean, Serializable {

  /**
   * The success value.
   */
  @PropertyDefinition(validate = "notNull")
  private final T value;
  /**
   * The failure items.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<FailureItem> failures;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance wrapping the success value and failures.
   *
   * @param <T>  the type of the success value
   * @param successValue  the success value
   * @param failures  the failures
   * @return an instance wrapping the value and failures
   */
  public static <T> ValueWithFailures<T> of(T successValue, FailureItem... failures) {
    return new ValueWithFailures<>(successValue, ImmutableList.copyOf(failures));
  }

  /**
   * Creates an instance wrapping the success value and failures.
   *
   * @param <T>  the type of the success value
   * @param successValue  the success value
   * @param failures  the failures
   * @return an instance wrapping the value and failures
   */
  public static <T> ValueWithFailures<T> of(T successValue, List<FailureItem> failures) {
    // this method is retained to ensure binary compatibility
    return new ValueWithFailures<>(successValue, failures);
  }

  /**
   * Creates an instance wrapping the success value and failures.
   *
   * @param <T>  the type of the success value
   * @param successValue  the success value
   * @param failures  the failures
   * @return an instance wrapping the value and failures
   */
  public static <T> ValueWithFailures<T> of(T successValue, Collection<FailureItem> failures) {
    return new ValueWithFailures<>(successValue, ImmutableList.copyOf(failures));
  }

  /**
   * Creates an instance using a supplier.
   * <p>
   * If the supplier succeeds normally, the supplied value will be returned.
   * If the supplier fails, the empty value will be returned along with a failure.
   * <p>
   * This recognizes and handles {@link FailureItemProvider} exceptions.
   * If the exception type is not recognized, the failure item will have a reason of {@code ERROR}.
   *
   * @param <T> the type of the value
   * @param emptyValue  the empty value
   * @param supplier  supplier of the result value
   * @return an instance containing the supplied value, or a failure if an exception is thrown
   */
  public static <T> ValueWithFailures<T> of(T emptyValue, Supplier<T> supplier) {
    try {
      return of(supplier.get());
    } catch (Exception ex) {
      return ValueWithFailures.of(emptyValue, FailureItem.from(ex));
    }
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a {@code BinaryOperator} that combines {@code ValueWithFailures} objects using the provided combiner
   * function.
   * <p>
   * This would be used as follows (with a static import):
   * <pre>
   *   stream.reduce(combiningValues(Guavate::concatToList));
   *
   *   stream.reduce(baseValueWithFailures, combiningValues(Guavate::concatToList));
   * </pre>
   * <p>
   * This replaces code of the form:
   * <pre>
   *   stream.reduce((vwf1, vwf2) -> vwf1.combinedWith(vwf2, Guavate::concatToList));
   *
   *   stream.reduce(baseValueWithFailures, (vwf1, vwf2) -> vwf1.combinedWith(vwf2, Guavate::concatToList));
   * </pre>
   *
   * @param combiner  the combiner of the values
   * @param <T>  the type of the values
   * @return the combining binary operator
   */
  public static <T> BinaryOperator<ValueWithFailures<T>> combiningValues(BinaryOperator<T> combiner) {
    ArgChecker.notNull(combiner, "combiner");
    return (vwf1, vwf2) -> vwf1.combinedWith(vwf2, combiner);
  }

  /**
   * Returns a collector that can be used to create a combined {@code ValueWithFailure}
   * from a stream of separate instances.
   * <p>
   * The {@link Collector} returned performs a reduction of its {@link ValueWithFailures} input elements under a
   * specified {@link BinaryOperator} using the provided identity.
   * <p>
   * This collects a {@code Stream<ValueWithFailures<T>>} to a {@code ValueWithFailures<T>}.
   *
   * @param <T>  the type of the success value in the {@link ValueWithFailures}
   * @param identityValue  the identity value
   * @param operator  the operator used for the reduction.
   * @return a {@link Collector}
   */
  public static <T> Collector<ValueWithFailures<T>, ?, ValueWithFailures<T>> toValueWithFailures(
      T identityValue,
      BinaryOperator<T> operator) {

    return Collectors.reducing(ValueWithFailures.of(identityValue), combiningValues(operator));
  }

  /**
   * Returns a collector that creates a combined {@code ValueWithFailure} from a stream
   * of separate instances, combining into an immutable list.
   * <p>
   * This collects a {@code Stream<ValueWithFailures<T>>} to a {@code ValueWithFailures<List<T>>}.
   *
   * @param <T>  the type of the success value in the {@link ValueWithFailures}
   * @return a {@link Collector}
   */
  public static <T> Collector<ValueWithFailures<? extends T>, ?, ValueWithFailures<List<T>>> toCombinedValuesAsList() {
    return Collector.of(
        () -> new StreamBuilder<>(ImmutableList.<T>builder()),
        StreamBuilder::add,
        StreamBuilder::combine,
        StreamBuilder::build);
  }

  /**
   * Returns a collector that creates a combined {@code ValueWithFailure} from a stream
   * of separate instances, combining into an immutable set.
   * <p>
   * This collects a {@code Stream<ValueWithFailures<T>>} to a {@code ValueWithFailures<Set<T>>}.
   *
   * @param <T>  the type of the success value in the {@link ValueWithFailures}
   * @return a {@link Collector}
   */
  public static <T> Collector<ValueWithFailures<? extends T>, ?, ValueWithFailures<Set<T>>> toCombinedValuesAsSet() {
    return Collector.of(
        () -> new StreamBuilder<>(ImmutableSet.<T>builder()),
        StreamBuilder::add,
        StreamBuilder::combine,
        StreamBuilder::build);
  }

  /**
   * Returns a collector that can be used to create a combined {@code ValueWithFailure}
   * from a stream of {@code Result} instances.
   * <p>
   * This collects a {@code Stream<Result<T>>} to a {@code ValueWithFailures<List<T>>}.
   *
   * @param <T>  the type of the success value in the {@link ValueWithFailures}
   * @return a {@link Collector}
   */
  public static <T> Collector<Result<? extends T>, ?, ValueWithFailures<List<T>>> toCombinedResultsAsList() {
    return Collector.of(
        () -> new StreamBuilder<>(ImmutableList.<T>builder()),
        StreamBuilder::addResult,
        StreamBuilder::combine,
        StreamBuilder::build);
  }

  /**
   * Returns a collector that can be used to create a combined {@code ValueWithFailure}
   * from a stream of entries where the values contain {@code Result} instances.
   * <p>
   * This collects a {@code Stream<Map.Entry<K, Result<V>>>} to a {@code ValueWithFailures<Map<K, V>>}.
   *
   * @param <K> the type of the map key
   * @param <V> the type of the success map values
   * @throws IllegalArgumentException if duplicate keys are collected
   * @return a {@link Collector}
   */
  public static <K, V> Collector<? super Map.Entry<K, Result<V>>, ? , ValueWithFailures<Map<K, V>>> toCombinedResultsAsMap() {
    return Collector.of(
        () -> new MapStreamBuilder<>(ImmutableMap.<K, V>builder()),
        MapStreamBuilder::addResult,
        MapStreamBuilder::combine,
        MapStreamBuilder::build);
  }

  /**
   * Returns a collector that creates a combined {@code ValueWithFailure} from a stream
   * of entries where the values contain {@code ValueWithFailure} instances.
   * <p>
   * This collects a {@code Stream<Map.Entry<K, ValueWithFailures<V>>} to a {@code ValueWithFailures<Map<K, V>>}.
   *
   * @param <K> the type of the map key
   * @param <V> the type of the entry success values in the {@link ValueWithFailures}
   * @throws IllegalArgumentException if duplicate keys are collected
   * @return a {@link Collector}
   */
  public static <K, V> Collector<? super Map.Entry<K, ValueWithFailures<V>>, ? , ValueWithFailures<Map<K, V>>> toCombinedValuesAsMap() {
    return Collector.of(
        () -> new MapStreamBuilder<>(ImmutableMap.<K, V>builder()),
        MapStreamBuilder::add,
        MapStreamBuilder::combine,
        MapStreamBuilder::build);
  }

  /**
   * Combines separate instances of {@code ValueWithFailure} into a single instance,
   * using a list to collect the values.
   * <p>
   * This converts {@code Iterable<ValueWithFailures<T>>} to {@code ValueWithFailures<List<T>>}.
   *
   * @param <T>  the type of the success value in the {@link ValueWithFailures}
   * @param items  the items to combine
   * @return a new instance with a list of success values
   */
  public static <T> ValueWithFailures<List<T>> combineValuesAsList(
      Iterable<? extends ValueWithFailures<? extends T>> items) {

    ImmutableList.Builder<T> values = ImmutableList.builder();
    ImmutableList<FailureItem> failures = combine(items, values);
    return ValueWithFailures.of(values.build(), failures);
  }

  /**
   * Combines separate instances of {@code ValueWithFailure} into a single instance,
   * using a set to collect the values.
   * <p>
   * This converts {@code Iterable<ValueWithFailures<T>>} to {@code ValueWithFailures<Set<T>>}.
   *
   * @param <T>  the type of the success value in the {@link ValueWithFailures}
   * @param items  the items to combine
   * @return a new instance with a set of success values
   */
  public static <T> ValueWithFailures<Set<T>> combineValuesAsSet(
      Iterable<? extends ValueWithFailures<? extends T>> items) {

    ImmutableSet.Builder<T> values = ImmutableSet.builder();
    ImmutableList<FailureItem> failures = combine(items, values);
    return ValueWithFailures.of(values.build(), failures);
  }

  // combines the collection into the specified builder
  private static <T> ImmutableList<FailureItem> combine(
      Iterable<? extends ValueWithFailures<? extends T>> items,
      ImmutableCollection.Builder<T> values) {

    ImmutableList.Builder<FailureItem> failures = ImmutableList.builder();
    for (ValueWithFailures<? extends T> item : items) {
      values.add(item.getValue());
      failures.addAll(item.getFailures());
    }
    return failures.build();
  }

  // mutable combined instance builder for use in stream collection
  // using a single dedicated collector is more efficient than a reduction with multiple calls to combinedWith()
  private static final class StreamBuilder<T, B extends ImmutableCollection.Builder<T>> {

    private final B values;
    private final ImmutableList.Builder<FailureItem> failures = ImmutableList.builder();

    private StreamBuilder(B values) {
      this.values = values;
    }

    private void add(ValueWithFailures<? extends T> item) {
      values.add(item.getValue());
      failures.addAll(item.getFailures());
    }

    private void addResult(Result<? extends T> item) {
      item.ifSuccess(value -> values.add(value));
      item.ifFailure(failure -> failures.addAll(failure.getItems()));
    }

    private StreamBuilder<T, B> combine(StreamBuilder<T, B> builder) {
      values.addAll(builder.values.build());
      failures.addAll(builder.failures.build());
      return this;
    }

    // cast to the right collection type, can assume the methods in this class are using the correct types
    @SuppressWarnings("unchecked")
    private <C extends Collection<T>> ValueWithFailures<C> build() {
      return (ValueWithFailures<C>) ValueWithFailures.of(values.build(), failures.build());
    }
  }

  private static final class MapStreamBuilder<K, V, B extends ImmutableMap.Builder<K, V>> {

    private final B values;
    private final ImmutableList.Builder<FailureItem> failures = ImmutableList.builder();

    private MapStreamBuilder(B values) {
      this.values = values;
    }

    private void add(Map.Entry<K, ValueWithFailures<V>> item) {
      values.put(item.getKey(), item.getValue().getValue());
      failures.addAll(item.getValue().getFailures());
    }

    private void addResult(Map.Entry<K, Result<V>> item) {
      Result<V> itemValue = item.getValue();
      itemValue.ifSuccess(value -> values.put(item.getKey(), value));
      itemValue.ifFailure(failure -> failures.addAll(failure.getItems()));
    }

    private MapStreamBuilder<K, V, B> combine(MapStreamBuilder<K, V, B> builder) {
      values.putAll(builder.values.build());
      failures.addAll(builder.failures.build());
      return this;
    }

    // cast to the right collection type, can assume the methods in this class are using the correct types
    @SuppressWarnings("unchecked")
    private <C extends Map<K, V>> ValueWithFailures<C> build() {
      return (ValueWithFailures<C>) ValueWithFailures.of(values.buildOrThrow(), failures.build());
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if there are any failures.
   *
   * @return true if there are any failures
   */
  public boolean hasFailures() {
    return !failures.isEmpty();
  }

  /**
   * Processes the value by applying a function that alters the value.
   * <p>
   * This operation allows post-processing of a result value.
   * The specified function represents a conversion to be performed on the value.
   * <p>
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @param <R>  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the transformed instance of value and failures
   */
  public <R> ValueWithFailures<R> map(Function<? super T, ? extends R> function) {
    R transformedValue = Objects.requireNonNull(function.apply(value));
    return ValueWithFailures.of(transformedValue, this.failures);
  }

  /**
   * Processes the value by applying a function that alters the failures.
   * <p>
   * This operation allows post-processing of a result failures.
   * The specified function represents a conversion to be performed on the failures.
   * <p>
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @param function  the function to transform the failures with
   * @return the transformed instance of value and failures
   */
  public ValueWithFailures<T> mapFailures(Function<FailureItem, FailureItem> function) {
    if (failures.isEmpty()) {
      return this;
    }
    return ValueWithFailures.of(value, failures.stream().map(function).collect(toImmutableList()));
  }

  /**
   * Processes the value by applying a function that returns another result.
   * <p>
   * This operation allows post-processing of a result value.
   * This is similar to {@link #map(Function)} but the function returns a {@code ValueWithFailures}.
   * The result of this method consists of the transformed value, and the combined list of failures.
   * <p>
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @param <R>  the type of the value in the returned result
   * @param function  the function to transform the value with
   * @return the transformed instance of value and failures
   */
  public <R> ValueWithFailures<R> flatMap(Function<? super T, ValueWithFailures<R>> function) {
    ValueWithFailures<R> transformedValue = Objects.requireNonNull(function.apply(value));
    ImmutableList<FailureItem> combinedFailures = concatToList(failures, transformedValue.failures);
    return ValueWithFailures.of(transformedValue.value, combinedFailures);
  }

  /**
   * Combines this instance with another.
   * <p>
   * If both instances contain lists of the same type, the combining function will
   * often be {@code Guavate::concatToList}.
   * <p>
   * It is strongly advised to ensure that the function cannot throw an exception.
   * Exceptions from the function are not caught.
   *
   * @param <U>  the type of the value in the other instance
   * @param <R>  the type of the value in the returned result
   * @param other  the other instance
   * @param combiner  the function that combines the two values
   * @return the combined instance of value and failures
   */
  public <U, R> ValueWithFailures<R> combinedWith(ValueWithFailures<U> other, BiFunction<T, U, R> combiner) {
    R combinedValue = Objects.requireNonNull(combiner.apply(value, other.value));
    ImmutableList<FailureItem> combinedFailures = concatToList(failures, other.failures);
    return ValueWithFailures.of(combinedValue, combinedFailures);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a new instance with the specified value, retaining the current failures.
   * <p>
   * This can be useful as an inline alternative to {@link #map(Function)}.
   *
   * @param <R>  the type of the value in the returned result
   * @param value  the new value
   * @return the combined instance of value and failures
   */
  public <R> ValueWithFailures<R> withValue(R value) {
    return ValueWithFailures.of(value, this.failures);
  }

  /**
   * Returns a new instance with the specified value, combining the failures.
   * <p>
   * This can be useful as an inline alternative to {@link #flatMap(Function)}.
   *
   * @param <R>  the type of the value in the returned result
   * @param value  the new value
   * @param additionalFailures  the additional failures
   * @return the combined instance of value and failures
   */
  public <R> ValueWithFailures<R> withValue(R value, List<FailureItem> additionalFailures) {
    return ValueWithFailures.of(value, concatToList(this.failures, additionalFailures));
  }

  /**
   * Returns a new instance with the specified value, combining the failures.
   * <p>
   * This can be useful as an inline alternative to {@link #flatMap(Function)}.
   *
   * @param <R>  the type of the value in the returned result
   * @param valueWithFailures  the new value with failures
   * @return the combined instance of value and failures
   */
  public <R> ValueWithFailures<R> withValue(ValueWithFailures<R> valueWithFailures) {
    return ValueWithFailures.of(
        valueWithFailures.getValue(),
        concatToList(this.failures, valueWithFailures.getFailures()));
  }

  /**
   * Returns a new instance with the specified failures, retaining the current value.
   *
   * @param additionalFailures  the additional failures
   * @return the combined instance of value and failures
   */
  public ValueWithFailures<T> withAdditionalFailures(List<FailureItem> additionalFailures) {
    return ValueWithFailures.of(value, concatToList(this.failures, additionalFailures));
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ValueWithFailures}.
   * @return the meta-bean, not null
   */
  @SuppressWarnings("rawtypes")
  public static ValueWithFailures.Meta meta() {
    return ValueWithFailures.Meta.INSTANCE;
  }

  /**
   * The meta-bean for {@code ValueWithFailures}.
   * @param <R>  the bean's generic type
   * @param cls  the bean's generic type
   * @return the meta-bean, not null
   */
  @SuppressWarnings("unchecked")
  public static <R> ValueWithFailures.Meta<R> metaValueWithFailures(Class<R> cls) {
    return ValueWithFailures.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ValueWithFailures.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private ValueWithFailures(
      T value,
      List<FailureItem> failures) {
    JodaBeanUtils.notNull(value, "value");
    JodaBeanUtils.notNull(failures, "failures");
    this.value = value;
    this.failures = ImmutableList.copyOf(failures);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ValueWithFailures.Meta<T> metaBean() {
    return ValueWithFailures.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the success value.
   * @return the value of the property, not null
   */
  public T getValue() {
    return value;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the failure items.
   * @return the value of the property, not null
   */
  public ImmutableList<FailureItem> getFailures() {
    return failures;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ValueWithFailures<?> other = (ValueWithFailures<?>) obj;
      return JodaBeanUtils.equal(value, other.value) &&
          JodaBeanUtils.equal(failures, other.failures);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(value);
    hash = hash * 31 + JodaBeanUtils.hashCode(failures);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("ValueWithFailures{");
    buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
    buf.append("failures").append('=').append(JodaBeanUtils.toString(failures));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ValueWithFailures}.
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
        this, "value", ValueWithFailures.class, Object.class);
    /**
     * The meta-property for the {@code failures} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<FailureItem>> failures = DirectMetaProperty.ofImmutable(
        this, "failures", ValueWithFailures.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "value",
        "failures");

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
        case 675938345:  // failures
          return failures;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends ValueWithFailures<T>> builder() {
      return new ValueWithFailures.Builder<>();
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public Class<? extends ValueWithFailures<T>> beanType() {
      return (Class) ValueWithFailures.class;
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
     * The meta-property for the {@code failures} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<FailureItem>> failures() {
      return failures;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 111972721:  // value
          return ((ValueWithFailures<?>) bean).getValue();
        case 675938345:  // failures
          return ((ValueWithFailures<?>) bean).getFailures();
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
   * The bean-builder for {@code ValueWithFailures}.
   * @param <T>  the type
   */
  private static final class Builder<T> extends DirectPrivateBeanBuilder<ValueWithFailures<T>> {

    private T value;
    private List<FailureItem> failures = ImmutableList.of();

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
        case 675938345:  // failures
          return failures;
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
        case 675938345:  // failures
          this.failures = (List<FailureItem>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public ValueWithFailures<T> build() {
      return new ValueWithFailures<>(
          value,
          failures);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("ValueWithFailures.Builder{");
      buf.append("value").append('=').append(JodaBeanUtils.toString(value)).append(',').append(' ');
      buf.append("failures").append('=').append(JodaBeanUtils.toString(failures));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
