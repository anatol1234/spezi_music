package at.jku.cp.spezi.alpha;

import java.util.ArrayList;
import java.util.List;

public class DetectionResult {

	/**
	 * this list contains the results of the onset detection step
	 * 
	 * (time is in seconds)
	 */
	private final List<Double> onsets = new ArrayList<>();

	/**
	 * this list contain the results of the beat detection step
	 * 
	 * (beat times in seconds)
	 */
	private final List<Double> beats = new ArrayList<>();
	
	/**
	 * this list contain the results of the tempo detection step
	 * 
	 * (time is in seconds)
	 */
	private final List<Double> tempo = new ArrayList<>();

	public List<Double> getOnsets() {
		return onsets;
	}

	public List<Double> getBeats() {
		return beats;
	}

	public List<Double> getTempo() {
		return tempo;
	}

	
	
		
	
	
}
