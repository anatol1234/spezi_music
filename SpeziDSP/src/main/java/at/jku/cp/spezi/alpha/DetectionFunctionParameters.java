package at.jku.cp.spezi.alpha;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class DetectionFunctionParameters<DF extends DetectionFunction> {
	private final List<Consumer<DF>> appliers = new ArrayList<>();
	
	protected void applyTo(DF detectionFunction){
		appliers.forEach(a -> a.accept(detectionFunction));
	}
	
	protected void pushParam(Consumer<DF> applier){
		appliers.add(applier);
	}
	
}
