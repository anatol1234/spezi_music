package at.jku.cp.spezi.alpha;

import at.jku.cp.spezi.dsp.AudioFile;

public interface DetectionFunction {

	void detect(AudioFile file, DetectionResult result);
	
}
