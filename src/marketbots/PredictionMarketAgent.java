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
	double trueValueOrig;
	int numDecoys;
	boolean gotHeads;
	double exp_eps = 0;
	
	int exposure = 0;
	
	double margin = 2;
	
	int rd = 0;
	long prevTimeStamp;
	
	public PredictionMarketAgent(String host, int port, String name) throws AgentCreationException {
		super(host, port, name);
	}

	@Override
	public void onMarketStart() {
		System.err.println("STARTING");
		this.numDecoys = this.getNumDecoys();
		this.gotHeads = this.getCoin();
		this.prevTimeStamp = System.currentTimeMillis();
		
		// compute our initial true value
		this.trueValue = getTrueValueOneAgent(this.numDecoys, this.gotHeads);
		this.trueValueOrig = this.trueValue;
	}
	
	private double getTrueValueOneAgent(int n, boolean gotHeads) {
		double val = (n + 2.0)/(2.0*(n+1.0));	
		return gotHeads ? val : 1.0-val;
	}

	@Override
	public void onMarketRequest(CallMarketChannel channel) {
		
		OrderBook o = this.getOrderBook();
		List<Transaction> T = this.getLedger();
		
		long time = System.currentTimeMillis();
		double pH = this.trueValue;
		double pT = 1.0 - this.trueValue;
		
		if (rd == 1) {
			Iterator<BuyOrder> itb = o.getBuys().iterator();
			while (itb.hasNext()) {
				BuyOrder b = itb.next();
				pH = pH * ((b.price - this.exp_eps)/100.0);
				pT = pT * (1.0 - (b.price - this.exp_eps)/100.0);
			}
			
			Iterator<SellOrder> its = o.getSells().iterator();
			while (its.hasNext()) {
				SellOrder s = its.next();
				pH = pH * ((s.price + this.exp_eps)/100.0);
				pT = pT * (1.0 - (s.price + this.exp_eps)/100.0);
			}
			
		}
		// compute updated true values based off order book and ledger
		
		for (Transaction t : T) {
			long transTime = t.TIMESTAMP;
			// transaction happened this round:
			
			if (this.prevTimeStamp < transTime) {
				if (t.FROM != null && t.TO != null) {
					pH = pH * ((t.PRICE+this.exp_eps)/100.0)* ((t.PRICE-this.exp_eps)/100.0);
					pT = pT * (1.0 - ((t.PRICE+this.exp_eps)/100.0))* (1.0 - (t.PRICE-this.exp_eps)/100.0);
				}
			}
		}		
		
		double newTrueValue = (pH)/(pH+pT);
		
		if (rd > 1 && (newTrueValue - this.trueValue) > .2) {
			// too big of a leap after a couple rounds
			this.trueValue = this.trueValueOrig;
		} else {
			this.trueValue = (pH)/(pH + pT);
		}
		
		// compare to previous trueValue? 
		
		this.prevTimeStamp = time;
		
		Iterator<BuyOrder> itb = o.getBuys().iterator();
		while (itb.hasNext()) {
			BuyOrder b = itb.next();
			if (b.price > this.getLowestSell()) {
				this.sell(b.price, 1, channel);
				// do we sell all? should we be worried for tomorrow's lab
			}
		}
		
		Iterator<SellOrder> its = o.getSells().iterator();
		while (its.hasNext()) {
			SellOrder s = its.next();
			if (s.price < this.getHighestBuy()) {
				this.buy(s.price, 1, channel);
				// do we sell all? should we be worried for tomorrow's lab
			}
		}
		
		rd++;
	}
	
	private void initializeExpEps() {
		OrderBook o = this.getOrderBook();
		double buy = 0;
		int buyCt = 0;
		Iterator<BuyOrder> itb = o.getBuys().iterator();
		while (itb.hasNext()) {
			BuyOrder b = itb.next();
			buy += b.price;
			buyCt++;
		}
		buy = buy/buyCt;
		
		double sell = 0;
		int sellCt = 0;
		Iterator<SellOrder> its = o.getSells().iterator();
		while (its.hasNext()) {
			SellOrder s = its.next();
			sell += s.price;
			sellCt++;
		}			
		sell = sell/sellCt;
		
		if (buyCt >= 2 && sellCt >= 2) {
			this.exp_eps = buy-sell;
		}
	}

	@Override
	public void onTransaction(int quantity, double price) {
		if (price < 0) {
			exposure -= quantity;
		} else {
			exposure += quantity;
		}
		
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
		new UpdateAgent("localhost", 2121, "agent1");
		new UpdateAgent("localhost", 2121, "agent2");
		new UpdateAgent("localhost", 2121, "agent3");
		new UpdateAgent("localhost", 2121, "agent4");
		new UpdateAgent("localhost", 2121, "agent5");
		while (true) {
			
		}
	}
}

