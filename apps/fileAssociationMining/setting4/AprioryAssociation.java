package fileAssociationMining.setting4;

import weka.associations.Apriori;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;

/**
 * Created by nmtiwari on 7/20/16.
 */
public class AprioryAssociation {
    public static void performAssociation(String arff) throws Exception {
        ConverterUtils.DataSource dataSource = new ConverterUtils.DataSource(arff);
        Instances data = dataSource.getDataSet();
        Apriori model = new Apriori();
        String[] options = new String[2];
        options[0] = "-R";                // "range"
        options[1] = "first-last";                 // first attribute

        weka.filters.unsupervised.attribute.StringToNominal strToNom = new weka.filters.unsupervised.attribute.StringToNominal(); // new instance of filter

        strToNom.setOptions(options);                           // set options
        strToNom.setInputFormat(data);                          // inform filter about dataset **AFTER** setting options
        Instances data2 = Filter.useFilter(data, strToNom);
        model.buildAssociations(data2);
        System.out.println(model);
    }

    public static void runAssociation(String file){
        try {
            performAssociation(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        try {
            performAssociation("/Users/nmtiwari/Desktop/output.arff");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
