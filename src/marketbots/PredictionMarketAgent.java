package marketbots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import brown.agent.AbsPredictionMarketAgent;
import brown.channels.library.CallMarketChannel;
import brown.exceptions.AgentCreationException;

public class PredictionMarketAgent extends AbsPredictionMarketAgent {
	
	double trueValue;
	int numDecoys;
	boolean gotHeads;
	
	double buyMargin;
	double sellMargin;
	
	double credibleInterval = .5;
	
	
	public PredictionMarketAgent(String host, int port, String name) throws AgentCreationException {
		super(host, port, name);
	}

	@Override
	public void onMarketStart() {
		this.numDecoys = this.getNumDecoys();
		this.gotHeads = this.getCoin();
		
		this.trueValue = getTrueValueOneAgent(this.numDecoys, this.gotHeads);
		// TODO anything you want to compute before trading begins
		
		generatePossibleValuesForMargins();
	}
	
	private double getTrueValueOneAgent(int n, boolean gotHeads) {
		double val = (n + 2.0)/(2.0*(n+1.0));	
		return gotHeads ? val : 1.0-val;
	}
	
	private void generatePossibleValuesForMargins() {
		
		// generate all possible results of coin flips
		List<boolean []> coinResults = IntStream.range(0, (int)Math.pow(2, 5))
		        .mapToObj(i -> new long[] { i })
		        .map(BitSet::valueOf)
		        .map(bs -> bitSetToArray(bs, 5))
		        .collect(Collectors.toList());
		 
		
		List<Integer> decoys = Arrays.asList(1,1,2,2,3,3);
		// remove a single occurance of the number of decoys we recieved 
		decoys.remove(2*(this.numDecoys-1));
		
		ArrayList<Double> vals = coinResults.stream()
				.map(coin -> computeTrueVal(decoys,coin))
				.collect(Collectors.toCollection(ArrayList::new));
				
		Collections.sort(vals);
		int captured_vals = (int) (this.credibleInterval*vals.size());
		
		double min_range = vals.get(vals.size()-1) - vals.get(0);
		
		for (int i = captured_vals; i < vals.size(); i++) {
			double range = vals.get(i) - vals.get(i-captured_vals);
			if (range < min_range) {
				min_range = range;
				this.buyMargin = this.trueValue - vals.get(i-captured_vals);
				this.sellMargin = vals.get(i) - this.trueValue;
			}
		}
		   
	}

	private Double computeTrueVal(List<Integer> decoys, boolean[] coin) {
		Double givenH = this.trueValue;
		Double givenT = 1.0-this.trueValue;
		
		Iterator<Integer> it = decoys.iterator();
		for (int i = 0; i < 5; i++) {
			Integer n = it.next();
			double val = getTrueValueOneAgent(n,coin[i]);
			givenH = givenH*val;
			givenT = givenT*(1.0-val);
		}
		return givenH/(givenH + givenT);
	}	

	private boolean[] bitSetToArray(BitSet bs, int length) {
		boolean[] result = new boolean[length];
		bs.stream().forEach(i -> result[i] = true);
		return result;
	}


	@Override
	public void onMarketRequest(CallMarketChannel channel) {
		// TODO decide if you want to bid/offer or not
	}

	@Override
	public void onTransaction(int quantity, double price) {
		// TODO anything your bot should do after a trade it's involved
		// in is completed
	}

	@Override
	public double getHighestBuy() {
		// TODO upper bound you would buy at
		return 0;
	}

	@Override
	public double getLowestSell() {
		// TODO lower bound they
		return 0;
	}

	public static void main(String[] args) throws AgentCreationException {
		new PredictionMarketAgent("localhost", 2121, "bot name goes here"); // TODO: name your bot
		while (true) {
		}
	}
}

