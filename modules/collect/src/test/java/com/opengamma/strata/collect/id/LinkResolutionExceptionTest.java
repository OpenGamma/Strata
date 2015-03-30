/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class LinkResolutionExceptionTest {

  public void test_constructor_LinkString() {
    LinkResolutionException test = new LinkResolutionException("Msg");
    assertThat(test).isNotNull();
    assertThat(test.getMessage()).isNotNull();
  }

  public void test_constructor_LinkString_nullCause() {
    LinkResolutionException test = new LinkResolutionException((String) null);
    assertThat(test).isNotNull();
    assertThat(test.getMessage()).isNull();
  }

}
