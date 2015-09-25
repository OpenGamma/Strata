/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Test {@link XmlElement}.
 */
@Test
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
  public void test_ofChildren_empty() {
    XmlElement test = XmlElement.ofChildren("test", CHILD_LIST_EMPTY);
    assertEquals(test.getName(), "test");
    assertEquals(test.getAttributes(), ATTR_MAP_EMPTY);
    assertEquals(test.hasContent(), false);
    assertEquals(test.getContent(), "");
    assertEquals(test.getChildren(), CHILD_LIST_EMPTY);
    assertThrowsIllegalArg(() -> test.getAttribute("notFound"));
    assertThrows(() -> test.getChild(0), IndexOutOfBoundsException.class);
    assertThrowsIllegalArg(() -> test.getChild("notFound"));
    assertEquals(test.findChild("notFound"), Optional.empty());
    assertEquals(test.getChildren("notFound"), ImmutableList.of());
    assertEquals(test.toString(), "<test></test>");
  }

  public void test_ofChildren_one() {
    XmlElement test = XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_ONE);
    assertEquals(test.getName(), "test");
    assertEquals(test.getAttributes(), ATTR_MAP);
    assertEquals(test.hasContent(), false);
    assertEquals(test.getContent(), "");
    assertEquals(test.getChildren(), CHILD_LIST_ONE);
    assertEquals(test.getAttribute("key"), "value");
    assertEquals(test.findAttribute("key"), Optional.of("value"));
    assertEquals(test.findAttribute("none"), Optional.empty());
    assertEquals(test.getChild(0), LEAF1);
    assertEquals(test.getChild("leaf1"), LEAF1);
    assertEquals(test.findChild("leaf1"), Optional.of(LEAF1));
    assertEquals(test.getChildren("leaf1"), ImmutableList.of(LEAF1));
    assertEquals(test.toString(), "<test key=\"value\" og=\"strata\">" +
        System.lineSeparator() + " <leaf1 ... />" + System.lineSeparator() + "</test>");
  }

  public void test_ofChildren_multi() {
    XmlElement test = XmlElement.ofChildren("test", ATTR_MAP, CHILD_LIST_MULTI);
    assertEquals(test.getName(), "test");
    assertEquals(test.getAttributes(), ATTR_MAP);
    assertEquals(test.getAttribute("key"), "value");
    assertEquals(test.hasContent(), false);
    assertEquals(test.getContent(), "");
    assertEquals(test.getChildren(), CHILD_LIST_MULTI);
    assertEquals(test.getAttribute("key"), "value");
    assertEquals(test.getChild(0), LEAF1);
    assertEquals(test.getChild(1), LEAF2A);
    assertEquals(test.getChild(2), LEAF2B);
    assertEquals(test.getChild("leaf1"), LEAF1);
    assertThrowsIllegalArg(() -> test.getChild("leaf2"));
    assertEquals(test.findChild("leaf1"), Optional.of(LEAF1));
    assertThrowsIllegalArg(() -> test.findChild("leaf2"));
    assertEquals(test.getChildren("leaf1"), ImmutableList.of(LEAF1));
    assertEquals(test.getChildren("leaf2"), ImmutableList.of(LEAF2A, LEAF2B));
  }

  //-------------------------------------------------------------------------
  public void test_ofContent() {
    XmlElement test = XmlElement.ofContent("test", ATTR_MAP_EMPTY, "hello");
    assertEquals(test.getName(), "test");
    assertEquals(test.getAttributes(), ATTR_MAP_EMPTY);
    assertEquals(test.hasContent(), true);
    assertEquals(test.getContent(), "hello");
    assertEquals(test.getChildren(), CHILD_LIST_EMPTY);
    assertThrowsIllegalArg(() -> test.getAttribute("notFound"));
    assertThrows(() -> test.getChild(0), IndexOutOfBoundsException.class);
    assertThrowsIllegalArg(() -> test.getChild("notFound"));
    assertEquals(test.findChild("notFound"), Optional.empty());
    assertEquals(test.getChildren("notFound"), ImmutableList.of());
    assertEquals(test.toString(), "<test>hello</test>");
  }

  public void test_ofContent_empty() {
    XmlElement test = XmlElement.ofContent("test", "");
    assertEquals(test.getName(), "test");
    assertEquals(test.getAttributes(), ATTR_MAP_EMPTY);
    assertEquals(test.hasContent(), false);
    assertEquals(test.getContent(), "");
    assertEquals(test.getChildren(), CHILD_LIST_EMPTY);
    assertThrowsIllegalArg(() -> test.getAttribute("notFound"));
    assertThrows(() -> test.getChild(0), IndexOutOfBoundsException.class);
    assertThrowsIllegalArg(() -> test.getChild("notFound"));
    assertEquals(test.findChild("notFound"), Optional.empty());
    assertEquals(test.getChildren("notFound"), ImmutableList.of());
    assertEquals(test.toString(), "<test></test>");
  }

  //-------------------------------------------------------------------------
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
