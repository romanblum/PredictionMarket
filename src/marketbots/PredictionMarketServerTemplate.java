package marketbots;

import brown.server.library.PredictionMarketServer;

public class PredictionMarketServerTemplate {
	private static int seconds = 30;
	private static int nSims = 1;
	private static int init_delay = 5;
	private static int lag = 25;
	
	public static void main(String[] args) throws InterruptedException {
		PredictionMarketServer server = new PredictionMarketServer(seconds, nSims, init_delay, lag);
		server.runAll();
	}
}