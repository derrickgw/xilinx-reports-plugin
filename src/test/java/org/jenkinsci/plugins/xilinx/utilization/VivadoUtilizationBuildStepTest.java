package org.jenkinsci.plugins.xilinx.utilization;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import net.praqma.jenkins.memorymap.graph.MemoryMapGraphConfiguration;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class VivadoUtilizationBuildStepTest {

    private static final String TEST_REPORT= "utilization.rpt";

    private class GenerateReport extends TestBuilder {
        private String reportName;

        public GenerateReport(String reportName) {
            this.reportName = reportName;
        }

        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                BuildListener listener) throws InterruptedException, IOException {
            FilePath file = Objects.requireNonNull(build.getWorkspace()).child(reportName);
            file.copyFrom(VivadoUtilizationBuildStepTest.class.getResource(TEST_REPORT));
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

    private String reportFile = "post_route_util.rpt";
    private FreeStyleProject project;
    private static Logger log = Logger.getLogger("VivadoUtilizationBuildStepTest");
    private List<MemoryMapGraphConfiguration> graphConfig = new ArrayList<>(VivadoUtilizationParser.getDefaultGraphConfig());

    @Test
    public void testConfigRoundtrip() throws Exception {
        setUpFreeStyle();
        FreeStyleProject config_project = jenkins.configRoundtrip(project);
        jenkins.assertEqualDataBoundBeans(new VivadoUtilizationBuildStep(reportFile, graphConfig),
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
        graphConfig.add(new MemoryMapGraphConfiguration("MMCME2_ADV+PLLE2_ADV", "Clocking"));
        VivadoUtilizationBuildStep dut = new VivadoUtilizationBuildStep(reportFile, graphConfig);
        project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new GenerateReport(reportFile));
        project.getPublishersList().add(dut);
    }

    private String getReportFileString(){
        try {

            InputStream inputStream = VivadoUtilizationBuildStepTest.class.getResourceAsStream(TEST_REPORT);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

        // StandardCharsets.UTF_8.name() > JDK 7
            return result.toString("UTF-8");
        } catch (IOException e){
            e.printStackTrace();
        }
        return "";
    }


    @Test
    public void testSeedJob() throws Exception {
        FreeStyleProject seedJob = jenkins.createFreeStyleProject();

        ExecuteDslScripts dslStep = new ExecuteDslScripts();
        dslStep.setScriptText(dslScript);
        seedJob.getBuildersList().add(dslStep);
        jenkins.buildAndAssertSuccess(seedJob);

        List<FreeStyleProject> projects = jenkins.getInstance().getAllItems(FreeStyleProject.class);
        for (FreeStyleProject project :
                projects) {
            if (project.getName().equals("utilization_GEN")) {
                VivadoUtilizationBuildStep defaultConfig = new VivadoUtilizationBuildStep("utilization.rpt", VivadoUtilizationParser.getDefaultGraphConfig());
                jenkins.assertEqualDataBoundBeans(defaultConfig, project.getPublishersList().get(0));

                project.getBuildersList().add(new GenerateReport("utilization.rpt"));
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
                + "  xilinxUtilization([xilinxParser(report: 'utilization.rpt' \n"
                + "      graphConfiguration: [[graphCaption: 'DSPs', graphDataList: 'DSPs'], \n"
                + "                           [graphCaption: 'BRAM', graphDataList: 'Block_RAM_Tile'], \n"
                + "                           [graphCaption: 'Slices', graphDataList: 'Slice_LUTs,Slice_Registers']])\n"
                + "  ]) \n"
                + "}";
        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        WorkflowRun completedBuild = jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
        jenkins.assertLogContains("Xilinx Utilization", completedBuild);
    }


}