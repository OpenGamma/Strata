package com.opengamma.platform.source;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Tests the default methods of the {@link SourceProvider} interface.
 */
@Test
public class SourceProviderTest {

  public void bulkGetSingleItem() {

    StandardId id = StandardId.of("test_scheme", "1");
    TesterIdentifiable bean = TesterIdentifiable.builder()
        .standardId(id)
        .build();

    SourceProvider sourceProvider = createSourceProvider(ImmutableSet.of(bean));

    assertThat(sourceProvider.bulkGet(ImmutableSet.of(id)))
        .hasSize(1)
        .containsEntry(id, bean);
  }

  public void bulkGetMultipleItemsAllMissing() {

    StandardId id1 = StandardId.of("test_scheme", "1");
    StandardId id2 = StandardId.of("test_scheme", "2");
    StandardId id3 = StandardId.of("test_scheme", "3");

    SourceProvider sourceProvider = createSourceProvider(ImmutableSet.of());

    assertThat(sourceProvider.bulkGet(ImmutableSet.of(id1, id2, id3)))
        .isEmpty();
  }

  public void bulkGetMultipleItemsSomeMissing() {

    StandardId id1 = StandardId.of("test_scheme", "1");
    StandardId id2 = StandardId.of("test_scheme", "2");
    StandardId id3 = StandardId.of("test_scheme", "3");

    TesterIdentifiable bean1 = TesterIdentifiable.builder()
        .standardId(id1)
        .build();

    TesterIdentifiable bean2 = TesterIdentifiable.builder()
        .standardId(id2)
        .build();

    SourceProvider sourceProvider = createSourceProvider(ImmutableSet.of(bean1, bean2));

    assertThat(sourceProvider.bulkGet(ImmutableSet.of(id1, id2, id3)))
        .hasSize(2)
        .containsEntry(id1, bean1)
        .containsEntry(id2, bean2)
        .doesNotContainKey(id3);
  }

  public void bulkGetMultipleItemsSomeMissingSomeUnused() {

    StandardId id1 = StandardId.of("test_scheme", "1");
    StandardId id2 = StandardId.of("test_scheme", "2");
    StandardId id3 = StandardId.of("test_scheme", "3");
    StandardId id4 = StandardId.of("test_scheme", "4");

    TesterIdentifiable bean1 = TesterIdentifiable.builder()
        .standardId(id1)
        .build();

    TesterIdentifiable bean2 = TesterIdentifiable.builder()
        .standardId(id2)
        .build();

    TesterIdentifiable bean3 = TesterIdentifiable.builder()
        .standardId(id3)
        .build();

    SourceProvider sourceProvider = createSourceProvider(ImmutableSet.of(bean1, bean2, bean3));

    assertThat(sourceProvider.bulkGet(ImmutableSet.of(id1, id2, id4)))
        .hasSize(2)
        .containsEntry(id1, bean1)
        .containsEntry(id2, bean2)
        .doesNotContainKey(id3)
        .doesNotContainKey(id4);
  }

  public void bulkGetMultipleItemsAllPresent() {

    StandardId id1 = StandardId.of("test_scheme", "1");
    StandardId id2 = StandardId.of("test_scheme", "2");
    StandardId id3 = StandardId.of("test_scheme", "3");

    TesterIdentifiable bean1 = TesterIdentifiable.builder()
        .standardId(id1)
        .build();

    TesterIdentifiable bean2 = TesterIdentifiable.builder()
        .standardId(id2)
        .build();

    TesterIdentifiable bean3 = TesterIdentifiable.builder()
        .standardId(id3)
        .build();

    SourceProvider sourceProvider =
        createSourceProvider(ImmutableSet.of(bean1, bean2, bean3));

    assertThat(sourceProvider.bulkGet(ImmutableSet.of(id1, id2, id3)))
        .hasSize(3)
        .containsEntry(id1, bean1)
        .containsEntry(id2, bean2)
        .containsEntry(id3, bean3);
  }

  public void changedSinceReturnsEmptyInputs() {
    SourceProvider sourceProvider = createSourceProvider(ImmutableSet.of());
    assertThat(sourceProvider.changedSince(ImmutableSet.of(), Instant.now())).isEmpty();
  }

  public void changedSinceReturnsPopulatedInputs() {

    StandardId id1 = StandardId.of("test_scheme", "1");
    StandardId id2 = StandardId.of("test_scheme", "2");

    SourceProvider sourceProvider = createSourceProvider(ImmutableSet.of());
    assertThat(sourceProvider.changedSince(ImmutableSet.of(id1, id2), Instant.now()))
        .hasSize(2)
        .containsExactly(id1, id2);
  }

  // Primarily for coverage at this point
  // TODO - add more when it actually does something (and rename test!)
  public void registerForUpdatesDoesNothing() {

    SourceProvider sourceProvider = createSourceProvider(ImmutableSet.of());
    sourceProvider.registerForUpdates(new UpdateNotificationListener() {
      @Override
      public void idsUpdated(Collection<StandardId> updates) {
      }

      @Override
      public void dataUpdated(Collection<IdentifiableBean> updates) {
      }
    });
  }

  private SourceProvider createSourceProvider(Set<IdentifiableBean> items) {
    return id -> items.stream().filter(b -> b.getStandardId().equals(id)).findFirst();
  }

}
