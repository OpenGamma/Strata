/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.explain;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * Test {@link ExplainMap}.
 */
public class ExplainMapTest {

  private static final String EOL = System.lineSeparator();
  private static final LocalDate DATE1 = date(2015, 6, 30);
  private static final LocalDate DATE2 = date(2015, 9, 30);
  private static final CurrencyAmount AMOUNT1 = CurrencyAmount.of(GBP, 1000);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    Map<ExplainKey<?>, Object> map = new HashMap<>();
    map.put(ExplainKey.START_DATE, DATE1);
    map.put(ExplainKey.END_DATE, DATE2);
    map.put(ExplainKey.PRESENT_VALUE, AMOUNT1);
    ExplainMap test = ExplainMap.of(map);
    assertThat(test.getMap()).isEqualTo(map);
    assertThat(test.get(ExplainKey.START_DATE)).isEqualTo(Optional.of(DATE1));
    assertThat(test.get(ExplainKey.END_DATE)).isEqualTo(Optional.of(DATE2));
    assertThat(test.get(ExplainKey.ACCRUAL_DAY_COUNT)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_empty() {
    ExplainMap test = ExplainMap.empty();
    assertThat(test.getMap()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_simple() {
    ExplainMapBuilder builder = ExplainMap.builder();
    builder.put(ExplainKey.ACCRUAL_DAYS, 2);
    ExplainMap test = builder.build();
    assertThat(test.getMap()).hasSize(1);
    assertThat(test.get(ExplainKey.ACCRUAL_DAYS)).isEqualTo(Optional.of(2));
    assertThat(test.get(ExplainKey.ACCRUAL_DAY_COUNT)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_builder_openClose() {
    ExplainMapBuilder builder = ExplainMap.builder();
    ExplainMapBuilder child = builder.openListEntry(ExplainKey.LEGS);
    child.put(ExplainKey.ACCRUAL_DAYS, 2);
    ExplainMapBuilder result = child.closeListEntry(ExplainKey.LEGS);
    ExplainMap test = result.build();
    assertThat(test.getMap()).hasSize(1);
    assertThat(test.get(ExplainKey.LEGS)).isPresent();
    assertThat(test.get(ExplainKey.LEGS).get()).hasSize(1);
    assertThat(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ACCRUAL_DAYS)).isEqualTo(Optional.of(2));
  }

  @Test
  public void test_builder_openClose_wrongCloseKey() {
    ExplainMapBuilder builder = ExplainMap.builder();
    ExplainMapBuilder child = builder.openListEntry(ExplainKey.LEGS);
    child.put(ExplainKey.ACCRUAL_DAYS, 2);
    assertThatIllegalStateException()
        .isThrownBy(() -> child.closeListEntry(ExplainKey.PAYMENT_PERIODS));
  }

  @Test
  public void test_builder_addListEntry() {
    ExplainMapBuilder base = ExplainMap.builder();
    ExplainMapBuilder result1 = base.addListEntry(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 2));
    ExplainMapBuilder result2 = result1.addListEntry(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 3));
    ExplainMap test = result2.build();
    assertThat(test.getMap()).hasSize(1);
    assertThat(test.get(ExplainKey.LEGS)).isPresent();
    assertThat(test.get(ExplainKey.LEGS).get()).hasSize(2);
    assertThat(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ACCRUAL_DAYS)).isEqualTo(Optional.of(2));
    assertThat(test.get(ExplainKey.LEGS).get().get(1).get(ExplainKey.ACCRUAL_DAYS)).isEqualTo(Optional.of(3));
  }

  @Test
  public void test_builder_addListEntryWithIndex() {
    ExplainMapBuilder base = ExplainMap.builder();
    ExplainMapBuilder result1 = base.addListEntryWithIndex(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 2));
    ExplainMapBuilder result2 = result1.addListEntryWithIndex(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 3));
    ExplainMap test = result2.build();
    assertThat(test.getMap()).hasSize(1);
    assertThat(test.get(ExplainKey.LEGS)).isPresent();
    assertThat(test.get(ExplainKey.LEGS).get()).hasSize(2);
    assertThat(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ENTRY_INDEX)).isEqualTo(Optional.of(0));
    assertThat(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ACCRUAL_DAYS)).isEqualTo(Optional.of(2));
    assertThat(test.get(ExplainKey.LEGS).get().get(1).get(ExplainKey.ENTRY_INDEX)).isEqualTo(Optional.of(1));
    assertThat(test.get(ExplainKey.LEGS).get().get(1).get(ExplainKey.ACCRUAL_DAYS)).isEqualTo(Optional.of(3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_explanationString() {
    Map<ExplainKey<?>, Object> child1map = new LinkedHashMap<>();
    child1map.put(ExplainKey.PAYMENT_PERIODS, ImmutableList.of(ExplainMap.of(ImmutableMap.of())));
    child1map.put(ExplainKey.INDEX_VALUE, 1.2d);
    child1map.put(ExplainKey.COMBINED_RATE, 1.4d);

    Map<ExplainKey<?>, Object> child2map = new LinkedHashMap<>();
    child2map.put(ExplainKey.INDEX_VALUE, 2.3d);

    List<ExplainMap> list1 = new ArrayList<>();
    List<ExplainMap> list2 = new ArrayList<>();
    list2.add(ExplainMap.of(child1map));
    list2.add(ExplainMap.of(child2map));

    Map<ExplainKey<?>, Object> map = new LinkedHashMap<>();
    map.put(ExplainKey.LEGS, list1);
    map.put(ExplainKey.START_DATE, DATE1);
    map.put(ExplainKey.END_DATE, DATE2);
    map.put(ExplainKey.OBSERVATIONS, list2);
    map.put(ExplainKey.PRESENT_VALUE, AMOUNT1);

    ExplainMap test = ExplainMap.of(map);
    assertThat(test.explanationString()).isEqualTo("" +
        "ExplainMap {" + EOL +
        "  Legs = []," + EOL +
        "  StartDate = 2015-06-30," + EOL +
        "  EndDate = 2015-09-30," + EOL +
        "  Observations = [{" + EOL +
        "    PaymentPeriods = [{" + EOL +
        "    }]," + EOL +
        "    IndexValue = 1.2," + EOL +
        "    CombinedRate = 1.4" + EOL +
        "  },{" + EOL +
        "    IndexValue = 2.3" + EOL +
        "  }]," + EOL +
        "  PresentValue = GBP 1000" + EOL +
        "}" + EOL);
  }

  @Test
  public void test_isEmpty() {
    ExplainMap test = ExplainMap.empty();
    assertThat(test.isEmpty()).isTrue();

    ExplainMap test2 = ExplainMap.of(ImmutableMap.of(ExplainKey.DAYS, 2));
    assertThat(test2.isEmpty()).isFalse();
  }

  @Test
  public void test_convertCurrencyEntries() {
    Map<ExplainKey<?>, Object> map = new HashMap<>();
    map.put(ExplainKey.START_DATE, DATE1);
    map.put(ExplainKey.END_DATE, DATE2);
    map.put(ExplainKey.PRESENT_VALUE, AMOUNT1);
    ExplainMap test = ExplainMap.of(map);
    FxRateProvider provider = (ccy1, ccy2) -> 2.5d;
    ExplainMap testConverted = test.convertedTo(USD, provider);
    assertThat(testConverted.get(ExplainKey.START_DATE)).isEqualTo(Optional.of(DATE1));
    assertThat(testConverted.get(ExplainKey.END_DATE)).isEqualTo(Optional.of(DATE2));
    assertThat(testConverted.get(ExplainKey.PRESENT_VALUE))
        .isEqualTo(Optional.of(CurrencyAmount.of(USD, AMOUNT1.getAmount() * 2.5d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    Map<ExplainKey<?>, Object> map = new HashMap<>();
    map.put(ExplainKey.START_DATE, DATE1);
    map.put(ExplainKey.END_DATE, DATE2);
    ExplainMap test = ExplainMap.of(map);
    coverImmutableBean(test);
    Map<ExplainKey<?>, Object> map2 = new HashMap<>();
    map.put(ExplainKey.START_DATE, DATE2);
    ExplainMap test2 = ExplainMap.of(map2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    Map<ExplainKey<?>, Object> map = new HashMap<>();
    map.put(ExplainKey.START_DATE, DATE1);
    map.put(ExplainKey.END_DATE, DATE2);
    ExplainMap test = ExplainMap.of(map);
    assertSerialization(test);
  }

}
