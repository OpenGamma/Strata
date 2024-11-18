/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import static com.opengamma.strata.collect.Guavate.entriesToImmutableMap;
import static com.opengamma.strata.collect.Guavate.entry;
import static com.opengamma.strata.collect.Guavate.in;
import static com.opengamma.strata.collect.Guavate.pairsToImmutableMap;
import static com.opengamma.strata.collect.TestHelper.assertUtilityClass;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Ordering;
import com.opengamma.strata.collect.tuple.ObjIntPair;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test Guavate.
 */
public class GuavateTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_concatToList() {
    Iterable<String> iterable1 = Arrays.asList("a", "b", "c");
    Iterable<String> iterable2 = Arrays.asList("d", "e", "f");
    List<String> test = Guavate.concatToList(iterable1, iterable2);
    assertThat(test).isEqualTo(ImmutableList.of("a", "b", "c", "d", "e", "f"));
  }

  @Test
  public void test_concatItemsToListItems() {
    List<String> test = Guavate.concatItemsToList(Arrays.asList("a", "b", "c"), "d", "e", "f");
    assertThat(test).isEqualTo(ImmutableList.of("a", "b", "c", "d", "e", "f"));
  }

  @Test
  public void test_concatToList_differentTypes() {
    Iterable<Integer> iterable1 = Arrays.asList(1, 2, 3);
    Iterable<Double> iterable2 = Arrays.asList(10d, 20d, 30d);
    ImmutableList<Number> test = Guavate.concatToList(iterable1, iterable2);
    assertThat(test).isEqualTo(ImmutableList.of(1, 2, 3, 10d, 20d, 30d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_concatToSet() {
    Iterable<String> iterable1 = Arrays.asList("a", "b", "c");
    Iterable<String> iterable2 = Arrays.asList("d", "e", "f", "a");
    Set<String> test = Guavate.concatToSet(iterable1, iterable2);
    assertThat(test).isEqualTo(ImmutableSet.of("a", "b", "c", "d", "e", "f"));
  }

  @Test
  public void test_concatToSet_differentTypes() {
    Iterable<Integer> iterable1 = Arrays.asList(1, 2, 3, 2);
    Iterable<Double> iterable2 = Arrays.asList(10d, 20d, 30d);
    Set<Number> test = Guavate.concatToSet(iterable1, iterable2);
    assertThat(test).isEqualTo(ImmutableSet.of(1, 2, 3, 10d, 20d, 30d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combineMaps() {
    Map<String, String> map1 = ImmutableMap.of("a", "one", "b", "two");
    Map<String, String> map2 = ImmutableMap.of("c", "three", "d", "four");
    Map<String, String> test = Guavate.combineMaps(map1, map2);
    assertThat(test).isEqualTo(ImmutableMap.of("a", "one", "b", "two", "c", "three", "d", "four"));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Guavate.combineMaps(map1, ImmutableMap.of("a", "xxx")));
  }

  @Test
  public void test_combineMaps_differentTypes() {
    Map<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
    Map<String, Double> map2 = ImmutableMap.of("c", 3d, "d", 4d);
    Map<String, Number> test = Guavate.combineMaps(map1, map2);
    assertThat(test).isEqualTo(ImmutableMap.of("a", 1, "b", 2, "c", 3d, "d", 4d));
  }

  @Test
  public void test_combineMaps_merge() {
    Map<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
    Map<String, Integer> map2 = ImmutableMap.of("a", 5, "c", 3);
    Map<String, Integer> test = Guavate.combineMaps(map1, map2, Integer::sum);
    assertThat(test).isEqualTo(ImmutableMap.of("a", 6, "b", 2, "c", 3));
  }

  @Test
  public void test_combineMaps_mergeDifferentTypes() {
    Map<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
    Map<String, Double> map2 = ImmutableMap.of("a", 5d, "c", 3d);
    Map<String, Number> test = Guavate.combineMaps(
        map1,
        map2,
        (a, b) -> Double.sum(a.doubleValue(), b.doubleValue()));
    assertThat(test).isEqualTo(ImmutableMap.of("a", 6d, "b", 2, "c", 3d));
  }

  @Test
  public void test_combineMapsOverwriting() {
    Map<String, String> map1 = ImmutableMap.of("a", "one", "b", "two");
    Map<String, String> map2 = ImmutableMap.of("a", "xxx", "c", "three", "d", "four");
    Map<String, String> test = Guavate.combineMapsOverwriting(map1, map2);
    assertThat(test).isEqualTo(ImmutableMap.of("a", "xxx", "b", "two", "c", "three", "d", "four"));
  }

  @Test
  public void test_combineMapsOverwriting_entries() {
    Map<String, String> map1 = ImmutableMap.of("a", "one", "b", "two");
    Map<String, String> test = Guavate.combineMapsOverwriting(
        map1, entry("a", "yyy"), entry("c", "three"), entry("a", "xxx"));
    assertThat(test).isEqualTo(ImmutableMap.of("a", "xxx", "b", "two", "c", "three"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_tryCatchToOptional() {
    assertThat(Guavate.tryCatchToOptional(() -> LocalDate.parse("2020-06-01"))).hasValue(LocalDate.of(2020, 6, 1));
    assertThat(Guavate.tryCatchToOptional(() -> null)).isEmpty();
    assertThat(Guavate.tryCatchToOptional(() -> LocalDate.parse("XXX"))).isEmpty();
  }

  @Test
  public void test_tryCatchToOptional_observed() {
    List<RuntimeException> extracted = new ArrayList<>();
    Consumer<RuntimeException> handler = ex -> extracted.add(ex);
    assertThat(Guavate.tryCatchToOptional(() -> LocalDate.parse("2020-06-01"), handler)).hasValue(LocalDate.of(2020, 6, 1));
    assertThat(extracted).isEmpty();
    assertThat(Guavate.tryCatchToOptional(() -> null, handler)).isEmpty();
    assertThat(extracted).isEmpty();
    assertThat(Guavate.tryCatchToOptional(() -> LocalDate.parse("XXX"), handler)).isEmpty();
    assertThat(extracted).singleElement().isInstanceOf(DateTimeParseException.class);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_firstNonEmpty_supplierMatch1() {
    Optional<Number> test = Guavate.firstNonEmpty(
        () -> Optional.of(Integer.valueOf(1)),
        () -> Optional.of(Double.valueOf(2d)));
    assertThat(test).isEqualTo(Optional.of(Integer.valueOf(1)));
  }

  @Test
  public void test_firstNonEmpty_supplierMatch2() {
    Optional<Number> test = Guavate.firstNonEmpty(
        () -> Optional.empty(),
        () -> Optional.of(Double.valueOf(2d)));
    assertThat(test).isEqualTo(Optional.of(Double.valueOf(2d)));
  }

  @Test
  public void test_firstNonEmpty_supplierMatchNone() {
    Optional<Number> test = Guavate.firstNonEmpty(
        () -> Optional.empty(),
        () -> Optional.empty());
    assertThat(test).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_firstNonEmpty_optionalMatch1() {
    Optional<Number> test = Guavate.firstNonEmpty(Optional.of(Integer.valueOf(1)), Optional.of(Double.valueOf(2d)));
    assertThat(test).isEqualTo(Optional.of(Integer.valueOf(1)));
  }

  @Test
  public void test_firstNonEmpty_optionalMatch2() {
    Optional<Number> test = Guavate.firstNonEmpty(Optional.empty(), Optional.of(Double.valueOf(2d)));
    assertThat(test).isEqualTo(Optional.of(Double.valueOf(2d)));
  }

  @Test
  public void test_firstNonEmpty_optionalMatchNone() {
    Optional<Number> test = Guavate.firstNonEmpty(Optional.empty(), Optional.empty());
    assertThat(test).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_first() {
    assertThat(Guavate.first(ImmutableSet.of())).isEmpty();
    assertThat(Guavate.first(ImmutableSet.of("a"))).hasValue("a");
    assertThat(Guavate.first(ImmutableSet.of("a", "b"))).hasValue("a");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_only() {
    assertThat(Guavate.only(ImmutableSet.of())).isEmpty();
    assertThat(Guavate.only(ImmutableSet.of("a"))).hasValue("a");
    assertThat(Guavate.only(ImmutableSet.of("a", "b"))).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_list() {
    assertThat(Guavate.list("a")).isEqualTo(ImmutableList.of("a"));
    assertThat(Guavate.list("a", "b", "c")).isEqualTo(ImmutableList.of("a", "b", "c"));
  }

  @Test
  public void test_set() {
    assertThat(Guavate.set("a")).isEqualTo(ImmutableSet.of("a"));
    assertThat(Guavate.set("a", "b", "c")).isEqualTo(ImmutableSet.of("a", "b", "c"));
    assertThat(Guavate.set("a", "b", "b")).isEqualTo(ImmutableSet.of("a", "b"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_boxed() {
    assertThat(Guavate.boxed(OptionalInt.of(1))).isEqualTo(Optional.of(1));
    assertThat(Guavate.boxed(OptionalInt.empty())).isEqualTo(Optional.empty());
    assertThat(Guavate.boxed(OptionalLong.of(1L))).isEqualTo(Optional.of(1L));
    assertThat(Guavate.boxed(OptionalLong.empty())).isEqualTo(Optional.empty());
    assertThat(Guavate.boxed(OptionalDouble.of(1d))).isEqualTo(Optional.of(1d));
    assertThat(Guavate.boxed(OptionalDouble.empty())).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_stream_Iterable() {
    Iterable<String> iterable = Arrays.asList("a", "b", "c");
    List<String> test = Guavate.stream(iterable)
        .collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of("a", "b", "c"));
  }

  @Test
  public void test_stream_Optional() {
    Optional<String> optional = Optional.of("foo");
    List<String> test1 = Guavate.stream(optional).collect(Collectors.toList());
    assertThat(test1).isEqualTo(ImmutableList.of("foo"));

    Optional<String> empty = Optional.empty();
    List<String> test2 = Guavate.stream(empty).collect(Collectors.toList());
    assertThat(test2).isEqualTo(ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_in_Stream() {
    List<String> extracted = new ArrayList<>();
    for (String str : in(Stream.of("a", "b", "c"))) {
      extracted.add(str);
    }
    assertThat(extracted).containsExactly("a", "b", "c");
  }

  @Test
  public void test_inOptional_present() {
    Optional<String> optional = Optional.of("a");
    List<String> extracted = new ArrayList<>();
    for (String str : Guavate.inOptional(optional)) {
      extracted.add(str);
    }
    assertThat(extracted).containsExactly("a");
  }

  @Test
  public void test_inOptional_empty() {
    Optional<String> optional = Optional.empty();
    List<String> extracted = new ArrayList<>();
    for (String str : Guavate.inOptional(optional)) {
      extracted.add(str);
    }
    assertThat(extracted).isEmpty();
  }

  @Test
  public void test_inNullable_present() {
    List<String> extracted = new ArrayList<>();
    for (String str : Guavate.inNullable("a")) {
      extracted.add(str);
    }
    assertThat(extracted).containsExactly("a");
  }

  @Test
  public void test_inNullable_null() {
    List<String> extracted = new ArrayList<>();
    for (String str : Guavate.inNullable((String) null)) {
      extracted.add(str);
    }
    assertThat(extracted).isEmpty();
  }

  @Test
  public void test_inTryCatchIgnore_present() {
    List<String> extracted = new ArrayList<>();
    for (String str : Guavate.inTryCatchIgnore(() -> "a")) {
      extracted.add(str);
    }
    assertThat(extracted).containsExactly("a");
  }

  @Test
  public void test_inTryCatchIgnore_null() {
    List<String> extracted = new ArrayList<>();
    Supplier<String> supplier = () -> null;
    for (String str : Guavate.inTryCatchIgnore(supplier)) {
      extracted.add(str);
    }
    assertThat(extracted).isEmpty();
  }

  @Test
  public void test_inTryCatchIgnore_exception() {
    List<LocalDate> extracted = new ArrayList<>();
    for (LocalDate str : Guavate.inTryCatchIgnore(() -> LocalDate.parse("XXX"))) {
      extracted.add(str);
    }
    assertThat(extracted).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zipWithIndex() {
    Stream<String> base = Stream.of("a", "b", "c");
    List<ObjIntPair<String>> test = Guavate.zipWithIndex(base).collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of(ObjIntPair.of("a", 0), ObjIntPair.of("b", 1), ObjIntPair.of("c", 2)));
  }

  @Test
  public void test_zipWithIndex_empty() {
    Stream<String> base = Stream.of();
    List<ObjIntPair<String>> test = Guavate.zipWithIndex(base).collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_zip() {
    Stream<String> base1 = Stream.of("a", "b", "c");
    Stream<Integer> base2 = Stream.of(1, 2, 3);
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3)));
  }

  @Test
  public void test_zip_firstLonger() {
    Stream<String> base1 = Stream.of("a", "b", "c");
    Stream<Integer> base2 = Stream.of(1, 2);
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of(Pair.of("a", 1), Pair.of("b", 2)));
  }

  @Test
  public void test_zip_secondLonger() {
    Stream<String> base1 = Stream.of("a", "b");
    Stream<Integer> base2 = Stream.of(1, 2, 3);
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of(Pair.of("a", 1), Pair.of("b", 2)));
  }

  @Test
  public void test_zip_empty() {
    Stream<String> base1 = Stream.of();
    Stream<Integer> base2 = Stream.of();
    List<Pair<String, Integer>> test = Guavate.zip(base1, base2).collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_not_Predicate() {
    List<String> data = Arrays.asList("a", "", "c");
    List<String> test = data.stream()
        .filter(Guavate.not(String::isEmpty))
        .collect(Collectors.toList());
    assertThat(test).isEqualTo(ImmutableList.of("a", "c"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ensureOnlyOne() {
    assertThat(Stream.empty().reduce(Guavate.ensureOnlyOne())).isEqualTo(Optional.empty());
    assertThat(Stream.of("a").reduce(Guavate.ensureOnlyOne())).isEqualTo(Optional.of("a"));
    assertThatIllegalArgumentException().isThrownBy(() -> Stream.of("a", "b").reduce(Guavate.ensureOnlyOne()));
  }

  @Test
  public void test_ensureOnlyOne_withCustomMessage() {
    String message = "Expected one letter but found multiple for date {}";
    LocalDate arg = LocalDate.of(2024, 4, 24);
    assertThat(Stream.empty().reduce(Guavate.ensureOnlyOne(message, arg))).isEqualTo(Optional.empty());
    assertThat(Stream.of("a").reduce(Guavate.ensureOnlyOne(message, arg))).isEqualTo(Optional.of("a"));
    assertThatIllegalArgumentException().isThrownBy(() -> Stream.of("a", "b")
            .reduce(Guavate.ensureOnlyOne(message, arg)))
        .withMessage(Messages.format(message, arg));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_casting() {
    assertThat(Stream.empty().map(Guavate.casting(Integer.class)).collect(toList())).isEqualTo(ImmutableList.of());
    List<Number> baseList = Arrays.asList(1, 2, 3);
    List<Integer> castList = baseList.stream().map(Guavate.casting(Integer.class)).collect(toList());
    assertThat(castList).isEqualTo(baseList);
    List<Number> baseListMixed = ImmutableList.of(1, 2f, 3);
    assertThatExceptionOfType(ClassCastException.class)
        .isThrownBy(() -> baseListMixed.stream().map(Guavate.casting(Short.class)).collect(toList()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filtering() {
    List<Number> list = ImmutableList.of(1, 2d, 3f, 4, (short) 5, 6L, 7);
    assertThat(Stream.empty().flatMap(Guavate.filtering(Integer.class)).collect(toList())).isEqualTo(ImmutableList.of());
    assertThat(list.stream().flatMap(Guavate.filtering(Integer.class)).collect(toList())).isEqualTo(ImmutableList.of(1, 4, 7));
    assertThat(list.stream().flatMap(Guavate.filtering(Double.class)).collect(toList())).isEqualTo(ImmutableList.of(2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filteringOptional() {
    List<Optional<String>> list = ImmutableList.of(Optional.of("A"), Optional.empty(), Optional.of("C"));
    assertThat(list.stream().flatMap(Guavate.filteringOptional()).collect(toList())).isEqualTo(ImmutableList.of("A", "C"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toOptional() {
    List<String> list = Arrays.asList("a", "ab");
    assertThat(list.stream().filter(s -> s.length() == 1).collect(Guavate.toOptional()))
        .isEqualTo(Optional.of("a"));
    assertThat(list.stream().filter(s -> s.length() == 0).collect(Guavate.toOptional()))
        .isEqualTo(Optional.empty());
    assertThatIllegalArgumentException().isThrownBy(() -> list.stream().collect(Guavate.toOptional()));
    assertThatNullPointerException().isThrownBy(() -> Stream.of((String[]) null).collect(Guavate.toOptional()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toOnly() {
    List<String> list = Arrays.asList("a", "ab");
    assertThat(list.stream().filter(s -> s.length() == 1).collect(Guavate.toOnly())).hasValue("a");
    assertThat(list.stream().filter(s -> s.length() == 0).collect(Guavate.toOnly())).isEmpty();
    assertThat(list.stream().collect(Guavate.toOnly())).isEmpty();
    assertThatNullPointerException().isThrownBy(() -> Stream.of((String[]) null).collect(Guavate.toOnly()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toImmutableList() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableList<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableList());
    assertThat(test).isEqualTo(ImmutableList.of("a", "b", "c", "a"));
  }

  @Test
  public void test_splittingBySize() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableList<ImmutableList<String>> test = list.stream()
        .collect(Guavate.splittingBySize(4));
    assertThat(test).isEqualTo(ImmutableList.of(ImmutableList.of("a", "ab", "b", "bb"), ImmutableList.of("c", "a")));
  }

  @Test
  public void test_toImmutableSet() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSet<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableSet());
    assertThat(test).isEqualTo(ImmutableSet.of("a", "b", "c"));
  }

  @Test
  public void test_toImmutableSortedSet() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSortedSet<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableSortedSet());
    assertThat(test).isEqualTo(ImmutableSortedSet.of("a", "b", "c"));
  }

  @Test
  public void test_toImmutableSortedSet_comparator() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSortedSet<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableSortedSet(Ordering.natural().reverse()));
    assertThat(test).isEqualTo(ImmutableSortedSet.reverseOrder().add("a").add("b").add("c").build());
  }

  @Test
  public void test_toImmutableMultiset() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableMultiset<String> test = list.stream()
        .filter(s -> s.length() == 1)
        .collect(Guavate.toImmutableMultiset());
    assertThat(test).isEqualTo(ImmutableMultiset.of("a", "a", "b", "c"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toImmutableMap_key() {
    List<String> list = Arrays.asList("a", "ab", "bob");
    ImmutableMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableMap(s -> s.length()));
    assertThat(test).isEqualTo(ImmutableMap.builder().put(1, "a").put(2, "ab").put(3, "bob").build());
  }

  @Test
  public void test_toImmutableMap_key_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> list.stream().collect(Guavate.toImmutableMap(s -> s.length())));
  }

  @Test
  public void test_toImmutableMap_mergeFn() {
    List<String> list = Arrays.asList("b", "a", "b", "b", "c", "a");
    Map<String, Integer> result = list.stream()
        .collect(Guavate.toImmutableMap(s -> s, s -> 1, (s1, s2) -> s1 + s2));
    Map<String, Integer> expected = ImmutableMap.of("a", 2, "b", 3, "c", 1);
    assertThat(result).isEqualTo(expected);
    Iterator<String> iterator = result.keySet().iterator();
    assertThat(iterator.next()).isEqualTo("b");
    assertThat(iterator.next()).isEqualTo("a");
    assertThat(iterator.next()).isEqualTo("c");
  }

  @Test
  public void test_toImmutableMap_keyValue() {
    List<String> list = Arrays.asList("a", "ab", "bob");
    ImmutableMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableMap(s -> s.length(), s -> "!" + s));
    assertThat(test).isEqualTo(ImmutableMap.builder().put(1, "!a").put(2, "!ab").put(3, "!bob").build());
  }

  @Test
  public void test_toImmutableMap_keyValue_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> list.stream().collect(Guavate.toImmutableMap(s -> s.length(), s -> "!" + s)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toImmutableSortedMap_key() {
    List<String> list = Arrays.asList("bob", "a", "ab");
    ImmutableSortedMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSortedMap(s -> s.length()));
    assertThat(test).isEqualTo(ImmutableSortedMap.naturalOrder().put(1, "a").put(2, "ab").put(3, "bob").build());
  }

  @Test
  public void test_toImmutableSortedMap_key_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "c", "bb", "b", "a");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> list.stream().collect(Guavate.toImmutableSortedMap(s -> s.length())));
  }

  @Test
  public void test_toImmutableSortedMap_keyValue() {
    List<String> list = Arrays.asList("bob", "a", "ab");
    ImmutableSortedMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSortedMap(s -> s.length(), s -> "!" + s));
    assertThat(test).isEqualTo(ImmutableSortedMap.naturalOrder().put(1, "!a").put(2, "!ab").put(3, "!bob").build());
  }

  @Test
  public void test_toImmutableSortedMap_keyValue_duplicateKeys() {
    List<String> list = Arrays.asList("a", "ab", "c", "bb", "b", "a");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> list.stream().collect(Guavate.toImmutableSortedMap(s -> s.length(), s -> "!" + s)));
  }

  @Test
  public void test_toImmutableSortedMap_keyValue_duplicateKeys_merge() {
    List<String> list = Arrays.asList("a", "ab", "c", "bb", "b", "a");
    ImmutableSortedMap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSortedMap(s -> s.length(), s -> "!" + s, String::concat));
    assertThat(test).isEqualTo(ImmutableSortedMap.naturalOrder().put(1, "!a!c!b!a").put(2, "!ab!bb").build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toImmutableListMultimap_key() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableListMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableListMultimap(s -> s.length()));
    ImmutableListMultimap<Object, Object> expected = ImmutableListMultimap.builder()
        .put(1, "a").put(2, "ab").put(1, "b").put(2, "bb").put(1, "c").put(1, "a").build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_toImmutableListMultimap_keyValue() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableListMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableListMultimap(s -> s.length(), s -> "!" + s));
    ImmutableListMultimap<Object, Object> expected = ImmutableListMultimap.builder()
        .put(1, "!a").put(2, "!ab").put(1, "!b").put(2, "!bb").put(1, "!c").put(1, "!a").build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toImmutableSetMultimap_key() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSetMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSetMultimap(s -> s.length()));
    ImmutableSetMultimap<Object, Object> expected = ImmutableSetMultimap.builder()
        .put(1, "a").put(2, "ab").put(1, "b").put(2, "bb").put(1, "c").put(1, "a").build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_toImmutableSetMultimap_keyValue() {
    List<String> list = Arrays.asList("a", "ab", "b", "bb", "c", "a");
    ImmutableSetMultimap<Integer, String> test = list.stream()
        .collect(Guavate.toImmutableSetMultimap(s -> s.length(), s -> "!" + s));
    ImmutableSetMultimap<Object, Object> expected = ImmutableSetMultimap.builder()
        .put(1, "!a").put(2, "!ab").put(1, "!b").put(2, "!bb").put(1, "!c").build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapEntriesToImmutableMap() {
    Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
    Map<String, Integer> expected = ImmutableMap.of("a", 1, "c", 3, "e", 5);
    ImmutableMap<String, Integer> output =
        input.entrySet()
            .stream()
            .filter(e -> e.getValue() % 2 == 1)
            .collect(entriesToImmutableMap());
    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void test_mapEntriesToImmutableMap_mergeFn() {
    Map<Integer, String> input = ImmutableMap.of(1, "a", 2, "b", 3, "c", 4, "d", 5, "e");
    Map<Integer, String> expected = ImmutableMap.of(0, "bd", 1, "ace");

    ImmutableMap<Integer, String> output =
        input.entrySet()
            .stream()
            .map(e -> Guavate.entry(e.getKey() % 2, e.getValue()))
            .collect(entriesToImmutableMap(String::concat));

    assertThat(output).isEqualTo(expected);
  }

  @Test
  public void test_pairsToImmutableMap() {
    Map<String, Integer> input = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4);
    Map<String, Double> expected = ImmutableMap.of("A", 1.0, "B", 4.0, "C", 9.0, "D", 16.0);

    ImmutableMap<String, Double> output =
        input.entrySet()
            .stream()
            .map(e -> Pair.of(e.getKey().toUpperCase(Locale.ENGLISH), Math.pow(e.getValue(), 2)))
            .collect(pairsToImmutableMap());
    assertThat(output).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_entry() {
    Map.Entry<String, Integer> test = Guavate.entry("A", 1);
    assertThat(test.getKey()).isEqualTo("A");
    assertThat(test.getValue()).isEqualTo((Integer) 1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combineFuturesAsList() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    List<CompletableFuture<String>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<String>> test = Guavate.combineFuturesAsList(input);

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    List<String> combined = test.join();
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(combined.size()).isEqualTo(2);
    assertThat(combined.get(0)).isEqualTo("A");
    assertThat(combined.get(1)).isEqualTo("B");
  }

  @Test
  public void test_combineFuturesAsList_exception() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      throw new IllegalStateException("Oops");
    });
    List<CompletableFuture<String>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<String>> test = Guavate.combineFuturesAsList(input);

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> test.join());
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(test.isCompletedExceptionally()).isEqualTo(true);
  }

  @Test
  public void test_toCombinedFuture() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    List<CompletableFuture<String>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<String>> test = input.stream().collect(Guavate.toCombinedFuture());

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    List<String> combined = test.join();
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(combined.size()).isEqualTo(2);
    assertThat(combined.get(0)).isEqualTo("A");
    assertThat(combined.get(1)).isEqualTo("B");
  }

  @Test
  public void test_combineFuturesAsList_Void() {
    CompletableFuture<Void> future1 = new CompletableFuture<>();
    future1.complete(null);
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<Void> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return null;
    });
    List<CompletableFuture<Void>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<Void>> test = Guavate.combineFuturesAsList(input);

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    List<Void> combined = test.join();
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(combined.size()).isEqualTo(2);
    assertThat(combined.get(0)).isNull();
    assertThat(combined.get(1)).isNull();
  }

  @Test
  public void test_combineFuturesAsList_Void_exception() {
    CompletableFuture<Void> future1 = new CompletableFuture<>();
    future1.complete(null);
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<Void> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      throw new IllegalStateException("Oops");
    });
    List<CompletableFuture<Void>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<Void>> test = Guavate.combineFuturesAsList(input);

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> test.join());
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(test.isCompletedExceptionally()).isEqualTo(true);
  }

  @Test
  public void test_toCombinedFuture_Void() {
    CompletableFuture<Void> future1 = new CompletableFuture<>();
    future1.complete(null);
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<Void> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return null;
    });
    List<CompletableFuture<Void>> input = ImmutableList.of(future1, future2);

    CompletableFuture<List<Void>> test = input.stream().collect(Guavate.toCombinedFuture());

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    List<Void> combined = test.join();
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(combined.size()).isEqualTo(2);
    assertThat(combined.get(0)).isNull();
    assertThat(combined.get(1)).isNull();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combineFuturesAsMap() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    Map<String, CompletableFuture<String>> input = ImmutableMap.of("a", future1, "b", future2);

    CompletableFuture<Map<String, String>> test = Guavate.combineFuturesAsMap(input);

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    Map<String, String> combined = test.join();
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(combined.size()).isEqualTo(2);
    assertThat(combined.get("a")).isEqualTo("A");
    assertThat(combined.get("b")).isEqualTo("B");
  }

  @Test
  public void test_combineFuturesAsMap_exception() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      throw new IllegalStateException("Oops");
    });
    Map<String, CompletableFuture<String>> input = ImmutableMap.of("a", future1, "b", future2);

    CompletableFuture<Map<String, String>> test = Guavate.combineFuturesAsMap(input);

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    assertThatExceptionOfType(CompletionException.class).isThrownBy(() -> test.join());
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(test.isCompletedExceptionally()).isEqualTo(true);
  }

  @Test
  public void test_toCombinedFutureMap() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    future1.complete("A");
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        // ignore
      }
      return "B";
    });
    Map<String, CompletableFuture<String>> input = ImmutableMap.of("a", future1, "b", future2);

    CompletableFuture<Map<String, String>> test = input.entrySet().stream().collect(Guavate.toCombinedFutureMap());

    assertThat(test.isDone()).isEqualTo(false);
    latch.countDown();
    Map<String, String> combined = test.join();
    assertThat(test.isDone()).isEqualTo(true);
    assertThat(combined.size()).isEqualTo(2);
    assertThat(combined.get("a")).isEqualTo("A");
    assertThat(combined.get("b")).isEqualTo("B");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_poll() {
    AtomicInteger counter = new AtomicInteger();
    Supplier<String> pollingFn = () -> {
      switch (counter.incrementAndGet()) {
        case 1:
          return null;
        case 2:
          return "Yes";
        default:
          throw new AssertionError("Test failed");
      }
    };

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    CompletableFuture<String> future =
        Guavate.poll(executor, Duration.ofMillis(100), Duration.ofMillis(100), pollingFn);
    assertThat(future.join()).isEqualTo("Yes");
  }

  @Test
  public void test_poll_exception() {
    AtomicInteger counter = new AtomicInteger();
    Supplier<String> pollingFn = () -> {
      switch (counter.incrementAndGet()) {
        case 1:
          return null;
        case 2:
          throw new IllegalStateException("Expected");
        default:
          throw new AssertionError("Test failed");
      }
    };

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    try {
      CompletableFuture<String> future =
          Guavate.poll(executor, Duration.ofMillis(100), Duration.ofMillis(100), pollingFn);
      assertThatExceptionOfType(CompletionException.class)
          .isThrownBy(() -> future.join())
          .withMessage("java.lang.IllegalStateException: Expected");
    } finally {
      executor.shutdown();
    }
  }

  //-------------------------------------------------------------------------
  private static void doNothing() {
  }

  @Test
  public void test_namedThreadFactory() {
    ThreadFactory threadFactory = Guavate.namedThreadFactory().build();
    assertThat(threadFactory.newThread(() -> doNothing()).getName()).isEqualTo("GuavateTest-0");
  }

  @Test
  public void test_namedThreadFactory_prefix() {
    ThreadFactory threadFactory = Guavate.namedThreadFactory("ThreadMaker").build();
    assertThat(threadFactory.newThread(() -> doNothing()).getName()).isEqualTo("ThreadMaker-0");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_genericClass() {
    Class<List<String>> test1 = Guavate.genericClass(List.class);
    assertThat(test1).isEqualTo(List.class);
    Class<List<Number>> test2 = Guavate.genericClass(List.class);
    assertThat(test2).isEqualTo(List.class);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_callerClass() {
    assertThat(Guavate.callerClass(0)).isEqualTo(Guavate.CallerClassSecurityManager.class);
    assertThat(Guavate.callerClass(1)).isEqualTo(Guavate.class);
    assertThat(Guavate.callerClass(2)).isEqualTo(GuavateTest.class);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_validUtilityClass() {
    assertUtilityClass(Guavate.class);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_substring() {
    return new Object[][] {
        {"a.b.c", ".", "a", "b.c", "a.b", "c"},
        {"a..b..c", "..", "a", "b..c", "a..b", "c"},
        {"...", ".", "", "..", "..", ""},
        {"", ".", "", "", "", ""},
    };
  }

  @ParameterizedTest
  @MethodSource("data_substring")
  public void test_substring(
      String input,
      String separator,
      String beforeFirst,
      String afterFirst,
      String beforeLast,
      String afterLast) {

    assertThat(Guavate.substringBeforeFirst(input, separator)).isEqualTo(beforeFirst);
    assertThat(Guavate.substringAfterFirst(input, separator)).isEqualTo(afterFirst);
    assertThat(Guavate.substringBeforeLast(input, separator)).isEqualTo(beforeLast);
    assertThat(Guavate.substringAfterLast(input, separator)).isEqualTo(afterLast);
  }

}
