package com.opengamma.sesame;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Tests {@link EngineTestUtils#compareMaps(Map, Map)}.
 */
public class MapCompareTest {

  @Test
  public void equal() {
    Map<Object, Object> m = map("a", 1, "b", 2);
    assertTrue(EngineTestUtils.compareMaps(m, m).isEmpty());
  }

  @Test
  public void equalNestedMaps() {
    Map<Object, Object> m = map("a", map("b", 2, "c", map("d", 4)));
    assertTrue(EngineTestUtils.compareMaps(m, m).isEmpty());
  }

  @Test
  public void equalLists() {
    Map<Object, Object> m = map("a", list(1, 2, 3), "b", list(2, 3, 4), "c", list(map("d", 4, "e", 5)));
    assertTrue(EngineTestUtils.compareMaps(m, m).isEmpty());
  }

  @Test
  public void differentMaps() {
    Map<Object, Object> m1 = map("a", 1, "b", 2);
    Map<Object, Object> m2 = map("a", 1, "b", 3);
    List<Object> expected = list(diff(2, 3, "b"));
    assertEquals(expected, EngineTestUtils.compareMaps(m1, m2));
  }

  @Test
  public void differentNestedMaps() {
    Map<Object, Object> m1 = map("a", 1, "b", map("c", 3));
    Map<Object, Object> m2 = map("a", 1, "b", map("c", 4));
    List<Object> expected = list(diff(3, 4, "b", "c"));
    assertEquals(expected, EngineTestUtils.compareMaps(m1, m2));
  }

  @Test
  public void multipleDifferences() {
    Map<Object, Object> m1 = map("a", 1, "b", map("c", 3), "d", map("e", 6));
    Map<Object, Object> m2 = map("a", 1, "b", map("c", 4), "d", map("e", 5));
    List<Object> expected = list(diff(3, 4, "b", "c"), diff(6, 5, "d", "e"));
    assertEquals(expected, EngineTestUtils.compareMaps(m1, m2));
  }

  @Test
  public void missingMapValues() {
    Map<Object, Object> m1 = map("a", 1, "b", 2);
    Map<Object, Object> m2 = map("a", 1);
    List<Object> expected = list(diff(2, null, "b"));
    assertEquals(expected, EngineTestUtils.compareMaps(m1, m2));
  }

  @Test
  public void differentLengthLists() {
    Map<Object, Object> m1 = map("a", list(1, 2, 3));
    Map<Object, Object> m2 = map("a", list(1, 2));
    List<Object> expected = list(diff(3, null, "a", 2));
    assertEquals(expected, EngineTestUtils.compareMaps(m1, m2));
  }

  private static Map<Object, Object> map(Object... keyVals) {
    ImmutableMap.Builder<Object, Object> builder = ImmutableMap.builder();

    for (int i = 0; i < keyVals.length / 2; i++) {
      builder.put(keyVals[2 * i], keyVals[(2 * i) + 1]);
    }
    return builder.build();
  }

  private static List<Object> list(Object... elements) {
    return ImmutableList.copyOf(elements);
  }

  private static EngineTestUtils.MapDifference diff(Object left, Object right, Object... path) {
    return new EngineTestUtils.MapDifference(left, right, Arrays.asList(path));
  }
}
