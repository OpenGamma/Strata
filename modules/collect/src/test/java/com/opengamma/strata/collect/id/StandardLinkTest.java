/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.id;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.id.MockIdentifiable.LINK_RESOLVABLE_MOCK2;
import static com.opengamma.strata.collect.id.MockIdentifiable.LINK_RESOLVED_MOCK2;
import static com.opengamma.strata.collect.id.MockIdentifiable.MOCK1;
import static com.opengamma.strata.collect.id.MockIdentifiable.MOCK2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;

/**
 * Simple tests for a link.
 */
@SuppressWarnings("unchecked")
@Test
public class StandardLinkTest {

  private static final StandardId STANDARD_ID = StandardId.of("LinkTest", "1");

  public void test_resolvable() {
    StandardLink<MockIdentifiable> test = StandardLink.resolvable(STANDARD_ID, MockIdentifiable.class);
    assertThat(test.isResolved()).isFalse();
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getTargetType()).isEqualTo(MockIdentifiable.class);
    assertThat(test.getTargetTypeToken()).isEqualTo(TypeToken.of(MockIdentifiable.class));
  }

  public void test_resolvable_null() {
    assertThrowsIllegalArg(() -> StandardLink.resolvable(null, MockIdentifiable.class));
    assertThrowsIllegalArg(() -> StandardLink.resolvable(STANDARD_ID, (Class<? extends IdentifiableBean>) null));
  }

  public void test_resolved() {
    StandardLink<MockIdentifiable> test = StandardLink.resolved(MOCK1);
    assertThat(test.isResolved()).isTrue();
    assertThat(test.getStandardId()).isEqualTo(MOCK1.getStandardId());
    assertThat(test.getTargetType()).isEqualTo(MOCK1.getClass());
    assertThat(test.getTargetTypeToken()).isEqualTo(TypeToken.of(MOCK1.getClass()));
  }

  public void test_resolved_null() {
    assertThrowsIllegalArg(() -> StandardLink.resolved(null));
  }

  //-------------------------------------------------------------------------
  public void test_builder_invalid_onlyId() {
    BeanBuilder<StandardLink<MockIdentifiable>> builder = StandardLink.meta().builder()
        .set(StandardLink.meta().standardId(), STANDARD_ID);
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_invalid_onlyType() {
    BeanBuilder<StandardLink<MockIdentifiable>> builder = StandardLink.meta().builder()
        .set(StandardLink.meta().targetType(), MockIdentifiable.class);
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_invalid_targetDoesNotMatchId() {
    BeanBuilder<StandardLink<MockIdentifiable>> builder = StandardLink.meta().builder()
        .set(StandardLink.meta().standardId(), STANDARD_ID)
        .set(StandardLink.meta().target(), MOCK1);
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_invalid_targetDoesNotMatchType() {
    BeanBuilder<StandardLink<MockIdentifiable>> builder = StandardLink.meta().builder()
        .set(StandardLink.meta().targetType(), MockIdentifiable.class)
        .set(StandardLink.meta().target(), MockIdentifiable2.builder().standardId(STANDARD_ID).build());
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_valid_targetMatchesIdentifier() {
    StandardLink<MockIdentifiable> test = StandardLink.metaStandardLink(MockIdentifiable.class).builder()
        .set(StandardLink.meta().standardId(), MOCK1.getStandardId())
        .set(StandardLink.meta().target(), MOCK1)
        .build();
    assertThat(test.isResolved()).isTrue();
  }

  public void test_builder_valid_targetMatchesType() {
    StandardLink<MockIdentifiable> test = StandardLink.metaStandardLink(MockIdentifiable.class).builder()
        .set(StandardLink.meta().targetType(), IdentifiableBean.class)
        .set(StandardLink.meta().target(), MOCK1)
        .build();
    assertThat(test.isResolved()).isTrue();
  }

  public void test_builder_valid_targetMatchesIdAndType() {
    StandardLink<MockIdentifiable> test = StandardLink.metaStandardLink(MockIdentifiable.class).builder()
        .set(StandardLink.meta().standardId(), MOCK1.getStandardId())
        .set(StandardLink.meta().targetType(), IdentifiableBean.class)
        .set(StandardLink.meta().target(), MOCK1)
        .build();
    assertThat(test.isResolved()).isTrue();
  }

  //-------------------------------------------------------------------------
  public void test_resolvable_resolve() {
    StandardLink<MockIdentifiable> link = StandardLink.resolvable(STANDARD_ID, MockIdentifiable.class);
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        assertThat(id).isEqualTo(STANDARD_ID);
        assertThat(targetType).isEqualTo(TypeToken.of(MockIdentifiable.class));
        return (T) MOCK1;
      }
    };
    assertThat(link.resolve(resolver)).isEqualTo(MOCK1);
  }

  //-------------------------------------------------------------------------
  public void test_resolved_resolve() {
    // LinkResolver is not used, use null to check that is allowed
    StandardLink<MockIdentifiable> link = StandardLink.resolved(MOCK1);
    assertThat(link.resolve(null)).isSameAs(MOCK1);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolveNeeded() {
    StandardLink<MockIdentifiable> link = LINK_RESOLVABLE_MOCK2;
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        assertThat(id).isEqualTo(LINK_RESOLVABLE_MOCK2.getStandardId());
        assertThat(targetType).isEqualTo(TypeToken.of(MockIdentifiable.class));
        return (T) MOCK2;
      }
    };
    assertThat(link.resolveLinks(resolver)).isEqualTo(LINK_RESOLVED_MOCK2);
  }

  public void test_resolveLinks_noResolveNeeded() {
    StandardLink<MockIdentifiable> link = LINK_RESOLVED_MOCK2;
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        fail();  // resolver must not be called as already resolved
        return (T) MOCK2;
      }
    };
    assertThat(link.resolveLinks(resolver)).isSameAs(link);
  }

  public void test_resolveLinks_notResolvable() {
    StandardLink<MockIdentifiable2> link = StandardLink.resolved(MockIdentifiable2.MOCK21);
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        fail();  // resolver must not be called as not resolvable
        return (T) MOCK2;
      }
    };
    assertThat(link.resolveLinks(resolver)).isSameAs(link);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    StandardLink<MockIdentifiable2> test = StandardLink.resolvable(STANDARD_ID, MockIdentifiable2.class);
    coverImmutableBean(test);
    StandardLink<MockIdentifiable> test2 = StandardLink.resolved(MOCK1);
    coverBeanEquals(test, test2);
    StandardLink<MockIdentifiable> test3 = StandardLink.resolved(MOCK2);
    coverBeanEquals(test2, test3);
    StandardLink<MockIdentifiable> test4 = StandardLink.metaStandardLink(MockIdentifiable.class).builder()
        .setString(StandardLink.meta().standardId(), STANDARD_ID.toString())
        .setString(StandardLink.meta().targetType().name(), MockIdentifiable.class.getName())
        .build();
    coverBeanEquals(test, test4);
  }

  public void test_serializable() {
    assertSerialization(StandardLink.resolvable(STANDARD_ID, MockIdentifiable.class));
    assertSerialization(StandardLink.resolved(MOCK1));
  }

}
