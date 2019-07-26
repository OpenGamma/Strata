/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import org.joda.beans.ser.JodaBeanSer;
import org.junit.jupiter.api.Test;

/**
 * Test {@link UriByteSource}.
 */
public class UriByteSourceTest {

  @Test
  public void test_of_Uri() throws IOException {
    URI uri = new File("pom.xml").toURI();
    UriByteSource test = UriByteSource.of(uri);
    assertThat(test.getUri()).isSameAs(uri);
    assertThat(test.isEmpty()).isFalse();
    assertThat(test.size()).isGreaterThan(100);
    assertThat(test.sizeIfKnown().isPresent()).isFalse();
    assertThat(test.read()[0]).isEqualTo((byte) '<');
    assertThat(test.readUtf8()).startsWith("<");
    assertThat(test.readUtf8UsingBom()).startsWith("<");
    assertThat(test.asCharSourceUtf8().read()).startsWith("<");
    assertThat(test.asCharSourceUtf8UsingBom().read()).startsWith("<");
  }

  @Test
  public void test_of_Url() throws MalformedURLException {
    URI uri = new File("pom.xml").toURI();
    UriByteSource test = UriByteSource.of(uri.toURL());
    assertThat(test.size()).isGreaterThan(100);
    assertThat(test.sizeIfKnown().isPresent()).isFalse();
    assertThat(test.read()[0]).isEqualTo((byte) '<');
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    UriByteSource test = UriByteSource.of(new File("pom.xml").toURI());
    coverImmutableBean(test);
    test.metaBean().metaProperty("uri").metaBean();
    test.metaBean().metaProperty("uri").propertyGenericType();
    test.metaBean().metaProperty("uri").annotations();
  }

  @Test
  public void testSerialize() {
    UriByteSource test = UriByteSource.of(new File("pom.xml").toURI());
    String json = JodaBeanSer.PRETTY.jsonWriter().write(test);
    UriByteSource roundTrip = JodaBeanSer.PRETTY.jsonReader().read(json, UriByteSource.class);
    assertThat(roundTrip).isEqualTo(test);
  }

}
