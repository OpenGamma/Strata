/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit;

import com.opengamma.strata.collect.id.StandardId;
import org.testng.annotations.Test;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.finance.credit.RestructuringClause.NO_RESTRUCTURING_2014;
import static com.opengamma.strata.finance.credit.SeniorityLevel.SENIOR_UNSECURED_FOREIGN;
import static org.testng.Assert.assertEquals;

/**
 * Test.
 */
@Test
public class SingleNameReferenceInformationTest {

  public void test_of() {
    SingleNameReferenceInformation expected = SingleNameReferenceInformation.builder()
        .referenceEntityId(StandardId.of("Test", "Test1"))
        .seniority(SENIOR_UNSECURED_FOREIGN)
        .currency(USD)
        .restructuringClause(NO_RESTRUCTURING_2014)
        .build();
    assertEquals(sut(), expected);
  }

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> SingleNameReferenceInformation.builder().build());
  }


  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static SingleNameReferenceInformation sut() {
    return SingleNameReferenceInformation.of(
        StandardId.of("Test", "Test1"),
        SENIOR_UNSECURED_FOREIGN,
        USD,
        NO_RESTRUCTURING_2014);
  }

}
