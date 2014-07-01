/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Utilities that help bridge the gap between Java 8 and Google Guava.
 * <p>
 * Guava has the {@link FluentIterable} concept which is similar to streams.
 * In many ways, fluent iterable is nicer, because it directly binds to the
 * immutable collection classes. However, on balance it seems wise to use
 * the stream API rather than {@code FluentIterable} in Java 8.
 */
public final class Guavate {

  /**
   * Restricted constructor.
   */
  private Guavate() {
  }

  //-------------------------------------------------------------------------
  /**
   * Converts an iterable to a serial stream.
   * <p>
   * This is harder than it should be, a method {@code Stream.of(Iterable)}
   * would have been appropriate, but cannot be added now.
   * 
   * @param <T>  the type of element in the list
   * @param iterable  the iterable to convert
   * @return the immutable list collector
   */
  public static <T> Stream<T> stream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable list.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableList}.
   * 
   * @param <T>  the type of element in the list
   * @return the immutable list collector
   */
  public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList() {
    return Collector.of(
        ImmutableList.Builder<T>::new,
        ImmutableList.Builder<T>::add,
        (l, r) -> l.addAll(r.build()),
        ImmutableList.Builder<T>::build);
  }

  /**
   * Collector used at the end of a stream to build an immutable set.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSet}.
   * 
   * @param <T>  the type of element in the set
   * @return the immutable set collector
   */
  public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toImmutableSet() {
    return Collector.of(
        ImmutableSet.Builder<T>::new,
        ImmutableSet.Builder<T>::add,
        (l, r) -> l.addAll(r.build()),
        ImmutableSet.Builder<T>::build,
        Collector.Characteristics.UNORDERED);
  }

  /**
   * Collector used at the end of a stream to build an immutable sorted set.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSet}.
   * 
   * @param <T>  the type of element in the sorted set
   * @return the immutable sorted set collector
   */
  public static <T extends Comparable<?>>
      Collector<T, ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>> toImmutableSortedSet() {
    return Collector.of(
        (Supplier<ImmutableSortedSet.Builder<T>>) ImmutableSortedSet::naturalOrder,
        ImmutableSortedSet.Builder<T>::add,
        (l, r) -> l.addAll(r.build()),
        ImmutableSortedSet.Builder<T>::build,
        Collector.Characteristics.UNORDERED);
  }

  /**
   * Collector used at the end of a stream to build an immutable sorted set.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSet}.
   * 
   * @param <T>  the type of element in the sorted set
   * @param comparator  the comparator
   * @return the immutable sorted set collector
   */
  public static <T> Collector<T, ImmutableSortedSet.Builder<T>, ImmutableSortedSet<T>>
      toImmutableSortedSet(Comparator<? super T> comparator) {
    return Collector.of(
        (Supplier<ImmutableSortedSet.Builder<T>>) () -> new ImmutableSortedSet.Builder<>(comparator),
        ImmutableSortedSet.Builder<T>::add,
        (l, r) -> l.addAll(r.build()),
        ImmutableSortedSet.Builder<T>::build,
        Collector.Characteristics.UNORDERED);
  }

  /**
   * Collector used at the end of a stream to build an immutable multiset.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableMultiset}.
   * 
   * @param <T>  the type of element in the multiset
   * @return the immutable multiset collector
   */
  public static <T> Collector<T, ImmutableMultiset.Builder<T>, ImmutableMultiset<T>> toImmutableMultiset() {
    return Collector.of(
        ImmutableMultiset.Builder<T>::new,
        ImmutableMultiset.Builder<T>::add,
        (l, r) -> l.addAll(r.build()),
        ImmutableMultiset.Builder<T>::build,
        Collector.Characteristics.UNORDERED);
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable map.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableMap}.
   * <p>
   * This returns a map by extracting a key from each element.
   * The input stream must resolve to unique keys.
   * The value associated with each key is the stream element.
   * See {@link Collectors#toMap(Function, Function)} for more details.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable map collector
   */
  public static <T, K> Collector<T, ?, ImmutableMap<K, T>> toImmutableMap(
      Function<? super T, ? extends K> keyExtractor) {
    
    return toImmutableMap(keyExtractor, Function.identity());
  }

  /**
   * Collector used at the end of a stream to build an immutable map.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableMap}.
   * <p>
   * This returns a map by converting each stream element to a key and value.
   * The input stream must resolve to unique keys.
   * See {@link Collectors#toMap(Function, Function)} for more details.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result map
   * @param <V> the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable map collector
   */
  public static <T, K, V> Collector<T, ?, ImmutableMap<K,V>> toImmutableMap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {

    return Collector.of(
        ImmutableMap.Builder<K, V>::new,
        (builder, val) -> builder.put(keyExtractor.apply(val), valueExtractor.apply(val)),
        (l, r) -> l.putAll(r.build()),
        ImmutableMap.Builder<K, V>::build,
        Collector.Characteristics.UNORDERED);
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable sorted map.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSortedMap}.
   * <p>
   * This returns a map by extracting a key from each element.
   * The input stream must resolve to unique keys.
   * The value associated with each key is the stream element.
   * See {@link Collectors#toMap(Function, Function)} for more details.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable sorted map collector
   */
  public static <T, K extends Comparable<?>> Collector<T, ?, ImmutableSortedMap<K, T>> toImmutableSortedMap(
      Function<? super T, ? extends K> keyExtractor) {
    
    return toImmutableSortedMap(keyExtractor, Function.identity());
  }

  /**
   * Collector used at the end of a stream to build an immutable sorted map.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSortedMap}.
   * <p>
   * This returns a map by converting each stream element to a key and value.
   * The input stream must resolve to unique keys.
   * See {@link Collectors#toMap(Function, Function)} for more details.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result map
   * @param <V> the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable sorted map collector
   */
  public static <T, K extends Comparable<?>, V> Collector<T, ?, ImmutableSortedMap<K,V>> toImmutableSortedMap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {

    return Collector.of(
        (Supplier<ImmutableSortedMap.Builder<K, V>>) ImmutableSortedMap::naturalOrder,
        (builder, val) -> builder.put(keyExtractor.apply(val), valueExtractor.apply(val)),
        (l, r) -> l.putAll(r.build()),
        ImmutableSortedMap.Builder<K, V>::build,
        Collector.Characteristics.UNORDERED);
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable multimap.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableListMultimap}.
   * <p>
   * This returns a multimap by extracting a key from each element.
   * The value associated with each key is the stream element.
   * Stream elements may be converted to the same key, with the values forming a multimap list.
   * See {@link Collectors#groupingBy(Function)} for more details.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable multimap collector
   */
  public static <T, K> Collector<T, ?, ImmutableListMultimap<K, T>> toImmutableListMultimap(
      Function<? super T, ? extends K> keyExtractor) {
    
    return toImmutableListMultimap(keyExtractor, Function.identity());
  }

  /**
   * Collector used at the end of a stream to build an immutable multimap.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableListMultimap}.
   * <p>
   * This returns a multimap by converting each stream element to a key and value.
   * Stream elements may be converted to the same key, with the values forming a multimap list.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result multimap
   * @param <V> the type of the values in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable multimap collector
   */
  public static <T, K, V> Collector<T, ?, ImmutableListMultimap<K,V>> toImmutableListMultimap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {

    return Collector.of(
        ImmutableListMultimap.Builder<K, V>::new,
        (builder, val) -> builder.put(keyExtractor.apply(val), valueExtractor.apply(val)),
        (l, r) -> l.putAll(r.build()),
        ImmutableListMultimap.Builder<K, V>::build,
        Collector.Characteristics.UNORDERED);
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to build an immutable multimap.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSetMultimap}.
   * <p>
   * This returns a multimap by extracting a key from each element.
   * The value associated with each key is the stream element.
   * Stream elements may be converted to the same key, with the values forming a multimap set.
   * See {@link Collectors#groupingBy(Function)} for more details.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @return the immutable multimap collector
   */
  public static <T, K> Collector<T, ?, ImmutableSetMultimap<K, T>> toImmutableSetMultimap(
      Function<? super T, ? extends K> keyExtractor) {
    
    return toImmutableSetMultimap(keyExtractor, Function.identity());
  }

  /**
   * Collector used at the end of a stream to build an immutable multimap.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSetMultimap}.
   * <p>
   * This returns a multimap by converting each stream element to a key and value.
   * Stream elements may be converted to the same key, with the values forming a multimap set.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result multimap
   * @param <V> the type of the values in the result multimap
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @return the immutable multimap collector
   */
  public static <T, K, V> Collector<T, ?, ImmutableSetMultimap<K,V>> toImmutableSetMultimap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {

    return Collector.of(
        ImmutableSetMultimap.Builder<K, V>::new,
        (builder, val) -> builder.put(keyExtractor.apply(val), valueExtractor.apply(val)),
        (l, r) -> l.putAll(r.build()),
        ImmutableSetMultimap.Builder<K, V>::build,
        Collector.Characteristics.UNORDERED);
  }

}
