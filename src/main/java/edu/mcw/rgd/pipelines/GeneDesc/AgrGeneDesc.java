package edu.mcw.rgd.pipelines.GeneDesc;

/**
 * Created by mtutaj on 3/29/2019.
 */
public abstract class AgrGeneDesc {

    abstract public String getGeneralInfo();
    abstract public void init(String speciesName) throws Exception;
    abstract public String getAutoGeneDesc(String curie) throws Exception;
    abstract public int getGeneCountNotInAgr();
}
