package org.jenkinsci.plugins.xilinx.utilization;

import hudson.FilePath;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

public class VivadoUtilizationBuildStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private String report_file = "utilization.rpt";
    private FreeStyleProject project;

    @Test
    public void testConfigRoundtrip() throws Exception {
        setUpFreeStyle();
        FreeStyleProject config_project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new VivadoUtilizationBuildStep(report_file),
                config_project.getPublishersList().get(0));
    }

    @Test
    public void testFreeStyleBuild() throws Exception {
        setUpFreeStyle();
        //Run job to create workspace
        jenkins.buildAndAssertSuccess(project);
        FilePath workspace = project.getSomeWorkspace();
        FilePath file = new FilePath(workspace, report_file);
        file.copyFrom(VivadoUtilizationBuildStepTest.class.getResource(report_file));

        //Now run for real
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        System.out.println(build.getLogText());
        jenkins.assertLogContains("Printing configuration", build);
        jenkins.assertLogContains("Xilinx Utilization", build);
    }

    public void setUpFreeStyle() throws Exception {
        VivadoUtilizationBuildStep dut = new VivadoUtilizationBuildStep(report_file);
        project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(dut);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        //TODO: figure out how to copy a resource to a pipeline workspace.
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  xilinxUtilization '" + report_file + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
//        File buildDir = job.getBuildDir();
//        FilePath file = new FilePath(new FilePath(buildDir), report_file);
//        file.copyFrom(VivadoUtilizationBuildStepTest.class.getResource(report_file));

        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
//        jenkins.assertLogContains("Printing Configuration", completedBuild);
//        jenkins.assertLogContains("Xilinx Utilization", completedBuild);
    }

}