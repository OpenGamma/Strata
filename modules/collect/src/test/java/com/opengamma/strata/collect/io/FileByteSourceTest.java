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

import org.joda.beans.ser.JodaBeanSer;
import org.junit.jupiter.api.Test;

import com.google.common.hash.Hashing;

/**
 * Test {@link FileByteSource}.
 */
public class FileByteSourceTest {

  @Test
  public void test_of_File() throws IOException {
    File file = new File("pom.xml");
    FileByteSource test = FileByteSource.of(file);
    assertThat(test.getFileName()).hasValue("pom.xml");
    assertThat(test.getFileNameOrThrow()).isEqualTo("pom.xml");
    assertThat(test.getFile()).isSameAs(file);
    assertThat(test.isEmpty()).isFalse();
    assertThat(test.size()).isGreaterThan(100);
    assertThat(test.sizeIfKnown().isPresent()).isTrue();
    assertThat(test.read()[0]).isEqualTo((byte) '<');
    assertThat(test.readUtf8()).startsWith("<");
    assertThat(test.readUtf8UsingBom()).startsWith("<");
    assertThat(test.asCharSourceUtf8().read()).startsWith("<");
    assertThat(test.asCharSourceUtf8UsingBom().read()).startsWith("<");
    assertThat(test.load().getFileName()).hasValue("pom.xml");
    assertThat(test.load().getFileNameOrThrow()).isEqualTo("pom.xml");
    assertThat(test.load().readUtf8()).startsWith("<");
  }

  @Test
  public void test_of_Path() {
    FileByteSource test = FileByteSource.of(new File("pom.xml").toPath());
    assertThat(test.getFileName()).hasValue("pom.xml");
    assertThat(test.size()).isGreaterThan(100);
    assertThat(test.sizeIfKnown().isPresent()).isTrue();
    assertThat(test.read()[0]).isEqualTo((byte) '<');
  }

  @Test
  public void test_of_hash() {
    FileByteSource test = FileByteSource.of(new File("pom.xml").toPath());
    ArrayByteSource loaded = test.load();
    assertThat(test.toHash(Hashing.sha256())).isEqualTo(loaded.toHash(Hashing.sha256()));
    assertThat(test.toHashString(Hashing.sha256())).isEqualTo(loaded.toHashString(Hashing.sha256()));
  }

  @Test
  public void test_of_base64() {
    FileByteSource test = FileByteSource.of(new File("pom.xml").toPath());
    ArrayByteSource loaded = test.load();
    assertThat(test.toBase64()).isEqualTo(loaded.toBase64());
    assertThat(test.toBase64String()).isEqualTo(loaded.toBase64String());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FileByteSource test = FileByteSource.of(new File("pom.xml"));
    coverImmutableBean(test);
    test.metaBean().metaProperty("file").metaBean();
    test.metaBean().metaProperty("file").propertyGenericType();
    test.metaBean().metaProperty("file").annotations();
  }

  @Test
  public void testSerialize() {
    FileByteSource test = FileByteSource.of(new File("pom.xml"));
    String json = JodaBeanSer.PRETTY.jsonWriter().write(test);
    FileByteSource roundTrip = JodaBeanSer.PRETTY.jsonReader().read(json, FileByteSource.class);
    assertThat(roundTrip).isEqualTo(test);
  }

}
