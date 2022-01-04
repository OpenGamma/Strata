/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ByteSourceCodec}.
 */
public class ByteSourceCodecTest {

  @Test
  public void test_base64() {
    String str = "Hello world!";
    ArrayByteSource test = ArrayByteSource.ofUtf8(str).withFileName("a.txt");
    assertThat(test.encode(ByteSourceCodec.BASE64).readUnsafe())
        .isEqualTo(Base64.getEncoder().encode(str.getBytes(StandardCharsets.UTF_8)));
    assertThat(test.encode(ByteSourceCodec.BASE64).getFileName()).hasValue("a.txt.base64");
    assertThat(test.encode(ByteSourceCodec.BASE64).decode(ByteSourceCodec.BASE64)).isEqualTo(test);
    assertThat(ByteSourceCodec.BASE64.toString()).isEqualTo("Base64");
    assertThat(ByteSourceCodec.of("Base64")).isEqualTo(ByteSourceCodec.BASE64);
  }

  @Test
  public void test_gz() throws IOException {
    String str = "Hello world!";
    ArrayByteSource test = ArrayByteSource.ofUtf8(str).withFileName("a.txt");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
      gzip.write(str.getBytes(StandardCharsets.UTF_8));
    }
    assertThat(test.encode(ByteSourceCodec.GZ).readUnsafe()).isEqualTo(baos.toByteArray());
    assertThat(test.encode(ByteSourceCodec.GZ).getFileName()).hasValue("a.txt.gz");
    assertThat(test.encode(ByteSourceCodec.GZ).decode(ByteSourceCodec.GZ)).isEqualTo(test);
    assertThat(ByteSourceCodec.GZ.toString()).isEqualTo("Gz");
    assertThat(ByteSourceCodec.of("Gz")).isEqualTo(ByteSourceCodec.GZ);
  }

  @Test
  public void test_gzBase64() throws IOException {
    String str = "Hello world!";
    ArrayByteSource test = ArrayByteSource.ofUtf8(str).withFileName("a.txt");
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (OutputStream base64 = Base64.getEncoder().wrap(baos)) {
      try (GZIPOutputStream gzip = new GZIPOutputStream(base64)) {
        gzip.write(str.getBytes(StandardCharsets.UTF_8));
      }
    }
    assertThat(test.encode(ByteSourceCodec.GZ_BASE64).readUnsafe()).isEqualTo(baos.toByteArray());
    assertThat(test.encode(ByteSourceCodec.GZ_BASE64).getFileName()).hasValue("a.txt.gz.base64");
    assertThat(test.encode(ByteSourceCodec.GZ_BASE64).decode(ByteSourceCodec.GZ_BASE64)).isEqualTo(test);
    assertThat(ByteSourceCodec.GZ_BASE64.toString()).isEqualTo("GzBase64");
    assertThat(ByteSourceCodec.of("GzBase64")).isEqualTo(ByteSourceCodec.GZ_BASE64);
  }

  @Test
  public void test_noName() throws IOException {
    String str = "Hello world!";
    ArrayByteSource test = ArrayByteSource.ofUtf8(str);
    assertThat(test.encode(ByteSourceCodec.GZ_BASE64).getFileName()).isEmpty();
    assertThat(test.encode(ByteSourceCodec.GZ_BASE64).decode(ByteSourceCodec.GZ_BASE64).getFileName()).isEmpty();
    assertThat(test.encode(ByteSourceCodec.GZ_BASE64).withFileName("foo").decode(ByteSourceCodec.GZ_BASE64).getFileName()).hasValue("foo");
  }

}
