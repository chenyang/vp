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

public class CategoryUrlCriblerThread implements Runnable{

	private Element el;
	private String markCode;
	private Logger logger;
	
	public CategoryUrlCriblerThread(Element el, String markCode){
		logger = LoggerFactory.getLogger(CategoryUrlCriblerThread.class);
		this.el = el;
		this.markCode = markCode;
	}

	private void analyzePageArticlesElements() throws InterruptedException, IOException, ExecutionException{
		String href4Article = el.select("a").attr("href");
		logger.debug(href4Article);
		Document typeOneArticlePage;
		typeOneArticlePage = Jsoup.connect("http://fr.vente-privee.com"+href4Article).get();
		//Category's element page
		Elements artList = typeOneArticlePage.select(".artList >li");
		if(!artList.isEmpty()){
			ExecutorService executor = Executors.newFixedThreadPool(SingletonShare.THREADPOOL_FOR_ARTICLE);
			Collection<Future<?>> futures = new LinkedList<Future<?>>();
			for(Element el : artList){
				Runnable worker= new ArticleUrlCriblerThread(el, this.markCode);
				futures.add(executor.submit(worker));
			}
			//halt execution until the ExecutorService has processed all of the Runnable tasks
			for (Future<?> future:futures) {
				future.get();
			}
			executor.shutdown();
			executor.awaitTermination(60, TimeUnit.SECONDS);
		}else{
			logger.warn("Article list page has nothing..");
		}
	}

	@Override
	public void run() {
		try {
			analyzePageArticlesElements();
		} catch (InterruptedException | IOException | ExecutionException e) {
			logger.error("FATAL ERROR in calling analyzePageArticlesElements", e);
		}
	}
}
