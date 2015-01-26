/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.id;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.collect.result.Failure;
import com.opengamma.collect.result.Result;

/**
 * Test.
 */
@Test
public class LinkResolutionExceptionTest {

  public void test_constructor_LinkString() {
    Link<?> link = Link.resolved(MockIdentifiable.MOCK1);
    LinkResolutionException test = new LinkResolutionException(link, "Msg");
    assertThat(test).isNotNull();
    assertThat(test.getMessage()).isNotNull();
  }

  public void test_constructor_LinkString_nullCause() {
    Link<?> link = Link.resolved(MockIdentifiable.MOCK1);
    LinkResolutionException test = new LinkResolutionException(link, (String) null);
    assertThat(test).isNotNull();
    assertThat(test.getMessage()).isNotNull();
  }

  //-------------------------------------------------------------------------
  public void test_constructor_LinkCause() {
    Link<?> link = Link.resolved(MockIdentifiable.MOCK1);
    Failure cause = Result.failure(new RuntimeException()).getFailure();
    LinkResolutionException test = new LinkResolutionException(link, cause);
    assertThat(test).isNotNull();
    assertThat(test.getMessage()).isNotNull();
  }

  public void test_constructor_LinkCause_nullCause() {
    Link<?> link = Link.resolved(MockIdentifiable.MOCK1);
    LinkResolutionException test = new LinkResolutionException(link, (Failure) null);
    assertThat(test).isNotNull();
    assertThat(test.getMessage()).isNotNull();
  }

}
