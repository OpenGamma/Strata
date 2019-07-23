/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * Test {@link ResourceLocator}.
 */
public class ResourceLocatorTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of_filePrefixed() throws Exception {
    ResourceLocator test = ResourceLocator.of("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getLocator()).isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
  }

  @Test
  public void test_of_fileNoPrefix() throws Exception {
    ResourceLocator test = ResourceLocator.of("src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getLocator()).isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
  }

  @Test
  public void test_of_classpath() throws Exception {
    ResourceLocator test = ResourceLocator.of("classpath:com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getLocator())
        .startsWith("classpath")
        .endsWith("com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString())
        .startsWith("classpath")
        .endsWith("com/opengamma/strata/collect/io/TestFile.txt");
  }

  @Test
  public void test_of_invalid() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() -> ResourceLocator.of("classpath:http:https:file:/foobar.txt"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofFile() throws Exception {
    File file = new File("src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    ResourceLocator test = ResourceLocator.ofFile(file);
    assertThat(test.getLocator()).isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
  }

  @Test
  public void test_ofPath() throws Exception {
    Path path = Paths.get("src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    ResourceLocator test = ResourceLocator.ofPath(path);
    assertThat(test.getLocator().replace('\\', '/'))
        .isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString().replace('\\', '/'))
        .isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
  }

  @Test
  public void test_ofPath_zipFile() throws Exception {
    Path path = Paths.get("src/test/resources/com/opengamma/strata/collect/io/TestFile.zip");
    ResourceLocator test = ResourceLocator.ofPath(path);
    assertThat(test.getLocator().replace('\\', '/'))
        .isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.zip");
    byte[] read = test.getByteSource().read();
    assertThat(read[0]).isEqualTo((byte) 80);  // these are the standard header of a zip file
    assertThat(read[1]).isEqualTo((byte) 75);
    assertThat(read[2]).isEqualTo((byte) 3);
    assertThat(read[3]).isEqualTo((byte) 4);
    assertThat(test.toString().replace('\\', '/'))
        .isEqualTo("file:src/test/resources/com/opengamma/strata/collect/io/TestFile.zip");
  }

  @Test
  public void test_ofPath_fileInZipFile() throws Exception {
    Path zip = Paths.get("src/test/resources/com/opengamma/strata/collect/io/TestFile.zip");
    try (FileSystem fs = FileSystems.newFileSystem(zip, null)) {
      Path path = fs.getPath("TestFile.txt").toAbsolutePath();
      ResourceLocator test = ResourceLocator.ofPath(path);
      assertThat(test.getLocator())
          .startsWith("url:jar:file:")
          .endsWith("src/test/resources/com/opengamma/strata/collect/io/TestFile.zip!/TestFile.txt");
      assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
      assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
      assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
      assertThat(test.toString()).isEqualTo(test.getLocator());
    }
  }

  @Test
  public void test_ofUrl() throws Exception {
    File file = new File("src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    URL url = file.toURI().toURL();
    ResourceLocator test = ResourceLocator.ofUrl(url);
    assertThat(test.getLocator())
        .startsWith("url:file:")
        .endsWith("src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo(test.getLocator());
  }

  @Test
  public void test_ofClasspath_absolute() throws Exception {
    ResourceLocator test = ResourceLocator.ofClasspath("/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getLocator())
        .startsWith("classpath:")
        .endsWith("com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo(test.getLocator());
  }

  @Test
  public void test_ofClasspath_relativeConvertedToAbsolute() throws Exception {
    ResourceLocator test = ResourceLocator.ofClasspath("com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getLocator())
        .startsWith("classpath:")
        .endsWith("com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo(test.getLocator());
  }

  @Test
  public void test_ofClasspath_withClass_absolute() throws Exception {
    ResourceLocator test =
        ResourceLocator.ofClasspath(ResourceLocator.class, "/com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getLocator())
        .startsWith("classpath:")
        .endsWith("com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo(test.getLocator());
  }

  @Test
  public void test_ofClasspath_withClass_relative() throws Exception {
    ResourceLocator test = ResourceLocator.ofClasspath(ResourceLocator.class, "TestFile.txt");
    assertThat(test.getLocator())
        .startsWith("classpath:")
        .endsWith("com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo(test.getLocator());
  }

  @Test
  public void test_ofClasspathUrl() throws Exception {
    URL url = Resources.getResource("com/opengamma/strata/collect/io/TestFile.txt");
    ResourceLocator test = ResourceLocator.ofClasspathUrl(url);
    assertThat(test.getLocator())
        .startsWith("classpath:")
        .endsWith("com/opengamma/strata/collect/io/TestFile.txt");
    assertThat(test.getByteSource().read()[0]).isEqualTo((byte) 'H');
    assertThat(test.getCharSource().readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.getCharSource(StandardCharsets.UTF_8).readLines()).isEqualTo(ImmutableList.of("HelloWorld"));
    assertThat(test.toString()).isEqualTo(test.getLocator());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() throws Exception {
    File file1 = new File("src/test/resources/com/opengamma/strata/collect/io/TestFile.txt");
    File file2 = new File("src/test/resources/com/opengamma/strata/collect/io/Other.txt");
    ResourceLocator a1 = ResourceLocator.ofFile(file1);
    ResourceLocator a2 = ResourceLocator.ofFile(file1);
    ResourceLocator b = ResourceLocator.ofFile(file2);

    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

}
