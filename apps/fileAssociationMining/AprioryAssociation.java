package fileAssociationMining;

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
        options[1] = "last";                 // first attribute

        weka.filters.unsupervised.attribute.StringToNominal ff = new weka.filters.unsupervised.attribute.StringToNominal(); // new instance of filter

        ff.setOptions(options);                           // set options
        ff.setInputFormat(data);                          // inform filter about dataset **AFTER** setting options
        Instances data2 = Filter.useFilter(data, ff);
        model.buildAssociations(data2);
        System.out.println(model);
    }

    public static void main(String[] args) {
        try {
            performAssociation("/Users/nmtiwari/Desktop/output.arff");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
