package at.jku.cp.spezi.alpha.detection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.jku.cp.spezi.alpha.DetectionFunction;
import at.jku.cp.spezi.alpha.DetectionFunctionParameters;
import at.jku.cp.spezi.alpha.DetectionResult;
import at.jku.cp.spezi.dsp.AudioFile;
import at.jku.cp.spezi.dsp.Frame;

public class MultiAgentTempoBeat implements DetectionFunction {

	private int tempoUpperLimit = 250;
	private int tempoLowerLimit = 40;
	@Override
	public void detect(AudioFile file, DetectionResult result) {
		
		double tempo = result.getTempo().get(0);
		
		//double tempoHypothesis[] = {tempo/3,tempo/2,2*tempo/3,tempo,3*tempo/2,2*tempo};
		DetectionUtils.filterMedian(result.getOnsetDetectionFunction(),20);
		List<Double> detectionFunction = DetectionUtils.normalize01(result.getOnsetDetectionFunction());
		double songDuration = file.getSamples().size()/file.getSampleRate();	
		List<Frame> frames = file.getFrames();
		double frameDuration = songDuration/frames.size();
		
		List<Double> events = result.getOnsets();
		
		//List<Double> tempoHypothesis = clusterIOI(events);
		
		List<Cluster> clusters = cluster(events);
		List<Double> tempoHypothesis = new ArrayList<>();

		Collections.sort(clusters);
		//only consider the 10 biggest clusters (that contain the most IOIs
		for(int i=clusters.size()-1;i>clusters.size()-10&&i>=0;i--) {
			double t = 60/clusters.get(i).average;
			if(t>tempoLowerLimit&&t<tempoUpperLimit)
				tempoHypothesis.add(t);
			
		}

			
			
		
		tempoHypothesis.add(tempo);

		if(2*tempo<tempoUpperLimit)
			tempoHypothesis.add(2*tempo);
		
		if(3*tempo/2<tempoUpperLimit)
			tempoHypothesis.add(3*tempo/2);
		
		if(2*tempo/3>tempoLowerLimit && 2*tempo/3<tempoUpperLimit)
			tempoHypothesis.add(2*tempo/3);
		
		if(tempo/4>tempoLowerLimit) {
			tempoHypothesis.add(tempo/4);
		}
		if(tempo/3>tempoLowerLimit)
			tempoHypothesis.add(tempo/3);
		
		if(tempo/2>tempoLowerLimit)
			tempoHypothesis.add(tempo/2);
		
		
		// 0.07 in the paper "Beat Tracking with Musical Knowledge", 0.06 worked better on the given data
		double innerWindow = 0.06d;
		
		List<Agent> agents =  new ArrayList<>();
		
		int considerEventsUntil = 10;
		// spawn agents for the first 10 seconds
		for(double event : events) {
			if(event<considerEventsUntil) {

				for(double t : tempoHypothesis) {
					double outerWindow = 60/(4*t);  //depends on the current tempo hypothesis
					agents.add(new Agent(t,innerWindow,outerWindow,event,agents,frameDuration,detectionFunction));
				}
			} else {
				// no agents spawned
				if(agents.size()==0) {
					considerEventsUntil+=5;
				} else {
					break;
				}
				
			}
		}
		
		
		for(double event : events) {
			for(Agent a : new ArrayList<Agent>(agents)) {
				a.processEvent(event);
			}
			agents = pruneAgents(agents,event,tempoHypothesis);	

		}
		
		Collections.sort(agents);
		Agent best = agents.get(agents.size()-1);
		System.out.println(best.correctedTempo);
		//result.getTempo().set(0,best.originalTempo);
		//result.getTempo().set(0,best.correctedTempo);
		result.getBeats().addAll(best.predictedBeats);
	}
	
	private List<Agent> pruneAgents(List<Agent> agents,double currentEvent,List<Double> tempoHypo) {
		
		List<List<Agent>> sortedByTempo = new ArrayList<>();
		for(int i=0;i<tempoHypo.size();i++) {
			sortedByTempo.add(new ArrayList<Agent>());
		}
		for(Agent a : agents) {
			for(int i=0;i<tempoHypo.size();i++) {
				if(Math.abs(a.originalTempo-tempoHypo.get(i))<0.00001) {
					sortedByTempo.get(i).add(a);
					break;
				}
			}
			
		}
		
		List<Agent> prunedList = new ArrayList<>();
		for(List<Agent> agentList : sortedByTempo) {
			Collections.sort(agentList); // sorted ascending by score
						
			for(int i=0;i<agentList.size();i++) {
				boolean acceptAgent = true;
				Agent a = agentList.get(i);
				if(Math.abs(a.predictedBeats.get(a.predictedBeats.size()-1)-currentEvent)<0.000001) {
		
					for(int j=i+1;j<agentList.size();j++) {
						Agent b = agentList.get(j);
						if(Math.abs(b.predictedBeats.get(b.predictedBeats.size()-1)-currentEvent)<0.000001) {
	
							if(Math.abs(a.predictedBeats.get(a.predictedBeats.size()-1)-b.predictedBeats.get(b.predictedBeats.size()-1))<0.000001) {
								acceptAgent = false;
								break;
							}
						}
					}
					

				}
				
				
				if(acceptAgent) {
					prunedList.add(a);
				}
			
			}
			
		}
		
		//just in case everything is pruned due to an error
		if(prunedList.size()==0) {
			return agents;
		}
		return prunedList;
	}
	
	private List<Cluster> cluster(List<Double> onsets) {
		List<Cluster> clusters = new ArrayList<>();
		for(int i=0;i<onsets.size();i++) {
			for(int j=i+1;j<onsets.size();j++) {
				double interval = onsets.get(j)-onsets.get(i);
				
				if(interval>0.025&&interval<2.5) {
					if(!fittingCluster(clusters,interval)) {
						//create new cluster
						Cluster cm = new Cluster();
						cm.add(interval);
						clusters.add(cm);
					}
				}
			}
		}
		combineClusters(clusters);
		return clusters;
	}
	
	private boolean fittingCluster(List<Cluster> clusters, double interval){
		double delta = 0.025;
		
		Cluster fitting = null;
		double min = 1000000;
		for(Cluster c : clusters) {
			if(Math.abs(c.average-interval)<min) {
				min = c.average-interval;
				fitting=c;
			}
		}
		
		if(fitting!=null) {
			if(Math.abs(fitting.average-interval)<delta) {
				fitting.add(interval);
				return true;
			}
		}
		return false;
	}
	
	private void combineClusters(List<Cluster> clusters) {

		double delta = 0.025;
	
		for(int i=0;i<clusters.size();i++) {
			Cluster cs = clusters.get(i);
			for(int j=i+1;j<clusters.size();j++) {
				Cluster ct = clusters.get(j);
				if(Math.abs(cs.average-ct.average)<delta) {
					cs.addMultiple(ct.items);
					clusters.remove(j);
				}
			}
		}
	}
	

	/*
	 * Not the algorithm described in "Beat Tracking with Musical Knowledge"
	 * This version is not sophisticated and was just for testing purposes
	 */
	private List<Double> clusterIOI(List<Double> onsets) {
		List<Double> hypothesis = new ArrayList<>();
		
		for(int i=0;i<onsets.size();i++) {
			for(int j=i+1;j<onsets.size();j++) {
				double hypo = 60/(Math.abs(onsets.get(i)-onsets.get(j)));
				if(hypo<tempoUpperLimit) {	
					if(hypo>tempoLowerLimit)	
						hypothesis.add(hypo);
				} else {
					break;
				}
				
			}
		}
		
		Collections.sort(hypothesis);
		
		
		
		List<Double> finalHyp = new ArrayList<>();
		Map<Integer,Integer> map = new HashMap<>();
		
		if(hypothesis.size()>0) {
			int currTempo = (int) hypothesis.get(0).doubleValue();
			int cnt = 1;
			for(int i=1;i<hypothesis.size();i++) {
				
				int tmp = (int) hypothesis.get(i).doubleValue();

				if(tmp == currTempo) {	//tmp < currTempo+3&&tmp>currTempo-3
					cnt++;
				} else {
					map.put(currTempo, cnt);
					currTempo = tmp;
					cnt = 1;
				}
				
			}
			
			Object vals[] =map.values().stream().sorted().toArray();
			int median = 0;
			if(vals.length%2!=0) {
				median = (int) vals[vals.length/2+1];
			} else {
				median=((int)vals[vals.length/2]+(int)vals[vals.length/2 -1])/2;
			}
			
			for(int tmp : map.keySet()) {
				if(map.get(tmp)>median) {

					finalHyp.add((double)tmp);
				}
			}
		}
		

		

		
		return finalHyp;
	}
	
	class Cluster implements Comparable<Cluster> {
		List<Double> items;
		double average;
		
		public Cluster() {
			this.items=new ArrayList<>();
			this.average=0;
		}
		
		public void add(double interval) {
			items.add(interval);
			updateAverage();
		}
		
		private void addMultiple(List<Double> intervals) {
			items.addAll(intervals);
			updateAverage();
		}
		
		private void updateAverage() {
			double sum = 0;
			for(double d : items) {
				sum+=d;
			}
			average=sum/items.size();
		}

		@Override
		public int compareTo(Cluster o) {
			 if (o.items.size() > this.items.size()) {
		            return -1;
		        } else if (o.items.size() < this.items.size()) {
		            return 1;
		        } else {
		            return 0;
		        }
		}
		
		
	}
	
	class Agent implements Comparable<Agent> {
		
		double originalTempo;
		double correctedTempo;
		double score;
		List<Double> predictedBeats;
		double innerWindow;
		double outerWindow;
		List<Agent> otherAgents;
		double frameDuration;
		List<Double> onsetDetectionFunction;
		
		public Agent(double tempo,double innerWindow,double outerWindow,double startEvent, List<Agent> otherAgents,
				double frameDuration, List<Double> onsetDetectionFunction) {
			this.originalTempo=tempo;
			this.correctedTempo=tempo;
			this.innerWindow=innerWindow;
			this.outerWindow=outerWindow;
			this.score=0;
			this.predictedBeats = new ArrayList<>();
			this.predictedBeats.add(startEvent);
			
			
			this.otherAgents = otherAgents;
			this.frameDuration=frameDuration;
			this.onsetDetectionFunction=onsetDetectionFunction;
		}
		
		public void processEvent(double event) {
			
			if(event>predictedBeats.get(0)) {
				
				double beatDuration = 60/correctedTempo;
				
				
				double prediction = predictedBeats.get(predictedBeats.size()-1)+beatDuration;
				
				// check if within inner window
				if(withinWindow(innerWindow/2,innerWindow/2,prediction,event)) {
					// event within inner window is accepted as beat time
					predictedBeats.add(event);
					double correction = (prediction-event);
					//double correction = prediction/event;
					correctedTempo +=correction;
					addScore(prediction,event);
				} else {
					List<Double> interpolatedBeats = new ArrayList<>();
					interpolatedBeats.add(prediction);
					boolean noBeat = false;
					while(true) {				
						double pred = interpolatedBeats.get(interpolatedBeats.size()-1)+beatDuration;			
						interpolatedBeats.add(pred);
						if(withinWindow(innerWindow/2,innerWindow/2,pred,event)) {		
							break;
						}
						
						if(pred+innerWindow>event) {
							noBeat = true;
							break;
						}
						
					}
					
					if(!noBeat) {	//beat found, add interpolated beats
						
						predictedBeats.addAll(interpolatedBeats);
						//processEvent(event);	// reprocess this event
						predictedBeats.add(event);
						addScore(prediction,event);
					} else {

						if(withinWindow(outerWindow/3,2*outerWindow/3,prediction,event)) { // check if within outer window

							Agent a = new Agent(correctedTempo,innerWindow,outerWindow,-1,otherAgents,this.frameDuration,this.onsetDetectionFunction);
							a.predictedBeats = new ArrayList<>(predictedBeats);
							a.score = this.score;
							//a.originalTempo = this.originalTempo;
							
							
							//accept event and spawn another agent
							predictedBeats.add(event);
							addScore(prediction,event);
							otherAgents.add(a);
					
						} else { }// event is ignored
					}
						
					
				}
			}
			
		}
		
		private void addScore(double prediction,double event) {
			// to avoid a very small or even 0 diff value
			double diff = Math.max(Math.abs(prediction-event),0.0001);	
			int index = (int) (event/this.frameDuration);
			double detectionVal = 0;
			if(index>=0&&index<onsetDetectionFunction.size()) {
				detectionVal = this.onsetDetectionFunction.get(index);
			}

			score+=(1/diff)*detectionVal;	//scale everything by the salience of the onset
		}

		
		private boolean withinWindow(double wSizeLeft,double wSizeRight,double reference,double event) {
			if(event>reference+wSizeRight||event<reference-wSizeLeft) {
				return false;
			}
			return true;
		}
		
	    @Override
	    public int compareTo(Agent a) {
	        if (a.score > this.score) {
	            return -1;
	        } else if (a.score < this.score) {
	            return 1;
	        } else {
	            return 0;
	        }
	    }


		
	
	}
	
	public static Parameters createParams(){
		return new Parameters();
	}
	
	public static class Parameters extends DetectionFunctionParameters<MultiAgentTempoBeat>{
		
		private Parameters(){};

		
	}

}
