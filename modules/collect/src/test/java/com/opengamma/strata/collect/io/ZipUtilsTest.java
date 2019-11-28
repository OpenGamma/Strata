/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ZipUtils}.
 */
public class ZipUtilsTest {

  @Test
  public void test_zipInMemory() throws Exception {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile2.txt");
    ArrayByteSource zipped = ZipUtils.zipInMemory(ImmutableList.of(source1, source2));
    
    try (ZipInputStream in = new ZipInputStream(zipped.openBufferedStream())) {
      ZipEntry entry1 = in.getNextEntry();
      assertThat(entry1.getName()).isEqualTo("TestFile1.txt");
      assertThat(entry1.getTime()).isCloseTo(System.currentTimeMillis(), within(2000L));
      ArrayByteSource file1 = ArrayByteSource.from(in);
      assertThat(file1.readUtf8()).isEqualTo("Hello World");

      ZipEntry entry2 = in.getNextEntry();
      assertThat(entry2.getName()).isEqualTo("TestFile2.txt");
      assertThat(entry2.getTime()).isEqualTo(entry1.getTime());
      ArrayByteSource file2 = ArrayByteSource.from(in);
      assertThat(file2.readUtf8()).isEqualTo("Hello Planet");

      assertThat(in.getNextEntry()).isNull();
    }
  }

  @Test
  public void test_unpackInMemory_zip() {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile2.txt");
    ArrayByteSource zipFile = ZipUtils.zipInMemory(ImmutableList.of(source1, source2)).withFileName("Test.zip");

    ZipUtils.unpackInMemory(zipFile, (name, extracted) -> {
      if (name.equals("TestFile1.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else if (name.equals("TestFile2.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile2.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello Planet");
      } else {
        fail("Unexpected file: " + name);
      }
    });
  }

  @Test
  public void test_unpackInMemory_zipWithFolders() {
    ArrayByteSource zipFile = ResourceLocator.ofClasspath(ZipUtilsTest.class, "TestFolder.zip").getByteSource().load();

    ZipUtils.unpackInMemory(zipFile, (name, extracted) -> {
      if (name.equals("test/alpha/Alpha.txt")) {
        assertThat(extracted.getFileName()).hasValue("Alpha.txt");
        assertThat(extracted.readUtf8()).isEqualTo("ALPHA");
      } else if (name.equals("test/beta/Beta1.txt")) {
        assertThat(extracted.getFileName()).hasValue("Beta1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("BETA1");
      } else if (name.equals("test/beta/Beta2.txt")) {
        assertThat(extracted.getFileName()).hasValue("Beta2.txt");
        assertThat(extracted.readUtf8()).isEqualTo("BETA2");
      } else {
        fail("Unexpected file: " + name);
      }
    });
  }

  @Test
  public void test_unpackInMemory_gz() throws Exception {
    ArrayByteSource gzFile;
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (GZIPOutputStream out = new GZIPOutputStream(baos)) {
        out.write("Hello World".getBytes(StandardCharsets.UTF_8));
      }
      gzFile = ArrayByteSource.ofUnsafe(baos.toByteArray()).withFileName("TestFile1.txt.gz");
    }

    ZipUtils.unpackInMemory(gzFile, (name, extracted) -> {
      if (name.equals("TestFile1.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
  }

  @Test
  public void test_unpackInMemory_base64() {
    ArrayByteSource base64File = ArrayByteSource.ofUtf8("Hello World").toBase64().withFileName("TestFile1.txt.base64");

    ZipUtils.unpackInMemory(base64File, (name, extracted) -> {
      if (name.equals("TestFile1.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
  }

  @Test
  public void test_unpackInMemory_plainNamed() {
    ArrayByteSource plainFile = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");

    ZipUtils.unpackInMemory(plainFile, (name, extracted) -> {
      if (name.equals("TestFile1.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
  }

  @Test
  public void test_unpackInMemory_plainNoName() {
    ArrayByteSource plainFile = ArrayByteSource.ofUtf8("Hello World");

    ZipUtils.unpackInMemory(plainFile, (name, extracted) -> {
      if (name.equals("")) {
        assertThat(extracted.getFileName()).isEmpty();
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
  }

}
