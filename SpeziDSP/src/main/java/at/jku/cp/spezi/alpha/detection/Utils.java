package at.jku.cp.spezi.alpha.detection;

import java.util.List;

public class Utils {

	static double getMean(List<Double> values) {
		double mean=0;
		for(int i=0;i<values.size();i++)
			mean+=values.get(i);
		return mean/values.size();
	}
	
	static double getSD(List<Double> values, double mean) {
		double sd = 0;
		
		for(int i=0;i<values.size();i++) {
			sd+=Math.pow(values.get(i)-mean, 2);
		}
		return Math.pow(sd/values.size(),0.5);
	}
	
}
