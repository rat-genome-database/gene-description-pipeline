package edu.mcw.rgd.pipelines.GeneDesc;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 * @author mtutaj
 * @since 1/9/19
 */
public class Main {

    private DAO dao = new DAO();
    private String version;
    private List<String> speciesProcessed;

    Logger log = Logger.getLogger("summary");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Main instance = (Main) (bf.getBean("main"));

        AgrGeneDesc agrGeneDescManager = null;

        for( String arg: args ) {
            switch(arg) {
                case "--jsonApi":
                    agrGeneDescManager = (AgrGeneDesc) (bf.getBean("jsonApi"));
                    break;

                case "--tsvFile":
                    agrGeneDescManager = (AgrGeneDesc) (bf.getBean("tsvFile"));
                    break;
            }
        }

        try {
            instance.run(agrGeneDescManager);
        }catch (Exception e) {
            Utils.printStackTrace(e, instance.log);
            throw e;
        }
    }

    public void run(AgrGeneDesc agrGeneDescManager) throws Exception {

        long time0 = System.currentTimeMillis();

        log.info(getVersion());
        log.info("   "+dao.getConnectionInfo());

        SimpleDateFormat sdt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        log.info("   started at "+sdt.format(new Date(time0)));

        log.info(agrGeneDescManager.getGeneralInfo());

        for( String species: getSpeciesProcessed() ) {
            run(species, agrGeneDescManager);
        }
        log.info("=== OK -- elapsed time "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    void run(String species, AgrGeneDesc agrGeneDescManager) throws Exception {
        log.info("running for species: "+species);
        agrGeneDescManager.init(species);

        int genesWithAgrDesc = 0;
        int newAgrDesc = 0;
        int upToDateAgrDesc = 0;

        int genesWithMergedDesc = 0;
        int newMergedDesc = 0;
        int upToDateMergedDesc = 0;

        int genesWithoutCurie = 0;
        int genesWithClearedDesc = 0;

        // validate species
        int speciesTypeKey = SpeciesType.parse(species);
        if( SpeciesType.getTaxonomicId(speciesTypeKey)<=0 ) {
            throw new Exception("invalid species: "+species);
        }

        List<Gene> activeGenes = dao.getGenesForSpecies(speciesTypeKey);
        log.info("  genes to be processed: "+Utils.formatThousands(activeGenes.size()));

        Map<Integer,String> geneRgdIdToCurieMap = getGeneRgdIdToCurieMap(speciesTypeKey, activeGenes);
        log.info("  gene curies loaded:    "+Utils.formatThousands(geneRgdIdToCurieMap.size()));

        Collections.shuffle(activeGenes);
        for( Gene gene: activeGenes ) {

            String curie = geneRgdIdToCurieMap.get(gene.getRgdId());
            if( curie==null ) {
                genesWithoutCurie++;

                // update the gene if it has an automated or merged description
                if( !Utils.isStringEmpty(gene.getAgrDescription()) || !Utils.isStringEmpty(gene.getMergedDescription()) ) {
                    dao.updateAgrDesc(gene, null, null);
                    genesWithClearedDesc++;
                }
                continue;
            }

            String auto = agrGeneDescManager.getAutoGeneDesc(curie);

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
        }

        log.info("  genes not in AGR: "+Utils.formatThousands(agrGeneDescManager.getGeneCountNotInAgr()));

        if( genesWithoutCurie!=0 ) {
            log.info("  genes in RGD without CURIE: " + Utils.formatThousands(genesWithoutCurie));
        }
        if( genesWithClearedDesc!=0 ) {
            log.info("  genes with cleared desc: " + Utils.formatThousands(genesWithClearedDesc));
        }

        log.info("  genes with automated AGR description: "+Utils.formatThousands(genesWithAgrDesc));
        log.info("  genes with new automated AGR description: "+Utils.formatThousands(newAgrDesc));
        log.info("  genes with up-to-date automated AGR description: "+Utils.formatThousands(upToDateAgrDesc));

        log.info("  genes with automated merged description: "+Utils.formatThousands(genesWithMergedDesc));
        log.info("  genes with new automated merged description: "+Utils.formatThousands(newMergedDesc));
        log.info("  genes with up-to-date automated merged description: "+Utils.formatThousands(upToDateMergedDesc));
        log.info("");
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

    Map<Integer,String> getGeneRgdIdToCurieMap(int speciesTypeKey, List<Gene> genes) throws Exception {

        // for rat it is simple
        if( speciesTypeKey==SpeciesType.RAT ) {
            Map<Integer,String> result = new HashMap<>(genes.size());
            for( Gene g: genes ) {
                result.put(g.getRgdId(), "RGD:"+g.getRgdId());
            }
            return result;
        }

        return dao.getGeneRgdIdToCurieMap(speciesTypeKey);
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
}

