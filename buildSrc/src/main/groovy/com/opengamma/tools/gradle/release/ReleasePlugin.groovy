/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.release

import com.opengamma.tools.gradle.git.task.GitPush
import com.opengamma.tools.gradle.git.task.GitTag
import com.opengamma.tools.gradle.release.task.CheckReleaseEnvironment
import groovy.transform.Memoized
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.language.base.plugins.LifecycleBasePlugin

class ReleasePlugin implements Plugin<Project>, ReleaseExtensionCreator, TaskNamer
{
    public final static String RELEASE_TASK_NAME = "release"
    public final static String PACKAGE_TASK_NAME = "package"
    public final static String CHECK_RELEASE_ENVIRONMENT_TASK_NAME = "checkReleaseEnvironment"
	public final static Optional<Class> DEPLOY_LOCAL_TASK_TYPE = safeGetClass("com.opengamma.tools.gradle.task.DeployLocal")
	public final static String GIT_TAG_TASK_NAME = "gitTagRelease"
	public final static String GIT_PUSH_TASK_NAME = "gitPushReleaseTag"
	public final static String RELEASE_EXTENSION_NAME = "release"

	Project project

    @Override
    void apply(Project target)
    {
	    this.project = target
	    createReleaseExtension()

	    addPackageTask()

	    addCheckReleaseEnvironmentTask()
	    addGitTagTask()
	    addGitPushTask()
	    addReleaseTask()
    }

	private Task addCheckReleaseEnvironmentTask()
	{
		Task t = project.tasks.create(CHECK_RELEASE_ENVIRONMENT_TASK_NAME, CheckReleaseEnvironment)
		return t
	}

	private Task addReleaseTask()
	{
		Task t = project.tasks.create(RELEASE_TASK_NAME, DefaultTask)
		t.configure {
			project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
				if(taskGraph.hasTask(project.tasks[RELEASE_TASK_NAME]))
				{
					reconfigureVersion()
				}
			}
		}
		t.dependsOn project.tasks[CHECK_RELEASE_ENVIRONMENT_TASK_NAME]
		t.dependsOn project.tasks[PACKAGE_TASK_NAME]
		t.dependsOn project.tasks[GIT_PUSH_TASK_NAME]
		t.dependsOn project.getTasksByName(BasePlugin.UPLOAD_ARCHIVES_TASK_NAME, true)
		return t
	}

	private void reconfigureVersion()
	{
		def setVersion = {
			project.allprojects*.version = project.release.releaseVersion.toString()
		}
		if(project.plugins.hasPlugin(AutoVersionPlugin))
			project.tasks[taskNameFor(AutoVersionPlugin.UPDATE_VERSION_TASK_BASE_NAME)].doLast setVersion
		else
			setVersion()
	}

	private void addPackageTask()
	{
		Task t = project.tasks.create(PACKAGE_TASK_NAME, DefaultTask)
		t.dependsOn project.rootProject.getTasksByName(LifecycleBasePlugin.BUILD_TASK_NAME, true)
		project.rootProject.tasks.withType(AbstractArchiveTask) { at ->
			t.dependsOn at
		}
		DEPLOY_LOCAL_TASK_TYPE.ifPresent { taskType ->
			project.rootProject.tasks.withType(taskType) { dl ->
				t.dependsOn dl
			}
		}
	}

	private void addGitTagTask()
	{
		GitTag t = project.tasks.create(GIT_TAG_TASK_NAME, GitTag)
		t.message = "Release ${-> project.version}"
		t.tagName = "${-> releaseTagName}"
		t.repositoryLocation = project.rootProject.projectDir
		t.dependsOn project.tasks[PACKAGE_TASK_NAME]
	}

	private void addGitPushTask()
	{
		GitPush t = project.tasks.create(GIT_PUSH_TASK_NAME, GitPush)
		t.repositoryLocation = project.rootProject.projectDir
		t.tags = true
		t.dependsOn project.tasks[GIT_TAG_TASK_NAME]
	}

	@Memoized
	String getReleaseTagName()
	{
		return project.release.releaseTagTemplate.replaceAll("@version@", project.version)
	}

	private static Optional<Class> safeGetClass(String className)
	{
		try {
			return Optional.of(Class.forName(className))
		} catch(ClassNotFoundException ignored) {
			return Optional.empty()
		}
	}
}
