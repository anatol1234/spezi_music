package at.jku.cp.spezi.alpha.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.jku.cp.spezi.alpha.DetectionResult;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;

public class DetectionUtils {

	public static void filterMedian(List<Double> list,int w) {
		for(int i=0;i<list.size();i++) {
			//double sum=0;
			//int counter=0;
			List<Double> window=new ArrayList<>();
			for(int j=i-w;j<i+w;j++) {
				if(j>=0 && j<list.size()) {
					window.add(list.get(j));
					//sum+=list.get(j);
					//counter++;
				}
			}
			double med=0;
			Collections.sort(window);
			if(window.size()%2!=0) {
				med = window.get(window.size()/2 +1);
			} else {
				med = (window.get(window.size()/2)+window.get(window.size()/2 -1))/2;
			}
			//sum=sum/counter;
			if(list.get(i)<med) {
				list.set(i, 0d);
			}
			
		}
	}
	
	public static double getFilteredMax(Frame x,int m) {
		double m1 = -1;
		double m2 = -1;
		double m3 = -1;
		
		if(m-1>=0&&m-1<x.magnitudes.length) {
			m1 = x.magnitudes[m-1];
		}
		if(m>=0&&m<x.magnitudes.length) {
			m2 = x.magnitudes[m];
		}
		if(m+1>=0&m+1<x.magnitudes.length) {
			m3 = x.magnitudes[m+1];
		}
		
		double max = Math.max(m1, m2);
		return Math.max(max, m3);
		
	}
	public static void applyMelFilter(AudioFile file,double alpha,int numOfFilters) {
		List<Frame> frames = file.getFrames();

		double bins[]=DetectionUtils.mel_frequencies(numOfFilters+2, 27.5, 16000);

		int freq[] = new int[bins.length];
		for(int i=0;i<freq.length;i++) {
			freq[i]=(int)Math.floor((1024+1)*bins[i]/44100);
		}
		

		for(int i=0;i<frames.size();i++) {
			Frame f = frames.get(i);
			//double melMag[]=Utils.hz2Mel(f.magnitudes);
			//int[] indices=Utils.frequencies2bins(f.magnitudes, freq);
			f.magnitudes=DetectionUtils.melFilter(f.magnitudes, freq, numOfFilters);
			for(int j=0;j<f.magnitudes.length;j++) {
				f.magnitudes[j]=Math.log10(1+alpha*f.magnitudes[j]);
			}
			frames.set(i,f);
			
		}
	}
	
	
	public static void peakPicking(List<Double> values,double frameDuration, DetectionResult result) {
		List<Double > sd = normalizeValues(values);
		int w = 3,m=3;
		double alpha=0.3;
		double threshold = 0.15;
		for(int i=0;i<sd.size();i++) {
			
			double fn = sd.get(i);
			boolean cond1 = true;
			double fk_sum=0;
			for(int j=i-w;j<i+w;j++) { //local maximum check
				if(j>=0&&j<sd.size()) {
					if(fn<sd.get(j)) {
						cond1=false;
					}
					fk_sum+=sd.get(j);
				}
			}
			boolean cond2=fn>=fk_sum/(m*w+w+1)+threshold; //mean value check
			boolean cond3=fn>=thresholdFunction(i-1,sd,alpha);
			
			
			if(cond1&&cond2&&cond3) {
				result.getOnsets().add(i * frameDuration);
				//System.out.println(i * frameDuration);
			}
		}
	}
	
	public static void pickPeaksLecture(List<Double> values,double frameDuration,int w1, int w2, int w3, int w4, int w5, double threshold, DetectionResult result) {
		List<Double > sd =values;//normalize01(values);//values;// normalizeValues(values);
		int lastOnsetFrame=-1;
		
		for(int i=0;i<sd.size();i++) {
			
			double fn = sd.get(i);
			boolean cond1 = true;
			double fk_sum=0;
			for(int j=i-w1;j<i+w2;j++) { //local maximum check
				if(j>=0&&j<sd.size()) {
					if(fn<sd.get(j)) {
						cond1=false;
					}
					fk_sum+=sd.get(j);
				}
			}
			//calculate the mean within a window
			int samples=0;
			for(int j=i-w3;j<i+w4;j++) {
				if(j>=0&&j<sd.size()) {
					fk_sum+=sd.get(j);
					samples++;
				}
			}
			
			// fixed threshold
			boolean cond2=fn>=fk_sum/samples+threshold; //mean value check
			
			//with adaptive thresholding
			//boolean cond2=fn>=fk_sum/samples+adaptiveThreshold(sd,i,0.2,threshold,7);
				
			//yet another threshold version adapted from "UNIVERSAL ONSET DETECTION WITH BIDIRECTIONAL LONG-SHORT-TERM MEMORY NEURAL NETWORKS"
			//boolean cond2=fn>=fk_sum/samples+Math.min(Math.max(0.1, 0.478712*getMedianOfWindow(sd,(int)sd.size()/2,(int)sd.size()/2)),0.3);
			
			if(cond1&&cond2) {
				int onsetFrame = i;
				// check two onsets are not too close together
				if(onsetFrame-lastOnsetFrame>w5) {
					result.getOnsets().add(i * frameDuration);
					lastOnsetFrame=onsetFrame;
				}
			
			}
		}
	}
	
	public static double adaptiveThreshold(List<Double> d,int t, double alpha, double threshold,int m) {	
		return threshold+alpha*getMedianOfWindow(d,t,m);
	}
	
	public static double getMedianOfWindow(List<Double> values,int t,int m) {
		List<Double> window=new ArrayList<>();
		for(int i=t-m;i<t+m;i++) {
			if(i>=0 && i<values.size()) {
				window.add(values.get(i));
			}
		}
		double med=0;
		Collections.sort(window);
		if(window.size()%2!=0) {
			med = window.get(window.size()/2 +1);
		} else {
			med = (window.get(window.size()/2)+window.get(window.size()/2 -1))/2;
		}
		return med;
	}
	
	public static double thresholdFunction(int n,List<Double> values,double alpha) {
		double g=0;
		if(n>=0&&n<values.size()) {
			double fn = values.get(n);
			double secondValue = alpha*thresholdFunction(n-1,values,alpha)+(1-alpha)*fn;
			g=Math.max(fn, secondValue);
		}
		
		
		return g;
	}
	
	public static List<Double> normalizeValues(List<Double> values) {
		double mean=getMean(values);
		double sd = getSD(values,mean);
		
		for(int i=0;i<values.size();i++) {
			values.set(i, (values.get(i)-mean)/sd);
		}
		return values;
	}
	
	public static List<Double> normalize01(List<Double> values) {
		
		double max = Collections.max(values);
		double min = Collections.min(values);
		
		for(int i=0;i<values.size();i++) {
			values.set(i, (values.get(i)-min)/(max-min));
		}
		
		return values;
	}
	
	static double getMean(List<Double> values) {
		double mean=0;
		for(int i=0;i<values.size();i++)
			mean+=values.get(i);
		return mean/values.size();
	}
	
	static double getSD(List<Double> values, double mean) {
		double sd = 0;
		
		for(int i=0;i<values.size();i++) {
			sd+=Math.pow(values.get(i)-mean, 2);
		}
		return Math.pow(sd/values.size(),0.5);
	}
	
	/*following parts are adapted from the madmom library*/
	public static double[] hz2Mel(double magnitudes[]) {
		for(int i=0;i<magnitudes.length;i++) {
			magnitudes[i]=1127.01048*Math.log((magnitudes[i]/700) +1);
			//magnitudes[i]=2959*Math.log10((magnitudes[i]/700) +1);
		}
		return magnitudes;
	}
	public static double[] mel2Hz(double[] mel) {
		for(int i=0;i<mel.length;i++) {
			mel[i]=700*(Math.exp(mel[i]/1127.01048)-1);
			//magnitudes[i]=2959*Math.log10((magnitudes[i]/700) +1);
		}
		return mel;
	}
	public static double[] mel_frequencies(int num_bands, double  fmin, double fmax) {
		double[] fmin_a = {fmin};
		double[] fmax_a = {fmax};
		return mel2Hz(linspace(hz2Mel(fmin_a)[0], hz2Mel(fmax_a)[0], num_bands));
	}
	
	public static double[] linspace(double start, double end, int num) {
		double interval = (double)(end-start)/(num-1);
		double res[] = new double[num];
		for(int i=0;i<num;i++) {
			res[i]=start+i*interval;
		}
		return res;
	}
	
	public static int[]	frequencies2bins(double[] frequencies, double[] bin_frequencies) {
		int indices[]=new int[frequencies.length];
		for(int i=0;i<frequencies.length;i++) {
			 double f = frequencies[i];
			 double distance = Math.abs(bin_frequencies[0] - f);
			 int idx = 0;
			 for(int c = 1; c < bin_frequencies.length; c++){
			     double cdistance = Math.abs(bin_frequencies[c] -f);
			     if(cdistance < distance){
			         idx = c;
			         distance = cdistance;
			     }
			 }
			 indices[i]=idx;
		}
		 
		return indices;
	}

	
	/**
	 * Taken from https://github.com/Sciss/SpeechRecognitionHMM/blob/master/src/main/java/org/ioe/tprsa/audio/feature/MFCC.java
	 * Performs mel filter operation
	 * 
	 * @param bin
	 *            magnitude spectrum (| |)^2 of fft
	 * @param cBin
	 *            mel filter coefficients
	 * @return mel filtered coefficients --> filter bank coefficients.
	 */
	public static double[] melFilter(double bin[], int cBin[],int numMelFilters) {
		final double temp[] = new double[numMelFilters + 2];
		for (int k = 1; k <= numMelFilters; k++) {
			double num1 = 0.0, num2 = 0.0;
			for (int i = cBin[k - 1]; i <= cBin[k]; i++) {
				num1 += ((i - cBin[k - 1] + 1) / (cBin[k] - cBin[k - 1] + 1)) * bin[i];
			}

			for (int i = cBin[k] + 1; i <= cBin[k + 1]; i++) {
				num2 += (1 - ((i - cBin[k]) / (cBin[k + 1] - cBin[k] + 1))) * bin[i];
			}

			temp[k] = num1 + num2;
		}
		final double fBank[] = new double[numMelFilters];
        System.arraycopy(temp, 1, fBank, 0, numMelFilters);
		return fBank;
	}

	
}
