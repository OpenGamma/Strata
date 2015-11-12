/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.MockSimpleProduct.LINK_RESOLVABLE_MOCK2;
import static com.opengamma.strata.product.MockSimpleProduct.LINK_RESOLVED_MOCK2;
import static com.opengamma.strata.product.MockSimpleProduct.MOCK1_SECURITY;
import static com.opengamma.strata.product.MockSimpleProduct.MOCK2_SECURITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

import com.google.common.reflect.TypeToken;
import com.opengamma.strata.collect.id.IdentifiableBean;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.id.MockIdentifiable;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test.
 */
@Test
public class SecurityLinkTest {

  private static final StandardId STANDARD_ID = StandardId.of("LinkTest", "1");
  @SuppressWarnings("serial")
  private static final TypeToken<Security<MockSimpleProduct>> TYPE_TOKEN =
      new TypeToken<Security<MockSimpleProduct>>() {};

  public void test_resolvable() {
    SecurityLink<MockSimpleProduct> test = SecurityLink.resolvable(STANDARD_ID, MockSimpleProduct.class);
    assertThat(test.isResolved()).isFalse();
    assertThat(test.getStandardId()).isEqualTo(STANDARD_ID);
    assertThat(test.getTargetType()).isEqualTo(Security.class);
    assertThat(test.getTargetTypeToken()).isEqualTo(TYPE_TOKEN);
    assertThat(test.getProductType()).isEqualTo(MockSimpleProduct.class);
    assertThrows(() -> test.resolvedTarget(), IllegalStateException.class);
  }

  public void test_resolvable_null() {
    assertThrowsIllegalArg(() -> SecurityLink.resolvable(null, MockSimpleProduct.class));
    assertThrowsIllegalArg(() -> SecurityLink.resolvable(STANDARD_ID, (Class<? extends Product>) null));
  }

  public void test_resolved() {
    SecurityLink<MockSimpleProduct> test = SecurityLink.resolved(MOCK1_SECURITY);
    assertThat(test.isResolved()).isTrue();
    assertThat(test.getStandardId()).isEqualTo(MOCK1_SECURITY.getStandardId());
    assertThat(test.getTargetType()).isEqualTo(Security.class);
    assertThat(test.getTargetTypeToken()).isEqualTo(TYPE_TOKEN);
    assertThat(test.getProductType()).isEqualTo(MockSimpleProduct.class);
    assertThat(test.resolvedTarget()).isEqualTo(MOCK1_SECURITY);
  }

  public void test_resolved_null() {
    assertThrowsIllegalArg(() -> SecurityLink.resolved(null));
  }

  //-------------------------------------------------------------------------
  public void test_builder_invalid_onlyId() {
    BeanBuilder<SecurityLink<MockSimpleProduct>> builder = SecurityLink.meta().builder()
        .standardId(STANDARD_ID);
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_invalid_onlyType() {
    BeanBuilder<SecurityLink<MockSimpleProduct>> builder = SecurityLink.meta().builder()
        .productType(MockIdentifiable.class);
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_invalid_targetDoesNotMatchId() {
    BeanBuilder<SecurityLink<MockSimpleProduct>> builder = SecurityLink.meta().builder()
        .standardId(STANDARD_ID)
        .target(MOCK1_SECURITY);
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_invalid_targetDoesNotMatchType() {
    BeanBuilder<SecurityLink<MockResolvableProduct>> builder = SecurityLink.meta().builder()
        .productType(MockResolvableProduct.class)
        .target(MOCK1_SECURITY);
    assertThrowsIllegalArg(() -> builder.build());
  }

  public void test_builder_valid_targetMatchesIdentifier() {
    SecurityLink<MockSimpleProduct> test = SecurityLink.metaSecurityLink(MockSimpleProduct.class).builder()
        .standardId(MOCK1_SECURITY.getStandardId())
        .target(MOCK1_SECURITY)
        .build();
    assertThat(test.isResolved()).isTrue();
  }

  public void test_builder_valid_targetMatchesType() {
    SecurityLink<MockSimpleProduct> test = SecurityLink.metaSecurityLink(MockSimpleProduct.class).builder()
        .set(SecurityLink.meta().productType(), Product.class)
        .target(MOCK1_SECURITY)
        .build();
    assertThat(test.isResolved()).isTrue();
  }

  public void test_builder_valid_targetMatchesIdAndType() {
    SecurityLink<MockSimpleProduct> test = SecurityLink.metaSecurityLink(MockSimpleProduct.class).builder()
        .standardId(MOCK1_SECURITY.getStandardId())
        .set(SecurityLink.meta().productType(), Product.class)
        .target(MOCK1_SECURITY)
        .build();
    assertThat(test.isResolved()).isTrue();
  }

  //-------------------------------------------------------------------------
  public void test_resolvable_resolve() {
    SecurityLink<MockSimpleProduct> link = SecurityLink.resolvable(STANDARD_ID, MockSimpleProduct.class);
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings({"unchecked", "serial"})
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        assertThat(id).isEqualTo(STANDARD_ID);
        assertThat(targetType).isEqualTo(TYPE_TOKEN);
        return (T) MOCK1_SECURITY;
      }
    };
    assertThat(link.resolve(resolver)).isEqualTo(MOCK1_SECURITY);
  }

  //-------------------------------------------------------------------------
  public void test_resolved_resolve() {
    // LinkResolver is not used, use null to check that is allowed
    SecurityLink<MockSimpleProduct> link = SecurityLink.resolved(MOCK1_SECURITY);
    assertThat(link.resolve(null)).isSameAs(MOCK1_SECURITY);
  }

  //-------------------------------------------------------------------------
  public void test_resolveLinks_resolveNeeded() {
    SecurityLink<MockSimpleProduct> link = LINK_RESOLVABLE_MOCK2;
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        assertThat(id).isEqualTo(LINK_RESOLVABLE_MOCK2.getStandardId());
        assertThat(targetType).isEqualTo(TYPE_TOKEN);
        return (T) MOCK2_SECURITY;
      }
    };
    assertThat(link.resolveLinks(resolver)).isEqualTo(LINK_RESOLVED_MOCK2);
  }

  public void test_resolveLinks_noResolveNeeded() {
    SecurityLink<MockSimpleProduct> link = LINK_RESOLVED_MOCK2;
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        fail();  // resolver must not be called as already resolved
        return (T) MOCK2_SECURITY;
      }
    };
    assertThat(link.resolveLinks(resolver)).isSameAs(link);
  }

  public void test_resolveLinks_notResolvable() {
    SecurityLink<MockSimpleProduct> link = SecurityLink.resolved(MockSimpleSecurity.MOCK1);
    LinkResolver resolver = new LinkResolver() {
      @SuppressWarnings("unchecked")
      @Override
      public <T extends IdentifiableBean> T resolve(StandardId id, TypeToken<T> targetType) {
        fail();  // resolver must not be called as not resolvable
        return (T) MOCK2_SECURITY;
      }
    };
    assertThat(link.resolveLinks(resolver)).isSameAs(link);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SecurityLink<MockSimpleProduct> test = SecurityLink.resolvable(STANDARD_ID, MockSimpleProduct.class);
    coverImmutableBean(test);
    SecurityLink<MockSimpleProduct> test2 = SecurityLink.resolved(MOCK1_SECURITY);
    coverBeanEquals(test, test2);
    SecurityLink<MockSimpleProduct> test3 = SecurityLink.resolved(MOCK2_SECURITY);
    coverBeanEquals(test2, test3);
    SecurityLink<MockResolvableProduct> test4 = SecurityLink.metaSecurityLink(MockResolvableProduct.class).builder()
        .setString(SecurityLink.meta().standardId(), STANDARD_ID.toString())
        .setString(SecurityLink.meta().productType().name(), MockResolvableProduct.class.getName())
        .build();
    coverBeanEquals(test, test4);
  }

  public void test_serializable() {
    assertSerialization(SecurityLink.resolvable(STANDARD_ID, MockSimpleProduct.class));
    assertSerialization(SecurityLink.resolved(MOCK1_SECURITY));
  }

}
