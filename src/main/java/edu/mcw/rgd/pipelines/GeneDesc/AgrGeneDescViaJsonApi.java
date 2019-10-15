package edu.mcw.rgd.pipelines.GeneDesc;

import com.google.gson.stream.JsonReader;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;

import java.io.File;
import java.io.FileReader;

/**
 * Created by mtutaj on 3/29/2019.
 */
public class AgrGeneDescViaJsonApi extends AgrGeneDesc {

    private String agrApiUrl;

    int genesNotInAgr = 0;

    public String getGeneralInfo() {
        return "   automated gene descriptions will be downloaded via JSON API";
    }

    public void init(String speciesName) throws Exception {
        if( !speciesName.equals("rat") ) {
            throw new Exception("ERROR! species different than rat!");
        }
    }

    public String getAutoGeneDesc(String curie) throws Exception {

        String localFile = downloadGeneFile(curie);

        String fileContents = Utils.readFileAsString(localFile);
        if( Utils.isStringEmpty(fileContents) ) {
            genesNotInAgr++;
            new File(localFile).delete();
            return null;
        }

        String autoDesc = parseField(localFile, "automatedGeneSynopsis");

        new File(localFile).delete();

        Thread.sleep(500); // be nice to AGR: sleep 500ms before making a next web request

        return autoDesc;
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

        String auto = null;
        try {
            JsonReader jsonReader = new JsonReader(new FileReader(fileName));
            jsonReader.beginObject();

            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();

                if (name.equals(fieldName)) {
                    if (auto == null) {
                        auto = jsonReader.nextString();
                    }
                } else {
                    jsonReader.skipValue();
                }
            }

            jsonReader.endObject();
            jsonReader.close();

        } catch (Exception e) {
            throw new Exception("Error parsing file " + fileName, e);
        }
        return auto;
    }

    public String getAgrApiUrl() {
        return agrApiUrl;
    }

    public void setAgrApiUrl(String agrApiUrl) {
        this.agrApiUrl = agrApiUrl;
    }

    public int getGeneCountNotInAgr() {
        return genesNotInAgr;
    }

    public void setGeneCountNotInAgr(int genesNotInAgr) {
        this.genesNotInAgr = genesNotInAgr;
    }
}
