package org.jenkinsci.plugins.xilinx.utilization;

import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
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
import net.praqma.jenkins.memorymap.parser.AbstractMemoryMapParser;
import net.praqma.jenkins.memorymap.parser.MemoryMapConfigFileParserDelegate;
import net.praqma.jenkins.memorymap.parser.MemoryMapMapParserDelegate;
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
    private final String report;
    private List<AbstractMemoryMapParser> chosenParsers;
    private MemoryMapRecorder recorder;

    @DataBoundConstructor
    public VivadoUtilizationBuildStep(String report) {
        this.report = report;
        chosenParsers = new ArrayList<>();
        chosenParsers.add(new VivadoUtilizationParser(report));
        recorder = new MemoryMapRecorder(chosenParsers);
        recorder.setScale("default");
        recorder.setShowBytesOnGraph(false);
        recorder.setWordSize(16); //Actually radix.
    }

    /**
     * @return the chosenParsers
     */
    public List<AbstractMemoryMapParser> getChosenParsers() {
        return chosenParsers;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public String getReport() {
        return this.report;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Report Files  = " + report + "!");

        PrintStream out = listener.getLogger();
        HashMap<String, MemoryMapConfigMemory> config;

        try {
            config = workspace.act(new MemoryMapConfigFileParserDelegate(getChosenParsers()));
            config = workspace.act(new MemoryMapMapParserDelegate(getChosenParsers(), config));
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
        buildAction.setChosenParsers(getChosenParsers());
        build.addAction(buildAction);
    }

    @Symbol("xilinxUtilization")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(VivadoUtilizationBuildStep.class);
            load();
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
