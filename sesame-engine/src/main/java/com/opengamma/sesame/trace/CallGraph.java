/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.trace;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

/**
 * Graph representing the calls made to calculate a single result.
 */
public class CallGraph {
// TODO maybe an interface instead? abstract class? different tracers will need similar functionality
// TODO joda bean so it can be serialized as part of the results
// there are no getters here currently

  /**
   * The invoked method.
   */
  private final Method _method;
  /**
   * The method arguments.
   */
  private final Object[] _args;
  /**
   * The child call graphs.
   */
  private final List<CallGraph> _callGraphs = Lists.newArrayList();
  /**
   * The return value.
   */
  private Object _returnValue;
  /**
   * The throwable.
   */
  private Throwable _throwable;

  /**
   * Creates an instance.
   * 
   * @param method  the invoked method, not null
   * @param args  the method arguments, not null
   */
  /* package */ CallGraph(Method method, Object... args) {
    _method = method;
    _args = args;
  }

  //-------------------------------------------------------------------------
  /* package */ void called(CallGraph callGraph) {
    _callGraphs.add(callGraph);
  }

  /* package */ void returned(Object returnValue) {
    _returnValue = returnValue;
  }

  /* package */ void threw(Throwable throwable) {
    _throwable = throwable;
  }

  /* package */ List<CallGraph> calls() {
    return _callGraphs;
  }

  //-------------------------------------------------------------------------
  /**
   * Provides a pretty-printed version of the call graph as a string.
   * 
   * @return a string representation of the call graph, not null
   */
  public String prettyPrint() {
    return prettyPrint(new StringBuilder(), this, "", "").toString();
  }

  private static StringBuilder prettyPrint(StringBuilder builder, CallGraph callGraph, String indent, String childIndent) {
    builder.append('\n').append(indent).append(callGraph.toString());
    for (Iterator<CallGraph> itr = callGraph.calls().iterator(); itr.hasNext(); ) {
      CallGraph next = itr.next();
      String newIndent;
      String newChildIndent;
      boolean isFinalChild = !itr.hasNext();
      if (!isFinalChild) {
        newIndent = childIndent + " |--";
        newChildIndent = childIndent + " |  ";
      } else {
        newIndent = childIndent + " `--";
        newChildIndent = childIndent + "    ";
      }
      // these are unicode characters for box drawing
/*
      if (!isFinalChild) {
        newIndent = childIndent + " \u251c\u2500\u2500";
        newChildIndent = childIndent + " \u2502  ";
      } else {
        newIndent = childIndent + " \u2514\u2500\u2500";
        newChildIndent = childIndent + "    ";
      }
*/
      prettyPrint(builder, next, newIndent, newChildIndent);
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
    final CallGraph other = (CallGraph) obj;
    return
        Objects.equals(this._method, other._method) &&
        Arrays.deepEquals(this._args, other._args) &&
        Objects.equals(this._callGraphs, other._callGraphs) &&
        Objects.equals(this._returnValue, other._returnValue) &&
        Objects.equals(this._throwable, other._throwable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_method, Arrays.deepHashCode(_args), _callGraphs, _returnValue, _throwable);
  }

  @Override
  public String toString() {
    return _method.getDeclaringClass().getSimpleName() + "." + _method.getName() + "()" +
        (_throwable == null ? " -> " + _returnValue : " threw " + _throwable) +
        (_args == null ? "" : ", args: " + Arrays.deepToString(_args));
  }

}
