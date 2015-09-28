package ventePriveMultiThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArticleUrlCriblerThread implements Runnable{

	private Element artEl;
	private String markCode;
	private Logger logger;

	public ArticleUrlCriblerThread(Element el, String markCode){
		logger = LoggerFactory.getLogger(ArticleUrlCriblerThread.class);
		this.artEl = el;
		this.markCode = markCode;
	}

	private void analyzePageExpressElements() throws IOException, InterruptedException, ExecutionException{
		String href4Express = artEl.select(".infoExpressBt").attr("hrefExpress");
		String[] temps = href4Express.split("/");
		String pfid = temps[temps.length-1]; // get product family id
		logger.debug(href4Express+", "+pfid);

		//this block take some time..
		//element express page
		Document typeOneExpressPage = Jsoup.connect("http://fr.vente-privee.com"+href4Express).get();
		List<MapperPidPfid> listMapper = new ArrayList<MapperPidPfid>();
		Elements models = typeOneExpressPage.select("#model >option");
		if(!models.isEmpty()){ //if multi-model product:
			for(Element model:models){
				String pid = model.attr("value");
				MapperPidPfid mapper = new MapperPidPfid();
				mapper.setPid(pid);
				mapper.setPfid(pfid);
				listMapper.add(mapper);
			}
		}else{//if no models to choose.. if single product..
			pfid = typeOneExpressPage.select("#singleProduct").attr("productfamilyid");
			String pid= typeOneExpressPage.select("#singleProduct").attr("productid");
			MapperPidPfid mapper = new MapperPidPfid();
			mapper.setPid(pid);
			mapper.setPfid(pfid);
			listMapper.add(mapper);
		}

		//add to panier
		ExecutorService executor = Executors.newFixedThreadPool(SingletonShare.THREADPOOL_FOR_CATEGORY);
		Collection<Future<?>> futures = new LinkedList<Future<?>>();
		for(MapperPidPfid mapper: listMapper){
			mapper.setMarkCode(markCode);
			String pid = mapper.getPid();
			pfid = mapper.getPfid();

			Runnable worker= new CallBuyItemThread(markCode, pid, pfid, mapper);
			futures.add(executor.submit(worker));	
		}
		//halt execution until the ExecutorService has processed all of the Runnable tasks
		for (Future<?> future:futures) {
			future.get();
		}
		executor.shutdown();
		executor.awaitTermination(60, TimeUnit.SECONDS);

	}



	@Override
	public void run() {
		try {
			analyzePageExpressElements();
		} catch (IOException | InterruptedException| ExecutionException e) {
			logger.error("FATAL ERROR in calling analyzePageExpressElements", e.toString());
		} 
	}

}
