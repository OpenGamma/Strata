/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.inject.Provider;

import com.opengamma.core.link.Link;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.Parameter;
import com.opengamma.util.ArgumentChecker;

/**
 * A node in the function model.
 */
public abstract class Node {

  /**
   * The expected type of the object created by this node, not null.
   */
  private final Class<?> _type;
  /**
   * The parameter this node satisfies, null if it's the root node.
   */
  private final Parameter _parameter;

  /**
   * Creates an instance.
   * 
   * @param type  the expected type of the object created by this node, not null
   * @param parameter  the parameter this node satisfies, null if it's the root node
   */
  Node(Class<?> type, Parameter parameter) {
    _type = ArgumentChecker.notNull(type, "type");
    _parameter = parameter;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the expected type of the object created by this node.
   * 
   * @return the expected type, not null
   */
  public Class<?> getType() {
    return _type;
  }

  /**
   * Gets the parameter that this node satisfies.
   * 
   * @return the parameter, not null
   */
  public Parameter getParameter() {
    return _parameter;
  }

  /**
   * Gets the concrete, non-proxy, node.
   * <p>
   * This is used to access the concrete node that has been proxied.
   * Most nodes simply return {@code this}.
   * 
   * @return the parameter, not null
   */
  Node getConcreteNode() {
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates the object represented by this node.
   * <p>
   * Implementations should override {@link #doCreate}, not this method.
   * 
   * @param componentMap  the map of infrastructure components, not null
   * @param dependencies  the dependencies of this node, not null
   * @return the object represented by this node, may be null
   */
  public Object create(ComponentMap componentMap, List<Object> dependencies) {
    Object object = doCreate(componentMap, dependencies);
    if (object instanceof Provider) {
      // TODO some slightly more robust checking of compatibility of types
      // TODO what's the logic I actually need here?
      return Provider.class.isAssignableFrom(_type) ? object : ((Provider<?>) object).get();
    } else if (object instanceof Link) {
      return Link.class.isAssignableFrom(_type) ? object : ((Link<?, ?>) object).resolve();
    } else {
      return object;
    }
  }

  /**
   * Returns the object represented by this node, creating if necessary.
   * <p>
   * If this node's object is a {@link Provider} this method should return it,
   * not the results of calling {@link Provider#get()}. This class will use the
   * expected type to decide whether to call {@code get()} or
   * to inject the provider instance directly.
   * 
   * @param componentMap  the map of infrastructure components, not null
   * @param dependencies  the dependencies of this node, not null
   * @return the object represented by this node, may be null
   */
  protected abstract Object doCreate(ComponentMap componentMap, List<Object> dependencies);

  //-------------------------------------------------------------------------
  /**
   * Gets the dependencies of this node.
   * 
   * @return the dependencies, not null
   */
  public List<Node> getDependencies() {
    return Collections.emptyList();
  }

  /**
   * Gets the complete set of exceptions in the tree of this node.
   * 
   * @return the list of exceptions, not null
   */
  public List<InvalidGraphException> getExceptions() {
    return Collections.emptyList();
  }

  /**
   * Checks if this node represents a valid object that can be constructed.
   * <p>
   * A true result implies that this node and all nodes below it in the dependency tree are valid.
   * 
   * @return true if this node and all its dependencies are valid
   */
  public boolean isValid() {
    return true;
  }

  /**
   * Checks if this node is an error node.
   * 
   * @return true if this node represents an object that is the source of an error
   */
  public boolean isError() {
    return false;
  }

  //-------------------------------------------------------------------------
  /**
   * Pretty prints this node.
   * <p>
   * Implementations should override this to perform the pretty print.
   * 
   * @param showProxies  true to include proxy nodes
   * @return the node structure, not null
   */
  public String prettyPrint(boolean showProxies) {
    return prettyPrint(new StringBuilder(), "", "", showProxies).toString();
  }

  /**
   * Provides the name of the parameter being satisfied ready for pretty printing.
   * 
   * @return a description of the node, not null
   */
  protected abstract String prettyPrintLine();

  /**
   * Performs the pretty print.
   * 
   * @param builder  the builder to add to, not null
   * @param indent  the current indent, not null
   * @param childIndent  the child indent, not null
   * @param showProxies  true to include proxy nodes
   * @return the node structure, not null
   */
  private StringBuilder prettyPrint(StringBuilder builder,
      String indent, String childIndent, boolean showProxies) {
    
    Node realNode = (showProxies ? this : getConcreteNode());
    // prefix the line with an indicator if the node is an error node for easier debugging
    String errorPrefix = isError() ? "->" : "  ";
    // prefix the line with the parameter name
    String paramPrefix = (realNode.getParameter() != null ? realNode.getParameter().getName() + ": " : "");
    builder.append('\n').append(errorPrefix).append(indent).append(paramPrefix).append(realNode.prettyPrintLine());
    for (Iterator<Node> it = realNode.getDependencies().iterator(); it.hasNext();) {
      Node child = it.next();
      String newIndent;
      String newChildIndent;
      boolean isFinalChild = !it.hasNext();
      if (!isFinalChild) {
        newIndent = childIndent + " |--";  // Unicode boxes: \u251c\u2500\u2500
        newChildIndent = childIndent + " |  ";  // Unicode boxes: \u2502
      } else {
        newIndent = childIndent + " `--";  // Unicode boxes: \u2514\u2500\u2500
        newChildIndent = childIndent + "    ";
      }
      child.prettyPrint(builder, newIndent, newChildIndent, showProxies);
    }
    return builder;
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Node other = (Node) obj;
    return Objects.equals(this._type, other._type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_type);
  }

  @Override
  public String toString() {
    String paramPrefix = (getParameter() != null ? getParameter().getName() + ": " : "");
    return paramPrefix + prettyPrintLine();
  }

}
