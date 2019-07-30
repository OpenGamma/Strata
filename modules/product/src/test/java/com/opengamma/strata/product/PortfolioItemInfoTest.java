/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link PortfolioItemInfo}.
 */
public class PortfolioItemInfoTest {

  private static final StandardId ID = StandardId.of("OG-Test", "123");
  private static final StandardId ID2 = StandardId.of("OG-Test", "321");

  @Test
  public void test_of() {
    PortfolioItemInfo test = PortfolioItemInfo.of(AttributeType.DESCRIPTION, "A");
    assertThat(test.getId()).isEmpty();
    assertThat(test.getAttributeTypes()).isEqualTo(ImmutableSet.of(AttributeType.DESCRIPTION));
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("A");
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.NAME));
  }

  @Test
  public void test_withers() {
    PortfolioItemInfo test = PortfolioItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertThat(test.getId()).isEqualTo(Optional.of(ID));
    assertThat(test.getAttributeTypes()).isEqualTo(ImmutableSet.of(AttributeType.DESCRIPTION));
    assertThat(test.getAttribute(AttributeType.DESCRIPTION)).isEqualTo("A");
    assertThat(test.findAttribute(AttributeType.DESCRIPTION)).hasValue("A");
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.NAME));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ItemInfo test = ItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    coverImmutableBean(test);
    ItemInfo test2 = ItemInfo.empty()
        .withId(ID2);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ItemInfo test = ItemInfo.empty()
        .withId(ID)
        .withAttribute(AttributeType.DESCRIPTION, "A");
    assertSerialization(test);
  }

}
