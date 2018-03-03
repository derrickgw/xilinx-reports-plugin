package org.jenkinsci.plugins.xilinx.utilization;

import hudson.Extension;
import hudson.model.Descriptor;
import net.praqma.jenkins.memorymap.graph.MemoryMapGraphConfiguration;
import net.praqma.jenkins.memorymap.parser.AbstractMemoryMapParser;
import net.praqma.jenkins.memorymap.parser.MemoryMapParserDescriptor;
import net.praqma.jenkins.memorymap.result.MemoryMapConfigMemory;
import net.praqma.jenkins.memorymap.result.MemoryMapConfigMemoryItem;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Extension
public class VivadoUtilizationParser extends AbstractMemoryMapParser implements Serializable {
    private static final Logger LOG = Logger.getLogger(VivadoUtilizationParser.class.getName());
    private static final int RADIX = 10;

    private static final ArrayList<MemoryMapGraphConfiguration> defaultGraphConfiguration = new ArrayList<MemoryMapGraphConfiguration>() {{
        add(new MemoryMapGraphConfiguration("Slice_LUTs,Slice_Registers,LUT_Flip_Flop_Pairs", "Slices"));
        add(new MemoryMapGraphConfiguration("DSPs", "DSPs"));
        add(new MemoryMapGraphConfiguration("Block_RAM_Tile", "BRAM"));
    }};

    public VivadoUtilizationParser(String report, List<MemoryMapGraphConfiguration> graphConfiguration) {
        super("Xilinx Utilization", report, report, RADIX, false, graphConfiguration);
    }

    public static List<MemoryMapGraphConfiguration> getDefaultGraphConfig()
    {
        return Collections.unmodifiableList(defaultGraphConfiguration);
    }

    public String getReport() {
        return super.getConfigurationFile();
    }

    public VivadoUtilizationParser() {
        super();
    }

       /*
2. Slice Logic Distribution
---------------------------

+-------------------------------------------------------------+-------------+-------+-----------+-------+
|                          Site Type                          |     Used    | Loced | Available | Util% |
+-------------------------------------------------------------+-------------+-------+-----------+-------+
| Slice                                                       |       30800 |     0 |     69350 | 44.41 |
| LUT as Logic                                                |       59802 |     0 |    277400 | 21.55 |
|   using O5 output only                                      |          16 |       |           |       |
|   using O6 output only                                      |       48228 |       |           |       |
|   using O5 and O6                                           |       11558 |       |           |       |
| LUT as Memory                                               |       11845 |     0 |    108200 | 10.94 |
|   LUT as Distributed RAM                                    |        1984 |     0 |           |       |
|     using O5 output only                                    |           0 |       |           |       |
|     using O6 output only                                    |         202 |       |           |       |
|     using O5 and O6                                         |        1782 |       |           |       |
|   LUT as Shift Register                                     |        9861 |     0 |           |       |
|     using O5 output only                                    |         234 |       |           |       |
|     using O6 output only                                    |        2988 |       |           |       |
|     using O5 and O6                                         |        6639 |       |           |       |
| LUT Flip Flop Pairs                                         |       96834 |     0 |    277400 | 34.90 |
|   fully used LUT-FF pairs                                   |       49315 |       |           |       |
|   LUT-FF pairs with unused LUT                              |       25437 |       |           |       |
|   LUT-FF pairs with unused Flip Flop                        |       22082 |       |           |       |
| Unique Control Sets                                         |        4780 |       |           |       |
| Minimum number of registers lost to control set restriction | 12790(Lost) |       |           |       |
+-------------------------------------------------------------+-------------+-------+-----------+-------+
     */

    public MemoryMapConfigMemory getResources(File f) throws IOException {
        CharSequence seq = createCharSequenceFromFile(f);
        MemoryMapConfigMemory items = new MemoryMapConfigMemory();

        Matcher sectionMatched = Pattern.compile("^\\| (?:([^\\|]+)\\|){5}$", Pattern.MULTILINE).matcher(seq);

        while (sectionMatched.find()) {
            //split returns an empty string for the first group because there is nothing in front of the first separator
            String[] cells = sectionMatched.group(0).split("\\|");
            if (cells[4].trim().matches("\\d+")) {
                String name = cells[1].trim().replace(' ', '_');
                // We can use fractional BRAMS, so we have to parse as a float.
                int used = (int) Float.parseFloat(cells[2].trim());
                int total = Integer.parseUnsignedInt(cells[4].trim());
                int unused = total - used;

                String total_str = Integer.toString(total);
                String used_str = Integer.toString(used);
                String unused_str = Integer.toString(unused);
                items.add(new MemoryMapConfigMemoryItem(name, "0", total_str, used_str, unused_str));
            }
        }

        return items;
    }

    @Override
    public MemoryMapConfigMemory parseMapFile(File f, MemoryMapConfigMemory configuration) throws IOException {
        return getResources(f);
    }

    @Override
    public MemoryMapConfigMemory parseConfigFile(File f) throws IOException {
        return getResources(f);
    }

    @Override
    /**
     * This is actually used as the radix
     */
    public int getDefaultWordSize() {
        return RADIX;
    }

    private static class MemoryMapMemItemComparator implements Comparator<MemoryMapConfigMemoryItem>, Serializable {
        @Override
        public int compare(MemoryMapConfigMemoryItem t, MemoryMapConfigMemoryItem t1) {
            return t.getName().compareTo(t1.getName());
        }
    }

    @Symbol("xilinxParser")
    @Extension
    public static final class DescriptorImpl extends MemoryMapParserDescriptor<VivadoUtilizationParser> {

        @Override
        public String getDisplayName() {
            return "Xilinx Utilization";
        }

        @Override
        public AbstractMemoryMapParser newInstance(StaplerRequest req, JSONObject formData, AbstractMemoryMapParser instance) throws Descriptor.FormException {
            VivadoUtilizationParser parser = (VivadoUtilizationParser) instance;
            save();
            return parser;
        }
    }
}
