package at.jku.cp.spezi.alpha.detection;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;

public class HarmContentChangeTempoDetection implements DetectionFunction {
	private int medianWindow;
	@Override
	public void detect(AudioFile file, DetectionResult result) {

		tempoHarmAutoCorr(file,result);

	}
	
	private void tempoHarmAutoCorr(AudioFile file, DetectionResult result) {

		double songDuration = file.getSamples().size()/file.getSampleRate();	
		List<Double> detectionFunction = result.getOnsetDetectionFunction();
		/*
		 * I tried to incorporate harmonic content changes as described in
		 * REDUCING OCTAVE ERRORS IN TEMPO ESTIMATION BY EMPLOYING HARMONIC CONTENT CHANGES
		 * by Kim and Lee
		 * https://iiav.org/icsv21/content/papers/papers/full_paper_451_20140318091819214.pdf
		 * There seem to be some some details missing respectively not explained well enough
		 *  which makes it quite hard to re-implement their system
		 */
	
		DetectionUtils.apply_semitoneFilter(file,110,3520);
		
		// get the filter frames
		List<Frame> frames = file.getFrames();
		double frameDuration = songDuration/frames.size();
		
		//List<Double> eta = new ArrayList<>();
		double eta[][] = new double[frames.size()][6];
		
		for(int i=0;i<frames.size();i++) {
			
			for(int d = 0;d<6;d++) {
				double sum = 0;
				double chroma[] = frames.get(i).magnitudes;
				for(int b = 0;b<12;b++) {
					sum+=phi(d,b)*chroma[b];
				}
				eta[i][d]=sum/chroma.length;
			}

		}
		
		List<Double> delta = new ArrayList<>();
		
		for(int i=1;i<frames.size();i++) {
			
			if(i+1<frames.size()) {
				double sum = 0;
				for(int d =0;d<6;d++) {
					sum+=Math.pow(eta[i+1][d]-eta[i-1][d], 2);
				}
				delta.add(Math.sqrt(sum));
			}
			
		}

		for(int i = 0;i<delta.size();i++) {
			if(delta.get(i)<DetectionUtils.adaptiveThreshold(delta, i, 0.3, 0.0, 5)) {
				delta.set(i, 0.);
			}
		}
		
		List<Double> fh = new ArrayList<>();
		double correctionFactor = 0.35;

		double detMax = Collections.max(detectionFunction);
		double deltaMax = Collections.max(delta);
		for(int i=0;i<detectionFunction.size();i++) {
			fh.add(detectionFunction.get(i)+correctionFactor*(detMax/deltaMax)*delta.get(i));
		}
		
		List<Double> harmCorr = new ArrayList<>();
		for(int k=0;k<=fh.size();k++) {
			double sum=0;
			for(int n=0;n<fh.size();n++) {
				if(n-k>=0&&k+n<fh.size()) {
					sum+=fh.get(n)*fh.get(n-k);
				} 
			}
			harmCorr.add(sum/fh.size());
		}
		
		DetectionUtils.normalize01(harmCorr);
		//(int)Math.floor(0.24/frameDuration)
		// find the index of the largest value
		int k=0;
		double kMax=-1;
		for(int i=10;i<harmCorr.size();i++) {
			if(harmCorr.get(i)>kMax) {
				kMax=harmCorr.get(i);
				k=i;
			}
		}
		
		StringBuffer sb = new StringBuffer();
		
		for(double d : harmCorr) {
			sb.append(d+",");
		}
		PrintWriter out;
		try {
			out = new PrintWriter("Harm.txt");
			out.println(sb.toString());
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		double k_val = 60.0f/(frameDuration*(k));
		
		
		DetectionUtils.filterMedian(detectionFunction,medianWindow);
		
	
		int lowerBound=(int)Math.floor(0.24/frameDuration);	//0.3 -> 200 bpm, 0.24 -> 250 bpm
		int upperBound=(int)Math.floor(1.5/frameDuration); 	//1 -> 60bpm,	1.5 -> 40bpm
		List<Double> autoCorr = new ArrayList<>();

		// perform the auto-correlation
		for(int i=lowerBound;i<=upperBound;i++) {
			double sum=0;
			for(int t=0;t<detectionFunction.size();t++) {
				if(i+t<detectionFunction.size()) {
					sum+=detectionFunction.get(t+i)*detectionFunction.get(t);
				} 
			}
			autoCorr.add(sum);
		}
		
		
		// find the index of the largest value
		int firstMax=0;
		double fMax=-1;
		for(int i=0;i<autoCorr.size();i++) {
			if(autoCorr.get(i)>fMax) {
				fMax=autoCorr.get(i);
				firstMax=i;
			}
		}
		
		// concert the index to the tempo
		double pred = 60.0f/(frameDuration*(lowerBound+firstMax));
		//System.out.println("Pred: "+(pred));
	
		
		double theta[] = {pred/6,pred/4,pred/3,pred/2,2*pred/3,pred,3*pred/2,2*pred}; //pred/6,pred/4,pred/3,,3*pred/2
		
		double minDiff = 100000;
		int index = 0;
		for(int i=0;i<theta.length;i++) {
			
			double diff = Math.abs(theta[i]-k_val);
			if(diff<minDiff) {
				index = i;
				minDiff = diff;
			}
		}
		
		result.getTempo().add(theta[index]);
	}
	
	/*
	 * Adapted from: Detecting Harmonic Change In Musical Audio by Harte et al.
	 */
	public static double phi(int d, int b) {
	
		double res = 0;
		switch(d) {
			case 0: res = 1*Math.sin(b*(7*Math.PI/6));break;
			case 1: res = 1*Math.cos(b*(7*Math.PI/6));break;
			case 2: res = 1*Math.sin(b*(3*Math.PI/2));break;
			case 3: res = 1*Math.cos(b*(3*Math.PI/2));break;
			case 4: res = 0.5*Math.sin(b*(2*Math.PI/3));break;
			case 5: res = 0.5*Math.cos(b*(2*Math.PI/3));break;
		}
		return res;
	}
	
	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<HarmContentChangeTempoDetection>{
		
		private Parameters(){};
		public Parameters medianWindow(int medianWindow){
			pushParam(df -> df.medianWindow= medianWindow);
			return this;
		}
		
	}


}
