/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.Guavate.entry;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;

/**
 * A stream implementation based on {@code Map.Entry}.
 * <p>
 * This stream wraps a {@code Stream&lt;Map.Entry&gt;}, providing convenient methods for
 * manipulating the keys and values. Unlike a {@code Map}, the keys in a {@code MapStream}
 * do not have to be unique, although certain methods will fail if they are not unique.
 *
 * @param <K>  the key type
 * @param <V>  the value type
 */
public final class MapStream<K, V>
    implements Stream<Map.Entry<K, V>> {

  /** The stream of map entries. */
  private final Stream<Map.Entry<K, V>> underlying;

  //-------------------------------------------------------------------------
  /**
   * Returns a stream over the entries in the map.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param map  the map to wrap
   * @return a stream over the entries in the map
   */
  public static <K, V> MapStream<K, V> of(Map<K, V> map) {
    return new MapStream<>(map.entrySet().stream());
  }

  /**
   * Returns a stream over all the entries in the multimap.
   * <p>
   * This will typically create a stream with duplicate keys.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param multimap  the multimap to wrap
   * @return a stream over the entries in the multimap
   */
  public static <K, V> MapStream<K, V> of(Multimap<K, V> multimap) {
    return new MapStream<>(multimap.entries().stream());
  }

  /**
   * Returns a stream of map entries where the keys and values are taken from a collection.
   *
   * @param <V>  the key and value type
   * @param collection  the collection
   * @return a stream of map entries derived from the values in the collection
   */
  public static <V> MapStream<V, V> of(Collection<V> collection) {
    return of(collection.stream());
  }

  /**
   * Returns a stream of map entries where the keys and values are taken from a stream.
   *
   * @param <V>  the key and value type
   * @param stream  the stream
   * @return a stream of map entries derived from the values in the stream
   */
  public static <V> MapStream<V, V> of(Stream<V> stream) {
    return of(stream, key -> key);
  }

  /**
   * Returns a stream of map entries where the values are taken from a collection
   * and the keys are created by applying a function to each value.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param collection  the collection of values
   * @param keyFunction  a function which returns the key for a value
   * @return a stream of map entries derived from the values in the collection
   */
  public static <K, V> MapStream<K, V> of(Collection<V> collection, Function<? super V, ? extends K> keyFunction) {
    return of(collection.stream(), keyFunction);
  }

  /**
   * Returns a stream of map entries where the keys and values are extracted from a
   * collection by applying a function to each item in the collection.
   *
   * @param <T>  the collection type
   * @param <K>  the key type
   * @param <V>  the value type
   * @param collection  the collection of values
   * @param keyFunction  a function which returns the key
   * @param valueFunction  a function which returns the value
   * @return a stream of map entries derived from the collection
   */
  public static <T, K, V> MapStream<K, V> of(
      Collection<T> collection,
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {

    return of(collection.stream(), keyFunction, valueFunction);
  }

  /**
   * Returns a stream of map entries where the values are taken from a stream
   * and the keys are created by applying a function to each value.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param stream  the stream of values
   * @param keyFunction  a function which returns the key for a value
   * @return a stream of map entries derived from the values in the stream
   */
  public static <K, V> MapStream<K, V> of(Stream<V> stream, Function<? super V, ? extends K> keyFunction) {
    return new MapStream<>(stream.map(v -> entry(keyFunction.apply(v), v)));
  }

  /**
   * Returns a stream of map entries where the keys and values are extracted from a
   * stream by applying a function to each item in the stream.
   *
   * @param <T>  the collection type
   * @param <K>  the key type
   * @param <V>  the value type
   * @param stream  the stream of values
   * @param keyFunction  a function which returns the key for a value
   * @param valueFunction  a function which returns the value
   * @return a stream of map entries derived from the stream
   */
  public static <T, K, V> MapStream<K, V> of(
      Stream<T> stream,
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {

    return new MapStream<K, V>(stream.map(item -> entry(keyFunction.apply(item), valueFunction.apply(item))));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a map stream that combines two other streams, continuing until either stream ends.
   * <p>
   * Note that this can produce a stream with non-unique keys.
   *
   * @param <K>  the key type
   * @param <V>  the value type
   * @param keyStream  the stream of keys
   * @param valueStream  the stream of values
   * @return a stream of map entries derived from the stream
   */
  public static <K, V> MapStream<K, V> zip(Stream<K> keyStream, Stream<V> valueStream) {
    return new MapStream<K, V>(Guavate.zip(keyStream, valueStream, Guavate::entry));
  }

  /**
   * Returns a stream of map entries where each key is the index of the value in the original stream.
   *
   * @param <V>  the value type
   * @param stream  the stream of values
   * @return a stream of map entries derived from the stream
   */
  public static <V> MapStream<Integer, V> zipWithIndex(Stream<V> stream) {
    Stream<Map.Entry<Integer, V>> zipped =
        Streams.mapWithIndex(stream, (value, index) -> entry(Math.toIntExact(index), value));
    return new MapStream<Integer, V>(zipped);
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

  /**
   * Creates a stream of map entries whose elements are those of the first stream followed by those of the second
   * stream.
   *
   * @param a  the first stream of entries
   * @param b  the second stream of entries
   * @param <K>  the key type
   * @param <V>  the value type
   * @return the concatenation of the two input streams
   */
  public static <K, V> MapStream<K, V> concat(
      MapStream<? extends K, ? extends V> a,
      MapStream<? extends K, ? extends V> b) {

    @SuppressWarnings("unchecked")
    MapStream<K, V> kvMapStream = new MapStream<>(Streams.concat(
        (Stream<? extends Map.Entry<K, V>>) a,
        (Stream<? extends Map.Entry<K, V>>) b));
    return kvMapStream;
  }

  //-------------------------------------------------------------------------
  // creates an instance
  private MapStream(Stream<Map.Entry<K, V>> underlying) {
    this.underlying = underlying;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the keys as a stream, dropping the values.
   * <p>
   * A {@link MapStream} may contain the same key more than once, so callers
   * may need to call {@link Stream#distinct()} on the result.
   *
   * @return a stream of the keys
   */
  public Stream<K> keys() {
    return underlying.map(Entry::getKey);
  }

  /**
   * Returns the values as a stream, dropping the keys.
   *
   * @return a stream of the values
   */
  public Stream<V> values() {
    return underlying.map(Entry::getValue);
  }

  //-------------------------------------------------------------------------
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
   * Filters the stream checking the type of each key.
   * <p>
   * Entries are included in the returned stream if the key is an instance of the specified type.
   *
   * @param <R>  the type to filter to
   * @param castToClass  the class to filter the keys to
   * @return a stream including only those entries where the key is an instance of the specified type
   */
  public <R> MapStream<R, V> filterKeys(Class<R> castToClass) {
    return wrap(underlying
        .filter(e -> castToClass.isInstance(e.getKey()))
        .map(e -> entry(castToClass.cast(e.getKey()), e.getValue())));
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
   * Filters the stream checking the type of each value.
   * <p>
   * Entries are included in the returned stream if the value is an instance of the specified type.
   *
   * @param <R>  the type to filter to
   * @param castToClass  the class to filter the values to
   * @return a stream including only those entries where the value is an instance of the specified type
   */
  public <R> MapStream<K, R> filterValues(Class<R> castToClass) {
    return wrap(underlying
        .filter(e -> castToClass.isInstance(e.getValue()))
        .map(e -> entry(e.getKey(), castToClass.cast(e.getValue()))));
  }

  //-------------------------------------------------------------------------
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
   * @param mapper  a mapper function whose return value is used as the new value
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
   * Transforms the entries in the stream to doubles by applying a mapper function to each key and value.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the double values returned from the mapper function
   */
  public DoubleStream mapToDouble(ToDoubleBiFunction<? super K, ? super V> mapper) {
    return underlying.mapToDouble(e -> mapper.applyAsDouble(e.getKey(), e.getValue()));
  }

  /**
   * Transforms the entries in the stream to integers by applying a mapper function to each key and value.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the integer values returned from the mapper function
   */
  public IntStream mapToInt(ToIntBiFunction<? super K, ? super V> mapper) {
    return underlying.mapToInt(e -> mapper.applyAsInt(e.getKey(), e.getValue()));
  }

  //-------------------------------------------------------------------------
  /**
   * Transforms the keys in the stream by applying a mapper function to each key.
   * <p>
   * The new keys produced will be associated with the original value.
   *
   * @param mapper  a mapper function whose return values are the keys in the new stream
   * @param <R>  the type of the new keys
   * @return a stream of entries with new keys from the mapper function assigned to the values
   */
  public <R> MapStream<R, V> flatMapKeys(Function<? super K, Stream<R>> mapper) {
    return wrap(underlying.flatMap(e -> mapper.apply(e.getKey()).map(newKey -> entry(newKey, e.getValue()))));
  }

  /**
   * Transforms the keys in the stream by applying a mapper function to each key and value.
   * <p>
   * The new keys produced will be associated with the original value.
   * <p>
   * For example this could turn a {@code MapStream<List<String>, LocalDate>} into a
   * {@code MapStream<String, LocalDate>}
   *
   * @param mapper  a mapper function whose return values are the keys in the new stream
   * @param <R>  the type of the new keys
   * @return a stream of entries with new keys from the mapper function assigned to the values
   */
  public <R> MapStream<R, V> flatMapKeys(BiFunction<? super K, ? super V, Stream<R>> mapper) {
    return wrap(underlying
        .flatMap(e -> mapper.apply(e.getKey(), e.getValue()).map(newKey -> entry(newKey, e.getValue()))));
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each value.
   * <p>
   * The new values produced will be associated with the original key.
   * <p>
   * For example this could turn a {@code MapStream<LocalDate, List<String>>} into a
   * {@code MapStream<LocalDate, String>}
   *
   * @param mapper  a mapper function whose return values are the values in the new stream
   * @param <R>  the type of the new values
   * @return a stream of entries with new values from the mapper function assigned to the keys
   */
  public <R> MapStream<K, R> flatMapValues(Function<? super V, Stream<R>> mapper) {
    return wrap(underlying.flatMap(e -> mapper.apply(e.getValue()).map(newValue -> entry(e.getKey(), newValue))));
  }

  /**
   * Transforms the values in the stream by applying a mapper function to each key and value.
   * <p>
   * The new values produced will be associated with the original key.
   * <p>
   * For example this could turn a {@code MapStream<LocalDate, List<String>>} into a
   * {@code MapStream<LocalDate, String>}
   *
   * @param mapper  a mapper function whose return values are the values in the new stream
   * @param <R>  the type of the new values
   * @return a stream of entries with new values from the mapper function assigned to the keys
   */
  public <R> MapStream<K, R> flatMapValues(BiFunction<? super K, ? super V, Stream<R>> mapper) {
    return wrap(underlying
        .flatMap(e -> mapper.apply(e.getKey(), e.getValue()).map(newValue -> entry(e.getKey(), newValue))));
  }

  /**
   * Transforms the entries in the stream by applying a mapper function to each key and value to produce a stream of
   * elements, and then flattening the resulting stream of streams.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @param <R>  the type of the elements in the new stream
   * @return a stream containing the values returned from the mapper function
   */
  public <R> Stream<R> flatMap(BiFunction<? super K, ? super V, Stream<R>> mapper) {
    return underlying.flatMap(e -> mapper.apply(e.getKey(), e.getValue()));
  }

  /**
   * Transforms the entries in the stream to doubles by applying a mapper function to each key and value to produce
   * a stream of doubles, and then flattening the resulting stream of streams.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the double values returned from the mapper function
   */
  public DoubleStream flatMapToDouble(BiFunction<? super K, ? super V, ? extends DoubleStream> mapper) {
    return underlying.flatMapToDouble(e -> mapper.apply(e.getKey(), e.getValue()));
  }

  /**
   * Transforms the entries in the stream to integers by applying a mapper function to each key and value to produce
   * a stream of integers, and then flattening the resulting stream of streams.
   *
   * @param mapper  a mapper function whose return values are included in the new stream
   * @return a stream containing the integer values returned from the mapper function
   */
  public IntStream flatMapToInt(BiFunction<? super K, ? super V, ? extends IntStream> mapper) {
    return underlying.flatMapToInt(e -> mapper.apply(e.getKey(), e.getValue()));
  }

  //-----------------------------------------------------------------------
  /**
   * Sorts the entries in the stream by comparing the keys using their natural ordering.
   * <p>
   * If the keys in this map stream are not {@code Comparable} a {@code java.lang.ClassCastException} may be thrown.
   * In this case use {@link #sortedKeys(Comparator)} instead.
   *
   * @return the sorted stream
   */
  @SuppressWarnings("unchecked")
  public MapStream<K, V> sortedKeys() {
    Comparator<K> comparator = (Comparator<K>) Comparator.naturalOrder();
    return wrap(underlying.sorted((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey())));
  }

  /**
   * Sorts the entries in the stream by comparing the keys using the supplied comparator.
   *
   * @param comparator  a comparator of keys
   * @return the sorted stream
   */
  public MapStream<K, V> sortedKeys(Comparator<? super K> comparator) {
    return wrap(underlying.sorted((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey())));
  }

  /**
   * Sorts the entries in the stream by comparing the values using their natural ordering.
   * <p>
   * If the values in this map stream are not {@code Comparable} a {@code java.lang.ClassCastException} may be thrown.
   * In this case use {@link #sortedValues(Comparator)} instead.
   *
   * @return the sorted stream
   */
  @SuppressWarnings("unchecked")
  public MapStream<K, V> sortedValues() {
    Comparator<V> comparator = (Comparator<V>) Comparator.naturalOrder();
    return wrap(underlying.sorted((e1, e2) -> comparator.compare(e1.getValue(), e2.getValue())));
  }

  /**
   * Sorts the entries in the stream by comparing the values using the supplied comparator.
   *
   * @param comparator  a comparator of values
   * @return the sorted stream
   */
  public MapStream<K, V> sortedValues(Comparator<? super V> comparator) {
    return wrap(underlying.sorted((e1, e2) -> comparator.compare(e1.getValue(), e2.getValue())));
  }

  //-----------------------------------------------------------------------
  /**
   * Finds the minimum entry in the stream by comparing the keys using the supplied comparator.
   * <p>
   * This is a terminal operation.
   *
   * @param comparator  a comparator of keys
   * @return the minimum entry
   */
  public Optional<Map.Entry<K, V>> minKeys(Comparator<? super K> comparator) {
    return underlying.min((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey()));
  }

  /**
   * Finds the minimum entry in the stream by comparing the values using the supplied comparator.
   * <p>
   * This is a terminal operation.
   *
   * @param comparator  a comparator of values
   * @return the minimum entry
   */
  public Optional<Map.Entry<K, V>> minValues(Comparator<? super V> comparator) {
    return underlying.min((e1, e2) -> comparator.compare(e1.getValue(), e2.getValue()));
  }

  /**
   * Finds the maximum entry in the stream by comparing the keys using the supplied comparator.
   * <p>
   * This is a terminal operation.
   *
   * @param comparator  a comparator of keys
   * @return the maximum entry
   */
  public Optional<Map.Entry<K, V>> maxKeys(Comparator<? super K> comparator) {
    return underlying.max((e1, e2) -> comparator.compare(e1.getKey(), e2.getKey()));
  }

  /**
   * Finds the maximum entry in the stream by comparing the values using the supplied comparator.
   * <p>
   * This is a terminal operation.
   *
   * @param comparator  a comparator of values
   * @return the maximum entry
   */
  public Optional<Map.Entry<K, V>> maxValues(Comparator<? super V> comparator) {
    return underlying.max((e1, e2) -> comparator.compare(e1.getValue(), e2.getValue()));
  }

  //-----------------------------------------------------------------------
  /**
   * Returns whether any elements of this stream match the provided predicate.
   * <p>
   * This is a short-circuiting terminal operation.
   *
   * @param predicate  the predicate to apply to the entries
   * @return whether any of the entries matched the predicate
   */
  public boolean anyMatch(BiPredicate<? super K, ? super V> predicate) {
    return underlying.anyMatch(e -> predicate.test(e.getKey(), e.getValue()));
  }

  /**
   * Returns whether all elements of this stream match the provided predicate.
   * <p>
   * This is a short-circuiting terminal operation.
   *
   * @param predicate  the predicate to apply to the entries
   * @return whether all of the entries matched the predicate
   */
  public boolean allMatch(BiPredicate<? super K, ? super V> predicate) {
    return underlying.allMatch(e -> predicate.test(e.getKey(), e.getValue()));
  }

  /**
   * Returns whether no elements of this stream match the provided predicate.
   * <p>
   * This is a short-circuiting terminal operation.
   *
   * @param predicate  the predicate to apply to the entries
   * @return whether none of the entries matched the predicate
   */
  public boolean noneMatch(BiPredicate<? super K, ? super V> predicate) {
    return underlying.noneMatch(e -> predicate.test(e.getKey(), e.getValue()));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an immutable map built from the entries in the stream.
   * <p>
   * The keys must be unique or an exception will be thrown.
   * Duplicate keys can be handled using {@link #toMap(BiFunction)}.
   * <p>
   * This is a terminal operation.
   *
   * @return an immutable map built from the entries in the stream
   * @throws IllegalArgumentException if the same key occurs more than once
   */
  public ImmutableMap<K, V> toMap() {
    return underlying.collect(Guavate.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  /**
   * Returns an immutable map built from the entries in the stream.
   * <p>
   * If the same key maps to multiple values the merge function is invoked with both values and the return
   * value is used in the map.
   * <p>
   * Can be used with {@link #concat(MapStream, MapStream)} to merge immutable
   * maps with duplicate keys.
   * <p>
   * For example, to merge immutable maps with duplicate keys preferring values in the first map:
   * <pre>
   *   MapStream.concat(mapStreamA, mapStreamB).toMap((a,b) -> a);
   * </pre>
   * <p>
   * This is a terminal operation.
   *
   * @param mergeFn  function used to merge values when the same key appears multiple times in the stream
   * @return an immutable map built from the entries in the stream
   */
  public ImmutableMap<K, V> toMap(BiFunction<? super V, ? super V, ? extends V> mergeFn) {
    return underlying.collect(Guavate.toImmutableMap(Entry::getKey, Entry::getValue, mergeFn));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an immutable map built from the entries in the stream, grouping by key.
   * <p>
   * Entries are grouped based on the equality of the key.
   * <p>
   * This is a terminal operation.
   *
   * @return an immutable map built from the entries in the stream
   */
  public ImmutableMap<K, List<V>> toMapGrouping() {
    return toMapGrouping(toList());
  }

  /**
   * Returns a stream built from a map of the entries in the stream, grouped by key.
   * <p>
   * Entries are grouped based on the equality of the key.
   *
   * @return a stream where the values have been grouped
   */
  public MapStream<K, List<V>> groupingAndThen() {
    return MapStream.of(toMapGrouping());
  }

  /**
   * Returns an immutable map built from the entries in the stream, grouping by key.
   * <p>
   * Entries are grouped based on the equality of the key.
   * The collector allows the values to be flexibly combined.
   * <p>
   * This is a terminal operation.
   *
   * @param <A>  the internal collector type
   * @param <R>  the type of the combined values
   * @param valueCollector  the collector used to combined the values
   * @return an immutable map built from the entries in the stream
   */
  public <A, R> ImmutableMap<K, R> toMapGrouping(Collector<? super V, A, R> valueCollector) {
    return underlying.collect(collectingAndThen(
        groupingBy(Entry::getKey, LinkedHashMap::new, mapping(Entry::getValue, valueCollector)),
        ImmutableMap::copyOf));
  }

  /**
   * Returns a stream built from a map of the entries in the stream, grouped by key.
   * <p>
   * Entries are grouped based on the equality of the key.
   * The collector allows the values to be flexibly combined.
   *
   * @param <A>  the internal collector type
   * @param <R>  the type of the combined values
   * @param valueCollector  the collector used to combined the values
   * @return a stream where the values have been grouped
   */
  public <A, R> MapStream<K, R> groupingAndThen(Collector<? super V, A, R> valueCollector) {
    return MapStream.of(toMapGrouping(valueCollector));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns an immutable list multimap built from the entries in the stream.
   * <p>
   * This is a terminal operation.
   *
   * @return an immutable list multimap built from the entries in the stream
   */
  public ImmutableListMultimap<K, V> toListMultimap() {
    return underlying.collect(Guavate.toImmutableListMultimap(Entry::getKey, Entry::getValue));
  }

  /**
   * Returns an immutable set multimap built from the entries in the stream.
   * <p>
   * This is a terminal operation.
   *
   * @return an immutable set multimap built from the entries in the stream
   */
  public ImmutableSetMultimap<K, V> toSetMultimap() {
    return underlying.collect(Guavate.toImmutableSetMultimap(Entry::getKey, Entry::getValue));
  }

  /**
   * Performs an action for each entry in the stream, passing the key and value to the action.
   * <p>
   * This is a terminal operation.
   *
   * @param action  an action performed for each entry in the stream
   */
  public void forEach(BiConsumer<? super K, ? super V> action) {
    underlying.forEach(e -> action.accept(e.getKey(), e.getValue()));
  }

  //-------------------------------------------------------------------------
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
  public <R> R collect(
      Supplier<R> supplier,
      BiConsumer<R, ? super Map.Entry<K, V>> accumulator,
      BiConsumer<R, R> combiner) {

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

  //-------------------------------------------------------------------------
  private static <K, V> MapStream<K, V> wrap(Stream<Map.Entry<K, V>> underlying) {
    return new MapStream<>(underlying);
  }

}
