package at.jku.cp.spezi.alpha;

import java.io.File;
import java.util.List;

import at.jku.cp.spezi.Utils;
import at.jku.cp.spezi.dsp.Processor;

public class Test {

	public static void main(String[] args) {
		
		Processor p = new Alpha();
		String pathParent = "src\\main\\java\\at\\jku\\cp\\spezi\\alpha\\train\\";
		//p.process(pathParent+"train1.wav");
		
		List<Double> data = Utils.listFromFile(pathParent+"train1.onsets.gt");
		System.out.println("Stop");
	}

}
