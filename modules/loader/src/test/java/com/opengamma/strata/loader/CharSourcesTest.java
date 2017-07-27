/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.io.ArrayByteSource;
import com.opengamma.strata.collect.io.ResourceLocator;

public class CharSourcesTest {

  private final String fileName = "src/test/resources/com/opengamma/strata/loader/utf16le.txt";

  @Test
  public void testOfFileName() throws Exception {
    CharSource charSource = CharSources.ofFileName(fileName);
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  @Test
  public void testOfFileNameWithCharset() throws Exception {
    CharSource charSource = CharSources.ofFileName(fileName, Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

  @Test
  public void testOfFile() throws Exception {
    CharSource charSource = CharSources.ofFile(new File(fileName));
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  @Test
  public void testOfFileWithCharset() throws Exception {
    CharSource charSource = CharSources.ofFile(new File(fileName), Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

  @Test
  public void testOfPath() throws Exception {
    CharSource charSource = CharSources.ofPath(Paths.get(fileName));
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  @Test
  public void testOfPathWithCharset() throws Exception {
    CharSource charSource = CharSources.ofPath(Paths.get(fileName), Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

  @Test
  public void testOfResourceLocator() throws Exception {
    ResourceLocator resourceLocator = ResourceLocator.ofClasspath(CharSources.class, "utf16le.txt");
    CharSource charSource = CharSources.ofResourceLocator(resourceLocator);
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  @Test
  public void testOfResourceLocatorWithCharset() throws Exception {
    ResourceLocator resourceLocator = ResourceLocator.ofClasspath(CharSources.class, "utf16le.txt");
    CharSource charSource = CharSources.ofResourceLocator(resourceLocator, Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

  @Test
  public void testOfByteSource() throws Exception {
    ArrayByteSource arrayByteSource = ArrayByteSource.copyOf("H\u0000e\u0000l\u0000l\u0000o\u0000".getBytes());
    CharSource charSource = CharSources.ofByteSource(arrayByteSource);
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  @Test
  public void testOfByteSourceWithCharset() throws Exception {
    ArrayByteSource arrayByteSource = ArrayByteSource.copyOf("H\u0000e\u0000l\u0000l\u0000o\u0000".getBytes());
    CharSource charSource = CharSources.ofByteSource(arrayByteSource, Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

}