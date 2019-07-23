/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test {@link XmlElement}.
 */
public class XmlElementTest {

  private static final Map<String, String> ATTR_MAP_EMPTY = ImmutableMap.of();
  private static final Map<String, String> ATTR_MAP = ImmutableMap.of("key", "value", "og", "strata");
  private static final XmlElement LEAF1 = XmlElement.ofContent("leaf1", ATTR_MAP_EMPTY, "leaf");
  private static final XmlElement LEAF2A = XmlElement.ofContent("leaf2", ATTR_MAP_EMPTY, "a");
  private static final XmlElement LEAF2B = XmlElement.ofContent("leaf2", ATTR_MAP_EMPTY, "b");
  private static final List<XmlElement> CHILD_LIST_EMPTY = ImmutableList.of();
  private static final List<XmlElement> CHILD_LIST_ONE = ImmutableList.of(LEAF1);
  private static final List<XmlElement> CHILD_LIST_MULTI = ImmutableList.of(LEAF1, LEAF2A, LEAF2B);

  //-------------------------------------------------------------------------
  @Test
  public void test_ofChildren_empty() {
    XmlElement test = XmlElement.ofChildren("test", CHILD_LIST_EMPTY);
    assertThat(test.getName()).isEqualTo("test");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(test.hasContent()).isEqualTo(false);
    assertThat(test.getContent()).isEqualTo("");
    assertThat(test.getChildren()).isEqualTo(CHILD_LIST_EMPTY);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute("notFound"));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getChild(0));
    assertThatIllegalArgumentException().isThrownBy(() -> test.getChild("notFound"));
    assertThat(test.findChild("notFound")).isEqualTo(Optional.empty());
    assertThat(test.getChildren("notFound")).isEqualTo(ImmutableList.of());
    assertThat(test.toString()).isEqualTo("<test></test>");
  }

  @Test
  public void test_ofChildren_one() {
    XmlElement test = XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_ONE);
    assertThat(test.getName()).isEqualTo("test");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP);
    assertThat(test.hasContent()).isEqualTo(false);
    assertThat(test.getContent()).isEqualTo("");
    assertThat(test.getChildren()).isEqualTo(CHILD_LIST_ONE);
    assertThat(test.getAttribute("key")).isEqualTo("value");
    assertThat(test.findAttribute("key")).isEqualTo(Optional.of("value"));
    assertThat(test.findAttribute("none")).isEqualTo(Optional.empty());
    assertThat(test.getChild(0)).isEqualTo(LEAF1);
    assertThat(test.getChild("leaf1")).isEqualTo(LEAF1);
    assertThat(test.findChild("leaf1")).isEqualTo(Optional.of(LEAF1));
    assertThat(test.getChildren("leaf1")).isEqualTo(ImmutableList.of(LEAF1));
    assertThat(test.toString()).isEqualTo("<test key=\"value\" og=\"strata\">" +
        System.lineSeparator() + " <leaf1 ... />" + System.lineSeparator() + "</test>");
  }

  @Test
  public void test_ofChildren_multi() {
    XmlElement test = XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_MULTI);
    assertThat(test.getName()).isEqualTo("test");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP);
    assertThat(test.getAttribute("key")).isEqualTo("value");
    assertThat(test.hasContent()).isEqualTo(false);
    assertThat(test.getContent()).isEqualTo("");
    assertThat(test.getChildren()).isEqualTo(CHILD_LIST_MULTI);
    assertThat(test.getAttribute("key")).isEqualTo("value");
    assertThat(test.getChild(0)).isEqualTo(LEAF1);
    assertThat(test.getChild(1)).isEqualTo(LEAF2A);
    assertThat(test.getChild(2)).isEqualTo(LEAF2B);
    assertThat(test.getChild("leaf1")).isEqualTo(LEAF1);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getChild("leaf2"));
    assertThat(test.findChild("leaf1")).isEqualTo(Optional.of(LEAF1));
    assertThatIllegalArgumentException().isThrownBy(() -> test.findChild("leaf2"));
    assertThat(test.getChildren("leaf1")).isEqualTo(ImmutableList.of(LEAF1));
    assertThat(test.getChildren("leaf2")).isEqualTo(ImmutableList.of(LEAF2A, LEAF2B));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofContent() {
    XmlElement test = XmlElement.ofContent("test", ATTR_MAP_EMPTY, "hello");
    assertThat(test.getName()).isEqualTo("test");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(test.hasContent()).isEqualTo(true);
    assertThat(test.getContent()).isEqualTo("hello");
    assertThat(test.getChildren()).isEqualTo(CHILD_LIST_EMPTY);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute("notFound"));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getChild(0));
    assertThatIllegalArgumentException().isThrownBy(() -> test.getChild("notFound"));
    assertThat(test.findChild("notFound")).isEqualTo(Optional.empty());
    assertThat(test.getChildren("notFound")).isEqualTo(ImmutableList.of());
    assertThat(test.toString()).isEqualTo("<test>hello</test>");
  }

  @Test
  public void test_ofContent_empty() {
    XmlElement test = XmlElement.ofContent("test", "");
    assertThat(test.getName()).isEqualTo("test");
    assertThat(test.getAttributes()).isEqualTo(ATTR_MAP_EMPTY);
    assertThat(test.hasContent()).isEqualTo(false);
    assertThat(test.getContent()).isEqualTo("");
    assertThat(test.getChildren()).isEqualTo(CHILD_LIST_EMPTY);
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute("notFound"));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getChild(0));
    assertThatIllegalArgumentException().isThrownBy(() -> test.getChild("notFound"));
    assertThat(test.findChild("notFound")).isEqualTo(Optional.empty());
    assertThat(test.getChildren("notFound")).isEqualTo(ImmutableList.of());
    assertThat(test.toString()).isEqualTo("<test></test>");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    XmlElement test = XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_MULTI);
    coverImmutableBean(test);
    XmlElement test2 = XmlElement.ofChildren("test2", ATTR_MAP_EMPTY, CHILD_LIST_EMPTY);
    coverBeanEquals(test, test2);
    XmlElement test3 = XmlElement.ofContent("test3", ATTR_MAP_EMPTY, "content");
    coverBeanEquals(test2, test3);
    coverBeanEquals(test, test3);
  }

}
