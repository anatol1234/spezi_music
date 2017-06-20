package at.jku.cp.spezi.alpha.detection;

import java.util.ArrayList;
import java.util.List;

import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;

public class CombFilterBeatDetection implements DetectionFunction{
	double peakDeltaRatio;	
	
	@Override
	public void detect(AudioFile file, DetectionResult result) {
		double songDuration = file.getSamples().size()/file.getSampleRate();	
		List<Frame> frames = file.getFrames();
		double frameDuration = songDuration/frames.size();
		
		List<Double> onsetDetectionFunction = result.getOnsetDetectionFunction();
		//DetectionUtils.filterMedian(onsetDetectionFunction, 10);
		List<Double> tempoDetectionFunction = result.getTempoDetectionFunction();
		List<Double> crossCorr = new ArrayList<>();
		
		// perform the cross-correlation
		for(int i=0;i < frames.size();i++) {
			double sum=0;
			int functionSize = Math.min(onsetDetectionFunction.size(), tempoDetectionFunction.size());
			for(int t=0;t<functionSize ;t++) {
				if(i+t<functionSize) {
					sum+=onsetDetectionFunction.get(t+i)*tempoDetectionFunction.get(t);
				} 
			}
			crossCorr.add(sum);
		}
		
		
		// find the index of the largest value
		int firstMax=0;
		double fMax=-1;
		for(int i=0;i<crossCorr.size();i++) {
			if(crossCorr.get(i)>fMax) {
				fMax=crossCorr.get(i);
				firstMax=i;
			}
		}
		
		List<Double> peakFunction = result.getOnsets();
		double tempoDelta = 60d/result.getTempo().get(0);
		double peakDelta = tempoDelta*peakDeltaRatio;
		double beattime = firstMax*frameDuration;
		double peaktime = -1;
		while(beattime < songDuration){
			if(peaktime == -1){
				result.getBeats().add(beattime);
			}else{
				result.getBeats().add(peaktime);
				/*double newTempoDelta = peaktime - result.getBeats().get(result.getBeats().size()-2);
				if(Math.abs(tempoDelta - newTempoDelta)/tempoDelta > 0.5){
					tempoDelta = newTempoDelta;
				}*/
				//beattime = peaktime;
			}
			beattime += tempoDelta;
			peaktime = -1;
			for(int i = 0; i < peakFunction.size(); i++){
				double currpeaktime = peakFunction.get(i);
				if(beattime + peakDelta < currpeaktime){
					break;
				}else if(beattime - peakDelta <= currpeaktime){
					if(Math.abs(beattime - currpeaktime) < Math.abs(beattime - peaktime)){
						peaktime = currpeaktime;						
					}
				}
			}			
		}
	}
	
	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<CombFilterBeatDetection>{
		
		private Parameters(){};		
		
		public Parameters peakDeltaRatio(double peakDeltaRatio){
			pushParam(df -> df.peakDeltaRatio = peakDeltaRatio);
			return this;
		}
		
	}

}
