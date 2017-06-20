package at.jku.cp.spezi.alpha.detection;

import java.util.ArrayList;
import java.util.List;

import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;
import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;

public class LFSFOnsetDetection implements DetectionFunction{
	
	private int w1,w2,w3,w4,w5;
	private double alpha,threshold;
	private int numOfFilters;
	
	@Override
	public void detect(AudioFile file, DetectionResult result) {
		LFSF(file, result);
	}
	
	private void LFSF(AudioFile file, DetectionResult result) {
		
		DetectionUtils.applyMelFilter(file,alpha,numOfFilters);
		double songDuration = file.getSamples().size()/file.getSampleRate();
		List<Frame> frames = file.getFrames();
		double frameDuration = songDuration/frames.size();
		
		List<Double> sd = new ArrayList<>();
		for(int i=1;i<frames.size();i++) {
			Frame a = frames.get(i);
			Frame b = frames.get(i-1);
			double sum=0;
			for(int j = 0;j<a.magnitudes.length;j++) {
				double x = a.magnitudes[j]-b.magnitudes[j];
				if(x<=0) {
					x=0;
				}
				sum+=x;
			}
			sd.add(sum);
		}
		
		result.setOnsetDetectionFunction(sd);
		DetectionUtils.pickPeaksLecture(sd,frameDuration,w1,w2, w3,w4, w5,threshold,result,false);
	}
	

	

	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<LFSFOnsetDetection>{
		
		private Parameters(){};
		
		public Parameters w1(int w1){
			pushParam(df -> df.w1 = w1);
			return this;
		}
		public Parameters w2(int w2){
			pushParam(df -> df.w2 = w2);
			return this;
		}
		
		public Parameters w3(int w3){
			pushParam(df -> df.w3 = w3);
			return this;
		}
		
		public Parameters w4(int w4){
			pushParam(df -> df.w4 = w4);
			return this;
		}
		
		public Parameters w5(int w5){
			pushParam(df -> df.w5 = w5);
			return this;
		}
			
		public Parameters alpha(double alpha){
			pushParam(df -> df.alpha = alpha);
			return this;
		}
		
		public Parameters threshold(double threshold){
			pushParam(df -> df.threshold = threshold);
			return this;
		}
		
		public Parameters numOfFilters(int numOfFilters){
			pushParam(df -> df.numOfFilters= numOfFilters);
			return this;
		}
		
	}

}
