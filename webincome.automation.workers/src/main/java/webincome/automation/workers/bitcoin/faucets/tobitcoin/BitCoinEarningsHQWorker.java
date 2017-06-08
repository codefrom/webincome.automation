package webincome.automation.workers.bitcoin.faucets.tobitcoin;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webincome.automation.workers.common.CommonWorker;
import webincome.automation.workers.common.Config;

public class BitCoinEarningsHQWorker extends CommonWorker {
	final static Logger LOG = LoggerFactory.getLogger(BitCoinEarningsHQWorker.class);

	public static class Properties {
		public static final String URL = "URL";
		public static final String WALLET = "WALLET";		
	}

	public BitCoinEarningsHQWorker(Config config)
	{
		super(config);
		initChromeDriver();
	}
	
	public Boolean doWork()
	{
		try {
			WebDriverWait wait = new WebDriverWait(driver, 30); // 30 seconds of timeout
			String url = config.getProperty(BitCoinEarningsHQWorker.Properties.URL);
			String wallet = config.getProperty(BitCoinEarningsHQWorker.Properties.WALLET);
			driver.get(url);
			wait.until(ExpectedConditions.elementToBeClickable(By.id("username")));
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("go_btn")));		
			takeNextScreenShot();
			driver.findElement(By.id("username")).sendKeys(wallet);
			WebElement button = driver.findElement(By.id("go_btn"));
			JavascriptExecutor js = (JavascriptExecutor)driver;
			js.executeScript(JS_CLICK_SCRIPT, button);
			wait.until(ExpectedConditions.elementToBeClickable(By.id("reflink")));
			takeNextScreenShot();
			button = driver.findElement(By.id("go_btn"));
			js.executeScript(JS_CLICK_SCRIPT, button);
			takeNextScreenShot();
			button = driver.findElement(By.cssSelector("a.btn-success"));
			js.executeScript(JS_CLICK_SCRIPT, button);
			takeNextScreenShot();
		} catch (Throwable e) { 
			LOG.error("Got error: {}", e);
		} finally {
			driver.quit();
		}
		return true;
	}
}
