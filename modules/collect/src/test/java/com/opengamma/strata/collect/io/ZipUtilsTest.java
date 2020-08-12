/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.within;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.google.common.collect.ImmutableList;
import com.google.common.io.MoreFiles;

/**
 * Test {@link ZipUtils}.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ZipUtilsTest {

  private Path tmpDir;

  @BeforeAll
  public void setup() throws IOException {
    tmpDir = Files.createTempDirectory("zip-utils-test");
  }

  @AfterAll
  public void tearDown() {
    try {
      MoreFiles.deleteRecursively(tmpDir);
    } catch (IOException ex) {
      // ignore
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unzipPathNames() {
    ArrayByteSource zipFile = load("TestFolder.zip");

    Set<String> names = ZipUtils.unzipPathNames(zipFile);

    assertThat(names).containsOnly("test/alpha/Alpha.txt", "test/beta/Beta1.txt", "test/beta/Beta2.txt");
  }

  @Test
  public void test_unzipPathNames_encrypted() {
    ArrayByteSource zipFile = load("TestFileEncrypted.zip");

    Set<String> names = ZipUtils.unzipPathNames(ZipUtils.decryptZip(zipFile, "ThePassword"));

    assertThat(names).containsOnly("TestFile.txt");
  }

  @Test
  public void test_unzipPathNames_zipSlip() {
    ArrayByteSource zipFile = load("zip-slip.zip");

    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> ZipUtils.unzipPathNames(zipFile));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unzipPathNameInMemory() {
    ArrayByteSource zipFile = load("TestFolder.zip");

    assertThat(ZipUtils.unzipPathNameInMemory(zipFile, "test/alpha/Alpha.txt"))
        .map(source -> source.readUtf8())
        .hasValue("ALPHA");
    assertThat(ZipUtils.unzipPathNameInMemory(zipFile, "test/beta/Beta1.txt"))
        .map(source -> source.readUtf8())
        .hasValue("BETA1");
    assertThat(ZipUtils.unzipPathNameInMemory(zipFile, "test/beta/Beta2.txt"))
        .map(source -> source.readUtf8())
        .hasValue("BETA2");
  }

  @Test
  public void test_unzipPathNameInMemory_zipSlip() {
    ArrayByteSource zipFile = load("zip-slip.zip");

    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> ZipUtils.unzipPathNameInMemory(zipFile, "test/alpha/Alpha.txt"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unzip_toPath() {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile2.txt");
    ArrayByteSource zipFile = ZipUtils.zipInMemory(ImmutableList.of(source1, source2)).withFileName("Test.foo");

    ZipUtils.unzip(zipFile, tmpDir);

    assertThat(tmpDir.resolve("TestFile1.txt")).hasContent("Hello World");
    assertThat(tmpDir.resolve("TestFile2.txt")).hasContent("Hello Planet");
  }

  @Test
  public void test_unzip_toPath_notNormalized() {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile3.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile4.txt");
    ArrayByteSource zipFile = ZipUtils.zipInMemory(ImmutableList.of(source1, source2)).withFileName("Test.foo");

    ZipUtils.unzip(zipFile, tmpDir.resolve("abc").resolve(".."));

    assertThat(tmpDir.resolve("TestFile3.txt")).hasContent("Hello World");
    assertThat(tmpDir.resolve("TestFile4.txt")).hasContent("Hello Planet");
  }

  @Test
  public void test_unzip_toPath_withFolders() {
    ArrayByteSource zipFile = load("TestFolder.zip");

    ZipUtils.unzip(zipFile, tmpDir);

    assertThat(tmpDir.resolve("test/alpha/Alpha.txt")).hasContent("ALPHA");
    assertThat(tmpDir.resolve("test/beta/Beta1.txt")).hasContent("BETA1");
    assertThat(tmpDir.resolve("test/beta/Beta2.txt")).hasContent("BETA2");
  }

  @Test
  public void test_unzip_toPath_zipSlip() {
    ArrayByteSource zipFile = load("zip-slip.zip");

    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> ZipUtils.unzip(zipFile, tmpDir));
  }

  //-------------------------------------------------------------------------
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

  //-------------------------------------------------------------------------
  @Test
  public void test_unzipInMemory_toMap_zip() {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile2.txt");
    ArrayByteSource zipFile = ZipUtils.zipInMemory(ImmutableList.of(source1, source2)).withFileName("Test.foo");

    Map<String, ArrayByteSource> map = ZipUtils.unzipInMemory(zipFile);
    assertThat(map).hasSize(2);
    assertThat(map.get("TestFile1.txt").getFileName()).hasValue("TestFile1.txt");
    assertThat(map.get("TestFile1.txt").readUtf8()).isEqualTo("Hello World");
    assertThat(map.get("TestFile2.txt").getFileName()).hasValue("TestFile2.txt");
    assertThat(map.get("TestFile2.txt").readUtf8()).isEqualTo("Hello Planet");
  }

  @Test
  public void test_unzipInMemory_toMap_encrypted() {
    ArrayByteSource zipFile = load("TestFileEncrypted.zip");

    Map<String, ArrayByteSource> map = ZipUtils.unzipInMemory(ZipUtils.decryptZip(zipFile, "ThePassword"));
    assertThat(map).hasSize(1);
    assertThat(map.get("TestFile.txt").getFileName()).hasValue("TestFile.txt");
    assertThat(map.get("TestFile.txt").readUtf8()).startsWith("HelloWorld");
  }

  @Test
  public void test_unzipInMemory_toMap_encryptedBadPassword() {
    ArrayByteSource zipFile = load("TestFileEncrypted.zip");

    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> ZipUtils.unzipInMemory(ZipUtils.decryptZip(zipFile, "WrongPassword")))
        .withMessageContaining("Unable to decrypt ZIP file, wrong password");
    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> ZipUtils.unzipInMemory(zipFile));
  }

  @Test
  public void test_unzipInMemory_toMap_noDecryptWhenNotEncrypted() {
    ArrayByteSource zipFile = load("TestFile.zip");  // not encrypted

    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> ZipUtils.unzipInMemory(ZipUtils.decryptZip(zipFile, "AnyPassword")))
        .withMessageContaining("Unable to decrypt ZIP file, ZIP is not password protected");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unzipInMemory() {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile2.txt");
    ArrayByteSource zipFile = ZipUtils.zipInMemory(ImmutableList.of(source1, source2)).withFileName("Test.foo");

    AtomicInteger counter = new AtomicInteger();
    ZipUtils.unzipInMemory(zipFile, (name, extracted) -> {
      counter.incrementAndGet();
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
    assertThat(counter).hasValue(2);
  }

  @Test
  public void test_unzipInMemory_zipSlip() {
    ArrayByteSource zipFile = load("zip-slip.zip");

    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> ZipUtils.unzipInMemory(
            zipFile, 
            (name, extracted) -> {
              if (!name.equals("good.txt")) {
                fail("Should not get here: " + name);
              }
            }));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unpackInMemory_toMap_zip() {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile2.txt");
    ArrayByteSource zipFile = ZipUtils.zipInMemory(ImmutableList.of(source1, source2)).withFileName("Test.zip");

    Map<String, ArrayByteSource> map = ZipUtils.unpackInMemory(zipFile);
    assertThat(map).hasSize(2);
    assertThat(map.get("TestFile1.txt").getFileName()).hasValue("TestFile1.txt");
    assertThat(map.get("TestFile1.txt").readUtf8()).isEqualTo("Hello World");
    assertThat(map.get("TestFile2.txt").getFileName()).hasValue("TestFile2.txt");
    assertThat(map.get("TestFile2.txt").readUtf8()).isEqualTo("Hello Planet");
  }

  @Test
  public void test_unpackInMemory_toMap_zipWithFolders() {
    ArrayByteSource zipFile = load("TestFolder.zip");

    Map<String, ArrayByteSource> map = ZipUtils.unpackInMemory(zipFile);
    assertThat(map).hasSize(3);
    assertThat(map.get("test/alpha/Alpha.txt").getFileName()).hasValue("Alpha.txt");
    assertThat(map.get("test/alpha/Alpha.txt").readUtf8()).isEqualTo("ALPHA");
    assertThat(map.get("test/beta/Beta1.txt").getFileName()).hasValue("Beta1.txt");
    assertThat(map.get("test/beta/Beta1.txt").readUtf8()).isEqualTo("BETA1");
    assertThat(map.get("test/beta/Beta2.txt").getFileName()).hasValue("Beta2.txt");
    assertThat(map.get("test/beta/Beta2.txt").readUtf8()).isEqualTo("BETA2");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_unpackInMemory_zip() {
    ArrayByteSource source1 = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");
    ArrayByteSource source2 = ArrayByteSource.ofUtf8("Hello Planet").withFileName("TestFile2.txt");
    ArrayByteSource zipFile = ZipUtils.zipInMemory(ImmutableList.of(source1, source2)).withFileName("Test.zip");

    AtomicInteger counter = new AtomicInteger();
    ZipUtils.unpackInMemory(zipFile, (name, extracted) -> {
      counter.incrementAndGet();
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
    assertThat(counter).hasValue(2);
  }

  @Test
  public void test_unpackInMemory_zipWithFolders() {
    ArrayByteSource zipFile = load("TestFolder.zip");

    AtomicInteger counter = new AtomicInteger();
    ZipUtils.unpackInMemory(zipFile, (name, extracted) -> {
      counter.incrementAndGet();
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
    assertThat(counter).hasValue(3);
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

    AtomicInteger counter = new AtomicInteger();
    ZipUtils.unpackInMemory(gzFile, (name, extracted) -> {
      counter.incrementAndGet();
      if (name.equals("TestFile1.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
    assertThat(counter).hasValue(1);
  }

  @Test
  public void test_unpackInMemory_base64() {
    ArrayByteSource base64File = ArrayByteSource.ofUtf8("Hello World").toBase64().withFileName("TestFile1.txt.base64");

    AtomicInteger counter = new AtomicInteger();
    ZipUtils.unpackInMemory(base64File, (name, extracted) -> {
      counter.incrementAndGet();
      if (name.equals("TestFile1.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
    assertThat(counter).hasValue(1);
  }

  @Test
  public void test_unpackInMemory_plainNamed() {
    ArrayByteSource plainFile = ArrayByteSource.ofUtf8("Hello World").withFileName("TestFile1.txt");

    AtomicInteger counter = new AtomicInteger();
    ZipUtils.unpackInMemory(plainFile, (name, extracted) -> {
      counter.incrementAndGet();
      if (name.equals("TestFile1.txt")) {
        assertThat(extracted.getFileName()).hasValue("TestFile1.txt");
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
    assertThat(counter).hasValue(1);
  }

  @Test
  public void test_unpackInMemory_plainNoName() {
    ArrayByteSource plainFile = ArrayByteSource.ofUtf8("Hello World");

    AtomicInteger counter = new AtomicInteger();
    ZipUtils.unpackInMemory(plainFile, (name, extracted) -> {
      counter.incrementAndGet();
      if (name.equals("")) {
        assertThat(extracted.getFileName()).isEmpty();
        assertThat(extracted.readUtf8()).isEqualTo("Hello World");
      } else {
        fail("Unexpected file: " + name);
      }
    });
    assertThat(counter).hasValue(1);
  }

  @Test
  public void test_unpackInMemory_toMap_encrypted() {
    ArrayByteSource zipFile = load("TestFileEncrypted.zip");

    Map<String, ArrayByteSource> map = ZipUtils.unpackInMemory(ZipUtils.decryptZip(zipFile, "ThePassword"));
    assertThat(map).hasSize(1);
    assertThat(map.get("TestFile.txt").getFileName()).hasValue("TestFile.txt");
    assertThat(map.get("TestFile.txt").readUtf8()).startsWith("HelloWorld");
  }

  //-------------------------------------------------------------------------
  private static ArrayByteSource load(String fileName) {
    return ResourceLocator.ofClasspath(ZipUtilsTest.class, fileName).getByteSource().load();
  }

}
