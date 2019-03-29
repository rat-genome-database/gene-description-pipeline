package edu.mcw.rgd.pipelines.GeneDesc;


import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mtutaj on 3/29/2019.
 */
public class AgrGeneDescViaTsvFile extends AgrGeneDesc {
    private String geneDescFile;

    private Map<String, String> descMap = new HashMap<>();
    private int geneCountNotInAgr;

    public String getGeneralInfo() {
        return "   automated gene descriptions will be downloaded via TSV FILE";
    }

    public void init(String speciesName) throws Exception {
        if( !speciesName.equals("rat") ) {
            throw new Exception("ERROR! species different than rat!");
        }

        FileDownloader fd = new FileDownloader();
        fd.setUseCompression(true);
        fd.setPrependDateStamp(true);
        fd.setExternalFile(getGeneDescFile());
        fd.setLocalFile("data/desc_rat.txt.gz");

        String localFile = fd.downloadNew();

        // sample two lines from the tab-separated-file:
        // RGD:11512775	LOC108349825	No description available
        // RGD:1582795	LOC691519	Orthologous to human POTEB (POTE ankyrin domain family member B) and POTEB2 (POTE ankyrin domain family member B2).
        BufferedReader in = Utils.openReader(localFile);
        String line;
        while( (line=in.readLine())!=null ) {
            String[] cols = line.split("[\\t]", -1);
            if( cols.length!=3 ) {
                throw new Exception("ERROR: was expecting 3 columns");
            }

            String rgdCurie = cols[0].trim();
            String autoDesc = cols[2].trim();
            if( autoDesc.equals("No description available") ) {
                autoDesc = "";
            }
            descMap.put(rgdCurie, autoDesc);
        }
        in.close();

        geneCountNotInAgr = 0;
    }

    public String getAutoGeneDesc(int rgdId) throws Exception {
        String rgdCurie = "RGD:"+rgdId;
        String autoDesc = descMap.get(rgdCurie);
        if( autoDesc==null ) {
            geneCountNotInAgr++;
        }
        else if( autoDesc.isEmpty() ) {
            autoDesc = null;
        }
        return autoDesc;
    }

    public void setGeneDescFile(String geneDescFile) {
        this.geneDescFile = geneDescFile;
    }

    public String getGeneDescFile() {
        return geneDescFile;
    }

    public int getGeneCountNotInAgr() {
        return geneCountNotInAgr;
    }
}
