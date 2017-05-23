package at.jku.cp.spezi.alpha.detection;

import java.util.List;

import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;

public class EnergyBasedOnsetDetection implements DetectionFunction{
	private double dThreshold;
	private double epmThreshold;
	
	@Override
	public void detect(AudioFile file, DetectionResult result) {
		double songDuration = file.getSamples().size()/file.getSampleRate();
		List<Frame> frames = file.getFrames();
		double frameDuration = songDuration/frames.size();

		for (int i = 1; i < frames.size(); i++){
			int epm = 0;
			Frame prevF =  frames.get(i-1);
			Frame currF =  frames.get(i);
			for(int j = 0; j < currF.magnitudes.length; j++){
				double di = Math.log(currF.magnitudes[j]/prevF.magnitudes[j])/Math.log(2);
				if(di > dThreshold){
					epm++;
				}
			}
			if(epm > epmThreshold){
				result.getOnsets().add(i * frameDuration);
			}
		}
	}
	
	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<EnergyBasedOnsetDetection>{
		
		private Parameters(){};		
		
		public Parameters dThreshold(double dThreshold){
			pushParam(df -> df.dThreshold = dThreshold);
			return this;
		}
		
		public Parameters epmThreshold(double epmThreshold){
			pushParam(df -> df.epmThreshold = epmThreshold);
			return this;
		}
		
	}

}
