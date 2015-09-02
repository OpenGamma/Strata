/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;

/**
 * Test {@link FloatingRateName}.
 */
@Test
public class FloatingRateNameTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "nameType")
  static Object[][] data_name_type() {
    return new Object[][] {
        {FloatingRateNames.GBP_LIBOR_BBA, "GBP-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateNames.CHF_LIBOR_BBA, "CHF-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateNames.EUR_LIBOR_BBA, "EUR-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateNames.JPY_LIBOR_BBA, "JPY-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateNames.USD_LIBOR_BBA, "USD-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateNames.EUR_EURIBOR_REUTERS, "EUR-EURIBOR-Reuters", FloatingRateType.IBOR},
        {FloatingRateNames.JPY_TIBOR_TIBM, "JPY-TIBOR-TIBM", FloatingRateType.IBOR},

        {FloatingRateNames.GBP_WMBA_SONIA_COMPOUND, "GBP-WMBA-SONIA-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateNames.CHF_TOIS_OIS_COMPOUND, "CHF-TOIS-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateNames.EUR_EONIA_OIS_COMPOUND, "EUR-EONIA-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateNames.JPY_TONA_OIS_COMPOUND, "JPY-TONA-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateNames.USD_FEDERAL_FUNDS_H15_OIS_COMPOUND, "USD-Federal Funds-H.15-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateNames.USD_FEDERAL_FUNDS_H15, "USD-Federal Funds-H.15", FloatingRateType.OVERNIGHT_AVERAGED},
    };
  }

  @Test(dataProvider = "nameType")
  public void test_name(FloatingRateName convention, String name, FloatingRateType type) {
    assertEquals(convention.getName(), name);
    assertEquals(convention.getType(), type);
  }

  @Test(dataProvider = "nameType")
  public void test_toString(FloatingRateName convention, String name, FloatingRateType type) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "nameType")
  public void test_of_lookup(FloatingRateName convention, String name, FloatingRateType type) {
    assertEquals(FloatingRateName.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FloatingRateName.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FloatingRateName.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void test_toIborIndex_tenor() {
    assertEquals(FloatingRateNames.GBP_LIBOR_BBA.toIborIndex(Tenor.TENOR_6M), IborIndices.GBP_LIBOR_6M);
    assertThrows(() -> FloatingRateNames.GBP_WMBA_SONIA_COMPOUND.toIborIndex(Tenor.TENOR_6M), IllegalStateException.class);
  }

  public void test_toOvernightIndex() {
    assertEquals(FloatingRateNames.GBP_WMBA_SONIA_COMPOUND.toOvernightIndex(), OvernightIndices.GBP_SONIA);
    assertThrows(() -> FloatingRateNames.GBP_LIBOR_BBA.toOvernightIndex(), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FloatingRateNames.class);
    coverImmutableBean((ImmutableBean) FloatingRateNames.GBP_LIBOR_BBA);
    coverBeanEquals((Bean) FloatingRateNames.GBP_LIBOR_BBA, (Bean) FloatingRateNames.USD_FEDERAL_FUNDS_H15);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FloatingRateName.class, FloatingRateNames.GBP_LIBOR_BBA);
  }

  public void test_serialization() {
    assertSerialization(FloatingRateNames.GBP_LIBOR_BBA);
  }

}
