package org.jenkinsci.plugins.xilinx.utilization;

import hudson.model.Action;
import hudson.model.Run;
import net.praqma.jenkins.memorymap.MemoryMapBuildAction;
import net.praqma.jenkins.memorymap.result.MemoryMapConfigMemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class VivadoUtilizationBuildAction extends MemoryMapBuildAction {
    private List<VivadoUtilizationProjectAction> projectActions;

    public VivadoUtilizationBuildAction(Run<?, ?> build, HashMap<String, MemoryMapConfigMemory> memoryMapConfig) {
        super(build, memoryMapConfig);
        projectActions = new ArrayList<>();
        projectActions.add(new VivadoUtilizationProjectAction(build.getParent()));
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return "Xilinx Utilization";
    }

    @Override
    public String getUrlName() {
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions() {
        return projectActions;
    }
}
