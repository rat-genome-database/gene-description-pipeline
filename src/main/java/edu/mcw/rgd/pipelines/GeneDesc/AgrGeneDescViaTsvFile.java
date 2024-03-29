package edu.mcw.rgd.pipelines.GeneDesc;

import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by mtutaj on 3/29/2019.
 */
public class AgrGeneDescViaTsvFile extends AgrGeneDesc {

    Logger log = LogManager.getLogger("status");

    private Map<String, String> descMap = new HashMap<>();
    private int geneCountNotInAgr;
    private Map<String,String> latestFiles;
    private String downloadUrlPrefix;

    public String getGeneralInfo() {
        return "   automated gene descriptions will be downloaded via TSV FILE";
    }

    public void init(String speciesName) throws Exception {

        // there must be a species file
        String latestFileUrl = getLatestFileUrlForSpecies(speciesName);

        FileDownloader fd = new FileDownloader();
        fd.setUseCompression(true);
        fd.setPrependDateStamp(true);
        fd.setExternalFile(latestFileUrl);
        fd.setLocalFile("data/desc_"+speciesName+".txt.gz");

        String localFile = fd.downloadNew();

        int linesWithData = 0;
        int linesWithoutDescription = 0;

        // sample two lines from the tab-separated-file:
        // RGD:11512775	LOC108349825	No description available
        // RGD:1582795	LOC691519	Orthologous to human POTEB (POTE ankyrin domain family member B) and POTEB2 (POTE ankyrin domain family member B2).
        BufferedReader in = Utils.openReader(localFile);
        String line;
        while( (line=in.readLine())!=null ) {

            // skip lines with comments and empty lines
            if( line.startsWith("#") || Utils.isStringEmpty(line) ) {
                continue;
            }

            String[] cols = line.split("[\\t]", -1);
            if( cols.length!=3 ) {
                throw new Exception("ERROR: was expecting 3 columns");
            }
            linesWithData++;

            String rgdCurie = cols[0].trim();
            String autoDesc = cols[2].trim();
            if( autoDesc.equals("No description available") ) {
                autoDesc = "";
                linesWithoutDescription++;
            }
            descMap.put(rgdCurie, autoDesc);
        }
        in.close();

        geneCountNotInAgr = 0;

        log.info("   incoming data lines: "+Utils.formatThousands(linesWithData));
        log.info("   data lines without description: "+Utils.formatThousands(linesWithoutDescription));
    }

    String getLatestFileUrlForSpecies(String speciesName) throws Exception {

        // there must be a species file
        String latestFileQuery = getLatestFiles().get(speciesName);
        if( latestFileQuery==null ) {
            throw new Exception("ERROR! species "+speciesName+" does not have a latest file entry defined!");
        }
        super.init(speciesName);

        FileDownloader fd = new FileDownloader();
        fd.setUseCompression(true);
        fd.setPrependDateStamp(true);
        fd.setExternalFile(latestFileQuery);
        fd.setLocalFile("data/latest_"+speciesName+".json.gz");

        String localFile = fd.downloadNew();

        // downloaded file content is json; example:
        // "s3Path":"3.0.0/GENE-DESCRIPTION-TSV/RGD/GENE-DESCRIPTION-TSV_RGD_21.tsv"

        // extract the content of "s3Path" attribute
        String json = readCompressedFileAsString(localFile);
        int s3PathPos = json.indexOf("\"s3Path\"");
        if( s3PathPos<0 ) {
            throw new Exception("Cannot find 's3Path' in contents of file "+localFile);
        }
        int dblQuoteStartPos = json.indexOf('\"', s3PathPos+8);
        if( dblQuoteStartPos <= s3PathPos ) {
            throw new Exception("Unexpected: not found start double quote for value of s3Path");
        }
        int dblQuoteStopPos = json.indexOf('\"', dblQuoteStartPos+1);
        if( dblQuoteStopPos <= dblQuoteStartPos ) {
            throw new Exception("Unexpected: not found stop double quote for value of s3Path");
        }
        String latestFilePath = json.substring(dblQuoteStartPos+1, dblQuoteStopPos);
        String fullUrl = getDownloadUrlPrefix()+latestFilePath;
        return fullUrl;
    }

    String readCompressedFileAsString(String fileName) throws IOException {
        StringBuilder buf = new StringBuilder();
        BufferedReader in = Utils.openReader(fileName);
        String line;
        while( (line=in.readLine())!=null ) {
            buf.append(line);
            buf.append("\n");
        }
        in.close();
        return buf.toString();
    }

    public String getAutoGeneDesc(String curie) throws Exception {
        String autoDesc = descMap.get(curie);
        if( autoDesc==null ) {
            geneCountNotInAgr++;
            LogManager.getLogger("rgdGenesNotInAgr").debug(getSpeciesName()+"  "+curie);
        }
        else if( autoDesc.isEmpty() ) {
            autoDesc = null;
        }
        return autoDesc;
    }

    public int getGeneCountNotInAgr() {
        return geneCountNotInAgr;
    }

    public void setLatestFiles(Map<String,String> latestFiles) {
        this.latestFiles = latestFiles;
    }

    public Map<String,String> getLatestFiles() {
        return latestFiles;
    }

    public String getDownloadUrlPrefix() {
        return downloadUrlPrefix;
    }

    public void setDownloadUrlPrefix(String downloadUrlPrefix) {
        this.downloadUrlPrefix = downloadUrlPrefix;
    }
}
