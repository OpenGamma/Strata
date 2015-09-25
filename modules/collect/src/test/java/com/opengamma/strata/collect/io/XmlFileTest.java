/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

/**
 * Test {@link XmlFile}.
 */
@Test
public class XmlFileTest {

  private static final String SAMPLE = "" +
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<!--Leading comment-->" +
      "<base>" +
      " <test key=\"value\" og=\"strata\">" +
      "  <leaf1>l<![CDATA[e]]>af</leaf1>" +
      "  <leaf2>a<!-- comment ignored --></leaf2>" +
      "  <leaf2>b</leaf2>" +
      " </test>" +
      "</base>";
  private static final String SAMPLE_MISMATCHED_TAGS = "" +
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<base>" +
      " <test>" +
      " </foo>" +
      "</base>";
  private static final String SAMPLE_BAD_END = "" +
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<base>" +
      " <test>" +
      " </foo>";
  private static final String SAMPLE_NAMESPACE = "" +
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<base xmlns=\"http://www.opengamma.com/test\" xmlns:h=\"http://www.w3.org/TR/html4/\">" +
      " <h:p>Some text</h:p>" +
      " <leaf1 h:foo='bla' og='strata'>leaf</leaf1>" +
      "</base>";

  private static final Map<String, String> ATTR_MAP_EMPTY = ImmutableMap.of();
  private static final Map<String, String> ATTR_MAP = ImmutableMap.of("key", "value", "og", "strata");
  private static final XmlElement LEAF1 = XmlElement.ofContent("leaf1", ATTR_MAP_EMPTY, "leaf");
  private static final XmlElement LEAF2A = XmlElement.ofContent("leaf2", ATTR_MAP_EMPTY, "a");
  private static final XmlElement LEAF2B = XmlElement.ofContent("leaf2", ATTR_MAP_EMPTY, "b");
  private static final List<XmlElement> CHILD_LIST_MULTI = ImmutableList.of(LEAF1, LEAF2A, LEAF2B);

  //-------------------------------------------------------------------------
  public void test_of_ByteSource() {
    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source);
    XmlElement root = test.getRoot();
    assertEquals(root.getName(), "base");
    assertEquals(root.getAttributes(), ATTR_MAP_EMPTY);
    assertEquals(root.getContent(), "");
    assertEquals(root.getChildren().size(), 1);
    XmlElement child = root.getChild(0);
    assertEquals(child, XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_MULTI));
    assertEquals(test.getReferences(), ImmutableMap.of());
  }

  public void test_of_ByteSource_namespace() {
    ByteSource source = ByteSource.wrap(SAMPLE_NAMESPACE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source);
    XmlElement root = test.getRoot();
    assertEquals(root.getName(), "base");
    assertEquals(root.getAttributes(), ImmutableMap.of());
    assertEquals(root.getContent(), "");
    assertEquals(root.getChildren().size(), 2);
    XmlElement child1 = root.getChild(0);
    assertEquals(child1.getName(), "p");
    assertEquals(child1.getContent(), "Some text");
    assertEquals(child1.getAttributes(), ImmutableMap.of());
    XmlElement child2 = root.getChild(1);
    assertEquals(child2.getName(), "leaf1");
    assertEquals(child2.getContent(), "leaf");
    assertEquals(child2.getAttributes(), ImmutableMap.of("foo", "bla", "og", "strata"));
    assertEquals(test.getReferences(), ImmutableMap.of());
  }

  public void test_of_ByteSource_mismatchedTags() {
    ByteSource source = ByteSource.wrap(SAMPLE_MISMATCHED_TAGS.getBytes(StandardCharsets.UTF_8));
    assertThrowsIllegalArg(() -> XmlFile.of(source));
  }

  public void test_of_ByteSource_badEnd() {
    ByteSource source = ByteSource.wrap(SAMPLE_BAD_END.getBytes(StandardCharsets.UTF_8));
    assertThrowsIllegalArg(() -> XmlFile.of(source));
  }

  public void test_of_ByteSource_ioException() {
    ByteSource source = Files.asByteSource(new File("/oh-dear-no-such-file"));
    assertThrows(() -> XmlFile.of(source), UncheckedIOException.class);
  }

  public void test_of_ByteSource_parsedReferences() {
    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source, "key");
    XmlElement root = test.getRoot();
    assertEquals(root.getName(), "base");
    assertEquals(root.getAttributes(), ATTR_MAP_EMPTY);
    assertEquals(root.getContent(), "");
    assertEquals(root.getChildren().size(), 1);
    XmlElement child = root.getChild(0);
    assertEquals(child, XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_MULTI));
    assertEquals(test.getReferences(), ImmutableMap.of("value", root.getChild(0)));
  }

  public void test_of_ByteSource_parsedReferences_ioException() {
    ByteSource source = Files.asByteSource(new File("/oh-dear-no-such-file"));
    assertThrows(() -> XmlFile.of(source, "key"), UncheckedIOException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCodeToString() {
    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source);
    XmlFile test2 = XmlFile.of(source);
    assertFalse(test.equals(null));
    assertFalse(test.equals(""));
    assertEquals(test, test);
    assertEquals(test, test2);
    assertEquals(test.hashCode(), test2.hashCode());
    assertEquals(test.toString(), test2.toString());
  }

}
