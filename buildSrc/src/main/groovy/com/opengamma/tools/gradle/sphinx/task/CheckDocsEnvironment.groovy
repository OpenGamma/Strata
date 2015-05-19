/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx.task


import com.opengamma.tools.gradle.sphinx.Environment
import com.opengamma.tools.gradle.sphinx.EnvironmentNotSuitableException
import com.opengamma.tools.gradle.sphinx.error.EnvironmentError
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.ParallelizableTask
import org.gradle.api.tasks.TaskAction

@ParallelizableTask
class CheckDocsEnvironment extends DefaultTask
{
	public final static String TASK_NAME = "checkDocsEnvironment"

	final Environment environment = new Environment()

	@TaskAction
	void checkEnvironment()
	{
		List<EnvironmentError> errors = []

		errors.addAll environment.checkPythonOnPath()

		project.sphinx.requiredPackages.each { String pkg ->
			errors.addAll owner.environment.checkPythonPackage(pkg)
		}

		errors.addAll environment.checkMake()

		if (errors.empty)
			markMarker()
		else
			throw new EnvironmentNotSuitableException(errors)
	}

	@OutputFile
	File getMarker()
	{
		return new File(project.buildDir, "/tmp/.DOCSENV")
	}

	private void markMarker()
	{
		marker.parentFile.mkdirs()
		marker.createNewFile()
	}
}
