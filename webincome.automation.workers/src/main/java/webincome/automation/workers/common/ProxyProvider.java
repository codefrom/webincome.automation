package webincome.automation.workers.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyProvider extends CommonWorker {
	final static Logger LOG = LoggerFactory.getLogger(ProxyProvider.class);

	private List<String> proxies = null;
	private int currentProxy = 0;
	private int currentPage = 0;
	private String myExternalIp = "";
	
	private void getMyIP() {
		try {
			URL whatismyip = new URL("https://icanhazip.com/");
			BufferedReader in = new BufferedReader(new InputStreamReader(
			                whatismyip.openStream()));
			myExternalIp = in.readLine();
			LOG.info("My real ip: {}", myExternalIp);
			
		} catch (Exception e) {
			LOG.error("Got error", e);
		}
	}
	
	public ProxyProvider(Config cfg) {
		super(cfg);
		getMyIP();
	}
	
	private boolean testProxyPhantom(String proxy) {
		LOG.info("Testing proxy {}", proxy);
		config.setProperty(Properties.PROXY, proxy);
		initPhantomJSDriver();
		wait = new WebDriverWait(driver, 10);
		try {
			driver.get("https://whoer.net/ru"); // TODO : any other https ip checker will do
			WebElement myIp = untilOneClickableByCss(".your-ip");
			takeNextScreenShot();
			String myNewIp = myIp.getText();
			if (myNewIp.contains(myExternalIp)) {
				LOG.info("Proxy {} test fail", proxy);
				return false;
			} else {
				LOG.info("Proxy {} test success, new ip: {}", proxy, myNewIp);
				return true;
			}
		} catch (Exception e) {
			takeNextScreenShot();
			LOG.info("Proxy {} error", proxy, e);
			return false;
		} finally {
			driver.quit();
			config.setProperty(Properties.PROXY, null);			
		}		
	}

	private boolean testProxyJava(String proxy) {
		try {
			LOG.info("Testing proxy {}", proxy);
			String url = "https://icanhazip.com/"; // TODO : any other https ip checker will do
			String[] pparts = proxy.split(":");
			URL server = new URL(url);
			Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(pparts[0], Integer.parseInt(pparts[1])));
			java.util.Properties systemProperties = System.getProperties();
			systemProperties.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36");
			HttpsURLConnection connection = (HttpsURLConnection)server.openConnection(p);
			connection.connect();
			InputStream in = connection.getInputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			String s = r.readLine();
			while (s != null) {
			    sb.append(s);
			    s = r.readLine();
			}
			r.close();
			if (sb.toString().contains(myExternalIp)) {
				LOG.info("Proxy {} test fail", proxy);
				return false;
			} else {
				LOG.info("Proxy {} test success", proxy);
				return true;
			}
		} catch (Exception e) {
			LOG.info("Proxy {} error {}", proxy, e);
			return false;
		}
		
	}
	
	public String getNextCheckedProxy() {
		do {
			if (currentProxy >= proxies.size()) {
				doWork();
				currentProxy = 0;
			}

			String proxy = proxies.get(currentProxy);
			currentProxy++;
			
			// TODO : chose test based on config
			//if (testProxyJava(proxy))
			if (testProxyPhantom(proxy))
				return proxy;
		} while (true);		
	}
	
	public void init(List<String> inProxies) {
		proxies = inProxies;
	}	
	
	@Override
	public Boolean doWork() {
		initPhantomJSDriver();
		try {
			if (currentPage > 0) {
				driver.get(String.format("https://hidemy.name/ru/proxy-list/?type=s&anon=4&start=%d#list", currentPage * 64));
			} else {
				driver.get("https://hidemy.name/ru/proxy-list/?type=s&anon=4#list");
			}
			currentPage++;
			List<WebElement> proxies = untilClickableByCss(".proxy__t > tbody > tr > td:nth-child(1)");
			List<WebElement> ports = untilClickableByCss(".proxy__t > tbody > tr > td:nth-child(2)");
			takeNextScreenShot();
			if (proxies.size() == ports.size()) {
				ArrayList<String> proxieStrings = new ArrayList<String>(); 
				for (int i = 0; i < proxies.size(); i++) {
					String address = proxies.get(i).getText();
					String port = ports.get(i).getText();
					proxieStrings.add(String.format("%s:%s", address, port));
				}
				init(proxieStrings);
			}
		} catch (Throwable e) {
			LOG.error("Throwable", e);
		} finally {
			driver.quit();
		}
		return true;
	}
	
}
