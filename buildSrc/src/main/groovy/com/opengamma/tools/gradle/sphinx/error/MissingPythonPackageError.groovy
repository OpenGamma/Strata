/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx.error

import groovy.transform.Immutable

@Immutable
class MissingPythonPackageError implements EnvironmentError
{
	String packageName

	public String getMessage()
	{
		"The Python package ${-> packageName} is required"
	}

	@Override
	String getHelp()
	{
		"Try to install missing Python packages with 'pip install packageName'"
	}
}
