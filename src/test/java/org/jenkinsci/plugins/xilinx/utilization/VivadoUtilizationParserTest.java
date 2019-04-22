package org.jenkinsci.plugins.xilinx.utilization;

import net.praqma.jenkins.memorymap.result.MemoryMapConfigMemory;
import net.praqma.jenkins.memorymap.result.MemoryMapConfigMemoryItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VivadoUtilizationParserTest {

    private String filename;
    private VivadoUtilizationParser dut;

    @Before
    public void setUp() throws Exception {
        dut = new VivadoUtilizationParser();
        filename = VivadoUtilizationParserTest.class.getResource("utilization.rpt").getFile();
    }

    @After
    public void tearDown() throws Exception {

    }

    private static void ItemFactory(String name, int used, int available)
    {
        String safe_name = name.trim().replace(" ", "_");
        int unused = available - used;
        //String length, String used, String unused
        myMap.put(safe_name, new MemoryMapConfigMemoryItem(safe_name, "0",
                Integer.toString(available),
                Integer.toString(used),
                Integer.toString(unused)));
    }
    private static Map<String, MemoryMapConfigMemoryItem> myMap;
    static {
        myMap = new HashMap<>();
        ItemFactory("Slice LUTs           " ,10122, 17600);
        ItemFactory("LUT as Logic         " , 9472, 17600);
        ItemFactory("LUT as Memory        " ,  650,  6000);
        ItemFactory("Slice Registers      " ,11935, 35200);
        ItemFactory("Register as Flip Flop" ,11933, 35200);
        ItemFactory("Register as Latch    " ,    2, 35200);
        ItemFactory("Slice                " , 3718,  4400);
        ItemFactory("LUT as Logic         " , 9472, 17600);
        ItemFactory("LUT as Memory        " ,  650,  6000);
        ItemFactory("LUT Flip Flop Pairs  " , 4930, 17600);
        ItemFactory("Block RAM Tile       " ,   47,    60);
        ItemFactory("RAMB36/FIFO          " ,   45,    60);
        ItemFactory("RAMB18               " ,    5,   120);
        ItemFactory("DSPs                 " ,   10,    80);
    }

    @Test
    public void getResources() throws IOException{
        File f = new File(filename);

        MemoryMapConfigMemory configMemory;
        //dut.parseConfigFile(f) returns the same thing.
        //configMemory = dut.parseMapFile(f, null);
        configMemory = dut.getResources(f);
        assertTrue(configMemory.size() > 0);

        int assertions = 0;
        for (MemoryMapConfigMemoryItem item : configMemory) {
            MemoryMapConfigMemoryItem golden_item = myMap.get(item.getName());
            if (golden_item != null) {
                assertEquals(item.getName(), Integer.getInteger(golden_item.getLength()), Integer.getInteger(item.getLength()));
                assertEquals(item.getName(), Integer.getInteger(golden_item.getUsed()), Integer.getInteger(item.getUsed()));
                assertEquals(item.getName(), Integer.getInteger(golden_item.getUnused()), Integer.getInteger(item.getUnused()));
                assertions++;
            }
        }
        assertEquals(assertions, myMap.size());
    }
}
