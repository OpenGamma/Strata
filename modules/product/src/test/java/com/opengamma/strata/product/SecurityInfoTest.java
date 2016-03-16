/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link SecurityInfo}.
 */
@Test
public class SecurityInfoTest {

  private static final SecurityId ID = SecurityId.of("OG-Test", "Test");
  private static final SecurityId ID2 = SecurityId.of("OG-Test", "Test2");
  private static final ImmutableMap<SecurityInfoType<?>, Object> INFO_MAP = ImmutableMap.of(SecurityInfoType.NAME, "A");

  //-------------------------------------------------------------------------
  public void test_of() {
    SecurityInfo test = SecurityInfo.of(ID);
    assertEquals(test.getId(), ID);
    assertEquals(test.getInfo(), ImmutableMap.of());
    assertThrowsIllegalArg(() -> test.getInfo(SecurityInfoType.NAME));
    assertEquals(test.findInfo(SecurityInfoType.NAME), Optional.empty());
  }

  public void test_of_withMap() {
    SecurityInfo test = SecurityInfo.of(ID, INFO_MAP);
    assertEquals(test.getId(), ID);
    assertEquals(test.getInfo(), INFO_MAP);
    assertEquals(test.getInfo(SecurityInfoType.NAME), "A");
    assertEquals(test.findInfo(SecurityInfoType.NAME), Optional.of("A"));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SecurityInfo test = SecurityInfo.of(ID);
    coverImmutableBean(test);
    SecurityInfo test2 = SecurityInfo.of(ID2);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    SecurityInfo test = SecurityInfo.of(ID);
    assertSerialization(test);
  }

}
