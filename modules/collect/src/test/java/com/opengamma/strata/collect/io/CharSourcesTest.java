/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.testng.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import org.testng.annotations.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.opengamma.strata.collect.TestHelper;

/**
 * Tests {@link CharSources}
 */
@Test
public class CharSourcesTest {

  private final String fileName = "src/test/resources/com/opengamma/strata/collect/io/utf16le.txt";

  public void testPrivateConstructor() throws Exception {
    TestHelper.coverPrivateConstructor(CharSources.class);
  }

  public void testOfFileName() throws Exception {
    CharSource charSource = CharSources.ofFileName(fileName);
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  public void testOfFileNameWithCharset() throws Exception {
    CharSource charSource = CharSources.ofFileName(fileName, Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

  public void testOfFile() throws Exception {
    CharSource charSource = CharSources.ofFile(new File(fileName));
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  public void testOfFileWithCharset() throws Exception {
    CharSource charSource = CharSources.ofFile(new File(fileName), Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

  public void testOfPath() throws Exception {
    CharSource charSource = CharSources.ofPath(Paths.get(fileName));
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  public void testOfPathWithCharset() throws Exception {
    CharSource charSource = CharSources.ofPath(Paths.get(fileName), Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }

  @Test
  public void testOfUrl() throws Exception {
    String fullPathToFile = "file:///" + System.getProperty("user.dir") + "/" + fileName;
    URL url = new URL(fullPathToFile);
    CharSource charSource = CharSources.ofUrl(url);
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
  }

  @Test
  public void testOfUrlWithCharset() throws Exception {
    String fullPathToFile = "file:///" + System.getProperty("user.dir") + "/" + fileName;
    URL url = new URL(fullPathToFile);
    CharSource charSource = CharSources.ofUrl(url, Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
  }

  @Test
  public void testOfContentString() throws Exception {
    CharSource charSource = CharSources.ofContent("H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  @Test
  public void testOfContentByteArray() throws Exception {
    byte[] inputText = "H\u0000e\u0000l\u0000l\u0000o\u0000".getBytes(Charsets.UTF_8);
    CharSource charSource = CharSources.ofContent(inputText);
    assertEquals(charSource.readFirstLine(), "H\u0000e\u0000l\u0000l\u0000o\u0000");
    assertEquals(charSource.length(), 10);
  }

  @Test
  public void testOfContentByteArrayWithCharset() throws Exception {
    byte[] inputText = "H\u0000e\u0000l\u0000l\u0000o\u0000".getBytes(Charsets.UTF_8);
    CharSource charSource = CharSources.ofContent(inputText, Charsets.UTF_16LE);
    assertEquals(charSource.readFirstLine(), "Hello");
    assertEquals(charSource.length(), 5);
  }
}
