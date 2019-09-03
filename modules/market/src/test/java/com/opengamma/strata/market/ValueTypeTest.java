/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ValueType}.
 */
public class ValueTypeTest {

  @Test
  public void test_validation() {
    assertThatIllegalArgumentException().isThrownBy(() -> ValueType.of(null));
    assertThatIllegalArgumentException().isThrownBy(() -> ValueType.of(""));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueType.of("Foo Bar"))
        .withMessageMatching(".*must only contain the characters.*");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueType.of("Foo_Bar"))
        .withMessageMatching(".*must only contain the characters.*");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ValueType.of("FooBar!"))
        .withMessageMatching(".*must only contain the characters.*");

    // these should execute without throwing an exception
    ValueType.of("FooBar");
    ValueType.of("Foo-Bar");
    ValueType.of("123");
    ValueType.of("FooBar123");
  }

  //-----------------------------------------------------------------------
  @Test
  public void checkEquals() {
    ValueType test = ValueType.of("Foo");
    test.checkEquals(test, "Error");
    assertThatIllegalArgumentException().isThrownBy(() -> test.checkEquals(ValueType.PRICE_INDEX, "Error"));
  }

  //-----------------------------------------------------------------------
  @Test
  public void coverage() {
    ValueType test = ValueType.of("Foo");
    assertThat(test.toString()).isEqualTo("Foo");
    assertSerialization(test);
    assertJodaConvert(ValueType.class, test);
  }

}
