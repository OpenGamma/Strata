/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.source.id;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.opengamma.strata.collect.TestHelper;

/**
 * Test.
 */
@Test
public class ExternalIdSearchTest {

  private final ExternalId _id11 = ExternalId.of("D1", "V1");
  private final ExternalId _id21 = ExternalId.of("D2", "V1");
  private final ExternalId _id12 = ExternalId.of("D1", "V2");

  //-------------------------------------------------------------------------
  public void test_constructor_noargs() {
    ExternalIdSearch test = ExternalIdSearch.of();
    assertEquals((Object) test.size(), 0);
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_ExternalId_null() {
    ExternalIdSearch.of((ExternalId) null);
  }

  public void test_constructor_ExternalId() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11);
    assertEquals((Object) test.size(), 1);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11));
    assertEquals(test.getSearchType(), ExternalIdSearchType.ANY);
  }

  //-------------------------------------------------------------------------
  public void test_constructor_varargs_noExternalIds() {
    ExternalId[] args = new ExternalId[0];
    ExternalIdSearch test = ExternalIdSearch.of(args);
    assertEquals((Object) test.size(), 0);
  }

  public void test_constructor_varargs_oneExternalId() {
    ExternalId[] args = new ExternalId[] {_id11};
    ExternalIdSearch test = ExternalIdSearch.of(args);
    assertEquals((Object) test.size(), 1);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11));
  }

  public void test_constructor_varargs_twoExternalIds() {
    ExternalId[] args = new ExternalId[] {_id11, _id12};
    ExternalIdSearch test = ExternalIdSearch.of(args);
    assertEquals((Object) test.size(), 2);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11, _id12));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_varargs_null() {
    ExternalId[] args = null;
    ExternalIdSearch.of(args);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_varargs_noNulls() {
    ExternalId[] args = new ExternalId[] {_id11, null, _id12};
    ExternalIdSearch.of(args);
  }

  //-------------------------------------------------------------------------
  public void test_constructor_Iterable_empty() {
    ExternalIdSearch test = ExternalIdSearch.of(new ArrayList<ExternalId>());
    assertEquals((Object) test.size(), 0);
  }

  public void test_constructor_Iterable_two() {
    ExternalIdSearch test = ExternalIdSearch.of(Arrays.asList(_id11, _id12));
    assertEquals((Object) test.size(), 2);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11, _id12));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_Iterable_null() {
    ExternalIdSearch.of((Iterable<ExternalId>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_Iterable_noNulls() {
    ExternalIdSearch.of(Arrays.asList(_id11, null, _id12));
  }

  //-------------------------------------------------------------------------
  public void test_constructor_IterableType_empty() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.EXACT, new ArrayList<ExternalId>());
    assertEquals((Object) test.size(), 0);
    assertEquals(test.getSearchType(), ExternalIdSearchType.EXACT);
  }

  public void test_constructor_IterableType_two() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.EXACT, Arrays.asList(_id11, _id12));
    assertEquals((Object) test.size(), 2);
    assertEquals(test.getExternalIds(), Sets.newHashSet(_id11, _id12));
    assertEquals(test.getSearchType(), ExternalIdSearchType.EXACT);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_IterableType_null() {
    ExternalIdSearch.of(ExternalIdSearchType.EXACT, (Iterable<ExternalId>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_IterableType_noNulls() {
    ExternalIdSearch.of(ExternalIdSearchType.EXACT, Arrays.asList(_id11, null, _id12));
  }

  //-------------------------------------------------------------------------
  public void test_singleExternalIdDifferentConstructors() {
    assertTrue(ExternalIdSearch.of(_id11).equals(ExternalIdSearch.of(Collections.singleton(_id11))));
  }

  public void test_singleVersusMultipleExternalId() {
    assertFalse(ExternalIdSearch.of(_id11).equals(ExternalIdSearch.of(_id11, _id12)));
    assertFalse(ExternalIdSearch.of(_id11, _id12).equals(ExternalIdSearch.of(_id11)));
  }

  //-------------------------------------------------------------------------
  public void test_withExternalIdAdded() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals((Object) base.size(), 1);
    ExternalIdSearch test = base.withExternalIdAdded(ExternalId.of("A", "C"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 2);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
  }

  public void test_withExternalIdAdded_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> test.withExternalIdAdded(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withExternalIdsAdded_array() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals((Object) base.size(), 1);
    ExternalIdSearch test = base.withExternalIdsAdded(ExternalId.of("A", "C"), ExternalId.of("D", "E"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 3);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("D", "E")));
  }

  public void test_withExternalIdsAdded_array_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> test.withExternalIdsAdded((ExternalId[]) null), IllegalArgumentException.class);
  }

  public void test_withExternalIdsAdded_iterable() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals((Object) base.size(), 1);
    ExternalIdSearch test = base.withExternalIdsAdded(Arrays.asList(ExternalId.of("A", "C"), ExternalId.of("D", "E")));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 3);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("D", "E")));
  }

  public void test_withExternalIdsAdded_iterable_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> test.withExternalIdsAdded((Iterable<ExternalId>) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withExternalIdRemoved_match() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals((Object) base.size(), 1);
    ExternalIdSearch test = base.withExternalIdRemoved(ExternalId.of("A", "B"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 0);
  }

  public void test_withExternalIdRemoved_noMatch() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals((Object) base.size(), 1);
    ExternalIdSearch test = base.withExternalIdRemoved(ExternalId.of("A", "C"));
    assertEquals((Object) base.size(), 1);
    assertEquals((Object) test.size(), 1);
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
  }

  public void test_withExternalIdRemoved_null() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> base.withExternalIdRemoved(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withSearchType() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals(base.getSearchType(), ExternalIdSearchType.ANY);
    ExternalIdSearch test = base.withSearchType(ExternalIdSearchType.EXACT);
    assertEquals(test.getSearchType(), ExternalIdSearchType.EXACT);
  }

  public void test_withSearchType_same() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalIdSearchType.ALL, ExternalId.of("A", "B"));
    assertSame(base.withSearchType(ExternalIdSearchType.ALL), base);
  }

  public void test_withSearchType_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    TestHelper.assertThrows(() -> test.withSearchType(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_size() {
    assertEquals((Object) ExternalIdSearch.of().size(), 0);
    assertEquals((Object) ExternalIdSearch.of(_id11).size(), 1);
    assertEquals((Object) ExternalIdSearch.of(_id11, _id12).size(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_matches1_EXACT() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.EXACT, _id11);
    assertEquals((Object) test1.matches(_id11), true);
    assertEquals((Object) test1.matches(_id21), false);
    assertEquals((Object) test1.alwaysMatches(), false);

    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.EXACT, _id11, _id21);
    assertEquals((Object) test2.matches(_id11), false);
    assertEquals((Object) test2.matches(_id12), false);
    assertEquals((Object) test2.matches(_id21), false);
    assertEquals((Object) test2.alwaysMatches(), false);
  }

  public void test_matches1_ALL() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.ALL, _id11);
    assertEquals((Object) test1.matches(_id11), true);
    assertEquals((Object) test1.matches(_id12), false);
    assertEquals((Object) test1.alwaysMatches(), false);

    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.ALL, _id11, _id21);
    assertEquals((Object) test2.matches(_id11), false);
    assertEquals((Object) test2.matches(_id12), false);
    assertEquals((Object) test2.matches(_id21), false);
    assertEquals((Object) test2.alwaysMatches(), false);
  }

  public void test_matches1_ANY() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.ANY, _id11);
    assertEquals((Object) test1.matches(_id11), true);
    assertEquals((Object) test1.matches(_id12), false);
    assertEquals((Object) test1.alwaysMatches(), false);

    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.ANY, _id11, _id21);
    assertEquals((Object) test2.matches(_id11), true);
    assertEquals((Object) test2.matches(_id12), false);
    assertEquals((Object) test2.matches(_id21), true);
    assertEquals((Object) test2.alwaysMatches(), false);
  }

  public void test_matches1_NONE() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11);
    assertEquals((Object) test1.matches(_id11), false);
    assertEquals((Object) test1.matches(_id12), true);
    assertEquals((Object) test1.alwaysMatches(), false);

    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11, _id21);
    assertEquals((Object) test2.matches(_id11), false);
    assertEquals((Object) test2.matches(_id12), true);
    assertEquals((Object) test2.matches(_id21), false);
    assertEquals((Object) test2.alwaysMatches(), false);

    ExternalIdSearch test3 = ExternalIdSearch.of(ExternalIdSearchType.NONE);
    assertEquals((Object) test3.matches(_id11), true);
    assertEquals((Object) test3.matches(_id12), true);
    assertEquals((Object) test3.matches(_id21), true);
    assertEquals((Object) test3.alwaysMatches(), true);
  }

  public void test_matches1_OTHER() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11);
    assertEquals((Object) test1.matches(_id11), false);
    assertEquals((Object) test1.matches(_id12), true);
    assertEquals((Object) test1.alwaysMatches(), false);

    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11, _id21);
    assertEquals((Object) test2.matches(_id11), false);
    assertEquals((Object) test2.matches(_id12), true);
    assertEquals((Object) test2.matches(_id21), false);
    assertEquals((Object) test2.alwaysMatches(), false);

    ExternalIdSearch test3 = ExternalIdSearch.of(ExternalIdSearchType.NONE);
    assertEquals((Object) test3.matches(_id11), true);
    assertEquals((Object) test3.matches(_id12), true);
    assertEquals((Object) test3.matches(_id21), true);
    assertEquals((Object) test3.alwaysMatches(), true);
  }

  //-------------------------------------------------------------------------
  public void test_matches_EXACT() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.EXACT, _id11, _id12);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12)), true);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12, _id21)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id12)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id21)), false);
    assertEquals((Object) test.matches(ImmutableSet.of()), false);
  }

  public void test_matches_ALL() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ALL, _id11, _id12);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12)), true);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12, _id21)), true);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id12)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id21)), false);
    assertEquals((Object) test.matches(ImmutableSet.of()), false);
  }

  public void test_matches_ANY() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ANY, _id11, _id12);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12)), true);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12, _id21)), true);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11)), true);
    assertEquals((Object) test.matches(ImmutableSet.of(_id12)), true);
    assertEquals((Object) test.matches(ImmutableSet.of(_id21)), false);
    assertEquals((Object) test.matches(ImmutableSet.of()), false);
  }

  public void test_matches_NONE() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11, _id12);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11, _id12, _id21)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id11)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id12)), false);
    assertEquals((Object) test.matches(ImmutableSet.of(_id21)), true);
    assertEquals((Object) test.matches(ImmutableSet.of()), true);
  }

  //-------------------------------------------------------------------------
  public void test_containsAll1() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id11, _id12)), false);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id11)), true);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id12)), false);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id21)), false);
    assertEquals((Object) test.containsAll(ImmutableSet.of()), true);
  }

  public void test_containsAll2() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id11, _id12)), true);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id11)), true);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id12)), true);
    assertEquals((Object) test.containsAll(ImmutableSet.of(_id21)), false);
    assertEquals((Object) test.containsAll(ImmutableSet.of()), true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_containsAll_null() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    test.containsAll(null);
  }

  //-------------------------------------------------------------------------
  public void test_containsAny() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals((Object) test.containsAny(ImmutableSet.of(_id11, _id12)), true);
    assertEquals((Object) test.containsAny(ImmutableSet.of(_id11)), true);
    assertEquals((Object) test.containsAny(ImmutableSet.of(_id12)), true);
    assertEquals((Object) test.containsAny(ImmutableSet.of(_id21)), false);
    assertEquals((Object) test.containsAny(ImmutableSet.of()), false);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_containsAny_null() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    test.containsAny(null);
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals((Object) test.contains(_id11), true);
    assertEquals((Object) test.contains(_id11), true);
    assertEquals((Object) test.contains(_id21), false);
  }

  public void test_contains_null() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals((Object) test.contains(null), false);
  }

  //-------------------------------------------------------------------------
  public void test_canMatch_EXACT() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.EXACT);
    assertEquals((Object) test.canMatch(), false);
    test = test.withExternalIdAdded(_id11);
    assertEquals((Object) test.canMatch(), true);
  }

  public void test_canMatch_ALL() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ALL);
    assertEquals((Object) test.canMatch(), false);
    test = test.withExternalIdAdded(_id11);
    assertEquals((Object) test.canMatch(), true);
  }

  public void test_canMatch_ANY() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ANY);
    assertEquals((Object) test.canMatch(), false);
    test = test.withExternalIdAdded(_id11);
    assertEquals((Object) test.canMatch(), true);
  }

  public void test_canMatch_NONE() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.NONE);
    assertEquals((Object) test.canMatch(), true);
    test = test.withExternalIdAdded(_id11);
    assertEquals((Object) test.canMatch(), true);
  }

  //-------------------------------------------------------------------------
  public void test_equals_same_empty() {
    ExternalIdSearch a1 = ExternalIdSearch.of();
    ExternalIdSearch a2 = ExternalIdSearch.of();

    assertEquals((Object) a1.equals(a1), true);
    assertEquals((Object) a1.equals(a2), true);
    assertEquals((Object) a2.equals(a1), true);
    assertEquals((Object) a2.equals(a2), true);
  }

  public void test_equals_same_nonEmpty() {
    ExternalIdSearch a1 = ExternalIdSearch.of(_id11, _id12);
    ExternalIdSearch a2 = ExternalIdSearch.of(_id11, _id12);

    assertEquals((Object) a1.equals(a1), true);
    assertEquals((Object) a1.equals(a2), true);
    assertEquals((Object) a2.equals(a1), true);
    assertEquals((Object) a2.equals(a2), true);
  }

  public void test_equals_different() {
    ExternalIdSearch a = ExternalIdSearch.of();
    ExternalIdSearch b = ExternalIdSearch.of(_id11, _id12);

    assertEquals((Object) a.equals(a), true);
    assertEquals((Object) a.equals(b), false);
    assertEquals((Object) b.equals(a), false);
    assertEquals((Object) b.equals(b), true);

    assertEquals((Object) b.equals("Rubbish"), false);
    assertEquals((Object) b.equals(null), false);
  }

  public void test_hashCode() {
    ExternalIdSearch a = ExternalIdSearch.of(_id11, _id12);
    ExternalIdSearch b = ExternalIdSearch.of(_id11, _id12);

    assertEquals((Object) b.hashCode(), a.hashCode());
  }

  public void test_toString_empty() {
    ExternalIdSearch test = ExternalIdSearch.of();
    assertTrue(test.toString().contains("[]"));
  }

  public void test_toString_nonEmpty() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertTrue(test.toString().contains(_id11.toString()));
    assertTrue(test.toString().contains(_id12.toString()));
  }

  //-------------------------------------------------------------------------
  public void test_type_enum() {
    assertEquals((Object) ExternalIdSearchType.values().length, 4);
    assertEquals(ExternalIdSearchType.valueOf("ANY"), ExternalIdSearchType.ANY);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TestHelper.coverImmutableBean(ExternalIdSearch.of(_id11, _id12));
  }

}
