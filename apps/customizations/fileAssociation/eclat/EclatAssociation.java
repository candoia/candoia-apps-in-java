package customizations.fileAssociation.eclat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import ca.pfv.spmf.input.transaction_database_list_integers.TransactionDatabase;
import ca.pfv.spmf.tools.dataset_converter.TransactionDatabaseConverter;

/**
 * Created by nmtiwari on 7/20/16.
 */
public class EclatAssociation {
	public static void performAssociation(String arff) throws java.lang.Exception {
		String output = "output.txt";
		String spmfformat = "spmf_format.txt";
		ca.pfv.spmf.tools.dataset_converter.Formats inputFormat = ca.pfv.spmf.tools.dataset_converter.Formats.ARFF;
		int transactionCount = Integer.MAX_VALUE;
		TransactionDatabaseConverter converter = new TransactionDatabaseConverter();
		converter.convert(arff, spmfformat, inputFormat, transactionCount);
        System.out.println("converted");
		TransactionDatabase database = new TransactionDatabase();
		try {
			database.loadFile(spmfformat);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ca.pfv.spmf.algorithms.frequentpatterns.eclat.AlgoEclat eclat = new ca.pfv.spmf.algorithms.frequentpatterns.eclat.AlgoEclat();
		System.out.println("running");
		eclat.runAlgorithm(output, database, 0.90, true);
		System.out.println("ran");
		eclat.printStats();
		BufferedReader br = new BufferedReader(new FileReader(output));
		String line = null;
		while ((line = br.readLine()) != null) {
			System.out.println(line);
		}
		br.close();
	}

	public static void runAssociation(String file) {
		try {
			performAssociation(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
