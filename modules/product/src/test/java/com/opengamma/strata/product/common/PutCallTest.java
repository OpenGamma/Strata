/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.util.Locale;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link PutCall}.
 */
@Test
public class PutCallTest {

  //-------------------------------------------------------------------------
  public void test_ofPut() {
    assertEquals(PutCall.ofPut(true), PutCall.PUT);
    assertEquals(PutCall.ofPut(false), PutCall.CALL);
  }

  public void test_isPut() {
    assertEquals(PutCall.PUT.isPut(), true);
    assertEquals(PutCall.CALL.isPut(), false);
  }

  public void test_isCall() {
    assertEquals(PutCall.PUT.isCall(), false);
    assertEquals(PutCall.CALL.isCall(), true);
  }

  public void test_opposite() {
    assertEquals(PutCall.PUT.opposite(), PutCall.CALL);
    assertEquals(PutCall.CALL.opposite(), PutCall.PUT);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {PutCall.PUT, "Put"},
        {PutCall.CALL, "Call"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(PutCall convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(PutCall convention, String name) {
    assertEquals(PutCall.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupUpperCase(PutCall convention, String name) {
    assertEquals(PutCall.of(name.toUpperCase(Locale.ENGLISH)), convention);
  }

  @Test(dataProvider = "name")
  public void test_of_lookupLowerCase(PutCall convention, String name) {
    assertEquals(PutCall.of(name.toLowerCase(Locale.ENGLISH)), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PutCall.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PutCall.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(PutCall.class);
  }

  public void test_serialization() {
    assertSerialization(PutCall.PUT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(PutCall.class, PutCall.PUT);
  }

}
