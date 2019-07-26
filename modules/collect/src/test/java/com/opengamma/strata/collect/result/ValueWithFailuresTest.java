/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.result;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.Messages;

/**
 * Test {@link ValueWithFailures}.
 */
public class ValueWithFailuresTest {

  private static final FailureItem FAILURE1 = FailureItem.of(FailureReason.INVALID, "invalid");
  private static final FailureItem FAILURE2 = FailureItem.of(FailureReason.MISSING_DATA, "data");

  //-------------------------------------------------------------------------
  @Test
  public void test_of_array_noFailures() {
    ValueWithFailures<String> test = ValueWithFailures.of("success");
    assertThat(test.hasFailures()).isEqualTo(false);
    assertThat(test.getValue()).isEqualTo("success");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of());
  }

  @Test
  public void test_of_array() {
    ValueWithFailures<String> test = ValueWithFailures.of("success", FAILURE1, FAILURE2);
    assertThat(test.hasFailures()).isEqualTo(true);
    assertThat(test.getValue()).isEqualTo("success");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  @Test
  public void test_of_list() {
    ValueWithFailures<String> test = ValueWithFailures.of("success", ImmutableList.of(FAILURE1, FAILURE2));
    assertThat(test.hasFailures()).isEqualTo(true);
    assertThat(test.getValue()).isEqualTo("success");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  @Test
  public void test_of_supplier_success() {
    ValueWithFailures<String> test = ValueWithFailures.of("", () -> "A");
    assertThat(test.hasFailures()).isEqualTo(false);
    assertThat(test.getValue()).isEqualTo("A");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of());
  }

  @Test
  public void test_of_supplier_failure() {
    ValueWithFailures<String> test = ValueWithFailures.of("", () -> {
      throw new IllegalArgumentException();
    });
    assertThat(test.hasFailures()).isEqualTo(true);
    assertThat(test.getValue()).isEqualTo("");
    assertThat(test.getFailures().size()).isEqualTo(1);
    assertThat(test.getFailures().get(0).getReason()).isEqualTo(FailureReason.ERROR);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_map() {
    ValueWithFailures<List<String>> base = ValueWithFailures.of(ImmutableList.of("1", "2"), ImmutableList.of(FAILURE1));
    ValueWithFailures<List<Integer>> test =
        base.map(list -> list.stream().map(s -> Integer.valueOf(s)).collect(toImmutableList()));
    assertThat(test.getValue()).isEqualTo(ImmutableList.of(Integer.valueOf(1), Integer.valueOf(2)));
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1));
  }

  @Test
  public void test_flatMap() {
    ValueWithFailures<List<String>> base =
        ValueWithFailures.of(ImmutableList.of("1", "a", "2"), ImmutableList.of(FAILURE1));
    ValueWithFailures<List<Integer>> test = base.flatMap(this::flatMapFunction);
    assertThat(test.getValue()).isEqualTo(ImmutableList.of(Integer.valueOf(1), Integer.valueOf(2)));
    assertThat(test.getFailures().size()).isEqualTo(2);
    assertThat(test.getFailures().get(0)).isEqualTo(FAILURE1);
    assertThat(test.getFailures().get(1).getReason()).isEqualTo(FailureReason.INVALID);
  }

  private ValueWithFailures<List<Integer>> flatMapFunction(List<String> input) {
    List<Integer> integers = new ArrayList<>();
    List<FailureItem> failures = new ArrayList<>();
    for (String str : input) {
      try {
        integers.add(Integer.valueOf(str));
      } catch (NumberFormatException ex) {
        failures.add(FailureItem.of(FailureReason.INVALID, ex));
      }
    }
    return ValueWithFailures.of(integers, failures);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    ValueWithFailures<List<String>> base = ValueWithFailures.of(ImmutableList.of("a"), ImmutableList.of(FAILURE1));
    ValueWithFailures<List<String>> other =
        ValueWithFailures.of(ImmutableList.of("b", "c"), ImmutableList.of(FAILURE2));
    ValueWithFailures<List<String>> test = base.combinedWith(other, Guavate::concatToList);
    assertThat(test.getValue()).isEqualTo(ImmutableList.of("a", "b", "c"));
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  @Test
  public void test_combinedWith_differentTypes() {
    ValueWithFailures<Boolean> base = ValueWithFailures.of(Boolean.TRUE, ImmutableList.of(FAILURE1));
    ValueWithFailures<Integer> other = ValueWithFailures.of(Integer.valueOf(1), ImmutableList.of(FAILURE2));
    ValueWithFailures<String> test = base.combinedWith(other, (a, b) -> a.toString() + b.toString());
    assertThat(test.getValue()).isEqualTo("true1");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  @Test
  public void test_combining() {
    ValueWithFailures<List<String>> base = ValueWithFailures.of(ImmutableList.of("a"), ImmutableList.of(FAILURE1));
    ValueWithFailures<List<String>> other =
        ValueWithFailures.of(ImmutableList.of("b", "c"), ImmutableList.of(FAILURE2));

    ValueWithFailures<List<String>> test = Stream.of(base, other)
        .reduce(ValueWithFailures.combiningValues(Guavate::concatToList))
        .get();

    assertThat(test.getValue()).isEqualTo(ImmutableList.of("a", "b", "c"));
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withValue_value() {
    ValueWithFailures<List<String>> base = ValueWithFailures.of(ImmutableList.of("a"), ImmutableList.of(FAILURE1));
    ValueWithFailures<String> test = base.withValue("combined");
    assertThat(test.getValue()).isEqualTo("combined");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1));
  }

  @Test
  public void test_withValue_valueFailures() {
    ValueWithFailures<List<String>> base = ValueWithFailures.of(ImmutableList.of("a"), ImmutableList.of(FAILURE1));
    ValueWithFailures<String> test = base.withValue("combined", ImmutableList.of(FAILURE2));
    assertThat(test.getValue()).isEqualTo("combined");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  @Test
  public void test_withValue_ValueWithFailures() {
    ValueWithFailures<List<String>> base = ValueWithFailures.of(ImmutableList.of("a"), ImmutableList.of(FAILURE1));
    ValueWithFailures<String> test = base.withValue(ValueWithFailures.of("combined", ImmutableList.of(FAILURE2)));
    assertThat(test.getValue()).isEqualTo("combined");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  @Test
  public void test_withAdditionalFailures() {
    ValueWithFailures<String> base = ValueWithFailures.of("combined", ImmutableList.of(FAILURE1));
    ValueWithFailures<String> test = base.withAdditionalFailures(ImmutableList.of(FAILURE2));
    assertThat(test.getValue()).isEqualTo("combined");
    assertThat(test.getFailures()).isEqualTo(ImmutableList.of(FAILURE1, FAILURE2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toValueWithFailures() {
    ValueWithFailures<Double> result = Stream.of(5d, 6d, 7d)
        .map(value -> mockCalc(value))
        .collect(ValueWithFailures.toValueWithFailures(1d, (val1, val2) -> val1 * val2));
    assertThat(result.getValue()).isEqualTo(210d); //5 * 6 * 7 = 210

    List<FailureItem> failures = result.getFailures();
    assertThat(failures.size()).isEqualTo(3); //One failure item for each element in testList.
    assertThat(failures.get(0).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 5d));
    assertThat(failures.get(1).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 6d));
    assertThat(failures.get(2).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 7d));
  }

  @Test
  public void test_toCombinedValuesAsList() {
    ValueWithFailures<List<Double>> result = Stream.of(5d, 6d, 7d)
        .map(value -> mockCalc(value))
        .collect(ValueWithFailures.toCombinedValuesAsList());
    assertThat(result.getValue()).isEqualTo(ImmutableList.of(5d, 6d, 7d));

    List<FailureItem> failures = result.getFailures();
    assertThat(failures.size()).isEqualTo(3); //One failure item for each element in testList.
    assertThat(failures.get(0).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 5d));
    assertThat(failures.get(1).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 6d));
    assertThat(failures.get(2).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 7d));
  }

  @Test
  public void test_toCombinedValuesAsSet() {
    ValueWithFailures<Set<Double>> result = Stream.of(5d, 6d, 7d)
        .map(value -> mockCalc(value))
        .collect(ValueWithFailures.toCombinedValuesAsSet());
    assertThat(result.getValue()).isEqualTo(ImmutableSet.of(5d, 6d, 7d));

    List<FailureItem> failures = result.getFailures();
    assertThat(failures.size()).isEqualTo(3); //One failure item for each element in testList.
    assertThat(failures.get(0).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 5d));
    assertThat(failures.get(1).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 6d));
    assertThat(failures.get(2).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 7d));
  }

  @Test
  public void test_combineAsList() {
    ImmutableList<ValueWithFailures<Double>> listOfValueWithFailures = Stream.of(5d, 6d, 7d)
        .map(value -> mockCalc(value))
        .collect(toImmutableList());

    ValueWithFailures<List<Double>> result = ValueWithFailures.combineValuesAsList(listOfValueWithFailures);
    assertThat(result.getValue()).isEqualTo(ImmutableList.of(5d, 6d, 7d));

    List<FailureItem> failures = result.getFailures();
    assertThat(failures.size()).isEqualTo(3); //One failure item for each element in testList.
    assertThat(failures.get(0).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 5d));
    assertThat(failures.get(1).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 6d));
    assertThat(failures.get(2).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 7d));
  }

  @Test
  public void test_combineAsSet() {
    ImmutableList<ValueWithFailures<Double>> listOfValueWithFailures = Stream.of(5d, 6d, 7d)
        .map(value -> mockCalc(value))
        .collect(toImmutableList());

    ValueWithFailures<Set<Double>> result = ValueWithFailures.combineValuesAsSet(listOfValueWithFailures);
    assertThat(result.getValue()).isEqualTo(ImmutableSet.of(5d, 6d, 7d));

    List<FailureItem> failures = result.getFailures();
    assertThat(failures.size()).isEqualTo(3); //One failure item for each element in testList.
    assertThat(failures.get(0).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 5d));
    assertThat(failures.get(1).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 6d));
    assertThat(failures.get(2).getMessage()).isEqualTo(Messages.format("Error calculating result for input value {}", 7d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ValueWithFailures<String> test = ValueWithFailures.of("success", FAILURE1, FAILURE2);
    coverImmutableBean(test);
    ValueWithFailures<String> test2 = ValueWithFailures.of("test");
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ValueWithFailures<String> test = ValueWithFailures.of("success", FAILURE1, FAILURE2);
    assertSerialization(test);
  }

  private static ValueWithFailures<Double> mockCalc(double value) {
    FailureItem failure = FailureItem.of(
        FailureReason.CALCULATION_FAILED,
        "Error calculating result for input value {}",
        value);

    return ValueWithFailures.of(value, ImmutableList.of(failure));
  }

}
