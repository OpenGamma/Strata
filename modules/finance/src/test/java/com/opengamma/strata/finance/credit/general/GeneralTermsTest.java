/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.credit.general;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.finance.credit.common.RedCode;
import com.opengamma.strata.finance.credit.general.reference.ReferenceInformationType;
import com.opengamma.strata.finance.credit.general.reference.SeniorityLevel;
import org.testng.annotations.Test;

import java.time.LocalDate;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

/**
 * Test.
 */
@Test
public class GeneralTermsTest {

  private LocalDate start = LocalDate.of(2014, 6, 20);
  private LocalDate end = LocalDate.of(2019, 6, 20);
  private BusinessDayAdjustment adjustment = BusinessDayAdjustment.of(
      BusinessDayConventions.FOLLOWING,
      HolidayCalendars.USNY.combineWith(HolidayCalendars.GBLO)
  );

  private GeneralTerms singleNameTest = makeSingleName();

  private GeneralTerms indexTest = makeIndex();

  private GeneralTerms makeSingleName() {
    return GeneralTerms.singleName(
        start,
        end,
        adjustment,
        RedCode.of("3H98A7"),
        "Ford Motor Company",
        Currency.USD,
        SeniorityLevel.SeniorUnSec
    );
  }

  private GeneralTerms makeIndex() {
    return GeneralTerms.index(
        start,
        end,
        adjustment,
        RedCode.of("2I65BYCL7"),
        "CDX.NA.IG.15",
        15,
        1
    );
  }

  public void test_builder_singlename() {
    assertEquals(singleNameTest.getEffectiveDate(), start);
    assertEquals(singleNameTest.getScheduledTerminationDate(), end);
    assertEquals(singleNameTest.getDateAdjustments(), adjustment);
    assertEquals(singleNameTest.getReferenceInformation().getType(), ReferenceInformationType.SINGLE_NAME);
  }

  public void test_builder_index() {
    assertEquals(indexTest.getEffectiveDate(), start);
    assertEquals(indexTest.getScheduledTerminationDate(), end);
    assertEquals(indexTest.getDateAdjustments(), adjustment);
    assertEquals(indexTest.getReferenceInformation().getType(), ReferenceInformationType.INDEX);
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> GeneralTerms.singleName(null, null, null, null, null, null, null));
    assertThrowsIllegalArg(() -> GeneralTerms.singleName(start, end, adjustment, null, null, null, null));
    assertThrowsIllegalArg(() -> GeneralTerms.index(null, null, null, null, null, 0, 0));
    assertThrowsIllegalArg(() -> GeneralTerms.index(start, end, adjustment, null, null, 0, 0));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(singleNameTest);
    GeneralTerms singleNameTest2 = makeSingleName();
    coverBeanEquals(singleNameTest, singleNameTest2);

    coverImmutableBean(indexTest);
    GeneralTerms indexTest2 = makeIndex();
    coverBeanEquals(indexTest, indexTest2);
  }

  public void test_serialization() {
    assertSerialization(singleNameTest);
    assertSerialization(indexTest);
  }

}
