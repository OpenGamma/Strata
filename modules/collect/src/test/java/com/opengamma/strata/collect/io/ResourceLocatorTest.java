/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * Test {@link ResourceLocator}.
 */
@Test
public class ResourceLocatorTest {

  public void test_of_filePrefixed() throws Exception {
    ResourceLocator test = ResourceLocator.of("file:src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
    assertEquals(test.getLocator(), "file:src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
    assertEquals(test.getByteSource().read()[0], 'H');
    assertEquals(test.getCharSource().readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.getCharSource(StandardCharsets.UTF_8).readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.toString(), "file:src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
  }

  public void test_of_fileNoPrefix() throws Exception {
    ResourceLocator test = ResourceLocator.of("src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
    assertEquals(test.getLocator(), "file:src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
    assertEquals(test.getByteSource().read()[0], 'H');
    assertEquals(test.getCharSource().readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.getCharSource(StandardCharsets.UTF_8).readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.toString(), "file:src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
  }

  public void test_of_classpath() throws Exception {
    ResourceLocator test = ResourceLocator.of("classpath:com/opengamma/strata/config/base/TestFile.txt");
    assertEquals(test.getLocator().startsWith("classpath"), true);
    assertEquals(test.getLocator().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
    assertEquals(test.getByteSource().read()[0], 'H');
    assertEquals(test.getCharSource().readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.getCharSource(StandardCharsets.UTF_8).readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.toString().startsWith("classpath"), true);
    assertEquals(test.toString().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
  }

  public void test_of_invalid() throws Exception {
    assertThrowsIllegalArg(() -> ResourceLocator.of("classpath:http:https:file:/foobar.txt"));
  }

  //-------------------------------------------------------------------------
  public void test_ofFile() throws Exception {
    File file = new File("src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
    ResourceLocator test = ResourceLocator.ofFile(file);
    assertEquals(test.getLocator(), "file:src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
    assertEquals(test.getByteSource().read()[0], 'H');
    assertEquals(test.getCharSource().readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.getCharSource(StandardCharsets.UTF_8).readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.toString(), "file:src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
  }

  public void test_ofClasspath() throws Exception {
    ResourceLocator test = ResourceLocator.ofClasspath("com/opengamma/strata/config/base/TestFile.txt");
    assertEquals(test.getLocator().startsWith("classpath"), true);
    assertEquals(test.getLocator().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
    assertEquals(test.getByteSource().read()[0], 'H');
    assertEquals(test.getCharSource().readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.getCharSource(StandardCharsets.UTF_8).readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.toString().startsWith("classpath"), true);
    assertEquals(test.toString().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
  }

  public void test_ofClasspathUrl() throws Exception {
    URL url = Resources.getResource("com/opengamma/strata/config/base/TestFile.txt");
    ResourceLocator test = ResourceLocator.ofClasspathUrl(url);
    assertEquals(test.getLocator().startsWith("classpath"), true);
    assertEquals(test.getLocator().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
    assertEquals(test.getByteSource().read()[0], 'H');
    assertEquals(test.getCharSource().readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.getCharSource(StandardCharsets.UTF_8).readLines(), ImmutableList.of("HelloWorld"));
    assertEquals(test.toString().startsWith("classpath"), true);
    assertEquals(test.toString().endsWith("com/opengamma/strata/config/base/TestFile.txt"), true);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() throws Exception {
    File file1 = new File("src/test/resources/com/opengamma/strata/config/base/TestFile.txt");
    File file2 = new File("src/test/resources/com/opengamma/strata/config/base/Other.txt");
    ResourceLocator a1 = ResourceLocator.ofFile(file1);
    ResourceLocator a2 = ResourceLocator.ofFile(file1);
    ResourceLocator b = ResourceLocator.ofFile(file2);

    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

}
