package setting3.fileAssociationMining;

import weka.associations.Apriori;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;

/**
 * Created by nmtiwari on 7/20/16.
 */
public class AprioryAssociation {
    public static void performAssociation(String arff) throws java.lang.Exception {
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
        String[] option = new String[4];
		option[0] = "-C";
		option[1] = "0.001";
		option[2] = "-D";
		option[3] = "0.005";
		model.setOptions(option);
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
}
