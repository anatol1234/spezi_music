package at.jku.cp.spezi.alpha;


import at.jku.cp.spezi.Runner;

public class Test {

	public static void main(String[] args) {

		String pathParent = "src\\main\\java\\at\\jku\\cp\\spezi\\alpha\\train\\";
		//pathParent = "D:\\Studium\\6. Semester\\Audio and Music Processing\\Exercise Track\\backup\\beatles\\";
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
