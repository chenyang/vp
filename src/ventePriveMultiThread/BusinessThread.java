package ventePriveMultiThread;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BusinessThread implements Runnable{

	private String markCode;
	private String productTypeCatgory;
	private Logger logger;

	public BusinessThread(String markCode, String productTypeCatgory){
		logger = LoggerFactory.getLogger(BusinessThread.class);
		this.markCode = markCode;
		this.productTypeCatgory = productTypeCatgory;
	};

	@Override
	public void run() {
		try {
			startBusiness();
		} catch (IOException | InterruptedException | ExecutionException e) {
			logger.error("FATAL ERROR in calling analyzePageCaterogiesElements", e);
		}
	}
	
	private void startBusiness() throws IOException, InterruptedException, ExecutionException{

		/**  Analysis page and buy items*/
		analyzePageCaterogiesElements(markCode, productTypeCatgory);

		/** stuck VentePrive timer **/
		//while(true){
			//stuckTimer();
		//}
	}

	private void analyzePageCaterogiesElements(String markCode, String productTypeCatgory) throws IOException, InterruptedException, ExecutionException{
		String visitedUrl = "http://fr.vente-privee.com/catalog/"+productTypeCatgory+"/Operation/"+markCode+"/site/1";
		logger.info("--Analyzing url: "+visitedUrl+" ..");
		Document typeOneMainPage = Jsoup.connect(visitedUrl).get();
		//Mark's category page
		Elements lists = typeOneMainPage.select(".menuEV_Container >li");

		ExecutorService executor = Executors.newFixedThreadPool(SingletonShare.THREADPOOL_FOR_CATEGORY);
		Collection<Future<?>> futures = new LinkedList<Future<?>>();
		for(Element el : lists){
			Runnable worker= new CategoryUrlCriblerThread(el, markCode);
			futures.add(executor.submit(worker));
		}
		//halt execution until the ExecutorService has processed all of the Runnable tasks
		for (Future<?> future:futures) {
			future.get();
		}
		executor.shutdown();
		executor.awaitTermination(60, TimeUnit.SECONDS);
		//finally
		//out.println("CategoryUrlCriblerThread ends");
	}

	/**
	 * Mechanism to maker the timer suck..
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws ExecutionException 
	 * @throws Exception 
	 */	
	private void stuckTimer() throws InterruptedException, IOException, ExecutionException{
		logger.info("--Staring stuck timer..");
		Connection connection;
		/** login again **/
		SingletonShare.getInstance().loginPhase();

		if(!SingletonShare.getInstance().getBoughtItems().isEmpty()){
			MapperPidPfid mapper = SingletonShare.getInstance().getBoughtItems().peek();
			//increase 1 item
			logger.debug("Waiting for "+(SingletonShare.SLEEP_STUCKER/1000)+" seds to increase item to keep alive session..");
			Thread.sleep(SingletonShare.SLEEP_STUCKER);
			logger.info("Increasing item: pid:"+mapper.getPid()+", pfid:"+mapper.getPfid()+", markcode:"+mapper.getMarkCode());
			connection = Jsoup.connect(SingletonShare.urlIncreseItem);
			for (Entry<String, String> cookie : SingletonShare.getInstance().getLoginCookies().entrySet()) {
				connection.cookie(cookie.getKey(), cookie.getValue());
			} 
			connection
			.data("productId", mapper.getPid())
			.data("operationId", mapper.getMarkCode()) //mark id
			.data("productFamilyId", mapper.getPfid())
			.ignoreContentType(true)
			.post();

			//then decrease 1 item in order not to bother other clients..
			logger.debug("  Waiting for "+(SingletonShare.SLEEP_STUCKER/1000)+" seds to decrease item to keep session alive..");
			Thread.sleep(SingletonShare.SLEEP_STUCKER);
			logger.info("  Decreasing item: pid:"+mapper.getPid()+", pfid:"+mapper.getPfid()+", markcode:"+mapper.getMarkCode());
			connection = Jsoup.connect(SingletonShare.urlDecreaseItem);
			for (Entry<String, String> cookie : SingletonShare.getInstance().getLoginCookies().entrySet()) {
				connection.cookie(cookie.getKey(), cookie.getValue());
			}
			connection
			.data("productId", mapper.getPid())
			.data("operationId", mapper.getMarkCode()) //mark id
			.data("productFamilyId", mapper.getPfid())
			.ignoreContentType(true)
			.post();
		}else{
			logger.warn("--BUSINESS ERROR NO ITEMS CAN BE FOUND, will wait for "+(SingletonShare.SLEEP_ERROR/1000)+" seds and restart..");
			Thread.sleep(SingletonShare.SLEEP_ERROR);
			//restartBusiness until success
			startBusiness();
		}	
	}
	
}
