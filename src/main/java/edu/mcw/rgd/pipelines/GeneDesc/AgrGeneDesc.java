package edu.mcw.rgd.pipelines.GeneDesc;

/**
 * Created by mtutaj on 3/29/2019.
 */
public abstract class AgrGeneDesc {

    private String speciesName;

    public void init(String speciesName) throws Exception {
        this.speciesName = speciesName;
    }

    public String getSpeciesName() {
        return speciesName;
    }

    abstract public String getGeneralInfo();
    abstract public String getAutoGeneDesc(String curie) throws Exception;
    abstract public int getGeneCountNotInAgr();
}
