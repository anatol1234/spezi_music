package at.jku.cp.spezi.alpha.detection;

import java.util.ArrayList;
import java.util.List;

import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;
import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;

public class LFSFOnsetDetection implements DetectionFunction{
	private int w;
	private int m;
	private double alpha;
	private double threshold;
	
	@Override
	public void detect(AudioFile file, DetectionResult result) {
		spectralDifference(file, result);
	}
	
	private void spectralDifference(AudioFile file, DetectionResult result) {
		
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
		peakPicking(result.getOnsets(), sd, frameDuration);	
	}
	
	private void peakPicking(List<Double> onsets, List<Double> values,double frameDuration) {
		List<Double > sd = normalizeValues(values);		
		for(int i=0;i<sd.size();i++) {
			
			double fn = sd.get(i);
			boolean cond1 = true;
			double fk_sum=0;
			for(int j=i-w;j<i+w;j++) {
				if(j>=0&&j<sd.size()) {
					if(fn<sd.get(j)) {
						cond1=false;
					}
					fk_sum+=sd.get(j);
				}
			}
			boolean cond2=fn>=fk_sum/(m*w+w+1)+threshold;
			boolean cond3=fn>=thresholdFunction(i-1,sd,alpha);
			
			
			if(cond1&&cond2&&cond3) {
				onsets.add(i * frameDuration);
				//System.out.println(i * frameDuration);
			}
		}
	}
	private double thresholdFunction(int n,List<Double> values,double alpha) {
		double g=0;
		if(n>=0&&n<values.size()) {
			double fn = values.get(n);
			double secondValue = alpha*thresholdFunction(n-1,values,alpha)+(1-alpha)*fn;
			g=Math.max(fn, secondValue);
		}		
		
		return g;
	}
	
	private List<Double> normalizeValues(List<Double> values) {
		double mean= Utils.getMean(values);
		double sd = Utils.getSD(values,mean);
		
		for(int i=0;i<values.size();i++) {
			values.set(i, (values.get(i)-mean)/sd);
		}
		return values;
	}
	
	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<LFSFOnsetDetection>{
		
		private Parameters(){};
		
		public Parameters w(int w){
			pushParam(df -> df.w = w);
			return this;
		}
		
		public Parameters m(int m){
			pushParam(df -> df.m = m);
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
		
	}

}
