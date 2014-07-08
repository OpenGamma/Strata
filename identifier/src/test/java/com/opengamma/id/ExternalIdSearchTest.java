/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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
    assertEquals(0, test.size());
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_ExternalId_null() {
    ExternalIdSearch.of((ExternalId) null);
  }

  public void test_constructor_ExternalId() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11);
    assertEquals(1, test.size());
    assertEquals(Sets.newHashSet(_id11), test.getExternalIds());
    assertEquals(ExternalIdSearchType.ANY, test.getSearchType());
  }

  //-------------------------------------------------------------------------
  public void test_constructor_varargs_noExternalIds() {
    ExternalId[] args = new ExternalId[0];
    ExternalIdSearch test = ExternalIdSearch.of(args);
    assertEquals(0, test.size());
  }

  public void test_constructor_varargs_oneExternalId() {
    ExternalId[] args = new ExternalId[] {_id11};
    ExternalIdSearch test = ExternalIdSearch.of(args);
    assertEquals(1, test.size());
    assertEquals(Sets.newHashSet(_id11), test.getExternalIds());
  }

  public void test_constructor_varargs_twoExternalIds() {
    ExternalId[] args = new ExternalId[] {_id11, _id12};
    ExternalIdSearch test = ExternalIdSearch.of(args);
    assertEquals(2, test.size());
    assertEquals(Sets.newHashSet(_id11, _id12), test.getExternalIds());
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
    assertEquals(0, test.size());
  }

  public void test_constructor_Iterable_two() {
    ExternalIdSearch test = ExternalIdSearch.of(Arrays.asList(_id11, _id12));
    assertEquals(2, test.size());
    assertEquals(Sets.newHashSet(_id11, _id12), test.getExternalIds());
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
    assertEquals(0, test.size());
    assertEquals(ExternalIdSearchType.EXACT, test.getSearchType());
  }

  public void test_constructor_IterableType_two() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.EXACT, Arrays.asList(_id11, _id12));
    assertEquals(2, test.size());
    assertEquals(Sets.newHashSet(_id11, _id12), test.getExternalIds());
    assertEquals(ExternalIdSearchType.EXACT, test.getSearchType());
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
    assertEquals(1, base.size());
    ExternalIdSearch test = base.withExternalIdAdded(ExternalId.of("A", "C"));
    assertEquals(1, base.size());
    assertEquals(2, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
  }

  public void test_withExternalIdAdded_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertThrows(() -> test.withExternalIdAdded(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withExternalIdsAdded_array() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals(1, base.size());
    ExternalIdSearch test = base.withExternalIdsAdded(ExternalId.of("A", "C"), ExternalId.of("D", "E"));
    assertEquals(1, base.size());
    assertEquals(3, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("D", "E")));
  }

  public void test_withExternalIdsAdded_array_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertThrows(() -> test.withExternalIdsAdded((ExternalId[]) null), IllegalArgumentException.class);
  }

  public void test_withExternalIdsAdded_iterable() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals(1, base.size());
    ExternalIdSearch test = base.withExternalIdsAdded(Arrays.asList(ExternalId.of("A", "C"), ExternalId.of("D", "E")));
    assertEquals(1, base.size());
    assertEquals(3, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "C")));
    assertTrue(test.getExternalIds().contains(ExternalId.of("D", "E")));
  }

  public void test_withExternalIdsAdded_iterable_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertThrows(() -> test.withExternalIdsAdded((Iterable<ExternalId>) null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withExternalIdRemoved_match() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals(1, base.size());
    ExternalIdSearch test = base.withExternalIdRemoved(ExternalId.of("A", "B"));
    assertEquals(1, base.size());
    assertEquals(0, test.size());
  }

  public void test_withExternalIdRemoved_noMatch() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals(1, base.size());
    ExternalIdSearch test = base.withExternalIdRemoved(ExternalId.of("A", "C"));
    assertEquals(1, base.size());
    assertEquals(1, test.size());
    assertTrue(test.getExternalIds().contains(ExternalId.of("A", "B")));
  }

  public void test_withExternalIdRemoved_null() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertThrows(() -> base.withExternalIdRemoved(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withSearchType() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertEquals(ExternalIdSearchType.ANY, base.getSearchType());
    ExternalIdSearch test = base.withSearchType(ExternalIdSearchType.EXACT);
    assertEquals(ExternalIdSearchType.EXACT, test.getSearchType());
  }

  public void test_withSearchType_same() {
    ExternalIdSearch base = ExternalIdSearch.of(ExternalIdSearchType.ALL, ExternalId.of("A", "B"));
    assertSame(base, base.withSearchType(ExternalIdSearchType.ALL));
  }

  public void test_withSearchType_null() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalId.of("A", "B"));
    assertThrows(() -> test.withSearchType(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_size() {
    assertEquals(0, ExternalIdSearch.of().size());
    assertEquals(1, ExternalIdSearch.of(_id11).size());
    assertEquals(2, ExternalIdSearch.of(_id11, _id12).size());
  }

  //-------------------------------------------------------------------------
  public void test_matches1_EXACT() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.EXACT, _id11);
    assertEquals(true, test1.matches(_id11));
    assertEquals(false, test1.matches(_id21));
    assertEquals(false, test1.alwaysMatches());
    
    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.EXACT, _id11, _id21);
    assertEquals(false, test2.matches(_id11));
    assertEquals(false, test2.matches(_id12));
    assertEquals(false, test2.matches(_id21));
    assertEquals(false, test2.alwaysMatches());
  }

  public void test_matches1_ALL() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.ALL, _id11);
    assertEquals(true, test1.matches(_id11));
    assertEquals(false, test1.matches(_id12));
    assertEquals(false, test1.alwaysMatches());
    
    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.ALL, _id11, _id21);
    assertEquals(false, test2.matches(_id11));
    assertEquals(false, test2.matches(_id12));
    assertEquals(false, test2.matches(_id21));
    assertEquals(false, test2.alwaysMatches());
  }

  public void test_matches1_ANY() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.ANY, _id11);
    assertEquals(true, test1.matches(_id11));
    assertEquals(false, test1.matches(_id12));
    assertEquals(false, test1.alwaysMatches());
    
    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.ANY, _id11, _id21);
    assertEquals(true, test2.matches(_id11));
    assertEquals(false, test2.matches(_id12));
    assertEquals(true, test2.matches(_id21));
    assertEquals(false, test2.alwaysMatches());
  }

  public void test_matches1_NONE() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11);
    assertEquals(false, test1.matches(_id11));
    assertEquals(true, test1.matches(_id12));
    assertEquals(false, test1.alwaysMatches());
    
    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11, _id21);
    assertEquals(false, test2.matches(_id11));
    assertEquals(true, test2.matches(_id12));
    assertEquals(false, test2.matches(_id21));
    assertEquals(false, test2.alwaysMatches());
    
    ExternalIdSearch test3 = ExternalIdSearch.of(ExternalIdSearchType.NONE);
    assertEquals(true, test3.matches(_id11));
    assertEquals(true, test3.matches(_id12));
    assertEquals(true, test3.matches(_id21));
    assertEquals(true, test3.alwaysMatches());
  }

  public void test_matches1_OTHER() {
    ExternalIdSearch test1 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11);
    assertEquals(false, test1.matches(_id11));
    assertEquals(true, test1.matches(_id12));
    assertEquals(false, test1.alwaysMatches());
    
    ExternalIdSearch test2 = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11, _id21);
    assertEquals(false, test2.matches(_id11));
    assertEquals(true, test2.matches(_id12));
    assertEquals(false, test2.matches(_id21));
    assertEquals(false, test2.alwaysMatches());
    
    ExternalIdSearch test3 = ExternalIdSearch.of(ExternalIdSearchType.NONE);
    assertEquals(true, test3.matches(_id11));
    assertEquals(true, test3.matches(_id12));
    assertEquals(true, test3.matches(_id21));
    assertEquals(true, test3.alwaysMatches());
  }

  //-------------------------------------------------------------------------
  public void test_matches_EXACT() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.EXACT, _id11, _id12);
    assertEquals(true, test.matches(ImmutableSet.of(_id11, _id12)));
    assertEquals(false, test.matches(ImmutableSet.of(_id11, _id12, _id21)));
    assertEquals(false, test.matches(ImmutableSet.of(_id11)));
    assertEquals(false, test.matches(ImmutableSet.of(_id12)));
    assertEquals(false, test.matches(ImmutableSet.of(_id21)));
    assertEquals(false, test.matches(ImmutableSet.of()));
  }

  public void test_matches_ALL() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ALL, _id11, _id12);
    assertEquals(true, test.matches(ImmutableSet.of(_id11, _id12)));
    assertEquals(true, test.matches(ImmutableSet.of(_id11, _id12, _id21)));
    assertEquals(false, test.matches(ImmutableSet.of(_id11)));
    assertEquals(false, test.matches(ImmutableSet.of(_id12)));
    assertEquals(false, test.matches(ImmutableSet.of(_id21)));
    assertEquals(false, test.matches(ImmutableSet.of()));
  }

  public void test_matches_ANY() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ANY, _id11, _id12);
    assertEquals(true, test.matches(ImmutableSet.of(_id11, _id12)));
    assertEquals(true, test.matches(ImmutableSet.of(_id11, _id12, _id21)));
    assertEquals(true, test.matches(ImmutableSet.of(_id11)));
    assertEquals(true, test.matches(ImmutableSet.of(_id12)));
    assertEquals(false, test.matches(ImmutableSet.of(_id21)));
    assertEquals(false, test.matches(ImmutableSet.of()));
  }

  public void test_matches_NONE() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.NONE, _id11, _id12);
    assertEquals(false, test.matches(ImmutableSet.of(_id11, _id12)));
    assertEquals(false, test.matches(ImmutableSet.of(_id11, _id12, _id21)));
    assertEquals(false, test.matches(ImmutableSet.of(_id11)));
    assertEquals(false, test.matches(ImmutableSet.of(_id12)));
    assertEquals(true, test.matches(ImmutableSet.of(_id21)));
    assertEquals(true, test.matches(ImmutableSet.of()));
  }

  //-------------------------------------------------------------------------
  public void test_containsAll1() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11);
    assertEquals(false, test.containsAll(ImmutableSet.of(_id11, _id12)));
    assertEquals(true, test.containsAll(ImmutableSet.of(_id11)));
    assertEquals(false, test.containsAll(ImmutableSet.of(_id12)));
    assertEquals(false, test.containsAll(ImmutableSet.of(_id21)));
    assertEquals(true, test.containsAll(ImmutableSet.of()));
  }

  public void test_containsAll2() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals(true, test.containsAll(ImmutableSet.of(_id11, _id12)));
    assertEquals(true, test.containsAll(ImmutableSet.of(_id11)));
    assertEquals(true, test.containsAll(ImmutableSet.of(_id12)));
    assertEquals(false, test.containsAll(ImmutableSet.of(_id21)));
    assertEquals(true, test.containsAll(ImmutableSet.of()));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_containsAll_null() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    test.containsAll(null);
  }

  //-------------------------------------------------------------------------
  public void test_containsAny() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals(true, test.containsAny(ImmutableSet.of(_id11, _id12)));
    assertEquals(true, test.containsAny(ImmutableSet.of(_id11)));
    assertEquals(true, test.containsAny(ImmutableSet.of(_id12)));
    assertEquals(false, test.containsAny(ImmutableSet.of(_id21)));
    assertEquals(false, test.containsAny(ImmutableSet.of()));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_containsAny_null() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    test.containsAny(null);
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals(true, test.contains(_id11));
    assertEquals(true, test.contains(_id11));
    assertEquals(false, test.contains(_id21));
  }

  public void test_contains_null() {
    ExternalIdSearch test = ExternalIdSearch.of(_id11, _id12);
    assertEquals(false, test.contains(null));
  }

  //-------------------------------------------------------------------------
  public void test_canMatch_EXACT() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.EXACT);
    assertEquals(false, test.canMatch());
    test = test.withExternalIdAdded(_id11);
    assertEquals(true, test.canMatch());
  }

  public void test_canMatch_ALL() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ALL);
    assertEquals(false, test.canMatch());
    test = test.withExternalIdAdded(_id11);
    assertEquals(true, test.canMatch());
  }

  public void test_canMatch_ANY() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.ANY);
    assertEquals(false, test.canMatch());
    test = test.withExternalIdAdded(_id11);
    assertEquals(true, test.canMatch());
  }

  public void test_canMatch_NONE() {
    ExternalIdSearch test = ExternalIdSearch.of(ExternalIdSearchType.NONE);
    assertEquals(true, test.canMatch());
    test = test.withExternalIdAdded(_id11);
    assertEquals(true, test.canMatch());
  }

  //-------------------------------------------------------------------------
  public void test_equals_same_empty() {
    ExternalIdSearch a1 = ExternalIdSearch.of();
    ExternalIdSearch a2 = ExternalIdSearch.of();
    
    assertEquals(true, a1.equals(a1));
    assertEquals(true, a1.equals(a2));
    assertEquals(true, a2.equals(a1));
    assertEquals(true, a2.equals(a2));
  }

  public void test_equals_same_nonEmpty() {
    ExternalIdSearch a1 = ExternalIdSearch.of(_id11, _id12);
    ExternalIdSearch a2 = ExternalIdSearch.of(_id11, _id12);
    
    assertEquals(true, a1.equals(a1));
    assertEquals(true, a1.equals(a2));
    assertEquals(true, a2.equals(a1));
    assertEquals(true, a2.equals(a2));
  }

  public void test_equals_different() {
    ExternalIdSearch a = ExternalIdSearch.of();
    ExternalIdSearch b = ExternalIdSearch.of(_id11, _id12);
    
    assertEquals(true, a.equals(a));
    assertEquals(false, a.equals(b));
    assertEquals(false, b.equals(a));
    assertEquals(true, b.equals(b));
    
    assertEquals(false, b.equals("Rubbish"));
    assertEquals(false, b.equals(null));
  }

  public void test_hashCode() {
    ExternalIdSearch a = ExternalIdSearch.of(_id11, _id12);
    ExternalIdSearch b = ExternalIdSearch.of(_id11, _id12);
    
    assertEquals(a.hashCode(), b.hashCode());
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
    assertEquals(4, ExternalIdSearchType.values().length);
    assertEquals(ExternalIdSearchType.ANY, ExternalIdSearchType.valueOf("ANY"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ExternalIdSearch.of(_id11, _id12));
  }

}
