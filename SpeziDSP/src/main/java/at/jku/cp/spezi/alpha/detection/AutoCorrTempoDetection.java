package at.jku.cp.spezi.alpha.detection;

import java.util.ArrayList;
import java.util.List;

import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;

public class AutoCorrTempoDetection implements DetectionFunction {

	private int medianWindow;
	
	@Override
	public void detect(AudioFile file, DetectionResult result) {
		tempoAutoCorr(file,result);

	}
	
	private void tempoAutoCorr(AudioFile file, DetectionResult result) {
		double songDuration = file.getSamples().size()/file.getSampleRate();	
		List<Frame> frames = file.getFrames();
		double frameDuration = songDuration/frames.size();

		List<Double> detectionFunction = result.getOnsetDetectionFunction();
		DetectionUtils.filterMedian(detectionFunction,medianWindow);
		
	
		int lowerBound=(int)Math.floor(0.3/frameDuration);
		int upperBound=(int)Math.floor(1/frameDuration);
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
	
		result.getTempo().add(pred);
	}
	
	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<AutoCorrTempoDetection>{
		
		private Parameters(){};
		public Parameters medianWindow(int medianWindow){
			pushParam(df -> df.medianWindow= medianWindow);
			return this;
		}
		
	}

}
