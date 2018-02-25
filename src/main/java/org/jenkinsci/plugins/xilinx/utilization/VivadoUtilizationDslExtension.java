package org.jenkinsci.plugins.xilinx.utilization;

import hudson.Extension;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.RequiresPlugin;
import javaposse.jobdsl.dsl.helpers.publisher.PublisherContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;
import net.praqma.jenkins.memorymap.graph.MemoryMapGraphConfiguration;

import java.util.ArrayList;
import java.util.List;

/*
```
job {
    publishers {
        xilinxUtilization {
            reportName (String reportName)
            graphConfigs {
                graphCaption  (String graphCaption)
                graphDataList (String graphDataList)
            }
        }
    }
}
```

```
job ('utilization_GEN'){
    publishers {
        xilinxUtilization {
            reportName 'utilization.rpt'
            graph {
                graphCaption  'DSPs'
                graphDataList 'DSPs'
            }
            graph {
                graphCaption  'BRAM'
                graphDataList 'Block_RAM_Tile'
            }
            graph {
                graphCaption  'Slices'
                graphDataList 'Slice_LUTs,Slice_Registers,LUT_Flip_Flop_Pairs'
            }
        }
    }
}
```
*/

@Extension(optional = true)
public class VivadoUtilizationDslExtension extends ContextExtensionPoint {
    
    @RequiresPlugin(id = "xilinx-reports", minimumVersion = "0.1")
    @DslExtensionMethod(context = PublisherContext.class)
    public Object xilinxUtilization(Runnable closure){
        VivadoUtilizationJobDslContext context = new VivadoUtilizationJobDslContext();
        executeInContext(closure, context);

        return new VivadoUtilizationBuildStep(context.reportName, context.graphConfigs);
    }


    public class VivadoUtilizationJobDslContext implements Context {
        String reportName = "utilization.rpt";
        List<MemoryMapGraphConfiguration> graphConfigs = new ArrayList<>();

        public void reportName(String value) {
            reportName = value;
        }

        public void graph(Runnable closure){
            VivadoUtilizationGraphDslContext context = new VivadoUtilizationGraphDslContext();
            executeInContext(closure, context);

            graphConfigs.add(new MemoryMapGraphConfiguration(context.graphDataList, context.graphCaption));
        }

    }

    public class VivadoUtilizationGraphDslContext implements Context {
        String graphDataList;
        String graphCaption;

        public VivadoUtilizationGraphDslContext() {
        }

        public void graphDataList(String value) {
            this.graphDataList = value;
        }

        public void graphCaption(String value) {
            this.graphCaption = value;
        }
    }
}