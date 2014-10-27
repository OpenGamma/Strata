package com.opengamma.platform.source;

import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;
import com.opengamma.platform.source.id.StandardIdentifiable;

@Test
public class SearchTest {

  public void schemeCannotBeNull() {
    assertThrows(() -> new SearchBuilder(null), IllegalArgumentException.class);
  }

  public void matchingTypesAreOk() {
    Search search = new SearchBuilder("some_scheme")
        .withSuperType(StandardIdentifiable.class)
        .withSpecificType(TesterIdentifiable.class)
        .build();
    assertThat(search).isNotNull();
  }

  public void nullTypesAreIgnored() {
    Search search = new SearchBuilder("some_scheme")
        .withSuperType(null)
        .withSpecificType(null)
        .build();
    assertThat(search).isNotNull();
  }

  public void specificTypeMustNotBeInterface() {
    assertThrows(() ->
        new SearchBuilder("some_scheme")
            .withSpecificType(IdentifiableBean.class)
            .build(),
        IllegalArgumentException.class);
  }

  public void mismatchedTypesThrowException() {
    assertThrows(() ->
        new SearchBuilder("some_scheme")
            .withSuperType(String.class)
            .withSpecificType(TesterIdentifiable.class)
            .build(),
        IllegalArgumentException.class);
  }

  public void attributeNameCannotBeNull() {
    assertThrows(() ->
        new SearchBuilder("some_scheme")
            .withAttribute(null, "123")
            .build(),
        IllegalArgumentException.class);
 }

  public void attributeValueCannotBeNull() {
    assertThrows(() ->
        new SearchBuilder("some_scheme")
             .withAttribute("a1", null)
             .build(),
        IllegalArgumentException.class);
  }

  public void searchCanValidateAgainstScheme() {

    Search search = new SearchBuilder("some_scheme").build();
    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();
    IdentifiableBean ib2 = TesterIdentifiable.builder()
        .standardId(StandardId.of("another_scheme", "1234"))
        .build();

    assertThat(search.validateItem(ib1)).isTrue();
    assertThat(search.validateItem(ib2)).isFalse();
  }

  public void searchCanValidateAgainstSuperclass() {

    Search search = new SearchBuilder("some_scheme")
        .withSuperType(Tester.class)
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    IdentifiableBean ib2 = NonTesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    assertThat(search.validateItem(ib1)).isTrue();
    assertThat(search.validateItem(ib2)).isFalse();
  }

  public void searchCanValidateAgainstSpecificClass() {

    Search search = new SearchBuilder("some_scheme")
        .withSpecificType(TesterIdentifiable.class)
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    IdentifiableBean ib2 = NonTesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    assertThat(search.validateItem(ib1)).isTrue();
    assertThat(search.validateItem(ib2)).isFalse();
  }

  public void searchCanValidateAgainstSuperclassAndSpecificClass() {

    Search search = new SearchBuilder("some_scheme")
        .withSuperType(Tester.class)
        .withSpecificType(TesterIdentifiable.class)
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    IdentifiableBean ib2 = NonTesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    assertThat(search.validateItem(ib1)).isTrue();
    assertThat(search.validateItem(ib2)).isFalse();
  }

  public void searchCanValidateAgainstSingleAttribute() {

    Search search = new SearchBuilder("some_scheme")
        .withAttribute("widgetCount", "100")
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(100)
        .build();

    IdentifiableBean ib2 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(150)
        .build();

    IdentifiableBean ib3 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    assertThat(search.validateItem(ib1)).isTrue();
    assertThat(search.validateItem(ib2)).isFalse();
    assertThat(search.validateItem(ib3)).isFalse();
  }

  public void searchCanValidateAgainstMultipleAttribute() {

    Search search = new SearchBuilder("some_scheme")
        .withAttribute("widgetCount", "100")
        .withAttribute("name", "foo")
        .build();

    IdentifiableBean ib1 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(100)
        .name("foo")
        .build();

    IdentifiableBean ib2 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(150)
        .name("bar")
        .build();

    IdentifiableBean ib3 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(100)
        .name("bar")
        .build();

    IdentifiableBean ib4 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(100)
        // no name
        .build();

    IdentifiableBean ib5 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .widgetCount(150)
        .name("foo")
        .build();

    IdentifiableBean ib6 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        // no count
        .name("foo")
        .build();

    IdentifiableBean ib7 = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        // no count
        // no name
        .build();

    assertThat(search.validateItem(ib1)).isTrue();
    assertThat(search.validateItem(ib2)).isFalse();
    assertThat(search.validateItem(ib3)).isFalse();
    assertThat(search.validateItem(ib4)).isFalse();
    assertThat(search.validateItem(ib5)).isFalse();
    assertThat(search.validateItem(ib6)).isFalse();
    assertThat(search.validateItem(ib7)).isFalse();
  }

  public void coverage() {
    Search search = new SearchBuilder("some_scheme")
        .withAttribute("widgetCount", "100")
        .withAttribute("name", "foo")
        .build();
    coverImmutableBean(search);
  }

}
