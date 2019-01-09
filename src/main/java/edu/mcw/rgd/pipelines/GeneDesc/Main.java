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

        // validate species
        int speciesTypeKey = SpeciesType.parse(species);
        if( SpeciesType.getTaxonomicId(speciesTypeKey)<=0 ) {
            throw new Exception("invalid species: "+species);
        }

        List<Gene> activeGenes = dao.getGenesForSpecies(speciesTypeKey);
        Collections.shuffle(activeGenes);
        for( Gene gene: activeGenes ) {

            String agrCuri = "RGD:"+gene.getRgdId();
            String localFile = downloadGeneFile(agrCuri);

            String auto = parseField(localFile, "automatedGeneSynopsis");
            if( auto==null ) {
                System.out.println("*** NO AUTO FOR: "+agrCuri+" ***");
            } else {
                System.out.println(agrCuri + " : " + auto);
            }
        }
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

