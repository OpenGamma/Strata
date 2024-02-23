/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static java.util.stream.Collectors.collectingAndThen;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MoreCollectors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.opengamma.strata.collect.function.CheckedSupplier;
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
   * Concatenates a number of iterables into a single list.
   * <p>
   * This is harder than it should be, a method {@code Stream.of(Iterable)}
   * would have been appropriate, but cannot be added now.
   *
   * @param <T>  the type of element in the iterable
   * @param iterables  the iterables to combine
   * @return the list that combines the inputs
   */
  @SafeVarargs
  public static <T> ImmutableList<T> concatToList(Iterable<? extends T>... iterables) {
    return ImmutableList.copyOf(Iterables.concat(iterables));
  }

  /**
   * Concatenates a number of items onto a single base list.
   * <p>
   * This returns a new list, the input is unaltered.
   *
   * @param <T>  the type of element in the iterable
   * @param baseList  the base list
   * @param additionalItems  the additional items
   * @return the list that combines the inputs
   */
  @SafeVarargs
  public static <T> ImmutableList<T> concatItemsToList(Iterable<? extends T> baseList, T... additionalItems) {
    // this cannot be named concatToList() as it would be ambiguous for callers
    return ImmutableList.<T>builder()
        .addAll(baseList)
        .addAll(ImmutableList.copyOf(additionalItems))
        .build();
  }

  /**
   * Concatenates a number of iterables into a single set.
   * <p>
   * This is harder than it should be, a method {@code Stream.of(Iterable)}
   * would have been appropriate, but cannot be added now.
   *
   * @param <T>  the type of element in the iterable
   * @param iterables  the iterables to combine
   * @return the set that combines the inputs
   */
  @SafeVarargs
  public static <T> ImmutableSet<T> concatToSet(Iterable<? extends T>... iterables) {
    return ImmutableSet.copyOf(Iterables.concat(iterables));
  }

  //-------------------------------------------------------------------------
  /**
   * Combines two distinct maps into a single map, throwing an exception for duplicate keys.
   *
   * @param first  the first map
   * @param second  the second map
   * @param <K>  the type of the keys
   * @param <V>  the type of the values
   * @return a combined map
   * @throws IllegalArgumentException if the same key is encountered in both maps
   */
  public static <K, V> ImmutableMap<K, V> combineMaps(
      Map<? extends K, ? extends V> first,
      Map<? extends K, ? extends V> second) {

    return Stream.concat(first.entrySet().stream(), second.entrySet().stream())
        .collect(entriesToImmutableMap());
  }

  /**
   * Combines two distinct maps into a single map, choosing the key from the second map in case of duplicates.
   *
   * @param first  the first map
   * @param second  the second map
   * @param <K>  the type of the keys
   * @param <V>  the type of the values
   * @return a combined map
   */
  public static <K, V> ImmutableMap<K, V> combineMapsOverwriting(
      Map<? extends K, ? extends V> first,
      Map<? extends K, ? extends V> second) {

    return Stream.concat(first.entrySet().stream(), second.entrySet().stream())
        .collect(entriesToImmutableMap((a, b) -> b));
  }

  /**
   * Combines two maps into a single map.
   * <p>
   * If the maps have shared keys then the merge function is used on the two values
   * and the result is placed in the resulting map.
   *
   * @param first  the first map
   * @param second  the second map
   * @param mergeFn  the function used to merge values
   * @param <K>  the type of the keys
   * @param <V>  the type of the values
   * @return a combined map
   */
  public static <K, V> ImmutableMap<K, V> combineMaps(
      Map<? extends K, ? extends V> first,
      Map<? extends K, ? extends V> second,
      BiFunction<? super V, ? super V, ? extends V> mergeFn) {

    return Stream.concat(first.entrySet().stream(), second.entrySet().stream())
        .collect(entriesToImmutableMap(mergeFn));
  }

  //-------------------------------------------------------------------------
  /**
   * Combines a map with new entries, choosing the last entry if there is a duplicate key.
   *
   * @param baseMap  the base map
   * @param additionalEntries  the additional entries
   * @param <K>  the type of the keys
   * @param <V>  the type of the values
   * @return the combined map
   */
  @SafeVarargs
  public static <K, V> ImmutableMap<K, V> combineMapsOverwriting(
      Map<? extends K, ? extends V> baseMap,
      Entry<? extends K, ? extends V>... additionalEntries) {

    return Stream.concat(baseMap.entrySet().stream(), Stream.of(additionalEntries))
        .collect(entriesToImmutableMap((a, b) -> b));
  }

  //-------------------------------------------------------------------------
  /**
   * Wraps a try-catch block around an expression, avoiding exceptions.
   * <p>
   * This converts an exception throwing method into a method that returns an optional by discarding the exception.
   * In most cases it is better to write a `findXxx()` or 'tryXxx()' method to the code you want to call.
   * <p>
   * To handle checked exceptions, use {@link Unchecked#supplier(CheckedSupplier)}.
   * 
   * @param <T>  the type of the result in the optional
   * @param resultSupplier  the supplier that might throw an exception
   * @return the value wrapped in an optional, empty if the method returns null or an exception is thrown
   */
  public static <T> Optional<T> tryCatchToOptional(Supplier<T> resultSupplier) {
    try {
      return Optional.ofNullable(resultSupplier.get());
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }

  /**
   * Wraps a try-catch block around an expression, avoiding exceptions.
   * <p>
   * This variant allows the exception to be observed, such as for logging.
   * <p>
   * This converts an exception throwing method into a method that returns an optional by discarding the exception.
   * In most cases it is better to write a `findXxx()` or 'tryXxx()' method to the code you want to call.
   * <p>
   * To handle checked exceptions, use {@link Unchecked#supplier(CheckedSupplier)}.
   * 
   * @param <T>  the type of the result in the optional
   * @param resultSupplier  the supplier that might throw an exception
   * @param exceptionHandler  the exception handler
   * @return the value wrapped in an optional, empty if the method returns null or an exception is thrown
   */
  public static <T> Optional<T> tryCatchToOptional(Supplier<T> resultSupplier, Consumer<RuntimeException> exceptionHandler) {
    try {
      return Optional.ofNullable(resultSupplier.get());
    } catch (RuntimeException ex) {
      exceptionHandler.accept(ex);
      return Optional.empty();
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Uses a number of suppliers to create a single optional result.
   * <p>
   * This invokes each supplier in turn until a non empty optional is returned.
   * As such, not all suppliers are necessarily invoked.
   * <p>
   * The Java 8 {@link Optional} class does not have an {@code or} method,
   * so this provides an alternative.
   * 
   * @param <T>  the type of element in the optional
   * @param suppliers  the suppliers to combine
   * @return the first non empty optional
   */
  @SuppressWarnings("unchecked")
  @SafeVarargs
  public static <T> Optional<T> firstNonEmpty(Supplier<Optional<? extends T>>... suppliers) {
    for (Supplier<Optional<? extends T>> supplier : suppliers) {
      Optional<? extends T> result = supplier.get();
      if (result.isPresent()) {
        // safe, because Optional is read-only
        return (Optional<T>) result;
      }
    }
    return Optional.empty();
  }

  /**
   * Chooses the first optional that is not empty.
   * <p>
   * The Java 8 {@link Optional} class does not have an {@code or} method,
   * so this provides an alternative.
   * 
   * @param <T>  the type of element in the optional
   * @param optionals  the optionals to combine
   * @return the first non empty optional
   */
  @SuppressWarnings("unchecked")
  @SafeVarargs
  public static <T> Optional<T> firstNonEmpty(Optional<? extends T>... optionals) {
    for (Optional<? extends T> optional : optionals) {
      if (optional.isPresent()) {
        // safe, because Optional is read-only
        return (Optional<T>) optional;
      }
    }
    return Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the first value from the iterable, returning empty if the iterable is empty.
   * 
   * @param <T>  the type of element in the optional
   * @param iterable  the iterable to query
   * @return the first value, empty if empty
   */
  public static <T> Optional<T> first(Iterable<T> iterable) {
    Iterator<T> it = iterable.iterator();
    return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a single {@code Map.Entry}.
   * <p>
   * The entry is immutable.
   * 
   * @param <K>  the type of the key
   * @param <V>  the type of the value
   * @param key  the key
   * @param value  the value
   * @return the map entry
   */
  public static <K, V> Map.Entry<K, V> entry(K key, V value) {
    return new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a list from the first element and remaining varargs.
   * <p>
   * This can be used to create a list of at least size one.
   * 
   * @param <T>  the type of the elements
   * @param first  the first element
   * @param remaining  the remaining elements
   * @return a list formed from the first and remaining elements
   */
  @SafeVarargs
  public static <T> ImmutableList<T> list(T first, T... remaining) {
    ImmutableList.Builder<T> builder = ImmutableList.<T>builderWithExpectedSize(remaining.length + 1);
    builder.add(first);
    for (T element : remaining) {
      builder.add(element);
    }
    return builder.build();
  }

  /**
   * Converts a set from the first element and remaining varargs.
   * <p>
   * This can be used to create a set of at least size one.
   * The input may contain duplicates, which will be combined.
   * 
   * @param <T>  the type of the elements
   * @param first  the first element
   * @param remaining  the remaining elements
   * @return a set formed from the first and remaining elements
   */
  @SafeVarargs
  public static <T> ImmutableSet<T> set(T first, T... remaining) {
    ImmutableSet.Builder<T> builder = ImmutableSet.<T>builderWithExpectedSize(remaining.length + 1);
    builder.add(first);
    for (T element : remaining) {
      builder.add(element);
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Boxes an {@code OptionalInt}.
   * <p>
   * {@code OptionalInt} has almost no useful methods and no easy way to convert it to an {@code Optional}.
   * This method provides the conversion to {@code Optional<Integer>}.
   * 
   * @param optional the {@code OptionalInt}
   * @return an equivalent optional
   */
  public static Optional<Integer> boxed(OptionalInt optional) {
    return optional.isPresent() ? Optional.of(optional.getAsInt()) : Optional.empty();
  }

  /**
   * Boxes an {@code OptionalLong}.
   * <p>
   * {@code OptionalLong} has almost no useful methods and no easy way to convert it to an {@code Optional}.
   * This method provides the conversion to {@code Optional<Long>}.
   * 
   * @param optional the {@code OptionalLong}
   * @return an equivalent optional
   */
  public static Optional<Long> boxed(OptionalLong optional) {
    return optional.isPresent() ? Optional.of(optional.getAsLong()) : Optional.empty();
  }

  /**
   * Boxes an {@code OptionalDouble}.
   * <p>
   * {@code OptionalDouble} has almost no useful methods and no easy way to convert it to an {@code Optional}.
   * This method provides the conversion to {@code Optional<Double>}.
   * 
   * @param optional the {@code OptionalDouble}
   * @return an equivalent optional
   */
  public static Optional<Double> boxed(OptionalDouble optional) {
    return optional.isPresent() ? Optional.of(optional.getAsDouble()) : Optional.empty();
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
   * Converts a stream to an iterable for use in the for-each statement.
   * <p>
   * For some use cases this approach is nicer than {@link Stream#forEach(Consumer)}.
   * Notably code that mutates a local variable or has to handle checked exceptions will benefit.
   * <p>
   * <pre>
   *  for (Item item : in(stream)) {
   *    // lazily use each item in the stream
   *  }
   * </pre>
   * <p>
   * NOTE: The result of this method can only be iterated once, which does not
   * meet the expected specification of {@code Iterable}.
   * Use in the for-each statement is safe as it will only be called once.
   *
   * @param <T>  the type of stream element
   * @param stream  the stream
   * @return an iterable representation of the stream that can only be invoked once
   */
  public static <T> Iterable<T> in(Stream<T> stream) {
    return stream::iterator;
  }

  /**
   * Converts an optional to an iterable for use in the for-each statement.
   * <p>
   * For some use cases this approach is nicer than {@link Optional#isPresent()}
   * followed by {@link Optional#get()}.
   * <p>
   * <pre>
   *  for (Item item : inOptional(optional)) {
   *    // use the optional value, code not called if the optional is empty
   *  }
   * </pre>
   * <p>
   * NOTE: This method is intended only for use with the for-each statement.
   * It does in fact return a general purpose {@code Iterable}, but the method name
   * is focussed on the for-each use case.
   *
   * @param <T>  the type of optional element
   * @param optional  the optional
   * @return an iterable representation of the optional
   */
  public static <T> Iterable<T> inOptional(Optional<T> optional) {
    if (optional.isPresent()) {
      return ImmutableList.of(optional.get());
    } else {
      return ImmutableList.of();
    }
  }

  /**
   * Converts a nullable value to an iterable for use in the for-each statement.
   * <p>
   * In most cases {@code if (nullable != null)} would be preferred.
   * <p>
   * <pre>
   *  for (Item item : inNullable(nullable)) {
   *    // use the nullable value, code not called if the value is null
   *  }
   * </pre>
   * <p>
   * NOTE: This method is intended only for use with the for-each statement.
   * It does in fact return a general purpose {@code Iterable}, but the method name
   * is focussed on the for-each use case.
   *
   * @param <T>  the type of the nullable value
   * @param nullable  the nullable value
   * @return an iterable representation of the nullable
   */
  public static <T> Iterable<T> inNullable(T nullable) {
    if (nullable != null) {
      return ImmutableList.of(nullable);
    } else {
      return ImmutableList.of();
    }
  }

  /**
   * Converts an expression for use in the for-each statement wrapping it with a try-catch block to avoid exceptions.
   * <p>
   * This allows a method that throws an exception to be used with the for-each statement.
   * In most cases it is better to write a `findXxx()` or 'tryXxx()' method to the code you want to call.
   * The typical use case is for parsing, where the parser throws an exception if invalid.
   * <p>
   * This method is intended for use with the for-each statement:
   * <p>
   * <pre>
   *  for (Item item : inTryCatchIgnore(() -> parse(str))) {
   *    // use the value, code not called if parse(str) threw an exception
   *  }
   * </pre>
   * <p>
   * NOTE: This method is intended only for use with the for-each statement.
   * It does in fact return a general purpose {@code Iterable}, but the method name
   * is focussed on the for-each use case.
   * 
   * @param <T>  the type of the result in the optional
   * @param supplier  the supplier that might throw an exception
   * @return the value wrapped in an optional, empty if the method returns null or an exception is thrown
   */
  public static <T> Iterable<T> inTryCatchIgnore(Supplier<T> supplier) {
    try {
      return inNullable(supplier.get());
    } catch (RuntimeException ex) {
      return ImmutableList.of();
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a stream that wraps a stream with the index.
   * <p>
   * Each input object is decorated with an {@link ObjIntPair}.
   * The {@code int} is the index of the element in the stream.
   * <p>
   * See also {@link MapStream#zipWithIndex(Stream)}.
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
    Spliterator<ObjIntPair<T>> split =
        Spliterators.spliterator(it, split1.getExactSizeIfKnown(), split1.characteristics());
    return StreamSupport.stream(split, false);
  }

  /**
   * Creates a stream that combines two other streams, continuing until either stream ends.
   * <p>
   * Each pair of input objects is combined into a {@link Pair}.
   * <p>
   * See also {@link MapStream#zip(Stream, Stream)}.
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
  static <A, B, R> Stream<R> zip(Stream<A> stream1, Stream<B> stream2, BiFunction<A, B, R> zipper) {
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
   * This would be used as follows (with a static import):
   * <pre>
   *   stream.filter(...).reduce(ensureOnlyOne()).get();
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
   * Function used in a stream to cast instances to a particular type without filtering.
   * <p>
   * This method returns a function that can be used with {@link Stream#map(Function)}
   * to cast elements in a stream to a particular type, throwing an exception if any
   * element is not of the specified type.
   * <p>
   * This would be used as follows (with a static import):
   * <pre>
   *   stream.map(casting(Foo.class));
   * </pre>
   * <p>
   * This replaces code of the form:
   * <pre>
   *   stream.map(Foo.class::cast);
   * </pre>
   *
   * @param <T>  the type of element in the input stream
   * @param <R>  the type of element in the output stream
   * @param cls  the type of element in the output stream
   * @return the function
   */
  public static <T, R extends T> Function<T, R> casting(Class<R> cls) {
    return input -> cls.cast(input);
  }

  /**
   * Function used in a stream to filter instances to a particular type.
   * <p>
   * This method returns a function that can be used with {@link Stream#flatMap(Function)}
   * to filter elements in a stream to a particular type.
   * <p>
   * This would be used as follows (with a static import):
   * <pre>
   *   stream.flatMap(filtering(Foo.class));
   * </pre>
   * <p>
   * This replaces code of the form:
   * <pre>
   *   stream.filter(Foo.class::isInstance).map(Foo.class::cast);
   * </pre>
   *
   * @param <T>  the type of element in the input stream
   * @param <R>  the type of element in the output stream
   * @param cls  the type of element in the output stream
   * @return the function
   */
  public static <T, R extends T> Function<T, Stream<R>> filtering(Class<R> cls) {
    return input -> cls.isInstance(input) ? Stream.of(cls.cast(input)) : Stream.empty();
  }

  /**
   * Function used in a stream to filter optionals.
   * <p>
   * This method returns a function that can be used with {@link Stream#flatMap(Function)}
   * to filter optional elements in a stream.
   * The resulting stream only contains the optional elements that are present.
   * <p>
   * This would be used as follows (with a static import):
   * <pre>
   *   stream.flatMap(filteringOptional());
   * </pre>
   * <p>
   * This replaces code of the form:
   * <pre>
   *   stream.filter(Optional::isPresent).map(Optional::get);
   * </pre>
   *
   * @param <T>  the type of element in the output stream
   * @return the function
   */
  public static <T> Function<Optional<T>, Stream<T>> filteringOptional() {
    return opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty();
  }

  //-------------------------------------------------------------------------
  /**
   * Collector used at the end of a stream to extract either zero or one elements.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into an {@link Optional}.
   * The collector throws {@code IllegalArgumentException} if the stream consists of two or more elements.
   * The collector throws {@code NullPointerException} if the stream consists of a null element.
   *
   * @param <T>  the type of element in the list
   * @return the immutable list collector
   */
  public static <T> Collector<T, ?, Optional<T>> toOptional() {
    return MoreCollectors.toOptional();
  }

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
   * Collector used at the end of a stream to build an immutable list of
   * immutable lists of size equal to or less than size.
   * For example, the following list [a, b, c, d, e] with a partition
   * size of 2 will give [[a, b], [c, d], [e]].
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableList} of {@link ImmutableList}.
   *
   * @param size  the size of the partitions of the original list
   * @param <T>  the type of element in the list
   * @return the immutable list of lists collector
   */
  public static <T> Collector<T, ?, ImmutableList<ImmutableList<T>>> splittingBySize(int size) {
    return Collectors.collectingAndThen(
        Collectors.collectingAndThen(
            Guavate.toImmutableList(),
            objects -> Lists.partition(objects, size)),
        Guavate::toImmutables);
  }

  /**
   * Helper method to transform a list of lists into their immutable counterparts.
   *
   * @param lists  the list of lists
   * @param <T>  the type of element in the list
   * @return the immutable lists
   */
  private static <T> ImmutableList<ImmutableList<T>> toImmutables(List<List<T>> lists) {
    return lists.stream()
        .map(ImmutableList::copyOf)
        .collect(Guavate.toImmutableList());
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
   * an {@link ImmutableMap}, retaining insertion order.
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
   * an {@link ImmutableMap}, retaining insertion order.
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
   * an {@link ImmutableMap}, retaining insertion order.
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
        LinkedHashMap<K, V>::new,
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

  /**
   * Collector used at the end of a stream to build an immutable sorted map.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableSortedMap}.
   * <p>
   * This returns a map by converting each stream element to a key and value.
   * See {@link Collectors#toMap(Function, Function)} for more details.
   *
   * @param <T> the type of the stream elements
   * @param <K> the type of the keys in the result map
   * @param <V> the type of the values in the result map
   * @param keyExtractor  function to produce keys from stream elements
   * @param valueExtractor  function to produce values from stream elements
   * @param mergeFn  function to merge values with the same key
   * @return the immutable sorted map collector
   */
  public static <T, K extends Comparable<?>, V> Collector<T, ?, ImmutableSortedMap<K, V>> toImmutableSortedMap(
      Function<? super T, ? extends K> keyExtractor,
      Function<? super T, ? extends V> valueExtractor,
      BiFunction<? super V, ? super V, ? extends V> mergeFn) {

    return Collector.of(
        TreeMap<K, V>::new,
        (map, val) -> map.merge(keyExtractor.apply(val), valueExtractor.apply(val), mergeFn),
        (m1, m2) -> mergeMaps(m1, m2, mergeFn),
        map -> ImmutableSortedMap.copyOfSorted(map),
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
   *       Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
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
  public static <K, V> Collector<Map.Entry<? extends K, ? extends V>, ?, ImmutableMap<K, V>> entriesToImmutableMap() {
    return toImmutableMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  /**
   * Collector used at the end of a stream to build an immutable map
   * from a stream containing map entries which could have duplicate keys.
   * <p>
   * This is a common case if a map's {@code entrySet} has undergone a {@code map} operation. For example:
   * <pre>
   *   {@code
   *       Map<Integer, String> input = ImmutableMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
   *       ImmutableMap<String, Integer> output =
   *         input.entrySet()
   *           .stream()
   *           .map(e -> Guavate.entry(e.getKey() % 2, e.getValue()))
   *           .collect(entriesToImmutableMap(String::concat));
   *
   *       // Produces map with 0 -> "bd", 1 -> "ace"
   *   }
   * </pre>
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing streams to be gathered into
   * an {@link ImmutableMap}.
   * <p>
   * This returns a map by converting each {@code Map.Entry} to a key and value.
   *
   * @param mergeFn function to merge values with the same key
   * @param <K> the type of the keys in the result map
   * @param <V> the type of the values in the result map
   * @return the immutable map collector
   */
  public static <K, V> Collector<Map.Entry<? extends K, ? extends V>, ?, ImmutableMap<K, V>> entriesToImmutableMap(
      BiFunction<? super V, ? super V, ? extends V> mergeFn) {

    return toImmutableMap(Map.Entry::getKey, Map.Entry::getValue, mergeFn);
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
  public static <K, V> Collector<Pair<? extends K, ? extends V>, ?, ImmutableMap<K, V>> pairsToImmutableMap() {
    return toImmutableMap(Pair::getFirst, Pair::getSecond);
  }

  //-------------------------------------------------------------------------
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
   * @param <M>  the type of the first map
   * @return {@code map1} with the values from {@code map2} inserted
   */
  private static <K, V, M extends Map<K, V>> M mergeMaps(
      M map1,
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

  //-------------------------------------------------------------------------
  /**
   * Converts a list of futures to a single future, combining the values into a list.
   * <p>
   * The {@link CompletableFuture#allOf(CompletableFuture...)} method is useful
   * but it returns {@code Void}. This method combines the futures but also
   * returns the resulting value as a list.
   * Effectively, this converts {@code List<CompletableFuture<T>>} to {@code CompletableFuture<List<T>>}.
   * <p>
   * If any input future completes exceptionally, the result will also complete exceptionally.
   *
   * @param <T> the type of the values in the list
   * @param futures the futures to convert, may be empty
   * @return a future that combines the input futures as a list
   */
  public static <T> CompletableFuture<List<T>> combineFuturesAsList(
      List<? extends CompletableFuture<? extends T>> futures) {

    int size = futures.size();
    CompletableFuture<? extends T>[] futuresArray = futures.toArray(new CompletableFuture[size]);
    return CompletableFuture.allOf(futuresArray)
        .thenApply(unused -> {
          List<T> builder = new ArrayList<>(size);
          for (int i = 0; i < size; i++) {
            builder.add(futuresArray[i].join());
          }
          return builder;
        });
  }

  /**
   * Collector used at the end of a stream to convert a list of futures to a single future,
   * combining the values into a list.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing a stream of futures to be combined into a single future.
   * This converts {@code List<CompletableFuture<T>>} to {@code CompletableFuture<List<T>>}.
   *
   * @param <S> the type of the input futures
   * @param <T> the type of the values
   * @return a collector that combines the input futures as a list
   */
  public static <T, S extends CompletableFuture<? extends T>>
      Collector<S, ?, CompletableFuture<List<T>>> toCombinedFuture() {

    return collectingAndThen(toImmutableList(), Guavate::combineFuturesAsList);
  }

  //-------------------------------------------------------------------------
  /**
   * Converts a map of futures to a single future.
   * <p>
   * This is similar to {@link #combineFuturesAsList(List)} but for maps.
   * Effectively, this converts {@code Map<K, CompletableFuture<V>>} to {@code CompletableFuture<Map<K, V>>}.
   * <p>
   * If any input future completes exceptionally, the result will also complete exceptionally.
   * The results must be non-null.
   *
   * @param <K> the type of the keys in the map
   * @param <V> the type of the values in the map
   * @param <F> the type of the futures, must not be Void
   * @param futures the futures to convert, may be empty
   * @return a future that combines the input futures as a map
   */
  @SuppressWarnings("unchecked")
  public static <K, V, F extends CompletableFuture<? extends V>> CompletableFuture<Map<K, V>>
      combineFuturesAsMap(Map<? extends K, ? extends F> futures) {

    int size = futures.size();
    K[] keyArray = (K[]) new Object[size];
    CompletableFuture<? extends V>[] futuresArray = new CompletableFuture[size];
    int index = 0;
    for (Entry<? extends K, ? extends F> entry : futures.entrySet()) {
      keyArray[index] = entry.getKey();
      futuresArray[index] = entry.getValue();
      index++;
    }
    return CompletableFuture.allOf(futuresArray)
        .thenApply(unused -> {
          ImmutableMap.Builder<K, V> builder = ImmutableMap.builderWithExpectedSize(size);
          for (int i = 0; i < size; i++) {
            builder.put(keyArray[i], futuresArray[i].join());
          }
          return builder.build();
        });
  }

  /**
   * Collector used at the end of a stream to convert a map of futures to a single future,
   * combining the values into a map.
   * <p>
   * A collector is used to gather data at the end of a stream operation.
   * This method returns a collector allowing a stream of futures to be combined into a single future.
   * This converts {@code Map<K, CompletableFuture<V>>} to {@code CompletableFuture<Map<K, V>>}.
   *
   * @param <K> the type of the keys in the map
   * @param <V> the type of the values in the map
   * @param <F> the type of the input futures
   * @return a collector that combines the input futures as a map
   */
  public static <K, V, F extends CompletableFuture<? extends V>>
      Collector<Map.Entry<? extends K, ? extends F>, ?, CompletableFuture<Map<K, V>>> toCombinedFutureMap() {

    return collectingAndThen(entriesToImmutableMap(), Guavate::combineFuturesAsMap);
  }

  //-------------------------------------------------------------------------
  /**
   * Polls on a regular frequency until a result is found.
   * <p>
   * Polling is performed via the specified supplier, which must return null until the result is available.
   * If the supplier throws an exception, polling will stop and the future will complete exceptionally.
   * <p>
   * If the future is cancelled, the polling will also be cancelled.
   * It is advisable to consider using a timeout when querying the future.
   * <p>
   * In most cases, there needs to be an initial request, which might return an identifier to query.
   * This pattern may be useful for that case:
   * <pre>
   *  return CompletableFuture.supplyAsync(initPollingReturningId(), executorService)
   *      .thenCompose(id -&gt; poll(executorService, delay, freq, performPolling(id)));
   *  });
   * </pre>
   *
   * @param <T> the result type
   * @param executorService  the executor service to use for polling
   * @param initialDelay  the initial delay before starting to poll
   * @param frequency  the frequency to poll at
   * @param pollingTask  the task used to poll, returning null when not yet complete
   * @return the future representing the asynchronous operation
   */
  public static <T> CompletableFuture<T> poll(
      ScheduledExecutorService executorService,
      Duration initialDelay,
      Duration frequency,
      Supplier<T> pollingTask) {

    CompletableFuture<T> result = new CompletableFuture<>();
    Runnable decoratedPollingTask = () -> pollTask(pollingTask, result);
    ScheduledFuture<?> scheduledTask = executorService.scheduleAtFixedRate(
        decoratedPollingTask, initialDelay.toMillis(), frequency.toMillis(), TimeUnit.MILLISECONDS);
    return result.whenComplete((r, ex) -> scheduledTask.cancel(true));
  }

  // the task the executor calls
  private static <T> void pollTask(
      Supplier<T> pollingTask,
      CompletableFuture<T> resultFuture) {

    try {
      T result = pollingTask.get();
      if (result != null) {
        resultFuture.complete(result);
      }
    } catch (RuntimeException ex) {
      resultFuture.completeExceptionally(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a ThreadFactoryBuilder which names new threads with the name of the calling class plus a unique integer.
   *
   * @return the thread factory builder
   */
  public static ThreadFactoryBuilder namedThreadFactory() {
    return namedThreadFactory(callerClass(3).getSimpleName());
  }

  /**
   * Creates a ThreadFactoryBuilder which names new threads with the given name prefix plus a unique integer.
   *
   * @param threadNamePrefix  the name which new thread names should be prefixed by
   * @return the thread factory builder
   */
  public static ThreadFactoryBuilder namedThreadFactory(String threadNamePrefix) {
    return new ThreadFactoryBuilder().setNameFormat(threadNamePrefix + "-%d");
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a generified {@code Class} instance.
   * <p>
   * It is not possible in Java generics to get a {@code Class} object with the desired generic
   * signature, such as {@code Class<List<String>>}. This method provides a way to get such a value.
   * The method returns the input parameter, but the compiler sees the result as being a different type.
   * <p>
   * Note that the generic part of the resulting type is not checked and can be unsound.
   * The safest choice is to explicitly specify the type you want, by assigning to a variable or constant:
   * <pre>
   *   Class&lt;List&lt;String&gt;&gt; cls = genericClass(List.class);
   * </pre>
   *
   * @param <T> the partially specified generic type, such as {@code List} from a constant such as {@code List.class}
   * @param <S> the fully specified generic type, such as {@code List<String>}
   * @param cls  the class instance to base the result in, such as {@code List.class}
   * @return the class instance from the input, with whatever generic parameter is desired
   */
  @SuppressWarnings("unchecked")
  public static <T, S extends T> Class<S> genericClass(Class<T> cls) {
    return (Class<S>) cls;
  }

  /**
   * Finds the caller class.
   * <p>
   * This takes an argument which is the number of stack levels to look back.
   * This will be 2 to return the caller of this method, 3 to return the caller of the caller, and so on.
   *
   * @param callStackDepth  the depth of the stack to look back
   * @return the caller class
   */
  public static Class<?> callerClass(int callStackDepth) {
    return CallerClassSecurityManager.INSTANCE.callerClass(callStackDepth);
  }

  // on Java 9 or later could use StackWalker, but this is a good choice for Java 8
  static class CallerClassSecurityManager extends SecurityManager {
    private static final CallerClassSecurityManager INSTANCE = new CallerClassSecurityManager();

    Class<?> callerClass(int callStackDepth) {
      return getClassContext()[callStackDepth];
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the substring before the first occurrence of the separator.
   * <p>
   * This returns the string before the first occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  public static String substringBeforeFirst(String str, String separator) {
    int pos = str.indexOf(separator);
    if (pos < 0) {
      return str;
    }
    return str.substring(0, pos);
  }

  /**
   * Gets the substring after the first occurrence of the separator.
   * <p>
   * This returns the string after the first occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  public static String substringAfterFirst(String str, String separator) {
    int pos = str.indexOf(separator);
    if (pos < 0) {
      return str;
    }
    return str.substring(pos + separator.length());
  }

  /**
   * Gets the substring before the last occurrence of the separator.
   * <p>
   * This returns the string before the last occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  public static String substringBeforeLast(String str, String separator) {
    int pos = str.lastIndexOf(separator);
    if (pos < 0) {
      return str;
    }
    return str.substring(0, pos);
  }

  /**
   * Gets the substring after the last occurrence of the separator.
   * <p>
   * This returns the string after the last occurrence of the separator.
   * If the separator is not found, the input is returned.
   *
   * @param str  the input string to search
   * @param separator  the separator to find
   * @return the substring
   */
  public static String substringAfterLast(String str, String separator) {
    int pos = str.lastIndexOf(separator);
    if (pos < 0) {
      return str;
    }
    return str.substring(pos + separator.length());
  }

}
