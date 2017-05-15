package com.ist.cnv;


public class Main {

/* Example to show MultiLinear Regression working:

  Diet score	Male	age>20	BMI(parameter that will be estimated)
    4   	      0	    1	    27
    7	          1	    1	    29
    6	          1	    0	    23
    2	          0   	0	    20
    3         	0	    1	    21
		
*/

	public static void main(String [] args) throws NoSquareException{

    Matrix X = new Matrix(new double[][]{{4,0,1},{7,1,1},{6,1,0},{2,0,0},{3,0,1}});
    Matrix Y = new Matrix(new double[][]{{27},{29},{23},{20},{21}});
    MultiLinear ml = new MultiLinear(X, Y);
    Matrix beta = ml.calculate();
		getBetasAndResults(beta);
	}

	public static void getBetasAndResults(Matrix betas){
		System.out.println("\n--- Values obtained for the betas ---");
		double[] betaValues =new double[betas.getNrows()+1];
		for(int i=0;i<betas.getNrows();i++){
			System.out.println("b"+i+" : "+betas.getValueAt(i,0));
			betaValues[i]=betas.getValueAt(i,0);
		}
		System.out.println("--- End of betas ---\n");
		System.out.println("--- Estimated parameters ---");

		double[][] variables = new double[][]{{4,0,1},{7,1,1},{6,1,0},{2,0,0},{3,0,1}};
		int variablesNrows = 5;
		for(int j=0; j<variablesNrows;j++){
				double result = betaValues[0] + betaValues[1]*variables[j][0] + betaValues[2]*variables[j][1]+ betaValues[3]*variables[j][2];
				System.out.println("Estimated BMI : "+result);
		}
		System.out.println("--- End of Estimated parameters ---\n");

	}

}
