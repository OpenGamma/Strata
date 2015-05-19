/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx

import com.opengamma.tools.gradle.sphinx.error.EnvironmentError
import org.gradle.api.GradleException

class EnvironmentNotSuitableException extends GradleException
{
	public EnvironmentNotSuitableException(Iterable<EnvironmentError> errors)
	{
		super("""\
This environment is not suitable for building Sphinx documentation.

== Errors:
${getCollectedErrors(errors)}

== Things to try:
${getCollectedHelp(errors)}
""")
	}

	private static String getCollectedErrors(Iterable<EnvironmentError> errors)
	{
		errors.collect { e ->
			" * ${e.message}"
		}.join("\n")
	}

	private static String getCollectedHelp(Iterable<EnvironmentError> errors)
	{
		Set<Class<EnvironmentError>> helped = [].toSet()
		List<String> help = []
		for(EnvironmentError e: errors)
		{
			if(helped.contains(e.class)) continue
			help << " * ${e.help}"
			helped << e.class
		}
		return help.join("\n")
	}
}
