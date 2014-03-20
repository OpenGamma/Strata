/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.function.scenarios.marketdata;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class MarketDataMatcherTest {

  private static final String SCHEME = "scheme";
  private static final String VALUE1 = "value1";
  private static final String VALUE2 = "value2";
  private static final String VALUE3 = "value3";
  private static final ExternalId ID1 = ExternalId.of(SCHEME, VALUE1);
  private static final ExternalId ID2 = ExternalId.of(SCHEME, VALUE2);
  private static final ExternalId ID3 = ExternalId.of(SCHEME, VALUE3);
  private static final ExternalIdBundle BUNDLE1 = ExternalIdBundle.of(MarketDataMatcherTest.SCHEME, MarketDataMatcherTest.VALUE1);
  private static final ExternalIdBundle BUNDLE2 = ExternalIdBundle.of(SCHEME, VALUE2);
  private static final ExternalIdBundle BUNDLE3 = ExternalIdBundle.of(SCHEME, VALUE3);

  @Test
  public void idEquals() {
    MarketDataMatcher matcher = MarketDataMatcher.idEquals(SCHEME, VALUE1);
    assertTrue(matcher.matches(BUNDLE1));
    assertFalse(matcher.matches(BUNDLE2));
    ExternalIdBundle id = ExternalIdBundle.of(ID1, ID2);
    assertTrue(matcher.matches(id));
  }

  @Test
  public void idsEqual() {
    MarketDataMatcher matcher = MarketDataMatcher.idEquals(SCHEME, VALUE1, VALUE2);
    assertTrue(matcher.matches(BUNDLE1));
    assertTrue(matcher.matches(BUNDLE2));
    assertFalse(matcher.matches(BUNDLE3));
    assertTrue(matcher.matches(ExternalIdBundle.of(ID1, ID3)));
  }

  @Test
  public void idLike() {
    MarketDataMatcher valMatcher = MarketDataMatcher.idLike(SCHEME, "val*");
    MarketDataMatcher valueMatcher = MarketDataMatcher.idLike(SCHEME, "value?");
    MarketDataMatcher oneMatcher = MarketDataMatcher.idLike(SCHEME, "*1");
    ExternalId otherId = ExternalId.of(SCHEME, "otherValue");

    assertTrue(valMatcher.matches(BUNDLE1));
    assertTrue(valMatcher.matches(BUNDLE2));
    assertTrue(valMatcher.matches(BUNDLE3));
    assertTrue(valMatcher.matches(ExternalIdBundle.of(otherId, ID1)));
    assertFalse(valMatcher.matches(ExternalIdBundle.of(otherId)));

    assertTrue(valueMatcher.matches(BUNDLE1));
    assertTrue(valueMatcher.matches(BUNDLE2));
    assertTrue(valueMatcher.matches(BUNDLE3));
    assertTrue(valueMatcher.matches(ExternalIdBundle.of(otherId, ID1)));
    assertFalse(valueMatcher.matches(ExternalIdBundle.of(otherId)));

    assertTrue(oneMatcher.matches(BUNDLE1));
    assertFalse(oneMatcher.matches(BUNDLE2));
    assertFalse(oneMatcher.matches(BUNDLE3));
    assertTrue(oneMatcher.matches(ExternalIdBundle.of(ID1, ID2)));
    assertFalse(oneMatcher.matches(ExternalIdBundle.of(ID3, ID2)));
  }
}
