/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class BidirectionalMultiMapTest {

  private BidirectionalMultiMap<String, Integer> _bidirectionalMultiMap;

  @BeforeMethod
  public void setUp() {
    _bidirectionalMultiMap = new BidirectionalMultiMap<>();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullKeysProhibited() {
    _bidirectionalMultiMap.put(null, 42);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullValuesProhibited() {
    _bidirectionalMultiMap.put("forty two", null);
  }

  @Test
  public void testValuesAreAccumulated() {
    _bidirectionalMultiMap.put("A", 1);
    _bidirectionalMultiMap.put("A", 2);
    _bidirectionalMultiMap.put("A", 3);
    assertThat(_bidirectionalMultiMap.get("A"), containsInAnyOrder(1, 2, 3));
  }

  @Test
  public void testDuplicateEntryIsIgnored() {
    _bidirectionalMultiMap.put("A", 1);
    _bidirectionalMultiMap.put("A", 2);
    _bidirectionalMultiMap.put("A", 2);
    assertThat(_bidirectionalMultiMap.get("A"), containsInAnyOrder(1, 2));
  }

  @Test
  public void testGetWithNullKeyReturnsEmpty() {
    assertThat(_bidirectionalMultiMap.get(null).isEmpty(), is(true));
  }

  @Test
  public void testMappingsAndInversesAreHeld() {

    _bidirectionalMultiMap.put("A", 1);
    _bidirectionalMultiMap.put("A", 2);
    _bidirectionalMultiMap.put("A", 3);
    _bidirectionalMultiMap.put("B", 2);
    _bidirectionalMultiMap.put("B", 3);
    _bidirectionalMultiMap.put("C", 3);

    assertThat(_bidirectionalMultiMap.get("A"), containsInAnyOrder(1, 2, 3));
    assertThat(_bidirectionalMultiMap.get("B"), containsInAnyOrder(2, 3));
    assertThat(_bidirectionalMultiMap.get("C"), containsInAnyOrder(3));


    assertThat(_bidirectionalMultiMap.inverse().get(1), containsInAnyOrder("A"));
    assertThat(_bidirectionalMultiMap.inverse().get(2), containsInAnyOrder("A", "B"));
    assertThat(_bidirectionalMultiMap.inverse().get(3), containsInAnyOrder("A", "B", "C"));
  }

  @Test
  public void testContainsKey() {

    _bidirectionalMultiMap.put("A", 1);
    _bidirectionalMultiMap.put("A", 2);
    _bidirectionalMultiMap.put("A", 3);
    _bidirectionalMultiMap.put("B", 2);
    _bidirectionalMultiMap.put("B", 3);
    _bidirectionalMultiMap.put("C", 3);

    assertThat(_bidirectionalMultiMap.containsKey("A"), is(true));
    assertThat(_bidirectionalMultiMap.containsKey("Z"), is(false));
    assertThat(_bidirectionalMultiMap.containsKey(null), is(false));

    assertThat(_bidirectionalMultiMap.inverse().containsKey(1), is(true));
    assertThat(_bidirectionalMultiMap.inverse().containsKey(3), is(true));
    assertThat(_bidirectionalMultiMap.inverse().containsKey(99), is(false));
  }

  @Test
  public void testKeysAreHeld() {

    _bidirectionalMultiMap.put("A", 1);
    _bidirectionalMultiMap.put("A", 2);
    _bidirectionalMultiMap.put("A", 3);
    _bidirectionalMultiMap.put("B", 2);
    _bidirectionalMultiMap.put("B", 3);
    _bidirectionalMultiMap.put("C", 3);

    assertThat(_bidirectionalMultiMap.keySet(), containsInAnyOrder("A", "B", "C"));
    assertThat(_bidirectionalMultiMap.inverse().keySet(), containsInAnyOrder(1, 2, 3));
  }

  @Test
  public void testRemoval() {

    _bidirectionalMultiMap.put("A", 1);
    _bidirectionalMultiMap.put("A", 2);
    _bidirectionalMultiMap.put("A", 3);
    _bidirectionalMultiMap.put("B", 2);
    _bidirectionalMultiMap.put("B", 3);
    _bidirectionalMultiMap.put("C", 3);

    assertThat(_bidirectionalMultiMap.size(), is(6));
    assertThat(_bidirectionalMultiMap.containsKey("A"), is(true));
    assertThat(_bidirectionalMultiMap.inverse().containsKey(1), is(true));
    assertThat(_bidirectionalMultiMap.get("A"), containsInAnyOrder(1, 2, 3));
    assertThat(_bidirectionalMultiMap.inverse().get(1), containsInAnyOrder("A"));

    _bidirectionalMultiMap.remove("A", 1);

    assertThat(_bidirectionalMultiMap.size(), is(5));
    assertThat(_bidirectionalMultiMap.containsKey("A"), is(true));
    assertThat(_bidirectionalMultiMap.inverse().containsKey(1), is(false));
    assertThat(_bidirectionalMultiMap.get("A"), containsInAnyOrder(2, 3));
    assertThat(_bidirectionalMultiMap.inverse().get(1).isEmpty(), is(true));
  }

  @Test
  public void testRemovalOfNonExistentElementDoesNothing() {

    _bidirectionalMultiMap.put("A", 1);
    _bidirectionalMultiMap.put("A", 2);
    _bidirectionalMultiMap.put("A", 3);
    _bidirectionalMultiMap.put("B", 2);
    _bidirectionalMultiMap.put("B", 3);
    _bidirectionalMultiMap.put("C", 3);

    assertThat(_bidirectionalMultiMap.size(), is(6));

    _bidirectionalMultiMap.remove("A", 99);
    _bidirectionalMultiMap.remove("Z", 1);
    _bidirectionalMultiMap.remove("Z", 99);

    assertThat(_bidirectionalMultiMap.size(), is(6));
  }

  @Test
  public void testInverseOfInverseIsOriginal() {
    // Want to test reference equality
    assertThat(_bidirectionalMultiMap.inverse().inverse() == _bidirectionalMultiMap, is(true));
  }

  @Test
  public void testInverseOfInverseOfInverseIsInverse() {
    // Want to test reference equality
    assertThat(_bidirectionalMultiMap.inverse() == _bidirectionalMultiMap.inverse().inverse().inverse(), is(true));
  }
}
