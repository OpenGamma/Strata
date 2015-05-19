package com.opengamma.tools.gradle

import com.opengamma.tools.gradle.task.RunApp
import groovy.transform.Memoized
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ApplicationPlugin

class MultiScriptPlugin implements Plugin<Project>
{
    Project project

    @Override
    void apply(Project target)
    {
        this.project = target

        project.plugins.apply(ApplicationPlugin)
        disableApplication()

//        project.extensions.create("applications" /* TODO */, ApplicationsConvention)
//        ExtensionAware extensionAware = project.extensions.getByType(ApplicationPlugin) as ExtensionAware
//        iExtension.extensions.configure()
        project.convention.plugins.applications = new ApplicationsConvention(this)

        project.afterEvaluate(this.&configureDependencies)
    }

    private void configureDependencies()
    {
        project.tasks.withType(RunApp) { RunApp t ->
            applicationTasks*.dependsOn t
        }

        println project.convention.plugins.applications
    }

    @Memoized
    private Set<Task> getApplicationTasks()
    {
        return [
                project.tasks[ApplicationPlugin.TASK_DIST_TAR_NAME],
                project.tasks[ApplicationPlugin.TASK_DIST_ZIP_NAME],
                project.tasks[ApplicationPlugin.TASK_INSTALL_NAME]
        ]
    }

    private void disableApplication()
    {
        project.tasks[ApplicationPlugin.TASK_RUN_NAME].enabled = false
        project.tasks[ApplicationPlugin.TASK_START_SCRIPTS_NAME].enabled = false
    }
}
