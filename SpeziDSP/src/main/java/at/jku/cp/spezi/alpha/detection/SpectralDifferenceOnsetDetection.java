package at.jku.cp.spezi.alpha.detection;


import java.util.ArrayList;
import java.util.List;

import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;

public class SpectralDifferenceOnsetDetection implements DetectionFunction {

	private int numOfFilters;
	private double alpha;
	
	@Override
	public void detect(AudioFile file, DetectionResult result) {
		
		DetectionUtils.applyMelFilter(file, alpha, numOfFilters);
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
				sum+=Math.pow(x, 2);
			}
			sd.add(sum);
			//System.out.println(sum);
		}
		
		result.setOnsetDetectionFunction(sd);
		DetectionUtils.peakPicking(sd,frameDuration,result);
	}
	
	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<SpectralDifferenceOnsetDetection>{
		
		private Parameters(){};
		
		
		public Parameters alpha(double alpha){
			pushParam(df -> df.alpha = alpha);
			return this;
		}
		
		public Parameters numOfFilters(int numOfFilters){
			pushParam(df -> df.numOfFilters= numOfFilters);
			return this;
		}
		
	}

}
