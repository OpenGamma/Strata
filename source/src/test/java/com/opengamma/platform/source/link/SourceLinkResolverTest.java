package com.opengamma.platform.source.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.Guavate;
import com.opengamma.collect.result.FailureReason;
import com.opengamma.collect.result.Result;
import com.opengamma.platform.source.NonTesterIdentifiable;
import com.opengamma.platform.source.Source;
import com.opengamma.platform.source.TesterIdentifiable;
import com.opengamma.platform.source.id.IdentifiableBean;
import com.opengamma.platform.source.id.StandardId;
import com.opengamma.platform.source.id.StandardIdentifiable;

/**
 * Tests the source link resolver using a map-based source.
 */
public class SourceLinkResolverTest {

  @Test(
      expectedExceptions = LinkResolutionException.class,
      expectedExceptionsMessageRegExp = ".*Unable to find data.*")
  public void resolutionFailsIfItemNotFound() {

    Link<TesterIdentifiable> link =
        Link.resolvable(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class);

    SourceLinkResolver resolver = new SourceLinkResolver(new MapSource());
    link.resolve(resolver);
  }

  @Test(
      expectedExceptions = LinkResolutionException.class,
      expectedExceptionsMessageRegExp = ".*but expected type was.*")
  public void resolutionFailsIfWrongType() {

    Link<NonTesterIdentifiable> link =
        Link.resolvable(StandardId.of("some_scheme", "1234"), NonTesterIdentifiable.class);

    TesterIdentifiable bean = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    SourceLinkResolver resolver = new SourceLinkResolver(new MapSource(bean));
    link.resolve(resolver);
  }

  @Test
  public void resolutionSuccess() {

    Link<TesterIdentifiable> link =
        Link.resolvable(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class);

    TesterIdentifiable bean = TesterIdentifiable.builder()
        .standardId(StandardId.of("some_scheme", "1234"))
        .build();

    SourceLinkResolver resolver = new SourceLinkResolver(new MapSource(bean));
    TesterIdentifiable resolved = link.resolve(resolver);
    assertThat(resolved)
        .isNotNull()
        .isSameAs(bean);
  }

  private static class MapSource implements Source {

    private final Map<StandardId, IdentifiableBean> beanMap;

    public MapSource(Map<StandardId, IdentifiableBean> beanMap) {
      this.beanMap = ImmutableMap.copyOf(beanMap);
    }

    public MapSource(IdentifiableBean... beans) {
      this(Arrays.asList(beans)
          .stream()
          .collect(Guavate.<IdentifiableBean, StandardId>toImmutableMap(StandardIdentifiable::getStandardId)));
    }

    @Override
    public <T extends IdentifiableBean> Result<T> get(StandardId id, Class<T> type) {
      if (beanMap.containsKey(id)) {
        IdentifiableBean bean = beanMap.get(id);
        Class<? extends IdentifiableBean> receivedType = bean.getClass();
        if (type.isAssignableFrom(receivedType)) {
          return Result.success(type.cast(bean));
        } else {
          return Result.failure(FailureReason.MISSING_DATA,
              "Found data with id: {} of type: {} but expected type was: {}",
              id, receivedType.getSimpleName(), type.getSimpleName());
        }
      } else {
        return Result.failure(FailureReason.MISSING_DATA, "Unable to find data with id: {}", id);
      }
    }
  }
}
