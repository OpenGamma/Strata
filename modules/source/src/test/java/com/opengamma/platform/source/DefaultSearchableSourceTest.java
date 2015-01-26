/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source;

import static com.opengamma.collect.TestHelper.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.collect.id.IdentifiableBean;
import com.opengamma.collect.id.StandardId;
import com.opengamma.collect.result.FailureReason;
import com.opengamma.collect.result.Result;

/**
 * Tests for the DefaultSearchableSource.
 */
@Test
public class DefaultSearchableSourceTest {

  public void constructionRejectsNulls() {
    assertThrows(() ->
        new DefaultSearchableSource(null, createEmptySearchProvider()),
        IllegalArgumentException.class);
    assertThrows(() ->
        new DefaultSearchableSource(createEmptySourceProvider(), null),
        IllegalArgumentException.class);
  }

  public void getOfMissingItemGivesFailureResult() {

    SearchableSource searchableSource = createEmptySearchableSource();

    Result<TesterIdentifiable> result =
        searchableSource.get(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getFailure().getReason()).isEqualTo(FailureReason.MISSING_DATA);
    assertThat(result.getFailure().getMessage()).contains("Unable to find data");
  }

  public void getOfWrongTypeGivesFailureResult() {

    StandardId id = StandardId.of("some_scheme", "1234");
    IdentifiableBean bean = TesterIdentifiable.builder().standardId(id).build();

    SearchableSource searchableSource = createSearchableSource(ImmutableMap.of(id, bean));

    Result<NonTesterIdentifiable> result =
        searchableSource.get(id, NonTesterIdentifiable.class);

    assertThat(result.isFailure()).isTrue();
    assertThat(result.getFailure().getReason()).isEqualTo(FailureReason.MISSING_DATA);
    assertThat(result.getFailure().getMessage()).contains("expected type was");
  }

  public void getWithCorrectTypeGivesSuccessResult() {

    StandardId id = StandardId.of("some_scheme", "1234");
    IdentifiableBean bean = TesterIdentifiable.builder().standardId(id).build();

    SearchableSource searchableSource = createSearchableSource(ImmutableMap.of(id, bean));

    Result<TesterIdentifiable> result =
        searchableSource.get(id, TesterIdentifiable.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.getValue()).isSameAs(bean);
  }

  public void idSearchWithNoMatches() {

    SearchableSource searchableSource = createEmptySearchableSource();
    ImmutableSet<StandardId> ids = ImmutableSet.of(
        StandardId.of("some_scheme", "1234"),
        StandardId.of("some_scheme", "2345"));

    ImmutableSet<? extends IdentifiableBean> result = searchableSource.search(ids);
    assertThat(result).isEmpty();
  }

  public void emptyIdSearch() {

    StandardId id1 = StandardId.of("some_scheme", "1234");
    StandardId id2 = StandardId.of("some_scheme", "2345");

    Map<StandardId, IdentifiableBean> sourceData = ImmutableMap.of(
        id1, TesterIdentifiable.builder().standardId(id1).build(),
        id2, TesterIdentifiable.builder().standardId(id2).build());

    SearchableSource searchableSource = createSearchableSource(sourceData);

    ImmutableSet<? extends IdentifiableBean> result = searchableSource.search(ImmutableSet.of());
    assertThat(result).isEmpty();
  }

  public void idSearchReturnsMatchingIds() {

    StandardId id1 = StandardId.of("some_scheme", "1234");
    StandardId id2 = StandardId.of("some_scheme", "2345");
    StandardId id3 = StandardId.of("some_scheme", "3456");
    StandardId id4 = StandardId.of("some_scheme", "4567");

    IdentifiableBean bean1 = TesterIdentifiable.builder().standardId(id1).build();
    IdentifiableBean bean2 = TesterIdentifiable.builder().standardId(id2).build();
    IdentifiableBean bean3 = TesterIdentifiable.builder().standardId(id3).build();

    Map<StandardId, IdentifiableBean> sourceData = ImmutableMap.of(
        id1, bean1,
        id2, bean2,
        id3, bean3);

    SearchableSource searchableSource = createSearchableSource(sourceData);

    Set<IdentifiableBean> result =
        searchableSource.search(ImmutableSet.of(id2, id3, id4));

    assertThat(result)
        .isNotEmpty()
        .contains(bean2, bean3)
        .doesNotContain(bean1);
  }

  public void fullMatchReturnsAllItems() {

    StandardId id1 = StandardId.of("some_scheme", "1234");
    StandardId id2 = StandardId.of("some_scheme", "2345");
    StandardId id3 = StandardId.of("some_scheme", "3456");
    StandardId id4 = StandardId.of("some_scheme", "4567");

    IdentifiableBean bean1 = TesterIdentifiable.builder().standardId(id1).build();
    IdentifiableBean bean2 = TesterIdentifiable.builder().standardId(id2).build();
    IdentifiableBean bean3 = NonTesterIdentifiable.builder().standardId(id3).build();
    IdentifiableBean bean4 = NonTesterIdentifiable.builder().standardId(id4).build();

    Map<StandardId, IdentifiableBean> sourceData = ImmutableMap.of(
        id1, bean1,
        id2, bean2,
        id3, bean3,
        id4, bean4);

    SearchableSource searchableSource =
        createSearchableSource(sourceData, SearchResult.fullMatch(ImmutableSet.of(id1, id2)));

    Set<IdentifiableBean> result = searchableSource.search(
        Search.builder().categorisingType(TesterIdentifiable.class).build());

    assertThat(result)
        .isNotEmpty()
        .contains(bean1, bean2)
        .doesNotContain(bean3, bean4);
  }

  public void partialMatchReturnsFilteredItems() {

    StandardId id1 = StandardId.of("some_scheme", "1234");
    StandardId id2 = StandardId.of("some_scheme", "2345");
    StandardId id3 = StandardId.of("some_scheme", "3456");
    StandardId id4 = StandardId.of("some_scheme", "4567");

    IdentifiableBean bean1 = TesterIdentifiable.builder().standardId(id1).build();
    IdentifiableBean bean2 = TesterIdentifiable.builder().standardId(id2).build();
    IdentifiableBean bean3 = NonTesterIdentifiable.builder().standardId(id3).build();
    IdentifiableBean bean4 = NonTesterIdentifiable.builder().standardId(id4).build();

    Map<StandardId, IdentifiableBean> sourceData = ImmutableMap.of(
        id1, bean1,
        id2, bean2,
        id3, bean3,
        id4, bean4);

    // Our searchable just returns all items in a partial match
    SearchableSource searchableSource =
        createSearchableSource(sourceData, SearchResult.partialMatch(ImmutableSet.of(id1, id2, id3, id4)));

    // We expect post get filtering of the results
    Set<IdentifiableBean> result = searchableSource.search(
        Search.builder().categorisingType(TesterIdentifiable.class).build());

    assertThat(result)
        .isNotEmpty()
        .contains(bean1, bean2)
        .doesNotContain(bean3, bean4);
  }

  private SearchableSource createSearchableSource(
      Map<StandardId, IdentifiableBean> sourceData,
      SearchResult searchResult) {

    return new DefaultSearchableSource(
        id -> Optional.ofNullable(sourceData.get(id)),
        search -> searchResult);
  }

  private SearchableSource createSearchableSource(Map<StandardId, IdentifiableBean> sourceData) {
    return createSearchableSource(
        sourceData, SearchResult.fullMatch(ImmutableSet.of()));
  }

  private DefaultSearchableSource createEmptySearchableSource() {
    return new DefaultSearchableSource(
        createEmptySourceProvider(),
        createEmptySearchProvider());
  }

  private SourceProvider createEmptySourceProvider() {
    return id -> Optional.empty();
  }

  private SearchableProvider createEmptySearchProvider() {
    return search -> SearchResult.fullMatch(ImmutableSet.of());
  }
}
