/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx

import com.opengamma.tools.gradle.simpleexec.DefaultShellExecutor
import com.opengamma.tools.gradle.simpleexec.ShellExecutor
import com.opengamma.tools.gradle.sphinx.error.ApplicationVersionError
import com.opengamma.tools.gradle.sphinx.error.EnvironmentError
import com.opengamma.tools.gradle.sphinx.error.MissingApplicationError
import com.opengamma.tools.gradle.sphinx.error.MissingPythonPackageError

import java.util.regex.Matcher

class Environment
{
	private final ShellExecutor executor

	public Environment()
	{
		this(new DefaultShellExecutor())
	}

	public Environment(ShellExecutor executor)
	{
		this.executor = executor
	}

	List<EnvironmentError> checkPythonOnPath()
	{
		try {
			String python = executor.execute("${pythonPathPrefix}python --version", pythonEnvironment, new File(".")).stdErr
			Matcher pythonVersion = (python =~ /Python ([0-9]\.[0-9]+)/)
			if( ! pythonVersion) throw new Exception()
			def (Integer maj, Integer min) = pythonVersion.group(1).split(/\./, 2)*.toInteger()
			if( maj != 2 || min < 6 )
				return [new ApplicationVersionError("Python2", pythonVersion.group(1), "2.6 >= v < 3")]
		} catch(Exception ignored) {
			return [new MissingApplicationError("Python2")]
		}

		return []
	}

	List<EnvironmentError> checkPythonPackage(String packageName)
	{
		try {
			if( ! executor.execute(
					"${pythonPathPrefix}pip show ${packageName}",
					pythonEnvironment,
					new File(".")).stdOut.contains("Version:") )
				return [new MissingPythonPackageError(packageName)]
		} catch(Exception ignored) {
			return [new MissingApplicationError("pip")]
		}

		return []
	}

	List<EnvironmentError> checkMake()
	{
		try {
			String output = executor.execute("make --version").stdOut
			if( ! output.startsWith("GNU Make") )
				return [new ApplicationVersionError("Make", output, "GNU")]
		} catch(Exception ignored) {
			return [new MissingApplicationError("GNU Make")]
		}

		return []
	}

	private String getPythonPathPrefix()
	{
		String pythonEnvironment = System.getenv("FORCE_PYTHON_ENVIRONMENT")
		return pythonEnvironment ? new File(pythonEnvironment).canonicalPath + "/bin/" : ""
	}

	Map<String, String> getPythonEnvironment()
	{
		Map<String, String> inheritedEnvironment = [:]
		inheritedEnvironment.putAll System.getenv()
		String pythonEnvironment = System.getenv("FORCE_PYTHON_ENVIRONMENT")
		if(pythonEnvironment)
		{
			String resolvedPythonEnvironment = new File(pythonEnvironment).canonicalPath
			inheritedEnvironment['PATH'] = "${resolvedPythonEnvironment}/bin:${inheritedEnvironment['PATH']}"
			inheritedEnvironment['VIRTUAL_ENV'] = resolvedPythonEnvironment
		}
		return inheritedEnvironment
	}
}
