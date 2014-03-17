/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.util.test.TestGroup;

/**
 * Test.
 */
@Test(groups = TestGroup.UNIT)
public class CompositeNodeDecoratorTest {

  private static final NodeDecorator NODE_DECORATOR = new NodeDecorator() {
    @Override
    public FunctionModelNode decorateNode(FunctionModelNode node) {
      return node;
    }
  };

  @Test
  public void compose_array_empty() {
    NodeDecorator composed = CompositeNodeDecorator.compose(new NodeDecorator[0]);
    assertEquals(NodeDecorator.IDENTITY, composed);
  }

  @Test
  public void compose_array_one() {
    NodeDecorator composed = CompositeNodeDecorator.compose(new NodeDecorator[] {NODE_DECORATOR});
    assertEquals(NODE_DECORATOR, composed);
  }

  @Test
  public void compose_array_two() {
    final List<NodeDecorator> list = new ArrayList<>();
    NodeDecorator nd1 = new NodeDecorator() {
      @Override
      public FunctionModelNode decorateNode(FunctionModelNode node) {
        list.add(this);
        return node;
      }
    };
    NodeDecorator nd2 = new NodeDecorator() {
      @Override
      public FunctionModelNode decorateNode(FunctionModelNode node) {
        list.add(this);
        return node;
      }
    };
    NodeDecorator composed = CompositeNodeDecorator.compose(new NodeDecorator[] {nd1, nd2});
    assertTrue(composed instanceof CompositeNodeDecorator);
    composed.decorateNode(null);
    assertEquals(2, list.size());
    assertEquals(nd2, list.get(0));
    assertEquals(nd1, list.get(1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void compose_array_null() {
    CompositeNodeDecorator.compose((NodeDecorator[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void compose_array_nullEntry() {
    CompositeNodeDecorator.compose(new NodeDecorator[] {null});
  }

  //-------------------------------------------------------------------------
  @Test
  public void compose_list_empty() {
    NodeDecorator composed = CompositeNodeDecorator.compose(new ArrayList<NodeDecorator>());
    assertEquals(NodeDecorator.IDENTITY, composed);
  }

  @Test
  public void compose_list_one() {
    NodeDecorator composed = CompositeNodeDecorator.compose(Arrays.asList(NODE_DECORATOR));
    assertEquals(NODE_DECORATOR, composed);
  }

  @Test
  public void compose_list_two() {
    final List<NodeDecorator> list = new ArrayList<>();
    NodeDecorator nd1 = new NodeDecorator() {
      @Override
      public FunctionModelNode decorateNode(FunctionModelNode node) {
        list.add(this);
        return node;
      }
    };
    NodeDecorator nd2 = new NodeDecorator() {
      @Override
      public FunctionModelNode decorateNode(FunctionModelNode node) {
        list.add(this);
        return node;
      }
    };
    NodeDecorator composed = CompositeNodeDecorator.compose(Arrays.asList(nd1, nd2));
    assertTrue(composed instanceof CompositeNodeDecorator);
    composed.decorateNode(null);
    assertEquals(2, list.size());
    assertEquals(nd2, list.get(0));
    assertEquals(nd1, list.get(1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void compose_list_null() {
    CompositeNodeDecorator.compose((List<NodeDecorator>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void compose_list_nullEntry() {
    CompositeNodeDecorator.compose(Arrays.asList((NodeDecorator) null));
  }

}
