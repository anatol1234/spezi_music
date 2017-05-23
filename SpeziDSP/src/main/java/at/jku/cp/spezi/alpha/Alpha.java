package at.jku.cp.spezi.alpha;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import at.jku.cp.spezi.alpha.detection.EnergyBasedOnsetDetection;
import at.jku.cp.spezi.alpha.detection.LFSFOnsetDetection;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Processor;
import at.jku.cp.spezi.example.TooSimple;

public class Alpha implements Processor {
	private static final LFSFOnsetDetection.Parameters LFSF_ONSET_DETECTION_PARAMS = LFSFOnsetDetection.createParams()
			.w(3)
			.m(3)
			.alpha(0.3)
			.threshold(0.15); 
	private static final EnergyBasedOnsetDetection.Parameters EB_ONSET_DETECTION_PARAMS = EnergyBasedOnsetDetection.createParams()
			.dThreshold(3.1)
			.epmThreshold(30);
	
	private final static Consumer<Alpha> DEFAULT_DETECTION_FUNCS_MODEL = a -> {
		a.setDetectionFunction(
				DetectionType.ONSET,
				EnergyBasedOnsetDetection.class,
				EB_ONSET_DETECTION_PARAMS);
	};
	private static Supplier<Consumer<Alpha>> detectionFuncsModelSupplier = () -> DEFAULT_DETECTION_FUNCS_MODEL;
	private Consumer<Alpha> detectionFuncsModel;
	
	private AudioFile audioFile;
	private DetectionResult result;
	private final EnumMap<DetectionType, DetectionFunction> detectionsMap = new EnumMap<>(DetectionType.class);
	
	public enum DetectionType{
		ONSET,
		BEAT,
		TEMPO
	}
	
	@Override
	public void process(String filename) {
		System.out.println("Initializing Processor '" + TooSimple.class.getName() + "'...");
		if(detectionFuncsModel == null){
			detectionFuncsModel = detectionFuncsModelSupplier.get();
		}
		result = new DetectionResult();
		
		// an AudioFile object is created with the following parameters
		// AudioFile(WAVFILENAME, fftSize, hopSize, OPTIONAL: window function)
		// sizes are in samples

		// the WAV files you were provided with are all sampled at 44100 Hz

		// if you would like to work with multiple DFT resolutions, you would
		// simply create multiple AudioFile objects with different parameters
		System.out.println("Computing STFT ...");
		this.audioFile = new AudioFile(filename, 2048, 1024);
		System.out.println("Running Analysis...");
		detectionFuncsModel.accept(this);	
		detectionsMap.values().forEach(df -> df.detect(audioFile, result));				
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