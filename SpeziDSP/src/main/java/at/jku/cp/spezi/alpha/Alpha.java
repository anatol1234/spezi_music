package at.jku.cp.spezi.alpha;

import java.util.ArrayList;
import java.util.List;

import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;
import at.jku.cp.spezi.dsp.Processor;
import at.jku.cp.spezi.example.TooSimple;

public class Alpha implements Processor {

	
	private AudioFile audioFile;

	/**
	 * this list contains the results of the onset detection step
	 * 
	 * (time is in seconds)
	 */
	private List<Double> onsets;

	/**
	 * this list contain the results of the beat detection step
	 * 
	 * (beat times in seconds)
	 */
	private List<Double> beats;
	
	private List<Double> tempo;
	
	@Override
	public void process(String filename) {
		System.out.println("Initializing Processor '" + TooSimple.class.getName() + "'...");
		onsets = new ArrayList<Double>();
		beats = new ArrayList<Double>();
		tempo = new ArrayList<Double>();
		
		// an AudioFile object is created with the following parameters
		// AudioFile(WAVFILENAME, fftSize, hopSize, OPTIONAL: window function)
		// sizes are in samples

		// the WAV files you were provided with are all sampled at 44100 Hz

		// if you would like to work with multiple DFT resolutions, you would
		// simply create multiple AudioFile objects with different parameters
		System.out.println("Computing STFT ...");
		this.audioFile = new AudioFile(filename, 2048, 1024);
		AudioFile sec = new AudioFile(filename,2048,2048);
		System.out.println("Running Analysis...");
		onsetDetection();
		beatDetection();
		tempoEstimation();
		
	}

	@Override
	public List<Double> getOnsets() {
		return onsets;
	}

	@Override
	public List<Double> getTempo() {
		return tempo;
	}

	@Override
	public List<Double> getBeats() {
		return beats;
	}
	

	/**
	 * this is a 'Signal Envelope' implementation
	 * 
	 * TODO: you have to implement *at least* 2 more different onset detection
	 * functions have a look at the class 'Frame' - it contains the magnitudes,
	 * the phases, and more which you can use to implement your detection
	 * function
	 */
	private void onsetDetection() {
		System.out.println("Starting Onset Detection ...");
		spectralDifference();
	}
	
	private void spectralDifference() {
		
		double songDuration = audioFile.getSamples().size()/audioFile.getSampleRate();

		List<Frame> frames = audioFile.getFrames();
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
		peakPicking(sd,frameDuration);
		
		
		
	}
	
	private void peakPicking(List<Double> values,double frameDuration) {
		List<Double > sd = normalizeValues(values);
		int w = 3,m=3;
		double alpha=0.3;
		double threshold = 0.15;
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
		double mean=getMean(values);
		double sd = getSD(values,mean);
		
		for(int i=0;i<values.size();i++) {
			values.set(i, (values.get(i)-mean)/sd);
		}
		return values;
	}
	private double getMean(List<Double> values) {
		double mean=0;
		for(int i=0;i<values.size();i++)
			mean+=values.get(i);
		return mean/values.size();
	}
	private double getSD(List<Double> values, double mean) {
		double sd = 0;
		
		for(int i=0;i<values.size();i++) {
			sd+=Math.pow(values.get(i)-mean, 2);
		}
		return Math.pow(sd/values.size(),0.5);
	}

	/**
	 * TODO: we do not provide any beat detection example implementation. you
	 * ned to implement *at least* two different beat detection functions.
	 */
	private void beatDetection() {
		System.out.println("Starting Beat Detection (NOT IMPLEMENTED!) ...");
	}

	/**
	 * TODO: we do not provide any beat detection example implementation. you
	 * ned to implement *at least* two different tempo estimation functions.
	 */
	private void tempoEstimation() {
		System.out.println("Starting Tempo Estimation (NOT IMPLEMENTED!) ...");
	}
	
	
}