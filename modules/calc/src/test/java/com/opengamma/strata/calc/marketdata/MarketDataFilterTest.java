/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.NamedMarketDataId;

/**
 * Test {@link MarketDataFilter}.
 */
public class MarketDataFilterTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @Test
  public void test_ofIdType() {
    MarketDataFilter<String, MarketDataId<String>> test = MarketDataFilter.ofIdType(TestId.class);
    assertThat(test.getMarketDataIdType()).isEqualTo(TestId.class);
    assertThat(test.matches(new TestId("a"), null, REF_DATA)).isTrue();
  }

  @Test
  public void test_ofId() {
    MarketDataFilter<String, MarketDataId<String>> test = MarketDataFilter.ofId(new TestId("a"));
    assertThat(test.getMarketDataIdType()).isEqualTo(TestId.class);
    assertThat(test.matches(new TestId("a"), null, REF_DATA)).isTrue();
    assertThat(test.matches(new TestId("b"), null, REF_DATA)).isFalse();
  }

  @Test
  public void test_ofName() {
    MarketDataFilter<String, NamedMarketDataId<String>> test = MarketDataFilter.ofName(new TestingName("a"));
    assertThat(test.getMarketDataIdType()).isEqualTo(NamedMarketDataId.class);
    assertThat(test.matches(new TestingNamedId("a"), null, REF_DATA)).isTrue();
    assertThat(test.matches(new TestingNamedId("b"), null, REF_DATA)).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    MarketDataFilter<String, MarketDataId<String>> test1 = MarketDataFilter.ofIdType(TestId.class);
    coverImmutableBean((ImmutableBean) test1);
    MarketDataFilter<String, MarketDataId<String>> test2 = MarketDataFilter.ofId(new TestId("a"));
    coverImmutableBean((ImmutableBean) test2);
    MarketDataFilter<String, NamedMarketDataId<String>> test3 = MarketDataFilter.ofName(new TestingName("a"));
    coverImmutableBean((ImmutableBean) test3);
  }

}
