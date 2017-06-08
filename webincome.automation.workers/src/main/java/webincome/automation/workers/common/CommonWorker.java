package webincome.automation.workers.common;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webincome.automation.workers.bitcoin.faucets.tobitcoin.BitCoinEarningsHQWorker;

public abstract class CommonWorker {
	final static Logger LOG = LoggerFactory.getLogger(BitCoinEarningsHQWorker.class);
	public static class Properties {
		public static final String PROXY = "PROXY";
	}

	protected Config config;
	protected WebDriver driver = null;
	protected int screenShotNumber = 0;
	protected String screenShotDirectory = "c:\\tmp\\";
	protected Random rand = new Random();
	private String _proxy = null;
	
	
	protected void saveHtml(String filename) {
		String source = driver.getPageSource();
		try {
			FileUtils.write(new File(filename), source, Charset.defaultCharset());
		} catch (IOException e) {
			LOG.error("IOException: {}", e);
		}
	}
	
	protected void saveHtml() {
		saveHtml(String.format("%s\\html_%d_%d.html", screenShotDirectory, Thread.currentThread().getId(), screenShotNumber));
	}
	
	protected void takeScreenShot(String filename) {
		File file = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(file, new File(filename));			
		} catch (IOException e) {
			LOG.error("IOException: {}", e);
		}		
	}
	
	protected void takeNextScreenShot() {
		String filename = String.format("%s\\screen_%d_%d.png", screenShotDirectory, Thread.currentThread().getId(), screenShotNumber);
		screenShotNumber++;
		takeScreenShot(filename);		
	}
	
	protected void sleep(long milliseconds) {
		try {
			Thread.sleep(milliseconds);
		} catch (InterruptedException e) {
			LOG.error("Sleep interrupted: {}", e);
		}
	}
	
	private void initWait() {
		wait = new WebDriverWait(driver, 60);
	}
	
	protected void initPhantomJSDriver() {
		LoggingPreferences logs = new LoggingPreferences();
		logs.enable(LogType.BROWSER, Level.OFF);
        logs.enable(LogType.SERVER, Level.OFF);
        logs.enable(LogType.DRIVER, Level.OFF);
        logs.enable(LogType.PROFILER, Level.OFF);
        logs.enable(LogType.CLIENT, Level.OFF);

		ArrayList<String> args = new ArrayList<String>(Arrays.asList(new String[] {"--web-security=no", "--ignore-ssl-errors=yes", "--ssl-protocol=tlsv1", "--debug=false"}));
		_proxy = config.getProperty(Properties.PROXY);
		if (_proxy != null) {
			args.add(String.format("--proxy=%s", _proxy));
			args.add("--proxy-type=http");
		}		
		DesiredCapabilities desiredCapabilities = DesiredCapabilities.phantomjs();
		desiredCapabilities.setCapability("phantomjs.page.settings.userAgent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"); // TODO : in connfig
		desiredCapabilities.setCapability("phantomjs.page.settings.webSecurityEnabled", false);
		desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS, new String[] { "--logLevel=DEBUG" });
		desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, args.toArray(new String[0]));
		desiredCapabilities.setCapability(CapabilityType.LOGGING_PREFS, logs);
		driver = new PhantomJSDriver(desiredCapabilities);
		driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
		driver.manage().window().setSize(new Dimension(1920, 1080));
		initWait();
	}
	
	protected void initChromeDriver() {
		initChromeDriver(true);
	}
	protected void initChromeDriver(boolean headless) {
		String proxy = config.getProperty(Properties.PROXY);
		DesiredCapabilities desiredCapabilities = DesiredCapabilities.chrome();
		ChromeOptions opts = new ChromeOptions();
		if (proxy != null) {
			opts.addArguments(String.format("proxy-server=%s", proxy));
		} 
		if (headless) {
			opts.addArguments("headless", "disable-gpu");
		}
		desiredCapabilities.setCapability(ChromeOptions.CAPABILITY, opts);
		driver = new ChromeDriver(desiredCapabilities); 
		initWait();
	}
	
	protected static final String JS_MOUSE_OVER_SCRIPT = "if(document.createEvent){var evObj = document.createEvent('MouseEvents');evObj.initEvent('mouseover', "
			+ "true, false); arguments[0].dispatchEvent(evObj);} else if(document.createEventObject) "
			+ "{ arguments[0].fireEvent('onmouseover');}";
	
	protected static final String JS_RIGHTCLICK_SCRIPT = "if(document.createEvent){ "
			+ "var element = arguments[0]; "
			+ "var event = document.createEvent('HTMLEvents'); "
			+ "event.initEvent('contextmenu', true, false); "
			+ "element.dispatchEvent(event);"
			+ "}";

	protected static final String JS_CLICK_SCRIPT =  "if(document.createEvent){ "
			+ "var a = arguments[0]; "
			+ "var e = document.createEvent('MouseEvents');"
			+ "e.initMouseEvent('click', true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null); "
			+ "a.dispatchEvent(e);"
			+ "}";
	
	private CommonWorker() {}
	
	public CommonWorker(Config inConfig) {
		config = inConfig;
	}
	
	public abstract Boolean doWork();
	
	protected WebDriverWait wait = null;

	protected WebElement untilClickableById(String id) {
		By by = By.id(id);
		wait.until(ExpectedConditions.elementToBeClickable(by));
		return driver.findElement(by);
	}

	protected List<WebElement> untilClickableByCss(String css) {
		By by = By.cssSelector(css);
		wait.until(ExpectedConditions.elementToBeClickable(by));
		return driver.findElements(by);
	}
	
	protected WebElement untilOneClickableByCss(String css) {
		By by = By.cssSelector(css);
		wait.until(ExpectedConditions.elementToBeClickable(by));
		return driver.findElement(by);
	}
	
	protected WebElement untilOneClickableByCss(String css, int seconds) {
		WebDriverWait wait1 = new WebDriverWait(driver, seconds);
		By by = By.cssSelector(css);
		wait1.until(ExpectedConditions.elementToBeClickable(by));
		return driver.findElement(by);
	}
	
	protected void randomSleep(int min, int max) {
		int randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
		sleep(randomNum);	
	}

	protected void randomSleep() {
		randomSleep(1000, 2500);
	}
}
