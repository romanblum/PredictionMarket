package marketbots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import brown.accounting.library.Transaction;
import brown.agent.AbsPredictionMarketAgent;
import brown.agent.library.FixedAgent;
import brown.agent.library.RandomAgent;
import brown.agent.library.UpdateAgent;
import brown.channels.library.CallMarketChannel;
import brown.exceptions.AgentCreationException;
import brown.market.marketstate.library.BuyOrder;
import brown.market.marketstate.library.OrderBook;
import brown.market.marketstate.library.SellOrder;

public class PredictionMarketAgent extends AbsPredictionMarketAgent {
	
	double trueValue;
	int numDecoys;
	boolean gotHeads;
	
	double margin = 2;
	
	int rd = 0;
	
	public PredictionMarketAgent(String host, int port, String name) throws AgentCreationException {
		super(host, port, name);
	}

	@Override
	public void onMarketStart() {
		this.numDecoys = this.getNumDecoys();
		this.gotHeads = this.getCoin();
		
		// compute our initial true value
		this.trueValue = getTrueValueOneAgent(this.numDecoys, this.gotHeads);

	}
	
	private double getTrueValueOneAgent(int n, boolean gotHeads) {
		double val = (n + 2.0)/(2.0*(n+1.0));	
		return gotHeads ? val : 1.0-val;
	}

	@Override
	public void onMarketRequest(CallMarketChannel channel) {
		if (rd >= 1) {
			// compute updated true values based off order book and ledger
			
			// compare to previous trueValue? 
		}
		
		OrderBook o = this.getOrderBook();
		Iterator<BuyOrder> itb = o.getBuys().iterator();
		while (itb.hasNext()) {
			BuyOrder b = itb.next();
			if (b.price > this.getLowestSell()) {
				// do we sell all? should we be worried for tomorrow's lab
			}
		}
		
		Iterator<SellOrder> its = o.getSells().iterator();
		while (its.hasNext()) {
			SellOrder s = its.next();
			if (s.price < this.getHighestBuy()) {
				// do we sell all? should we be worried for tomorrow's lab
			}
		}
		
		
		rd++;
	}

	@Override
	public void onTransaction(int quantity, double price) {
		// TODO anything your bot should do after a trade it's involved
		// in is completed
	}

	@Override
	public double getHighestBuy() {
		return 100*this.trueValue - this.margin;
		
	}

	@Override
	public double getLowestSell() {
		return 100*this.trueValue + this.margin;
	}

	public static void main(String[] args) throws AgentCreationException {
		new PredictionMarketAgent("localhost", 2121, "us");
		new RandomAgent("localhost", 2121, "agent1");
		new RandomAgent("localhost", 2121, "agent2");
		new RandomAgent("localhost", 2121, "agent3");
		new RandomAgent("localhost", 2121, "agent4");
		new RandomAgent("localhost", 2121, "agent5");
		while (true) {
			
		}
	}
}

