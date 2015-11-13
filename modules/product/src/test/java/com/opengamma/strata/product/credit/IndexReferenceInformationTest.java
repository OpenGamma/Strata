/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class IndexReferenceInformationTest {

  public void test_of() {
    IndexReferenceInformation expected = IndexReferenceInformation.builder()
        .indexId(StandardId.of("Test", "Test1"))
        .indexSeries(32)
        .indexAnnexVersion(8)
        .build();
    assertEquals(sut(), expected);
  }

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> IndexReferenceInformation.builder().build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static IndexReferenceInformation sut() {
    return IndexReferenceInformation.of(
        StandardId.of("Test", "Test1"),
        32,
        8);
  }

}
