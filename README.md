# gene-description-pipeline

Generate merged gene descriptions for rat genes based on:
 1) automated gene descriptions from The Alliance
 2) automated gene descriptions from RGD

Automated gene descriptions can be retrieved from the Alliance in two ways:
 1) by downloading gene record via JSON API and parsing JSON file
 2) by downloading species-specific gene description file in tsv format from the Alliance
    
Since April 2020 the Alliance does not provide stable file paths anymore. Therefore the logic is to first
query AGR FMS about the latest file, and then that file will be downloaded.

Example for rat, as of Apr 3, 2020:
 1) download https://fms.alliancegenome.org/api/datafile/by/GENE-DESCRIPTION-TSV/RGD?latest=true
 2) extract "s3Path" property: which happens to have the value of "3.0.0/GENE-DESCRIPTION-TSV/RGD/GENE-DESCRIPTION-TSV_RGD_21.tsv"
 3) download actual file "https://download.alliancegenome.org/3.0.0/GENE-DESCRIPTION-TSV/RGD/GENE-DESCRIPTION-TSV_RGD_21.tsv"
 4) parse and process the downloaded file