package at.jku.cp.spezi.alpha;

import java.io.File;
import java.util.List;
import java.util.Map;

import at.jku.cp.spezi.Runner;
import at.jku.cp.spezi.Utils;
import at.jku.cp.spezi.dsp.Processor;

public class Test {

	public static void main(String[] args) {

		String pathParent = "src\\main\\java\\at\\jku\\cp\\spezi\\alpha\\train\\";

		try {
			//predict
			String arguments[] = {"-i",pathParent,"-n","alpha.Alpha","-p"};
			Runner.main(arguments);
			
			//evaluate
			arguments[arguments.length-1]="-e";
			Runner.main(arguments);
			
			//summarize
			arguments[arguments.length-1]="-s";
			Runner.main(arguments);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
