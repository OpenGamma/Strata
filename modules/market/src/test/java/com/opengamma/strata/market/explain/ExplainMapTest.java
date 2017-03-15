/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.explain;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.CurrencyAmount;

/**
 * Test {@link ExplainMap}.
 */
@Test
public class ExplainMapTest {

  private static final String EOL = System.lineSeparator();
  private static final LocalDate DATE1 = date(2015, 6, 30);
  private static final LocalDate DATE2 = date(2015, 9, 30);
  private static final CurrencyAmount AMOUNT1 = CurrencyAmount.of(GBP, 1000);

  //-------------------------------------------------------------------------
  public void test_of() {
    Map<ExplainKey<?>, Object> map = new HashMap<>();
    map.put(ExplainKey.START_DATE, DATE1);
    map.put(ExplainKey.END_DATE, DATE2);
    map.put(ExplainKey.PRESENT_VALUE, AMOUNT1);
    ExplainMap test = ExplainMap.of(map);
    assertEquals(test.getMap(), map);
    assertEquals(test.get(ExplainKey.START_DATE), Optional.of(DATE1));
    assertEquals(test.get(ExplainKey.END_DATE), Optional.of(DATE2));
    assertEquals(test.get(ExplainKey.ACCRUAL_DAY_COUNT), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_builder_simple() {
    ExplainMapBuilder builder = ExplainMap.builder();
    builder.put(ExplainKey.ACCRUAL_DAYS, 2);
    ExplainMap test = builder.build();
    assertEquals(test.getMap().size(), 1);
    assertEquals(test.get(ExplainKey.ACCRUAL_DAYS), Optional.of(2));
    assertEquals(test.get(ExplainKey.ACCRUAL_DAY_COUNT), Optional.empty());
  }

  public void test_builder_openClose() {
    ExplainMapBuilder builder = ExplainMap.builder();
    ExplainMapBuilder child = builder.openListEntry(ExplainKey.LEGS);
    child.put(ExplainKey.ACCRUAL_DAYS, 2);
    ExplainMapBuilder result = child.closeListEntry(ExplainKey.LEGS);
    ExplainMap test = result.build();
    assertEquals(test.getMap().size(), 1);
    assertEquals(test.get(ExplainKey.LEGS).isPresent(), true);
    assertEquals(test.get(ExplainKey.LEGS).get().size(), 1);
    assertEquals(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ACCRUAL_DAYS), Optional.of(2));
  }

  public void test_builder_openClose_wrongCloseKey() {
    ExplainMapBuilder builder = ExplainMap.builder();
    ExplainMapBuilder child = builder.openListEntry(ExplainKey.LEGS);
    child.put(ExplainKey.ACCRUAL_DAYS, 2);
    assertThrows(() -> child.closeListEntry(ExplainKey.PAYMENT_PERIODS), IllegalStateException.class);
  }

  public void test_builder_addListEntry() {
    ExplainMapBuilder base = ExplainMap.builder();
    ExplainMapBuilder result1 = base.addListEntry(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 2));
    ExplainMapBuilder result2 = result1.addListEntry(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 3));
    ExplainMap test = result2.build();
    assertEquals(test.getMap().size(), 1);
    assertEquals(test.get(ExplainKey.LEGS).isPresent(), true);
    assertEquals(test.get(ExplainKey.LEGS).get().size(), 2);
    assertEquals(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ACCRUAL_DAYS), Optional.of(2));
    assertEquals(test.get(ExplainKey.LEGS).get().get(1).get(ExplainKey.ACCRUAL_DAYS), Optional.of(3));
  }

  public void test_builder_addListEntryWithIndex() {
    ExplainMapBuilder base = ExplainMap.builder();
    ExplainMapBuilder result1 = base.addListEntryWithIndex(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 2));
    ExplainMapBuilder result2 = result1.addListEntryWithIndex(ExplainKey.LEGS, child -> child.put(ExplainKey.ACCRUAL_DAYS, 3));
    ExplainMap test = result2.build();
    assertEquals(test.getMap().size(), 1);
    assertEquals(test.get(ExplainKey.LEGS).isPresent(), true);
    assertEquals(test.get(ExplainKey.LEGS).get().size(), 2);
    assertEquals(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ENTRY_INDEX), Optional.of(0));
    assertEquals(test.get(ExplainKey.LEGS).get().get(0).get(ExplainKey.ACCRUAL_DAYS), Optional.of(2));
    assertEquals(test.get(ExplainKey.LEGS).get().get(1).get(ExplainKey.ENTRY_INDEX), Optional.of(1));
    assertEquals(test.get(ExplainKey.LEGS).get().get(1).get(ExplainKey.ACCRUAL_DAYS), Optional.of(3));
  }

  //-------------------------------------------------------------------------
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
    assertEquals(test.explanationString(), "" +
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

  //-------------------------------------------------------------------------
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

  public void test_serialization() {
    Map<ExplainKey<?>, Object> map = new HashMap<>();
    map.put(ExplainKey.START_DATE, DATE1);
    map.put(ExplainKey.END_DATE, DATE2);
    ExplainMap test = ExplainMap.of(map);
    assertSerialization(test);
  }

}
