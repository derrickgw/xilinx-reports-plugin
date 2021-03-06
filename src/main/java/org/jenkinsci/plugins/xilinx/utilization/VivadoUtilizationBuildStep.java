package org.jenkinsci.plugins.xilinx.utilization;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.praqma.jenkins.memorymap.MemoryMapBuildAction;
import net.praqma.jenkins.memorymap.MemoryMapRecorder;
import net.praqma.jenkins.memorymap.graph.MemoryMapGraphConfiguration;
import net.praqma.jenkins.memorymap.graph.MemoryMapGraphConfigurationDescriptor;
import net.praqma.jenkins.memorymap.parser.AbstractMemoryMapParser;
import net.praqma.jenkins.memorymap.parser.MemoryMapConfigFileParserDelegate;
import net.praqma.jenkins.memorymap.parser.MemoryMapMapParserDelegate;
import net.praqma.jenkins.memorymap.parser.MemoryMapParserDescriptor;
import net.praqma.jenkins.memorymap.result.MemoryMapConfigMemory;
import net.praqma.jenkins.memorymap.util.MemoryMapError;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VivadoUtilizationBuildStep extends Recorder implements SimpleBuildStep { //extends MemoryMapRecorder {

    private static final Logger logger = Logger.getLogger(MemoryMapRecorder.class.getName());

    public List<AbstractMemoryMapParser> chosenParsers;

    public VivadoUtilizationBuildStep(String report, List<MemoryMapGraphConfiguration> graphConfiguration) {
        this.chosenParsers = new ArrayList<>();
        this.chosenParsers.add(new VivadoUtilizationParser("xilinx-utilization", "Utilization", report, graphConfiguration));
    }

    @DataBoundConstructor
    public VivadoUtilizationBuildStep(List<VivadoUtilizationParser> chosenParsers) {
        this.chosenParsers = new ArrayList<>();
        this.chosenParsers.addAll(chosenParsers);
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public String getDefaultReport() {
        return "xilinx_utilization.rpt";
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        PrintStream out = listener.getLogger();
        HashMap<String, MemoryMapConfigMemory> config;
        MemoryMapRecorder recorder;

        recorder = new MemoryMapRecorder(chosenParsers);
        recorder.setScale("default");
        recorder.setShowBytesOnGraph(false);
        recorder.setWordSize(VivadoUtilizationParser.RADIX); //Actually radix.

        try {
            config = workspace.act(new MemoryMapConfigFileParserDelegate(chosenParsers));
            config = workspace.act(new MemoryMapMapParserDelegate(chosenParsers, config));
        } catch (IOException ex) {
            //Catch all known errors (By using a marker interface)
            if (ex instanceof MemoryMapError) {
                out.println(ex.getMessage());
            } else {
                out.println("Unspecified error. Writing trace to log");
                logger.log(Level.SEVERE, "Abnormal plugin execution, trace written to log", ex);
                throw new AbortException(String.format("Unspecified error. Please review error message.%nPlease install the logging plugin to record the standard java logger output stream."
                        + "%nThe plugin is described here: https://wiki.jenkins-ci.org/display/JENKINS/Logging+plugin and requires core 1.483  "));
            }
            build.setResult(Result.FAILURE);
            return;
        }

        out.println("Printing configuration");
        if (config != null) {
            out.println();
            out.println(config.toString());
        }

        MemoryMapBuildAction buildAction = new VivadoUtilizationBuildAction(build, config);
        buildAction.setRecorder(recorder);
        buildAction.setMemoryMapConfigs(config);
        buildAction.setChosenParsers(chosenParsers);
        build.addAction(buildAction);
    }

    @Symbol("xilinxUtilization")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(VivadoUtilizationBuildStep.class);
            load();
        }

        public List<MemoryMapParserDescriptor<?>> getParsers() {
            List<MemoryMapParserDescriptor<?>> ret = new ArrayList<>();
            ret.add(new VivadoUtilizationParser.DescriptorImpl());
            return ret;
        }

        public List<MemoryMapGraphConfigurationDescriptor<?>> getGraphOptions() {
            return MemoryMapGraphConfiguration.getDescriptors();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> type) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Xilinx Utilization Publisher";
        }

        public FormValidation doCheckReport(@QueryParameter String report) {
            return FormValidation.validateRequired(report);
        }

    }

}
