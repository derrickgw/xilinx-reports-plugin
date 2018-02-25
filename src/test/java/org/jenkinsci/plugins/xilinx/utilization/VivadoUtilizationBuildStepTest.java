package org.jenkinsci.plugins.xilinx.utilization;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Label;
import javaposse.jobdsl.dsl.DslFactory;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.RemovedJobAction;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.cps.DSL;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.logging.Logger;

public class VivadoUtilizationBuildStepTest {

    private class GetReport extends TestBuilder {
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                BuildListener listener) throws InterruptedException, IOException {
            FilePath file = build.getWorkspace().child(report_file);
            file.copyFrom(VivadoUtilizationBuildStepTest.class.getResource(report_file));
            return true;
        }
    }

    private static final String dslScript = "" +
            "job ('utilization_GEN'){\n" +
            "    publishers {\n" +
            "        xilinxUtilization {\n" +
            "            reportName 'utilization.rpt'\n" +
            "            graph {\n" +
            "                graphCaption  'Slices'\n" +
            "                graphDataList 'Slice_LUTs,Slice_Registers,LUT_Flip_Flop_Pairs'\n" +
            "            }\n" +
            "            graph {\n" +
            "                graphCaption  'DSPs'\n" +
            "                graphDataList 'DSPs'\n" +
            "            }\n" +
            "            graph {\n" +
            "                graphCaption  'BRAM'\n" +
            "                graphDataList 'Block_RAM_Tile'\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private String report_file = "utilization.rpt";
    private FreeStyleProject project;
    private static Logger log = Logger.getLogger("VivadoUtilizationBuildStepTest");

    @Test
    public void testConfigRoundtrip() throws Exception {
        setUpFreeStyle();
        FreeStyleProject config_project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new VivadoUtilizationBuildStep(report_file, VivadoUtilizationParser.defaultGraphConfiguration),
                config_project.getPublishersList().get(0));
    }

    @Test
    public void testFreeStyleBuild() throws Exception {
        setUpFreeStyle();
        FreeStyleBuild build = jenkins.buildAndAssertSuccess(project);
        jenkins.assertLogContains("Printing configuration", build);
        jenkins.assertLogContains("Xilinx Utilization", build);
    }

    private void setUpFreeStyle() throws Exception {
        VivadoUtilizationBuildStep dut = new VivadoUtilizationBuildStep(report_file, VivadoUtilizationParser.defaultGraphConfiguration);
        project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new GetReport());
        project.getPublishersList().add(dut);
    }

    private String getReportFileString(){
        try {

            InputStream inputStream = VivadoUtilizationBuildStepTest.class.getResourceAsStream(report_file);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

        // StandardCharsets.UTF_8.name() > JDK 7
            return result.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return "";
    }


    @Test
    public void testSeedJob() throws Exception {
        FreeStyleProject seedJob = jenkins.createFreeStyleProject();

        ExecuteDslScripts.ScriptLocation dslScriptLocation = new ExecuteDslScripts.ScriptLocation(null, "", dslScript);
        ExecuteDslScripts dslStep = new ExecuteDslScripts(dslScriptLocation, true, RemovedJobAction.IGNORE);
        seedJob.getBuildersList().add(dslStep);
        jenkins.buildAndAssertSuccess(seedJob);

        List<FreeStyleProject> projects = jenkins.getInstance().getAllItems(FreeStyleProject.class);
        for (FreeStyleProject project :
                projects) {
            if (project.getName().equals("utilization_GEN")) {
                VivadoUtilizationBuildStep defaultConfig = new VivadoUtilizationBuildStep(report_file, VivadoUtilizationParser.defaultGraphConfiguration);
                jenkins.assertEqualDataBoundBeans(defaultConfig, project.getPublishersList().get(0));

                project.getBuildersList().add(new GetReport());
                jenkins.buildAndAssertSuccess(project);
            }
        }
    }

    @Test
    public void testScriptedPipeline() throws Exception {
        log.info("I'm starting");
        String agentLabel = "my-agent";
        jenkins.createOnlineSlave(Label.get(agentLabel));
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted-pipeline");

        String pipelineScript
                = "node {\n"
                + "  writeFile file: 'utilization.rpt', text: '''" + getReportFileString() + "'''\n\n"
                + "  xilinxUtilization report: 'utilization.rpt' \n"
                + "      graphConfiguration: [[graphCaption: 'DSPs', graphDataList: 'DSPs'], \n"
                + "                           [graphCaption: 'BRAM', graphDataList: 'Block_RAM_Tile'], \n"
                + "                           [graphCaption: 'Slices', graphDataList: 'Slice_LUTs,Slice_Registers,LUT_Flip_Flop_Pairs']] \n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Xilinx Utilization", completedBuild);
    }


}