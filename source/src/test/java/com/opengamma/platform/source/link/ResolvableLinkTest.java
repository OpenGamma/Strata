package com.opengamma.platform.source.link;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.platform.source.TesterIdentifiable;
import com.opengamma.platform.source.id.StandardId;

/**
 * Simple tests for construction of a resolvable link.
 */
public class ResolvableLinkTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linkConstructionRequiresId() {
    Link.resolvable(null, TesterIdentifiable.class);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linkConstructionRequiresType() {
    Link.resolvable(StandardId.of("some_scheme", "1234"), null);
  }

  @Test
  public void successfulConstruction() {
    Link<TesterIdentifiable> link =
        Link.resolvable(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class);
    assertThat(link).isNotNull();
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void resolverMustNotBeNull() {
    Link<TesterIdentifiable> link =
        Link.resolvable(StandardId.of("some_scheme", "1234"), TesterIdentifiable.class);
    link.resolve(null);
  }

}
