package edu.mcw.rgd.pipelines.GeneDesc;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author mtutaj
 * @since 1/9/2019
 * wrapper to handle all DAO code
 */
public class DAO {

    GeneDAO gdao = new GeneDAO();
    Logger logChangedDesc = Logger.getLogger("changedDesc");

    public String getConnectionInfo() {
        return gdao.getConnectionInfo();
    }

    public List<Gene> getGenesForSpecies(int speciesTypeKey) throws Exception {
        return gdao.getActiveGenes(speciesTypeKey);
    }

    public void updateAgrDesc(Gene gene, String agrDesc, String mergedDesc) throws Exception {
        logChangedDesc.info(
            "RGD:"+gene.getRgdId()+", "+gene.getSymbol()+"\n"+
            "  OLD AGR DESC ["+ Utils.NVL(gene.getAgrDescription(),"")+"]\n"+
            "  NEW AGR DESC ["+ Utils.NVL(agrDesc,"")+"]\n"+
            "  OLD MERGED DESC ["+ Utils.NVL(gene.getMergedDescription(),"")+"]\n"+
            "  NEW MERGED DESC ["+ Utils.NVL(mergedDesc,"")+"]");

        gene.setAgrDescription(agrDesc);
        gene.setMergedDescription(mergedDesc);
        gdao.updateGene(gene);
    }
}
