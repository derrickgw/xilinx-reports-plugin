# fpga_reporting
Jenkins Plugin for various reporting that would be useful for an FPGA project

Example for plotting BRAM, Slice and DSP utilization in a pipeline:
```
node {
    stage('Publish'){
        xilinxUtilization graphConfiguration: [
            [graphCaption: 'BRAMs', graphDataList: 'Block_RAM_Tile'], 
            [graphCaption: 'Slices', graphDataList: 'Slice_LUTs,Slice_Registers,LUT_Flip_Flop_Pairs'],
            [graphCaption: 'DSPs', graphDataList: 'DSPs']], 
            report: 'utilization.rpt'
    }
}
```

Same Example in JobDSL:
```
publishers {
  xilinxUtilization {
    reportName '**/utilization.rpt'
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
```
