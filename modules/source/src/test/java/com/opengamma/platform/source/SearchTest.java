/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import static com.opengamma.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.id.IdentifiableBean;
import com.opengamma.collect.id.StandardId;
import com.opengamma.collect.id.StandardIdentifiable;

@Test
public class SearchTest {

  public void specifyingTypeIsOk() {
    Search search = Search.builder()
        .categorisingType(StandardIdentifiable.class)
        .build();
    assertThat(search).isNotNull();
  }

  public void typeCannotBeNull() {
    assertThrowsIllegalArg(() -> Search.builder().categorisingType(null).build());
  }

  public void attributesCannotBeNull() {
    assertThrowsIllegalArg(() -> Search.builder().attributes(null).build());
  }

  public void defaultSearchMatchesAll() {

    Search search = Search.builder().build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    IdentifiableBean ib2 = NonTesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "2345"))
        .build();

    assertThat(search.matches(ib1)).isTrue();
    assertThat(search.matches(ib2)).isTrue();
  }

  public void searchCanValidateAgainstSpecificCategorisingType() {

    Search search = Search.builder()
        .categorisingType(NonTesterIdentifiable.class)
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    IdentifiableBean ib2 = NonTesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "2345"))
        .build();

    assertThat(search.matches(ib1)).isFalse();
    assertThat(search.matches(ib2)).isTrue();
  }

  public void searchCanValidateAgainstGeneralCategorisingType() {

    Search search = Search.builder()
        .categorisingType(Tester.class)
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    IdentifiableBean ib2 = NonTesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "2345"))
        .build();

    assertThat(search.matches(ib1)).isTrue();
    assertThat(search.matches(ib2)).isFalse();
  }

  public void searchCanValidateAgainstSingleAttribute() {

    Search search = Search.builder()
        .attributes(ImmutableMap.of("widgetCount", "100"))
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(100)
        .build();

    IdentifiableBean ib2 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "2345"))
        .widgetCount(150)
        .build();

    IdentifiableBean ib3 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "3456"))
        .build();

    assertThat(search.matches(ib1)).isTrue();
    assertThat(search.matches(ib2)).isFalse();
    assertThat(search.matches(ib3)).isFalse();
  }

  public void searchCanValidateAgainstMultipleAttribute() {

    Search search = Search.builder()
        .attributes(ImmutableMap.of("widgetCount", "100", "name", "foo"))
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(100)
        .name("foo")
        .build();

    IdentifiableBean ib2 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "2345"))
        .widgetCount(150)
        .name("bar")
        .build();

    IdentifiableBean ib3 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "3456"))
        .widgetCount(100)
        .name("bar")
        .build();

    IdentifiableBean ib4 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "4567"))
        .widgetCount(100)
        // no name
        .build();

    IdentifiableBean ib5 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "5678"))
        .widgetCount(150)
        .name("foo")
        .build();

    IdentifiableBean ib6 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "6789"))
        // no count
        .name("foo")
        .build();

    IdentifiableBean ib7 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "7890"))
        // no count
        // no name
        .build();

    assertThat(search.matches(ib1)).isTrue();
    assertThat(search.matches(ib2)).isFalse();
    assertThat(search.matches(ib3)).isFalse();
    assertThat(search.matches(ib4)).isFalse();
    assertThat(search.matches(ib5)).isFalse();
    assertThat(search.matches(ib6)).isFalse();
    assertThat(search.matches(ib7)).isFalse();
  }

  public void coverage() {
    Search search = Search.builder()
        .attributes(ImmutableMap.of("widgetCount", "100", "name", "foo"))
        .build();
    coverImmutableBean(search);
  }

}
