/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx.error


class MissingPythonError extends MissingApplicationError
{
	public MissingPythonError()
	{
		super("Python2")
	}

	@Override
	String getHelp()
	{
		"""${super.getHelp()}. If you are using a Python VirtualEnvironment make sure you have told the build about it with the
		FORCE_PYTHON_ENVIRONMENT environment variable (eg '\$ FORCE_PYTHON_ENVIRONMENT=/path/to/virtualenv gradle sphinxBuild')"""
	}
}
