package webincome.automation.workers.byteverts;

import java.util.List;

import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webincome.automation.workers.common.CommonWorker;
import webincome.automation.workers.common.Config;

public class BytevertsWorker extends CommonWorker {
	final static Logger LOG = LoggerFactory.getLogger(BytevertsWorker.class);
	public static class Properties {
		public static final String URL = "URL";
		public static final String USER = "USER";		
		public static final String EMAIL = "EMAIL";		
		public static final String PASSWORD = "PASSWORD";		
	}

	private String username = "";
	private String password = "";
	private String email = "";	
	private String url = "";
	
	public BytevertsWorker(Config inConfig) {
		super(inConfig);
		url = inConfig.getProperty(Properties.URL);
		username = inConfig.getProperty(Properties.USER);
		password = inConfig.getProperty(Properties.PASSWORD);
		email = inConfig.getProperty(Properties.EMAIL);
	}
	
	private void needRegister() {
		initChromeDriver(false);
		try {
			driver.get(url);
			WebElement create = untilOneClickableByCss(".button_create");
			takeNextScreenShot();
			create.click();
			// TODO : user action needed (or recaptcha solver), let's wait for now...
			long currentIteration = 0;
			do {
				try {
					WebElement register_button = untilClickableById("register_button");
					WebElement register_username = untilClickableById("register_username");
					WebElement register_email = untilClickableById("register_email");
					WebElement register_password = untilClickableById("register_password");
					WebElement register_repassword = untilClickableById("register_repassword");
					WebElement register_gender = untilClickableById("register_gender");
					WebElement register_question = untilClickableById("register_question");
					WebElement register_answer = untilClickableById("register_answer");
					
					register_username.sendKeys(username);
					register_email.sendKeys(email);
					register_password.sendKeys(password);
					register_repassword.sendKeys(password);
					register_gender.sendKeys(Keys.DOWN);
					register_question.sendKeys(Keys.DOWN);
					register_question.sendKeys(Keys.DOWN);
					register_question.sendKeys(Keys.DOWN);
					register_answer.sendKeys(username + " byteverts");
					takeNextScreenShot();
					
					randomSleep();
					register_button.click();
					takeNextScreenShot();
					sleep(10000);
					break;
				} catch (NoSuchElementException e) {
				} catch (Exception ex) {
					LOG.error("Got error", ex);
				}
				currentIteration++;
			} while (currentIteration < 10);
			
		} finally {
			driver.quit();
		}
	}
	
	public Boolean doWork() {
		Boolean workEnded = false;
		initPhantomJSDriver();
		try {
			driver.get(url);
			takeNextScreenShot();
			WebElement button_login = untilOneClickableByCss(".button_login");
			randomSleep();
			button_login.click();
			WebElement userTextBox = untilClickableById("login_user");
			WebElement passwordTextBox = untilClickableById("login_password");
			WebElement loginButton = untilClickableById("login_button");
			takeNextScreenShot();
			userTextBox.sendKeys(username);
			passwordTextBox.sendKeys(password);
			randomSleep();
			loginButton.click();
			takeNextScreenShot();
			try {
				WebElement errorText = untilOneClickableByCss(".container_login_msg", 30);
				takeNextScreenShot();
				if ("The username does not exists.".equalsIgnoreCase(errorText.getText())) {
					WebDriver oldDriver = driver;
					needRegister();
					Boolean result = doWork();
					driver = oldDriver; 
					return result;
				}
			} catch (NoSuchElementException e) {
				LOG.error("NoSuchElementException", e);
			} catch (Throwable e) {
				LOG.error("Throwable", e);
			}

			List<WebElement> buttons = untilClickableByCss(".button_neut");
			takeNextScreenShot();
			boolean clicked = false;
			for (WebElement button : buttons) {
				if (button.getText().equalsIgnoreCase("Bonus Ad Points")) {
					clicked = true;
					randomSleep();
					button.click();
					break;
				}
			}
			
			if (!clicked) {
				LOG.error("Not found button to click");
				throw new Exception("No button_neut to click");
			}

			do {
				try {
					buttons = untilClickableByCss("button.container_baps_button");
				} catch (TimeoutException exc) {
					takeNextScreenShot();
					break;
				}
				takeNextScreenShot();
				if (buttons.size() > 0) {
					randomSleep();
					buttons.get(0).click();
					takeNextScreenShot();
				} else {
					takeNextScreenShot();
					break;
				}
				WebElement button = untilOneClickableByCss(".container_paidadsecond_button");
				takeNextScreenShot();
				randomSleep();
				button.click();
				sleep(30000);
				takeNextScreenShot();
				driver.switchTo().defaultContent();
				button = untilClickableById("button_confirmad");
				takeNextScreenShot();
				randomSleep();
				button.click();
			} while (true);
			
			driver.get(url);
			buttons = untilClickableByCss(".button_neut");
			takeNextScreenShot();
			clicked = false;
			for (WebElement button : buttons) {
				if (button.getText().equalsIgnoreCase("Paid Ads")) {
					clicked = true;
					randomSleep();
					button.click();
					break;
				}
			}
			
			if (!clicked) {
				LOG.error("Not found button to click");
				throw new Exception("No button_neut to click");
			}
			
			do {
				try {
					buttons = untilClickableByCss("button.container_cashout_button");
				} catch (TimeoutException exc) {
					workEnded = true;
					takeNextScreenShot();
					break;
				}
				takeNextScreenShot();
				if (buttons.size() > 0) {
					randomSleep();
					buttons.get(0).click();
					takeNextScreenShot();
				} else {
					workEnded = true;
					takeNextScreenShot();
					break;
				}
				WebElement button = untilOneClickableByCss(".container_paidadsecond_button");
				takeNextScreenShot();
				randomSleep();
				button.click();
				sleep(30000);
				takeNextScreenShot();
				driver.switchTo().defaultContent();
				button = untilClickableById("button_confirmad");
				takeNextScreenShot();
				randomSleep();
				button.click();
			} while (true);
			
		} catch (Exception e) {
			takeScreenShot(String.format("%s\\error_%d_%d.png", screenShotDirectory, Thread.currentThread().getId(), screenShotNumber));
			saveHtml();
			LOG.error("Got exception: {}", e);
		} finally {
			driver.quit();
		}
		
		return workEnded;
	}
}
