/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.curvedata;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class ExternalIdMapTest {

  private static final ExternalId ID1 = ExternalId.of("scheme", "1");
  private static final ExternalId ID2 = ExternalId.of("scheme", "2");
  private static final ExternalId ID3 = ExternalId.of("scheme", "3");
  private static final ExternalId ID4 = ExternalId.of("scheme", "4");

  @Test
  public void mappings() {
    ExternalIdBundle bundle1 = ExternalIdBundle.of(ID1, ID2);
    ExternalIdBundle bundle2 = ExternalIdBundle.of(ID3, ID4);
    String s1 = "s1";
    String s2 = "s2";
    Map<ExternalIdBundle, String> map = ImmutableMap.of(bundle1, s1, bundle2, s2);
    ExternalIdMap<String> idMap = new ExternalIdMap<>(map);

    assertEquals(s1, idMap.get(ID1));
    assertEquals(s1, idMap.get(ID2));
    assertEquals(s2, idMap.get(ID3));
    assertEquals(s2, idMap.get(ID4));

    assertEquals(bundle1, idMap.getBundle(ID1));
    assertEquals(bundle1, idMap.getBundle(ID2));
    assertEquals(bundle2, idMap.getBundle(ID3));
    assertEquals(bundle2, idMap.getBundle(ID4));

    assertEquals(s1, idMap.get(bundle1));
    assertEquals(s2, idMap.get(bundle2));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void overlappingBundles() {
    ExternalIdBundle bundle1 = ExternalIdBundle.of(ID1, ID2);
    ExternalIdBundle bundle2 = ExternalIdBundle.of(ID2, ID3);
    String s1 = "s1";
    String s2 = "s2";
    Map<ExternalIdBundle, String> map = ImmutableMap.of(bundle1, s1, bundle2, s2);
    new ExternalIdMap<>(map);
  }
}
