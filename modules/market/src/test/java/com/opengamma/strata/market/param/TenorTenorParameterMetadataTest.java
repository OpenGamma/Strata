/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_20Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_30Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_40Y;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link TenorTenorParameterMetadata}.
 */
@Test
public class TenorTenorParameterMetadataTest {

  //-------------------------------------------------------------------------
  public void test_of_noLabel() {
    TenorTenorParameterMetadata test = TenorTenorParameterMetadata.of(TENOR_10Y, TENOR_20Y, 10.0d, 20.0d);
    assertEquals(test.getExpiryTenor(), TENOR_10Y);
    assertEquals(test.getExpiryYearFraction(), 10.0d);
    assertEquals(test.getUnderlyingTenor(), TENOR_20Y);
    assertEquals(test.getUnderlyingTenorYearFraction(), 20.0d);

    assertEquals(test.getIdentifier(), Pair.of(TENOR_10Y, TENOR_20Y));
    assertEquals(test.getLabel(), "[10Y, 20Y]");
  }

  public void test_of_label() {
    TenorTenorParameterMetadata test = TenorTenorParameterMetadata.of(
        TENOR_10Y,
        TENOR_20Y,
        10.0d,
        20.0d,
        "10Y to 20Y");
    assertEquals(test.getExpiryTenor(), TENOR_10Y);
    assertEquals(test.getExpiryYearFraction(), 10.0d);
    assertEquals(test.getUnderlyingTenor(), TENOR_20Y);
    assertEquals(test.getUnderlyingTenorYearFraction(), 20.0d);

    assertEquals(test.getIdentifier(), Pair.of(TENOR_10Y, TENOR_20Y));
    assertEquals(test.getLabel(), "10Y to 20Y");
  }

  public void test_builder_defaultLabel() {
    BeanBuilder<? extends TenorTenorParameterMetadata> builder = TenorTenorParameterMetadata.meta().builder();
    builder.set(TenorTenorParameterMetadata.meta().expiryTenor(), TENOR_10Y);
    builder.set(TenorTenorParameterMetadata.meta().expiryYearFraction(), 10.0d);
    builder.set(TenorTenorParameterMetadata.meta().underlyingTenor(), TENOR_20Y);
    builder.set(TenorTenorParameterMetadata.meta().underlyingTenorYearFraction(), 20.0d);

    TenorTenorParameterMetadata test = builder.build();
    assertEquals(test.getExpiryTenor(), TENOR_10Y);
    assertEquals(test.getExpiryYearFraction(), 10.0d);
    assertEquals(test.getUnderlyingTenor(), TENOR_20Y);
    assertEquals(test.getUnderlyingTenorYearFraction(), 20.0d);

    assertEquals(test.getIdentifier(), Pair.of(TENOR_10Y, TENOR_20Y));
    assertEquals(test.getLabel(), "[10Y, 20Y]");
  }

  public void test_builder_specifyLabel() {
    BeanBuilder<? extends TenorTenorParameterMetadata> builder = TenorTenorParameterMetadata.meta().builder();
    builder.set(TenorTenorParameterMetadata.meta().expiryTenor(), TENOR_10Y);
    builder.set(TenorTenorParameterMetadata.meta().expiryYearFraction(), 10.0d);
    builder.set(TenorTenorParameterMetadata.meta().underlyingTenor(), TENOR_20Y);
    builder.set(TenorTenorParameterMetadata.meta().underlyingTenorYearFraction(), 20.0d);
    builder.set(TenorTenorParameterMetadata.meta().label(), "10Y to 20Y");

    TenorTenorParameterMetadata test = builder.build();
    assertEquals(test.getExpiryTenor(), TENOR_10Y);
    assertEquals(test.getExpiryYearFraction(), 10.0d);
    assertEquals(test.getUnderlyingTenor(), TENOR_20Y);
    assertEquals(test.getUnderlyingTenorYearFraction(), 20.0d);

    assertEquals(test.getIdentifier(), Pair.of(TENOR_10Y, TENOR_20Y));
    assertEquals(test.getLabel(), "10Y to 20Y");
  }

  public void test_builder_incomplete() {
    BeanBuilder<? extends TenorTenorParameterMetadata> builder = TenorTenorParameterMetadata.meta().builder();
    assertThrowsIllegalArg(builder::build);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    TenorTenorParameterMetadata test = TenorTenorParameterMetadata.of(TENOR_10Y, TENOR_20Y, 10.0d, 20.0d);
    coverImmutableBean(test);
    TenorTenorParameterMetadata test2 = TenorTenorParameterMetadata.of(TENOR_30Y, TENOR_40Y, 30.0d, 40.0d);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    TenorTenorParameterMetadata test = TenorTenorParameterMetadata.of(TENOR_10Y, TENOR_20Y, 10.0d, 20.0d);
    assertSerialization(test);
  }

}
