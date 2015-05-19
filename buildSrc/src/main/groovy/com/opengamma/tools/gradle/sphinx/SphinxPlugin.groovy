/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.sphinx

import com.opengamma.tools.gradle.git.task.GitClone
import com.opengamma.tools.gradle.simpleexec.SimpleExec
import com.opengamma.tools.gradle.sphinx.task.CheckDocsEnvironment
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

class SphinxPlugin implements Plugin<Project>
{
	private final static FETCH_DOCS_COMMON_TASK_NAME = "fetchDocsCommon"
	private final static DOCS_COMMON_REPO = "git@github.com:OpenGamma/London.git"
	private final static DOCS_COMMON_BRANCH = "develop"
	private final static String STAGE_DOCS_COMMON_TASK_NAME = "stageDocsCommon"
	private final static String STAGE_DOCS_SOURCE_TASK_NAME = "stageDocsSource"
	private final static String SPHINX_BUILD_TASK_NAME = "sphinxBuild"
	private final static String COLLECT_SPHINX_OUTPUT_TASK_NAME = "collectSphinxOutput"
	private final static String BUILD_GUIDE_TASK_NAME = "buildGuide"
	private final static String PACKAGE_GUIDE_TASK_NAME = "packageGuide"

	private Project project

	@Override
	void apply(Project target)
	{
		this.project = target
		project.extensions.create("sphinx", SphinxExtension)

		addCheckDocsEnvironmentTask()
		addFetchDocsCommonTask()
		addStageDocsCommonTask()
		addStageDocsSourceTask()
		addSphinxBuildTask()
		addCollectSphinxOutputTask()
		addBuildGuideTask()
		addPackageGuideTask()
	}

	private Task addCheckDocsEnvironmentTask()
	{
		Task t = project.tasks.create(CheckDocsEnvironment.TASK_NAME, CheckDocsEnvironment)
		t.description = "Check docs environment"
		t.group = "Documentation"
		return t
	}

	private Task addFetchDocsCommonTask()
	{
		GitClone t = project.tasks.create(FETCH_DOCS_COMMON_TASK_NAME, GitClone)
		t.dependsOn project.tasks[CheckDocsEnvironment.TASK_NAME]
		t.gitRepoURL = DOCS_COMMON_REPO
		t.outputDirectory = new File(project.buildDir, "/tmp/docs-common")
		t.gitBranch = DOCS_COMMON_BRANCH
		return t
	}

	private Task addStageDocsCommonTask()
	{
		Copy t = project.tasks.create(STAGE_DOCS_COMMON_TASK_NAME, Copy)
		t.from(project.tasks[FETCH_DOCS_COMMON_TASK_NAME]) {
			include "docs/**"
			exclude "**/*.sh", "**/.*", "docs/index.rst"
			eachFile { FileCopyDetails d ->
				d.path = d.path - "docs/"
			}
		}
		t.into(new File(project.buildDir, "/tmp/docs-stage"))
		return t
	}

	private Task addStageDocsSourceTask()
	{
		Task t = project.tasks.create(STAGE_DOCS_SOURCE_TASK_NAME, DefaultTask)
		t.doLast {
			File index = new File("${project.projectDir}", "index.rst")
			project.copy {
				from index
				into new File(project.buildDir, "/tmp/docs-stage")
			}

			project.copy {
				from project.projectDir
				into new File(project.buildDir, "/tmp/docs-stage")
				include("**/docs/**")
				exclude("**/build/")
				exclude("**/tmp/")
				includeEmptyDirs = false
			}
		}
		return t
	}

	private Task addSphinxBuildTask()
	{
		Environment env = new Environment()
		SimpleExec t = project.tasks.create(SPHINX_BUILD_TASK_NAME, SimpleExec)
		t.environment = env.pythonEnvironment
		t.command = "make html"
		t.workingDirectory = new File(project.buildDir, "/tmp/docs-stage")
		t.outputs.dir new File(t.workingDirectory, "/_build/html")
		t.dependsOn project.tasks[STAGE_DOCS_COMMON_TASK_NAME]
		t.dependsOn project.tasks[STAGE_DOCS_SOURCE_TASK_NAME]
		return t
	}

	private Task addCollectSphinxOutputTask()
	{
		Task t = project.tasks.create(COLLECT_SPHINX_OUTPUT_TASK_NAME, DefaultTask)
		SimpleExec sphinxBuild = project.tasks[SPHINX_BUILD_TASK_NAME]
		t.dependsOn sphinxBuild
		t.onlyIf = { sphinxBuild.didWork }
		t.doLast {
			if(sphinxBuild.output?.stdErr?.trim())
			{
				if(project.hasProperty("ignoreSphinxWarnings"))
					sphinxBuild.output.stdErr.eachLine { l ->
						logger.warn "sphinxBuild: ${l}"
					}
				else
					throw new GradleException("""\
Sphinx build failed or unclean.
=== SPHINX OUTPUT ===
${sphinxBuild.output.stdErr}
=== END SPHINX OUTPUT ===
""")
			}
		}

		project.tasks[SPHINX_BUILD_TASK_NAME].finalizedBy t
		return t
	}

	private Task addBuildGuideTask()
	{
		Copy t = project.tasks.create(BUILD_GUIDE_TASK_NAME, Copy)
		t.group = "Documentation"
		t.description = "Run Sphinx against the rst sources to generate the HTML User Guide"
		t.dependsOn project.tasks[COLLECT_SPHINX_OUTPUT_TASK_NAME]
		t.configure {
			from(project.tasks[SPHINX_BUILD_TASK_NAME])
			into(new File(project.buildDir, "/docs/guide"))
		}
		return t
	}

	private Task addPackageGuideTask()
	{
		Zip t = project.tasks.create(PACKAGE_GUIDE_TASK_NAME, Zip)
		t.group = "Documentation"
		t.description = "Package the HTML User Guide into a Zip archive"
		t.dependsOn project.tasks[BUILD_GUIDE_TASK_NAME]

		println "[!!] ${PACKAGE_GUIDE_TASK_NAME}.dependsOn: ${t.getDependsOn()}"

//		t.from new File(project.buildDir, "/docs/guide")
		t.from project.tasks[BUILD_GUIDE_TASK_NAME]
		t.into "html"

		t.doFirst {
			String userGuideArchiveName = project.extensions.getByType(SphinxExtension).userGuideArchiveName
			if( ! userGuideArchiveName?.trim())
				throw new GradleException("userGuideArchiveName is required property")

			archiveName = userGuideArchiveName.replaceAll("@project\\.version@", project.version.toString())
		}
		return t
	}
}
