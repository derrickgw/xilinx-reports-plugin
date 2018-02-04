package org.jenkinsci.plugins.xilinx.utilization;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class VivadoUtilizationBuildStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private String report_file;
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
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        //TODO: update for the actual expected strings.
        System.out.println(build.getLogText());
//        jenkins.assertLogContains("Hello, ", build);
    }

    public void setUpFreeStyle() throws Exception {
        report_file = VivadoUtilizationParserTest.class.getResource("utilization.rpt").getFile();
        VivadoUtilizationBuildStep dut = new VivadoUtilizationBuildStep(report_file);
        project = jenkins.createFreeStyleProject();
        project.getPublishersList().add(dut);
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");
        String pipelineScript
                = "node {\n"
                + "  xilinxUtilization '" + report_file + "'\n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
//        String expectedString = "Hello, " + "!";
//        jenkins.assertLogContains(expectedString, completedBuild);
    }

}