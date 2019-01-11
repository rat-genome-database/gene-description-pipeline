package edu.mcw.rgd.pipelines.GeneDesc;

import com.google.gson.stream.JsonReader;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * @author mtutaj
 * @since 1/9/19
 */
public class Main {

    private DAO dao = new DAO();
    private String version;
    private List<String> speciesProcessed;
    private String agrApiUrl;

    Logger log = Logger.getLogger("summary");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Main instance = (Main) (bf.getBean("main"));

        try {
            instance.run();
        }catch (Exception e) {
            Utils.printStackTrace(e, instance.log);
            throw e;
        }
    }

    public void run() throws Exception {

        long time0 = System.currentTimeMillis();

        log.info(getVersion());
        log.info("   "+dao.getConnectionInfo());

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(time0)));

        for( String species: getSpeciesProcessed() ) {
            run(species);
        }
        log.info("=== OK -- elapsed time "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    void run(String species) throws Exception {
        log.info("running for species: "+species);

        int genesNotInAgr = 0;

        int genesWithAgrDesc = 0;
        int newAgrDesc = 0;
        int upToDateAgrDesc = 0;

        int genesWithMergedDesc = 0;
        int newMergedDesc = 0;
        int upToDateMergedDesc = 0;

        // validate species
        int speciesTypeKey = SpeciesType.parse(species);
        if( SpeciesType.getTaxonomicId(speciesTypeKey)<=0 ) {
            throw new Exception("invalid species: "+species);
        }

        List<Gene> activeGenes = dao.getGenesForSpecies(speciesTypeKey);
        log.info("  genes to be processed: "+activeGenes.size());
        Collections.shuffle(activeGenes);
        for( Gene gene: activeGenes ) {
            Thread.sleep(500); // be nice to AGR: sleep 500ms before making a next web request

            String agrCuri = "RGD:"+gene.getRgdId();
            String localFile = downloadGeneFile(agrCuri);

            String fileContents = Utils.readFileAsString(localFile);
            if( Utils.isStringEmpty(fileContents) ) {
                genesNotInAgr++;
                new File(localFile).delete();
                continue;
            }

            String auto = parseField(localFile, "automatedGeneSynopsis");

            if( auto!=null ) {
                genesWithAgrDesc++;
            }

            boolean doGeneUpdate = false;

            if( Utils.stringsAreEqualIgnoreCase(auto, gene.getAgrDescription()) ) {
                upToDateAgrDesc++;
            } else {
                newAgrDesc++;
                doGeneUpdate = true;
            }

            String mergedDesc = generateMergedDesc(gene, auto);
            if( !Utils.isStringEmpty(mergedDesc) ) {
                genesWithMergedDesc++;
            }
            if( Utils.stringsAreEqualIgnoreCase(mergedDesc, gene.getMergedDescription()) ) {
                upToDateMergedDesc++;
            } else {
                newMergedDesc++;
                doGeneUpdate = true;
            }

            if( doGeneUpdate ) {
                dao.updateAgrDesc(gene, auto, mergedDesc);
            }

            new File(localFile).delete();
        }

        log.info("  genes not in AGR: "+genesNotInAgr);

        log.info("  genes with automated AGR desription: "+genesWithAgrDesc);
        log.info("  genes with new automated AGR desription: "+newAgrDesc);
        log.info("  genes with up-to-date automated AGR desription: "+upToDateAgrDesc);

        log.info("  genes with automated merged desription: "+genesWithMergedDesc);
        log.info("  genes with new automated merged desription: "+newMergedDesc);
        log.info("  genes with up-to-date automated merged desription: "+upToDateMergedDesc);
    }

    String generateMergedDesc(Gene gene, String agrDesc) throws Exception {
        String rgdDesc = Utils.getGeneDescription(gene);

        // CASE 1: both AGR and RGD desc are empty
        if( Utils.isStringEmpty(agrDesc) && Utils.isStringEmpty(rgdDesc) ) {
            return "";
        }

        // CASE 2: AGR desc is empty,  RGD desc is not empty
        if( Utils.isStringEmpty(agrDesc) && !Utils.isStringEmpty(rgdDesc) ) {
            return rgdDesc;
        }

        // CASE 3: AGR desc is not empty,  RGD desc is empty
        if( !Utils.isStringEmpty(agrDesc) && Utils.isStringEmpty(rgdDesc) ) {
            return agrDesc;
        }

        // CASE 4: merge RGD desc with AGR desc
        // pathway could be followed by phenotype/disease, cc, chebi
        String pathwayDesc = null;
        String chebiDesc = null;

        String pathwayPattern = "PARTICIPATES IN ";
        String phenoDiseasePattern = "ASSOCIATED WITH ";
        String ccPattern = "FOUND IN ";
        String chebiPattern = "INTERACTS WITH "; // always last pattern
        int pathwayPos = rgdDesc.indexOf(pathwayPattern);
        if( pathwayPos>=0 ) {
            // see if there are patterns for other categories
            int phenoDisPos = rgdDesc.indexOf(phenoDiseasePattern, pathwayPos);

            int pathwayStopPos = -1;
            if( phenoDisPos>0 ) {
                pathwayStopPos = phenoDisPos;
            } else {
                int ccPos = rgdDesc.indexOf(ccPattern, pathwayPos);
                if( ccPos>0 ) {
                    pathwayStopPos = ccPos;
                } else {
                    int chebiPos = rgdDesc.indexOf(chebiPattern, pathwayPos);
                    if( chebiPos>0 ) {
                        pathwayStopPos = chebiPos;
                    } else {
                        pathwayStopPos = rgdDesc.length();
                    }
                }
            }
            pathwayDesc = rgdDesc.substring(pathwayPos, pathwayStopPos);
        }

        int chebiPos = rgdDesc.indexOf(chebiPattern, pathwayPos);
        if( chebiPos>=0 ) {
            chebiDesc = rgdDesc.substring(chebiPos);
        }

        // append pathway and chebi desc to the AGR desc
        String mergedDesc = agrDesc.substring(0, agrDesc.length()-1);
        if( pathwayDesc!=null ) {
            mergedDesc +=  "; " + pathwayDesc;
        }
        if( chebiDesc!=null ) {
            if( !mergedDesc.endsWith("; ") ) {
                mergedDesc += "; ";
            }
            mergedDesc += chebiDesc;
        }
        mergedDesc += ".";
        return mergedDesc;
    }

    // return local file name
    String downloadGeneFile(String agrCuri) throws Exception {
        String rgdid = agrCuri.replace(":","");

        FileDownloader fd = new FileDownloader();
        fd.setExternalFile(getAgrApiUrl()+agrCuri);
        fd.setLocalFile("data/"+rgdid+".json");
        String localFile = fd.download();
        return localFile;
    }

    String parseField(String fileName, String fieldName) throws Exception {
        JsonReader jsonReader = new JsonReader(new FileReader(fileName));
        jsonReader.beginObject();

        String auto = null;
        while (jsonReader.hasNext()) {
            String name = jsonReader.nextName();

            if (name.equals(fieldName)) {
                if( auto==null ) {
                    auto = jsonReader.nextString();
                }
            } else {
                jsonReader.skipValue();
            }
        }

        jsonReader.endObject();
        jsonReader.close();

        return auto;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getSpeciesProcessed() {
        return speciesProcessed;
    }

    public void setSpeciesProcessed(List<String> speciesProcessed) {
        this.speciesProcessed = speciesProcessed;
    }

    public String getAgrApiUrl() {
        return agrApiUrl;
    }

    public void setAgrApiUrl(String agrApiUrl) {
        this.agrApiUrl = agrApiUrl;
    }
}

