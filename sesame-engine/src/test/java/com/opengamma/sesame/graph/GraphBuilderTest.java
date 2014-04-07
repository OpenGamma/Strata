/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import static com.opengamma.sesame.config.ConfigBuilder.column;
import static com.opengamma.sesame.config.ConfigBuilder.configureView;
import static com.opengamma.sesame.config.ConfigBuilder.output;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewConfig;
import com.opengamma.sesame.config.ViewOutput;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.AvailableImplementationsImpl;
import com.opengamma.sesame.function.AvailableOutputsImpl;
import com.opengamma.sesame.function.InvokableFunction;
import com.opengamma.sesame.function.NoOutputFunction;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.test.TestGroup;

@Test(groups = TestGroup.UNIT)
public class GraphBuilderTest {

  private static final String OUTPUT_NAME = "TheOutputName";
  private static final GraphBuilder EMPTY_GRAPH_BUILDER = new GraphBuilder(new AvailableOutputsImpl(),
                                                                           new AvailableImplementationsImpl(),
                                                                           Collections.<Class<?>>emptySet(),
                                                                           FunctionModelConfig.EMPTY,
                                                                           NodeDecorator.IDENTITY);
  private static final String COLUMN_NAME = "column name";

  /**
   * checks a no-op function is inserted when no output name is specified for an input type.
   */
  @Test
  public void noOutputNameForPortfolioOutput() {
    // have to create a column using the constructor because ConfigBuilder doesn't allow a column with no default
    // output name. and that's what we're trying to test
    ViewColumn column = new ViewColumn(COLUMN_NAME, null, Collections.<Class<?>, ViewOutput>emptyMap());
    List<ViewColumn> columns = ImmutableList.of(column);
    ViewConfig viewConfig = new ViewConfig("view name", FunctionModelConfig.EMPTY, columns);
    GraphModel graphModel = EMPTY_GRAPH_BUILDER.build(viewConfig, Collections.<Class<?>>singleton(String.class));
    FunctionModel functionModel = graphModel.getFunctionModel(COLUMN_NAME, String.class);

    ArgumentChecker.notNull(functionModel, "functionModel");
    InvokableFunction invokableFunction = functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY);
    Object receiver = invokableFunction.getReceiver();
    assertTrue(receiver instanceof NoOutputFunction);
  }

  /**
   * checks a no-op function is inserted when an output name is specified for an input type but there is no
   * function that can provide it.
   */
  @Test
  public void outputNameButNoFunctionForPortfolioOutput() {
    ViewConfig viewConfig = configureView("view name", column(OUTPUT_NAME, output(Integer.class)));
    GraphModel graphModel = EMPTY_GRAPH_BUILDER.build(viewConfig, Collections.<Class<?>>singleton(String.class));
    FunctionModel functionModel = graphModel.getFunctionModel(OUTPUT_NAME, String.class);

    assertNotNull(functionModel);
    InvokableFunction invokableFunction = functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY);
    Object receiver = invokableFunction.getReceiver();
    assertTrue(receiver instanceof NoOutputFunction);
  }
}
