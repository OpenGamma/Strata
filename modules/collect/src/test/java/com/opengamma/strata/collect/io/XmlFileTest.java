/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;

/**
 * Test {@link XmlFile}.
 */
public class XmlFileTest {

  private static final String SAMPLE = "" +
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
      "<!--Leading comment-->" +
      "<base>" +
      " <test key=\"value\" og=\"strata\">" +
      "  <leaf1>l<![CDATA[e]]>af</leaf1>" +
      "  <leaf2>a<!-- comment ignored --></leaf2>" +
      "  <leaf2>b</leaf2>" +
      "  <obj><leaf3>c</leaf3></obj>" +
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
      "<base xmlns=\"https://opengamma.com/test\" xmlns:h=\"http://www.w3.org/TR/html4/\">" +
      " <h:p>Some text</h:p>" +
      " <leaf1 h:foo='bla' og='strata'>leaf</leaf1>" +
      "</base>";

  private static final Map<String, String> ATTR_MAP_EMPTY = ImmutableMap.of();
  private static final Map<String, String> ATTR_MAP = ImmutableMap.of("key", "value", "og", "strata");
  private static final XmlElement LEAF1 = XmlElement.ofContent("leaf1", "leaf");
  private static final XmlElement LEAF2A = XmlElement.ofContent("leaf2", "a");
  private static final XmlElement LEAF2B = XmlElement.ofContent("leaf2", "b");
  private static final XmlElement LEAF3 = XmlElement.ofContent("leaf3", "c");
  private static final XmlElement OBJ = XmlElement.ofChildren("obj", ImmutableList.of(LEAF3));
  private static final List<XmlElement> CHILD_LIST_MULTI = ImmutableList.of(LEAF1, LEAF2A, LEAF2B, OBJ);
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_of_ByteSource() {
    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source);
    XmlElement root = test.getRoot();
    assertThat(root.getName()).isEqualTo("base");
    assertThat(root.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(root.getContent()).isEqualTo("");
    assertThat(root.getChildren().size()).isEqualTo(1);
    XmlElement child = root.getChild(0);
    assertThat(child).isEqualTo(XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_MULTI));
    assertThat(test.getReferences()).isEqualTo(ImmutableMap.of());
  }

  @Test
  public void test_of_ByteSource_namespace() {
    ByteSource source = ByteSource.wrap(SAMPLE_NAMESPACE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source);
    XmlElement root = test.getRoot();
    assertThat(root.getName()).isEqualTo("base");
    assertThat(root.getAttributes()).isEqualTo(ImmutableMap.of());
    assertThat(root.getContent()).isEqualTo("");
    assertThat(root.getChildren().size()).isEqualTo(2);
    XmlElement child1 = root.getChild(0);
    assertThat(child1.getName()).isEqualTo("p");
    assertThat(child1.getContent()).isEqualTo("Some text");
    assertThat(child1.getAttributes()).isEqualTo(ImmutableMap.of());
    XmlElement child2 = root.getChild(1);
    assertThat(child2.getName()).isEqualTo("leaf1");
    assertThat(child2.getContent()).isEqualTo("leaf");
    assertThat(child2.getAttributes()).isEqualTo(ImmutableMap.of("foo", "bla", "og", "strata"));
    assertThat(test.getReferences()).isEqualTo(ImmutableMap.of());
  }

  @Test
  public void test_of_ByteSource_mismatchedTags() {
    ByteSource source = ByteSource.wrap(SAMPLE_MISMATCHED_TAGS.getBytes(StandardCharsets.UTF_8));
    assertThatIllegalArgumentException().isThrownBy(() -> XmlFile.of(source));
  }

  @Test
  public void test_of_ByteSource_badEnd() {
    ByteSource source = ByteSource.wrap(SAMPLE_BAD_END.getBytes(StandardCharsets.UTF_8));
    assertThatIllegalArgumentException().isThrownBy(() -> XmlFile.of(source));
  }

  @Test
  public void test_of_ByteSource_ioException() {
    ByteSource source = Files.asByteSource(new File("/oh-dear-no-such-file"));
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> XmlFile.of(source));
  }

  @Test
  public void test_of_ByteSource_parsedReferences() {
    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source, "key");
    XmlElement root = test.getRoot();
    assertThat(root.getName()).isEqualTo("base");
    assertThat(root.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(root.getContent()).isEqualTo("");
    assertThat(root.getChildren().size()).isEqualTo(1);
    XmlElement child = root.getChild(0);
    assertThat(child).isEqualTo(XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_MULTI));
    assertThat(test.getReferences()).isEqualTo(ImmutableMap.of("value", root.getChild(0)));
  }

  @Test
  public void test_of_ByteSource_parsedReferences_ioException() {
    ByteSource source = Files.asByteSource(new File("/oh-dear-no-such-file"));
    assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> XmlFile.of(source, "key"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parseElements_ByteSource_Fn_noFilter() {
    List<XmlElement> expected = ImmutableList.of(
        XmlElement.ofContent("leaf1", ""),
        XmlElement.ofContent("leaf2", ""),
        XmlElement.ofContent("leaf2", ""),
        XmlElement.ofChildren("obj", ImmutableList.of(XmlElement.ofContent("leaf3", ""))));

    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlElement test = XmlFile.parseElements(source, name -> Integer.MAX_VALUE);
    assertThat(test.getName()).isEqualTo("base");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(test.getContent()).isEqualTo("");
    assertThat(test.getChildren().size()).isEqualTo(1);
    XmlElement child = test.getChild(0);
    assertThat(child).isEqualTo(XmlElement.ofChildren("test", expected));
  }

  @Test
  public void test_parseElements_ByteSource_Fn_filterAll() {
    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlElement test = XmlFile.parseElements(source, name -> name.equals("test") ? 0 : Integer.MAX_VALUE);
    assertThat(test.getName()).isEqualTo("base");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(test.getContent()).isEqualTo("");
    assertThat(test.getChildren().size()).isEqualTo(1);
    XmlElement child = test.getChild(0);
    assertThat(child).isEqualTo(XmlElement.ofContent("test", ""));
  }

  @Test
  public void test_parseElements_ByteSource_Fn_filterOneLevel() {
    List<XmlElement> expected = ImmutableList.of(
        XmlElement.ofContent("leaf1", ""),
        XmlElement.ofContent("leaf2", ""),
        XmlElement.ofContent("leaf2", ""),
        XmlElement.ofContent("obj", ""));

    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlElement test = XmlFile.parseElements(source, name -> name.equals("test") ? 1 : Integer.MAX_VALUE);
    assertThat(test.getName()).isEqualTo("base");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(test.getContent()).isEqualTo("");
    assertThat(test.getChildren().size()).isEqualTo(1);
    XmlElement child = test.getChild(0);
    assertThat(child).isEqualTo(XmlElement.ofChildren("test", expected));
  }

  @Test
  public void test_parseElements_ByteSource_Fn_mismatchedTags() {
    ByteSource source = ByteSource.wrap(SAMPLE_MISMATCHED_TAGS.getBytes(StandardCharsets.UTF_8));
    assertThatIllegalArgumentException().isThrownBy(() -> XmlFile.parseElements(source, name -> Integer.MAX_VALUE));
  }

  @Test
  public void test_parseElements_ByteSource_Fn_badEnd() {
    ByteSource source = ByteSource.wrap(SAMPLE_BAD_END.getBytes(StandardCharsets.UTF_8));
    assertThatIllegalArgumentException().isThrownBy(() -> XmlFile.parseElements(source, name -> Integer.MAX_VALUE));
  }

  @Test
  public void test_parseElements_ByteSource_Fn_ioException() {
    ByteSource source = Files.asByteSource(new File("/oh-dear-no-such-file"));
    assertThatExceptionOfType(UncheckedIOException.class)
        .isThrownBy(() -> XmlFile.parseElements(source, name -> Integer.MAX_VALUE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCodeToString() {
    ByteSource source = ByteSource.wrap(SAMPLE.getBytes(StandardCharsets.UTF_8));
    XmlFile test = XmlFile.of(source);
    XmlFile test2 = XmlFile.of(source);
    assertThat(test)
        .isEqualTo(test)
        .isEqualTo(test2)
        .isNotEqualTo(null)
        .isNotEqualTo(ANOTHER_TYPE)
        .hasSameHashCodeAs(test2);
    assertThat(test.toString()).isEqualTo(test2.toString());
  }

}
