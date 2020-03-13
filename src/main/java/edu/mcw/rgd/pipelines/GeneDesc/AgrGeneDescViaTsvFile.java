package edu.mcw.rgd.pipelines.GeneDesc;


import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mtutaj on 3/29/2019.
 */
public class AgrGeneDescViaTsvFile extends AgrGeneDesc {

    private Map<String, String> descMap = new HashMap<>();
    private int geneCountNotInAgr;
    private Map<String,String> geneDescFiles;

    public String getGeneralInfo() {
        return "   automated gene descriptions will be downloaded via TSV FILE";
    }

    public void init(String speciesName) throws Exception {

        // there must be a species file
        String geneDescFile = getGeneDescFiles().get(speciesName);
        if( geneDescFile==null ) {
            throw new Exception("ERROR! species "+speciesName+" does not have a gene description file name defined!");
        }
        super.init(speciesName);

        FileDownloader fd = new FileDownloader();
        fd.setUseCompression(true);
        fd.setPrependDateStamp(true);
        fd.setExternalFile(geneDescFile);
        fd.setLocalFile("data/desc_"+speciesName+".txt.gz");

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

    public String getAutoGeneDesc(String curie) throws Exception {
        String autoDesc = descMap.get(curie);
        if( autoDesc==null ) {
            geneCountNotInAgr++;
            Logger.getLogger("rgdGenesNotInAgr").debug(getSpeciesName()+"  "+curie);
        }
        else if( autoDesc.isEmpty() ) {
            autoDesc = null;
        }
        return autoDesc;
    }

    public int getGeneCountNotInAgr() {
        return geneCountNotInAgr;
    }

    public void setGeneDescFiles(Map geneDescFiles) {
        this.geneDescFiles = geneDescFiles;
    }

    public Map<String,String> getGeneDescFiles() {
        return geneDescFiles;
    }
}
