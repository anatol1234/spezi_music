package at.jku.cp.spezi.alpha;


import at.jku.cp.spezi.Runner;
import at.jku.cp.spezi.dsp.Processor;

public class Test {

	public static void main(String[] args) {

		String pathParent = "src\\main\\java\\at\\jku\\cp\\spezi\\alpha\\train\\";
//		pathParent = "D:\\Studium\\6. Semester\\Audio and Music Processing\\Exercise Track\\backup\\own_train\\";
//		for(int i=1;i<13;i++) {
//			if(i>=10) {
//				pathParent = "D:\\Studium\\6. Semester\\Audio and Music Processing\\Exercise Track\\backup\\beatles\\album_"+i+"\\";
//			} else {
//				pathParent = "D:\\Studium\\6. Semester\\Audio and Music Processing\\Exercise Track\\backup\\beatles\\album_0"+i+"\\";
//			}
//			
//			try {
//				//predict
//				String arguments[] = {"-i",pathParent,"-n","alpha.Alpha","-p"};
//				Runner.main(arguments);
//				
//				//evaluate
//				arguments[arguments.length-1]="-e";
//				Runner.main(arguments);
//				
//				//summarize
//				arguments[arguments.length-1]="-s";
//				Runner.main(arguments);
//				
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		pathParent = "D:\\Studium\\6. Semester\\Audio and Music Processing\\Exercise Track\\backup\\beatles\\album_01\\";
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
//		Processor p = new Alpha();
//		p.process(pathParent+"train10.wav");
//		p.process(pathParent+"beatles_12_Let_It_Be_07_Maggie_Mae.wav");
	}
	

}
