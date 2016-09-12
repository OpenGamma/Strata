/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

/**
 * A stream implementation which adds methods for manipulating keys and values when streaming over map entries.
 * 
 * @param <K>  the key type
 * @param <V>  the value type
 */
public final class MapStream<K, V>
    implements Stream<Map.Entry<K, V>> {

  /** The stream of map entries. */
  private final Stream<Map.Entry<K, V>> underlying;

  /**
   * Returns a stream over the entries in the map.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param map a map
   * @return a stream over the entries in the map
   */
  public static <K, V> MapStream<K, V> of(Map<K, V> map) {
    return new MapStream<>(map.entrySet().stream());
  }

  /**
   * Returns a stream of map entries where the values are taken from a collection and the keys are created by
   * applying a function to each value.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param collection  the collection of values
   * @param keyFunction  a function which returns the key for a value
   * @return a stream of map entries derived from the values in the collection
   */
  public static <K, V> MapStream<K, V> of(Collection<V> collection, Function<V, K> keyFunction) {
    return new MapStream<>(collection.stream().map(v -> entry(keyFunction.apply(v), v)));
  }

  /**
   * Returns a stream of map entries where the values are taken from a stream and the keys are created by
   * applying a function to each value.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param stream  the stream of values
   * @param keyFunction  a function which returns the key for a value
   * @return a stream of map entries derived from the values in the stream
   */
  public static <K, V> MapStream<K, V> of(Stream<V> stream, Function<V, K> keyFunction) {
    return new MapStream<>(stream.map(v -> entry(keyFunction.apply(v), v)));
  }

  /**
   * Returns an empty map stream.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @return an empty map stream
   */
  public static <K, V> MapStream<K, V> empty() {
    return new MapStream<>(Stream.empty());
  }

  private MapStream(Stream<Map.Entry<K, V>> underlying) {
    this.underlying = underlying;
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Filters the stream by applying the predicate function to each key and value.
   * <p>
   * Entries are included in the returned stream if the predicate function returns true.
   *
   * @param predicate  a predicate function applied to each key and value in the stream
   * @return a stream including the entries for which the predicate function returned true
   */
  public MapStream<K, V> filter(BiFunction<? super K, ? super V, Boolean> predicate) {
    return wrap(underlying.filter(e -> predicate.apply(e.getKey(), e.getValue())));
  }

  /**
   * Filters the stream by applying the predicate function to each key.
   * <p>
   * Entries are included in the returned stream if the predicate function returns true.
   *
   * @param predicate  a predicate function applied to each key in the stream
   * @return a stream including the entries for which the predicate function returned true
   */
  public MapStream<K, V> filterKeys(Predicate<? super K> predicate) {
    return wrap(underlying.filter(e -> predicate.test(e.getKey())));
  }

  /**
   * Filters the stream by applying the predicate function to each value.
   * <p>
   * Entries are included in the returned stream if the predicate function returns true.
   *
   * @param predicate  a predicate function applied to each value in the stream
   * @return a stream including the entries for which the predicate function returned true
   */
  public MapStream<K, V> filterValues(Predicate<? super V> predicate) {
    return wrap(underlying.filter(e -> predicate.test(e.getValue())));
  }

  /**
   * Transforms the keys in the stream by applying a mapper function to each key.
   * <p>
   * The values are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new key
   * @param <R>  the type of the new keys
   * @return a stream of entries with the keys transformed and the values unchanged
   */
  public <R> MapStream<R, V> mapKeys(Function<? super K, ? extends R> mapper) {
    return wrap(underlying.map(e -> entry(mapper.apply(e.getKey()), e.getValue())));
  }

  /**
   * Transforms the keys in the stream by applying a mapper function to each key and value.
   * <p>
   * The values are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new key
   * @param <R>  the type of the new keys
   * @return a stream of entries with the keys transformed and the values unchanged
   */
  public <R> MapStream<R, V> mapKeys(BiFunction<? super K, ? super V, ? extends R> mapper) {
    return wrap(underlying.map(e -> entry(mapper.apply(e.getKey(), e.getValue()), e.getValue())));
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each value.
   * <p>
   * The keys are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new value
   * @param <R>  the type of the new values
   * @return a stream of entries with the values transformed and the keys unchanged
   */
  public <R> MapStream<K, R> mapValues(Function<? super V, ? extends R> mapper) {
    return wrap(underlying.map(e -> entry(e.getKey(), mapper.apply(e.getValue()))));
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each key and value.
   * <p>
   * The keys are unchanged.
   *
   * @param mapper  a mapper function whose return value is used as the new value.
   * @param <R>  the type of the new values
   * @return a stream of entries with the values transformed and the keys unchanged
   */
  public <R> MapStream<K, R> mapValues(BiFunction<? super K, ? super V, ? extends R> mapper) {
    return wrap(underlying.map(e -> entry(e.getKey(), mapper.apply(e.getKey(), e.getValue()))));
  }

  /**
   * Transforms the entries in the stream by applying a mapper function to each key and value.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @param <R>  the type of elements in the new stream
   * @return a stream containing the values returned from the mapper function
   */
  public <R> Stream<R> map(BiFunction<? super K, ? super V, ? extends R> mapper) {
    return underlying.map(e -> mapper.apply(e.getKey(), e.getValue()));
  }

  /**
   * Returns an immutable map built from the entries in the stream.
   * <p>
   * The keys must be unique or an exception will be thrown. Duplicate keys can be handled by using
   * {@link #collect(Collector)} and {@code Collectors.toMap}.
   *
   * @return an immutable map built from the entries in the stream
   */
  public ImmutableMap<K, V> toMap() {
    return underlying.collect(Guavate.toImmutableMap(e -> e.getKey(), e -> e.getValue()));
  }

  /**
   * Returns an immutable map built from the entries in the stream.
   * <p>
   * If the same key maps to multiple values the merge function is invoked with both values and the return
   * value is used in the map.
   *
   * @param mergeFn  function used to merge values when the same key appears multiple times in the stream
   * @return an immutable map built from the entries in the stream
   */
  public ImmutableMap<K, V> toMap(BiFunction<? super V, ? super V, ? extends V> mergeFn) {
    return underlying.collect(Guavate.toImmutableMap(e -> e.getKey(), e -> e.getValue(), mergeFn));
  }

  /**
   * Performs an action for each entry in the stream, passing the key and value to the action.
   *
   * @param action  an action performed for each entry in the stream
   */
  public void forEach(BiConsumer<? super K, ? super V> action) {
    underlying.forEach(e -> action.accept(e.getKey(), e.getValue()));
  }

  //--------------------------------------------------------------------------------------------------

  @Override
  public MapStream<K, V> filter(Predicate<? super Map.Entry<K, V>> predicate) {
    return wrap(underlying.filter(predicate));
  }

  @Override
  public <R> Stream<R> map(Function<? super Map.Entry<K, V>, ? extends R> mapper) {
    return underlying.map(mapper);
  }

  @Override
  public IntStream mapToInt(ToIntFunction<? super Map.Entry<K, V>> mapper) {
    return underlying.mapToInt(mapper);
  }

  @Override
  public LongStream mapToLong(ToLongFunction<? super Map.Entry<K, V>> mapper) {
    return underlying.mapToLong(mapper);
  }

  @Override
  public DoubleStream mapToDouble(ToDoubleFunction<? super Map.Entry<K, V>> mapper) {
    return underlying.mapToDouble(mapper);
  }

  @Override
  public <R> Stream<R> flatMap(Function<? super Map.Entry<K, V>, ? extends Stream<? extends R>> mapper) {
    return underlying.flatMap(mapper);
  }

  @Override
  public IntStream flatMapToInt(Function<? super Map.Entry<K, V>, ? extends IntStream> mapper) {
    return underlying.flatMapToInt(mapper);
  }

  @Override
  public LongStream flatMapToLong(Function<? super Map.Entry<K, V>, ? extends LongStream> mapper) {
    return underlying.flatMapToLong(mapper);
  }

  @Override
  public DoubleStream flatMapToDouble(Function<? super Map.Entry<K, V>, ? extends DoubleStream> mapper) {
    return underlying.flatMapToDouble(mapper);
  }

  @Override
  public MapStream<K, V> distinct() {
    return wrap(underlying.distinct());
  }

  @Override
  public MapStream<K, V> sorted() {
    return wrap(underlying.sorted());
  }

  @Override
  public MapStream<K, V> sorted(Comparator<? super Map.Entry<K, V>> comparator) {
    return wrap(underlying.sorted(comparator));
  }

  @Override
  public MapStream<K, V> peek(Consumer<? super Map.Entry<K, V>> action) {
    return wrap(underlying.peek(action));
  }

  @Override
  public MapStream<K, V> limit(long maxSize) {
    return wrap(underlying.limit(maxSize));
  }

  @Override
  public MapStream<K, V> skip(long n) {
    return wrap(underlying.skip(n));
  }

  @Override
  public void forEach(Consumer<? super Map.Entry<K, V>> action) {
    underlying.forEach(action);
  }

  @Override
  public void forEachOrdered(Consumer<? super Map.Entry<K, V>> action) {
    underlying.forEachOrdered(action);
  }

  @Override
  public Object[] toArray() {
    return underlying.toArray();
  }

  @Override
  public <A> A[] toArray(IntFunction<A[]> generator) {
    return underlying.toArray(generator);
  }

  @Override
  public Map.Entry<K, V> reduce(Map.Entry<K, V> identity, BinaryOperator<Map.Entry<K, V>> accumulator) {
    return underlying.reduce(identity, accumulator);
  }

  @Override
  public Optional<Map.Entry<K, V>> reduce(BinaryOperator<Map.Entry<K, V>> accumulator) {
    return underlying.reduce(accumulator);
  }

  @Override
  public <U> U reduce(U identity, BiFunction<U, ? super Map.Entry<K, V>, U> accumulator, BinaryOperator<U> combiner) {
    return underlying.reduce(identity, accumulator, combiner);
  }

  @Override
  public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super Map.Entry<K, V>> accumulator, BiConsumer<R, R> combiner) {
    return underlying.collect(supplier, accumulator, combiner);
  }

  @Override
  public <R, A> R collect(Collector<? super Map.Entry<K, V>, A, R> collector) {
    return underlying.collect(collector);
  }

  @Override
  public Optional<Map.Entry<K, V>> min(Comparator<? super Map.Entry<K, V>> comparator) {
    return underlying.min(comparator);
  }

  @Override
  public Optional<Map.Entry<K, V>> max(Comparator<? super Map.Entry<K, V>> comparator) {
    return underlying.max(comparator);
  }

  @Override
  public long count() {
    return underlying.count();
  }

  @Override
  public boolean anyMatch(Predicate<? super Map.Entry<K, V>> predicate) {
    return underlying.anyMatch(predicate);
  }

  @Override
  public boolean allMatch(Predicate<? super Map.Entry<K, V>> predicate) {
    return underlying.allMatch(predicate);
  }

  @Override
  public boolean noneMatch(Predicate<? super Map.Entry<K, V>> predicate) {
    return underlying.noneMatch(predicate);
  }

  @Override
  public Optional<Map.Entry<K, V>> findFirst() {
    return underlying.findFirst();
  }

  @Override
  public Optional<Map.Entry<K, V>> findAny() {
    return underlying.findAny();
  }

  @Override
  public Iterator<Map.Entry<K, V>> iterator() {
    return underlying.iterator();
  }

  @Override
  public Spliterator<Map.Entry<K, V>> spliterator() {
    return underlying.spliterator();
  }

  @Override
  public boolean isParallel() {
    return underlying.isParallel();
  }

  @Override
  public MapStream<K, V> sequential() {
    return wrap(underlying.sequential());
  }

  @Override
  public MapStream<K, V> parallel() {
    return wrap(underlying.parallel());
  }

  @Override
  public MapStream<K, V> unordered() {
    return wrap(underlying.unordered());
  }

  @Override
  public MapStream<K, V> onClose(Runnable closeHandler) {
    return wrap(underlying.onClose(closeHandler));
  }

  @Override
  public void close() {
    underlying.close();
  }

  //--------------------------------------------------------------------------------------------------

  private static <K, V> Map.Entry<K, V> entry(K k, V v) {
    return new AbstractMap.SimpleImmutableEntry<>(k, v);
  }

  private static <K, V> MapStream<K, V> wrap(Stream<Map.Entry<K, V>> underlying) {
    return new MapStream<>(underlying);
  }
}
