package edu.mcw.rgd.pipelines.GeneDesc;

import edu.mcw.rgd.datamodel.Gene;
import edu.mcw.rgd.datamodel.SpeciesType;
import edu.mcw.rgd.datamodel.XdbId;
import edu.mcw.rgd.process.Utils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author mtutaj
 * @since 1/9/19
 */
public class Main {

    private DAO dao = new DAO();
    private String version;

    Logger log = Logger.getLogger("summary");

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        Main instance = (Main) (bf.getBean("instance"));

        try {
            instance.run();
        }catch (Exception e) {
            Utils.printStackTrace(e, manager.log);
            throw e;
        }
    }

    public void run() throws Exception {

        long time0 = System.currentTimeMillis();

        log.info(getVersion());
        log.info(dao.getConnectionInfo());

        log.info("=== OK -- elapsed time "+Utils.formatElapsedTime(time0, System.currentTimeMillis()));
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}

