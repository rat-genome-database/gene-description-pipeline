package edu.mcw.rgd.pipelines.GeneDesc;

import edu.mcw.rgd.dao.impl.GeneDAO;
import edu.mcw.rgd.datamodel.Gene;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mtutaj
 * @since 1/9/2019
 * wrapper to handle all DAO code
 */
public class DAO {

    GeneDAO gdao = new GeneDAO();

    public String getConnectionInfo() {
        return xdao.getConnectionInfo();
    }

}
