package at.jku.cp.spezi.alpha;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import at.jku.cp.spezi.alpha.detection.AutoCorrTempoDetection;
import at.jku.cp.spezi.alpha.detection.CombFilterBeatDetection;
import at.jku.cp.spezi.alpha.detection.EnergyBasedOnsetDetection;
import at.jku.cp.spezi.alpha.detection.LFSFOnsetDetection;
import at.jku.cp.spezi.alpha.detection.SpectralDifferenceOnsetDetection;
import at.jku.cp.spezi.alpha.detection.SuperFluxOnsetDetection;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Processor;

public class Alpha implements Processor {
	private static final LFSFOnsetDetection.Parameters LFSF_ONSET_DETECTION_PARAMS = LFSFOnsetDetection.createParams()
			.w1(11).w2(4).w3(18).w4(19).w5(6)
			.alpha(0.82655126)
			.threshold(0.2039732669227441)
			.numOfFilters(138);
	
	private static final EnergyBasedOnsetDetection.Parameters EB_ONSET_DETECTION_PARAMS = EnergyBasedOnsetDetection.createParams()
			.dThreshold(0.3) 
			.epmThreshold(22) 
			.alpha(0.82655126)
			.numOfFilters(138);
	
	private static final SpectralDifferenceOnsetDetection.Parameters SD_ONSET_DETECTION_PARAMS = SpectralDifferenceOnsetDetection.createParams()
			.alpha(0.82655126)
			.numOfFilters(138);
	
	private static final SuperFluxOnsetDetection.Parameters SF_ONSET_DETECTION_PARAMS = SuperFluxOnsetDetection.createParams()
			.w1(2).w2(10).w3(18).w4(18).w5(6).mu(3)
			.alpha(0.82655126)
			.threshold(0.15)//0.34489238772825725
			.numOfFilters(138);
	
	private static final AutoCorrTempoDetection.Parameters AC_TEMPO_DETECTION_PARAMS = AutoCorrTempoDetection.createParams()
			.medianWindow(10);
	
	private static final CombFilterBeatDetection.Parameters CB_BEAT_DETECTION_PARAMS = CombFilterBeatDetection.createParams()
			.peakDeltaRatio(0.3);
			
	private final static Consumer<Alpha> EB_DETECTION_FUNCS_MODEL = a -> {
		a.setSTFTParams(2048,1024);
		a.setDetectionFunction(
				DetectionType.ONSET,
				EnergyBasedOnsetDetection.class,
				EB_ONSET_DETECTION_PARAMS);
		a.setDetectionFunction(
				DetectionType.TEMPO,
				AutoCorrTempoDetection.class, 
				AC_TEMPO_DETECTION_PARAMS);
	};
	
	private final static Consumer<Alpha> SD_ONSET_DETECTION_FUNCS_MODEL = a -> {
		a.setSTFTParams(2048, 1024);
		a.setDetectionFunction(
				DetectionType.ONSET,
				SpectralDifferenceOnsetDetection.class, 
				SD_ONSET_DETECTION_PARAMS);
	};
	
	private final static Consumer<Alpha> LFSF_ONSET_DETECTION_FUNCS_MODEL = a -> {
		a.setSTFTParams(2048, 256);
		a.setDetectionFunction(
				DetectionType.ONSET,
				LFSFOnsetDetection.class, 
				LFSF_ONSET_DETECTION_PARAMS);
	};
	
	private final static Consumer<Alpha> SF_ONSET_DETECTION_FUNCS_MODEL = a -> {
		a.setSTFTParams(2048, 256);
		a.setDetectionFunction(
				DetectionType.ONSET,
				SuperFluxOnsetDetection.class, 
				SF_ONSET_DETECTION_PARAMS);

	};
	
	// SF Onset Detection + AC Tempo Detection
	private final static Consumer<Alpha> SF_AC_CB_DETECTION_FUNCS_MODEL = a -> {
		a.setSTFTParams(2048, 256);
		a.setDetectionFunction(
				DetectionType.ONSET,
				SuperFluxOnsetDetection.class, 
				SF_ONSET_DETECTION_PARAMS);		
		a.setDetectionFunction(
				DetectionType.TEMPO,
				AutoCorrTempoDetection.class, 
				AC_TEMPO_DETECTION_PARAMS);
		a.setDetectionFunction(
				DetectionType.BEAT,
				CombFilterBeatDetection.class, 
				CB_BEAT_DETECTION_PARAMS);
	};	
	
	private final static Consumer<Alpha> AC_TEMPO_DETECTION_FUNCS_MODEL = a -> {
		a.setSTFTParams(2048, 256);
		a.setDetectionFunction(
				DetectionType.TEMPO,
				AutoCorrTempoDetection.class, 
				AC_TEMPO_DETECTION_PARAMS);
	};
	
	private static Supplier<Consumer<Alpha>> detectionFuncsModelSupplier = () -> SF_ONSET_DETECTION_FUNCS_MODEL;//SF_AC_CB_DETECTION_FUNCS_MODEL;
	private Consumer<Alpha> detectionFuncsModel;
	
	private AudioFile audioFile;
	private DetectionResult result;
	private int fftsize;
	private int hopsize;
	private final LinkedHashMap<DetectionType, DetectionFunction> detectionsMap = new LinkedHashMap<>();
	
	public enum DetectionType{
		ONSET,
		BEAT,
		TEMPO
	}
	
	@Override
	public void process(String filename) {
		//System.out.println("Initializing Processor '" + TooSimple.class.getName() + "'...");	
		
		detectionFuncsModel = detectionFuncsModelSupplier.get();		
		result = new DetectionResult();
		
		// an AudioFile object is created with the following parameters
		// AudioFile(WAVFILENAME, fftSize, hopSize, OPTIONAL: window function)
		// sizes are in samples

		// the WAV files you were provided with are all sampled at 44100 Hz

		// if you would like to work with multiple DFT resolutions, you would
		// simply create multiple AudioFile objects with different parameters
		detectionFuncsModel.accept(this);	
		System.out.println("Computing STFT ...");
		this.audioFile = new AudioFile(filename, fftsize, hopsize);
		System.out.println("Running Analysis...");
		//detectionsMap.values().stream().forEachOrdered(df -> df.detect(audioFile, result));
		detectionsMap.get(DetectionType.ONSET).detect(audioFile, result);
		
		detectionFuncsModel = AC_TEMPO_DETECTION_FUNCS_MODEL;
		detectionFuncsModel.accept(this);	
		this.audioFile = new AudioFile(filename, fftsize, hopsize);
		detectionsMap.get(DetectionType.TEMPO).detect(audioFile, result);
		
		
	}

	public void setSTFTParams(int fftsize, int hopsize){
		this.fftsize = fftsize;
		this.hopsize = hopsize;		
	}
	
	public <DF extends DetectionFunction> void setDetectionFunction(DetectionType detectType, Class<DF> funcType, DetectionFunctionParameters<DF> parameters){
		DetectionFunction df = detectionsMap.get(detectType);
		if(df == null || df.getClass() == funcType){
			try {
				df = funcType.newInstance();
			} catch (IllegalAccessException | InstantiationException e) {
				throw new IllegalArgumentException("Invalid detection function");
			}
			detectionsMap.put(detectType, df);
		}
		parameters.applyTo(funcType.cast(df));
	}
	
	public static void setDetectionFuncsModelSupplier(Supplier<Consumer<Alpha>> detectionFuncsModelSupplier){
		Alpha.detectionFuncsModelSupplier = detectionFuncsModelSupplier;
	}

	@Override
	public List<Double> getOnsets() {
		return result.getOnsets();
	}

	@Override
	public List<Double> getTempo() {
		return result.getTempo();
	}

	@Override
	public List<Double> getBeats() {
		return result.getBeats();
	}	
	
}