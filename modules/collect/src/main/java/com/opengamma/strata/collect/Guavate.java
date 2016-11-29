/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
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
import com.opengamma.strata.collect.tuple.ObjIntPair;
import com.opengamma.strata.collect.tuple.Pair;

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
   * @param <T>  the type of element in the iterable
   * @param iterable  the iterable to convert
   * @return a stream of the elements in the iterable
   */
  public static <T> Stream<T> stream(Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  /**
   * Converts an optional to a stream with zero or one elements.
   *
   * @param <T>  the type of optional element
   * @param optional  the optional
   * @return a stream containing a single value if the optional has a value, else a stream with no values.
   */
  public static <T> Stream<T> stream(Optional<T> optional) {
    return optional.isPresent() ?
        Stream.of(optional.get()) :
        Stream.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a stream that wraps a stream with the index.
   * <p>
   * Each input object is decorated with an {@link ObjIntPair}.
   * The {@code int} is the index of the element in the stream.
   *
   * @param <T>  the type of the stream
   * @param stream  the stream to index
   * @return a stream of pairs, containing the element and index
   */
  public static <T> Stream<ObjIntPair<T>> zipWithIndex(Stream<T> stream) {
    Spliterator<T> split1 = stream.spliterator();
    Iterator<T> it1 = Spliterators.iterator(split1);
    Iterator<ObjIntPair<T>> it = new Iterator<ObjIntPair<T>>() {
      private int index;

      @Override
      public boolean hasNext() {
        return it1.hasNext();
      }

      @Override
      public ObjIntPair<T> next() {
        return ObjIntPair.of(it1.next(), index++);
      }
    };
    Spliterator<ObjIntPair<T>> split = Spliterators.spliterator(it, split1.getExactSizeIfKnown(), split1.characteristics());
    return StreamSupport.stream(split, false);
  }

  /**
   * Creates a stream that combines two other streams, continuing until either stream ends.
   * <p>
   * Each pair of input objects is combined into a {@link Pair}.
   *
   * @param <A>  the type of the first stream
   * @param <B>  the type of the second stream
   * @param stream1  the first stream
   * @param stream2  the first stream
   * @return a stream of pairs, one from each stream
   */
  public static <A, B> Stream<Pair<A, B>> zip(Stream<A> stream1, Stream<B> stream2) {
    return zip(stream1, stream2, (a, b) -> Pair.of(a, b));
  }

  /**
   * Creates a stream that combines two other streams, continuing until either stream ends.
   * <p>
   * The combiner function is called once for each pair of objects found in the input streams.
   *
   * @param <A>  the type of the first stream
   * @param <B>  the type of the second stream
   * @param <R>  the type of the resulting stream
   * @param stream1  the first stream
   * @param stream2  the first stream
   * @param zipper  the function used to combine the pair of objects
   * @return a stream of pairs, one from each stream
   */
  private static <A, B, R> Stream<R> zip(Stream<A> stream1, Stream<B> stream2, BiFunction<A, B, R> zipper) {
    // this is private for now, to see if it is really needed on the API
    // it suffers from generics problems at the call site with common zipper functions
    // as such, it is less useful than it might seem
    Spliterator<A> split1 = stream1.spliterator();
    Spliterator<B> split2 = stream2.spliterator();
    // merged stream lacks some characteristics
    int characteristics = split1.characteristics() & split2.characteristics() &
        ~(Spliterator.DISTINCT | Spliterator.SORTED);
    long size = Math.min(split1.getExactSizeIfKnown(), split2.getExactSizeIfKnown());

    Iterator<A> it1 = Spliterators.iterator(split1);
    Iterator<B> it2 = Spliterators.iterator(split2);
    Iterator<R> it = new Iterator<R>() {
      @Override
      public boolean hasNext() {
        return it1.hasNext() && it2.hasNext();
      }

      @Override
      public R next() {
        return zipper.apply(it1.next(), it2.next());
      }
    };
    Spliterator<R> split = Spliterators.spliterator(it, size, characteristics);
    return StreamSupport.stream(split, false);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a predicate that negates the original.
   * <p>
   * The JDK provides {@link Predicate#negate()} however this requires a predicate.
   * Sometimes, it can be useful to have a static method to achieve this.
   * <pre>
   *  stream.filter(not(String::isEmpty))
   * </pre>
   * 
   * @param <R>  the type of the object the predicate works on
   * @param predicate  the predicate to negate
   * @return the negated predicate
   */
  public static <R> Predicate<R> not(Predicate<R> predicate) {
    return predicate.negate();
  }

  //-------------------------------------------------------------------------
  /**
   * Reducer used in a stream to ensure there is no more than one matching element.
   * <p>
   * This method returns an operator that can be used with {@link Stream#reduce(BinaryOperator)}
   * that returns either zero or one elements from the stream. Unlike {@link Stream#findFirst()}
   * or {@link Stream#findAny()}, this approach ensures an exception is thrown if there
   * is more than one element in the stream.
   * <p>
   * This would be used as follows:
   * <pre>
   *   stream.filter(...).reduce(Guavate.ensureOnlyOne()).get();
   * </pre>
   * 
   * @param <T>  the type of element in the stream
   * @return the operator
   */
  public static <T> BinaryOperator<T> ensureOnlyOne() {
    return (a, b) -> {
      throw new IllegalArgumentException(Messages.format(
          "Multiple values found where only one was expected: {} and {}", a, b));
    };
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
   * @throws IllegalArgumentException if the same key is generated twice
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
   * @throws IllegalArgumentException if the same key is generated twice
   */
  public static <T, K, V> Collector<T, ?, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {

    return Collector.of(
        ImmutableMap.Builder<K, V>::new,
        (builder, val) -> builder.put(keyExtractor.apply(val), valueExtractor.apply(val)),
        (l, r) -> l.putAll(r.build()),
        ImmutableMap.Builder<K, V>::build,
        Collector.Characteristics.UNORDERED);
  }

  /**
   * Collector used at the end of a stream to build an immutable map.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableMap}.
   * <p>
   * This returns a map by converting each stream element to a key and value.
   * If the same key is generated more than once the merge function is applied to the
   * values and the return value of the function is used as the value in the map.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result map
   * @param <V> the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @param mergeFn  function to merge values with the same key
   * @return the immutable map collector
   */
  public static <T, K, V> Collector<T, Map<K, V>, ImmutableMap<K, V>> toImmutableMap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor,
      BiFunction<? super V, ? super V, ? extends V> mergeFn) {

    return Collector.of(
        HashMap<K, V>::new,
        (map, val) -> map.merge(keyExtractor.apply(val), valueExtractor.apply(val), mergeFn),
        (m1, m2) -> mergeMaps(m1, m2, mergeFn),
        map -> ImmutableMap.copyOf(map),
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
   * @throws IllegalArgumentException if the same key is generated twice
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
   * @throws IllegalArgumentException if the same key is generated twice
   */
  public static <T, K extends Comparable<?>, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
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
  public static <T, K, V> Collector<T, ?, ImmutableListMultimap<K, V>> toImmutableListMultimap(
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
  public static <T, K, V> Collector<T, ?, ImmutableSetMultimap<K, V>> toImmutableSetMultimap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor) {

    return Collector.of(
        ImmutableSetMultimap.Builder<K, V>::new,
        (builder, val) -> builder.put(keyExtractor.apply(val), valueExtractor.apply(val)),
        (l, r) -> l.putAll(r.build()),
        ImmutableSetMultimap.Builder<K, V>::build,
        Collector.Characteristics.UNORDERED);
  }

  /**
   * Collector used at the end of a stream to build an immutable map
   * from a stream containing map entries. This is a common case if a map's
   * {@code entrySet} has undergone a {@code filter} operation. For example:
   * <pre>
   *   {@code
   *       Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4);
   *       ImmutableMap<String, Integer> output =
   *         input.entrySet()
   *           .stream()
   *           .filter(e -> e.getValue() % 2 == 1)
   *           .collect(entriesToImmutableMap());
   *
   *       // Produces map with "a" -> 1, "c" -> 3, "e" -> 5
   *   }
   * </pre>
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableMap}.
   * <p>
   * This returns a map by converting each {@code Map.Entry} to a key and value.
   * The input stream must resolve to unique keys.
   *
   * @param <K> the type of the keys in the result map
   * @param <V> the type of the values in the result map
   * @return the immutable map collector
   * @throws IllegalArgumentException if the same key is generated twice
   */
  public static <K, V> Collector<Map.Entry<K, V>, ?, ImmutableMap<K, V>> entriesToImmutableMap() {
    return toImmutableMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  /**
   * Collector used at the end of a stream to build an immutable map
   * from a stream containing pairs. This is a common case if a map's
   * {@code entrySet} has undergone a {@code map} operation with the
   * {@code Map.Entry} converted to a {@code Pair}. For example:
   * <pre>
   *   {@code
   *       Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4);
   *       ImmutableMap<String, Double> output =
   *         input.entrySet()
   *           .stream()
   *           .map(e -> Pair.of(e.getKey().toUpperCase(), Math.pow(e.getValue(), 2)))
   *           .collect(pairsToImmutableMap());
   *
   *       // Produces map with "A" -> 1.0, "B" -> 4.0, "C" -> 9.0, "D" -> 16.0
   *   }
   * </pre>
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableMap}.
   * <p>
   * This returns a map by converting each stream element to a key and value.
   * The input stream must resolve to unique keys.
   *
   * @param <K> the type of the keys in the result map
   * @param <V> the type of the values in the result map
   * @return the immutable map collector
   * @throws IllegalArgumentException if the same key is generated twice
   */
  public static <K, V> Collector<Pair<K, V>, ?, ImmutableMap<K, V>> pairsToImmutableMap() {
    return toImmutableMap(Pair::getFirst, Pair::getSecond);
  }

  //--------------------------------------------------------------------------------------------------

  /**
   * Helper method to merge two mutable maps by inserting all values from {@code map2} into {@code map1}.
   * <p>
   * If {@code map1} already contains a mapping for a key the merge function is applied to the existing value and
   * the new value, and the return value is inserted.
   *
   * @param map1  the map into which values are copied
   * @param map2  the map from which values are copied
   * @param mergeFn  function applied to the existing and new values if the map contains the key
   * @param <K>  the key type
   * @param <V>  the value type
   * @return {@code map1} with the values from {@code map2} inserted
   */
  private static <K, V> Map<K, V> mergeMaps(
      Map<K, V> map1,
      Map<K, V> map2,
      BiFunction<? super V, ? super V, ? extends V> mergeFn) {

    for (Map.Entry<K, V> entry : map2.entrySet()) {
      V existingValue = map1.get(entry.getKey());

      if (existingValue == null) {
        map1.put(entry.getKey(), entry.getValue());
      } else {
        map1.put(entry.getKey(), mergeFn.apply(existingValue, entry.getValue()));
      }
    }
    return map1;
  }
}
