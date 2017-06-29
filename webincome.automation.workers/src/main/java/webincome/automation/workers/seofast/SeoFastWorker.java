package webincome.automation.workers.seofast;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import webincome.automation.workers.byteverts.BytevertsWorker;
import webincome.automation.workers.byteverts.BytevertsWorker.Properties;
import webincome.automation.workers.common.CaptchaSolver;
import webincome.automation.workers.common.CommonWorker;
import webincome.automation.workers.common.Config;

public class SeoFastWorker extends CommonWorker {
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

	public SeoFastWorker(Config inConfig) {
		super(inConfig);
		url = inConfig.getProperty(Properties.URL);
		username = inConfig.getProperty(Properties.USER);
		password = inConfig.getProperty(Properties.PASSWORD);
		email = inConfig.getProperty(Properties.EMAIL);
	}

	@Override
	public Boolean doWork() {
		Boolean workEnded = false;
		initChromeDriver(false);
		try {
			driver.get(url);
			takeNextScreenShot();
			WebElement button_login = untilOneClickableByCss("td.block:nth-child(3) > a:nth-child(2)");
			
			randomSleep();
			button_login.click();
			WebElement logusername = untilClickableById("logusername");
			WebElement logpassword = untilClickableById("logpassword");
			logusername.sendKeys(email);
			logpassword.sendKeys(password);			
			WebElement button_serfing = untilOneClickableByCss("#m_bl2 > span:nth-child(1) > a:nth-child(1)");
			takeNextScreenShot();
			randomSleep();
			button_serfing.click();
			
			untilClickableById("reit");
			takeNextScreenShot();
			randomSleep();
			String winHandleBefore = driver.getWindowHandle();
			List<WebElement> links = findByCss(".surf_ckick");
			for (WebElement link : links) {
				if (!link.isDisplayed())
					continue;
				if (!link.getAttribute("onclick").contains("start_surfing"))
					continue;
				randomSleep();
				link.click();
				sleep(5000);
				for(String winHandle : driver.getWindowHandles()){
					if (winHandle.equalsIgnoreCase(winHandleBefore))
						continue;
				    driver.switchTo().window(winHandle);
				    break;
				}
				driver.switchTo().defaultContent();
				WebElement captcha_mat_s = untilOneClickableByCss(".capcha_mat_s", 300);
				BufferedImage captchaImage = getElementScreenshot(captcha_mat_s);
				captchaImage = captchaImage.getSubimage(5, 5, captchaImage.getWidth() - 10, captchaImage.getHeight() - 10);				
				String captcha = CaptchaSolver.solveSeoFastMath(captchaImage);
				LOG.info("Got captcha: " + captcha);
				HashMap<WebElement, String> captchaAnswers = new HashMap<WebElement, String>(); 
				List<WebElement> capcha_mat_s_b = untilClickableByCss(".capcha_mat_s_b");
				for (WebElement captcha_answer : capcha_mat_s_b) {
					BufferedImage captchaAnswerImage = getElementScreenshot(captcha_answer);
					captchaAnswerImage = captchaAnswerImage.getSubimage(5, 5, captchaAnswerImage.getWidth() - 10, captchaAnswerImage.getHeight() - 10);				
					String captchaAnswer = CaptchaSolver.solveSeoFastDigit(captchaAnswerImage);
					LOG.info("Found possile answer: {}", captchaAnswer);	
					captchaAnswers.put(captcha_answer, captchaAnswer);
				}
				// first - try and calc
				boolean founded = false;
				try {
					Integer i1 = Integer.parseInt(captcha.substring(0, 1));
					Integer i2 = Integer.parseInt(captcha.substring(2, 3));
					String sum = Integer.toString(i1+i2);
					for (Map.Entry<WebElement, String> captchaAnswer : captchaAnswers.entrySet()) {
						if (captchaAnswer.getValue().equalsIgnoreCase(sum)) {
							LOG.info("Captcha answer is {}", captchaAnswer.getValue());
							founded = true;
							randomSleep();
							captchaAnswer.getKey().click();
							break;
						}
					}
				} catch (Exception ex) {
					LOG.error("Exception", ex);			
				}
				
				if (!founded) {
					// lets try and guess
					// 1. answer could be only uniq
					HashMap<WebElement, String> clearedAnswers = new HashMap<WebElement, String>();
					for (Map.Entry<WebElement, String> captchaAnswer1 : captchaAnswers.entrySet()) {
						boolean uniq = true;
						for (Map.Entry<WebElement, String> captchaAnswer2 : captchaAnswers.entrySet()) {
							if (captchaAnswer2.equals(captchaAnswer2))
								continue;
							if (captchaAnswer2.getValue().equalsIgnoreCase(captchaAnswer1.getValue())) {
								uniq = false;
								break;
							}
						}
						if (uniq)
							clearedAnswers.put(captchaAnswer1.getKey(), captchaAnswer1.getValue());						
					}
					captchaAnswers = clearedAnswers;
					clearedAnswers = new HashMap<WebElement, String>();
					
					// 2. looks alike digits in captcha error
					ArrayList<String> possibleSums = new ArrayList<String>();
					HashMap<String, String> lookAlike = new HashMap<String, String>();
					lookAlike.put("+", "4");
					lookAlike.put("1", "7");
					lookAlike.put("4", "1");
					lookAlike.put("7", "1");
					
					for (Map.Entry<String, String> alike : lookAlike.entrySet()) {
						try {
							String subCaptcha = captcha.replace(alike.getKey(), alike.getValue());
							Integer i1 = Integer.parseInt(subCaptcha.substring(0, 1));
							Integer i2 = Integer.parseInt(subCaptcha.substring(2, 3));
							String sum = Integer.toString(i1+i2);
							possibleSums.add(sum);
						} catch (Exception e) {}
					}
					
					for (Map.Entry<WebElement, String> captchaAnswer : captchaAnswers.entrySet()) {
						for (String sum : possibleSums) {
							if (captchaAnswer.getValue().equalsIgnoreCase(sum)) {
								LOG.info("GUESSING Captcha answer is {}", captchaAnswer.getValue());
								founded = true;
								randomSleep();
								captchaAnswer.getKey().click();
								break;
							}
						}
						if (founded)
							break;
					}
					
					if (!founded) {
						for (Map.Entry<WebElement, String> captchaAnswer : captchaAnswers.entrySet()) {
							LOG.info("GUESSING Captcha answer is {}", captchaAnswer.getValue());
							founded = true;
							randomSleep();
							captchaAnswer.getKey().click();
							break;
						}
					}		
				}

				randomSleep(1000, 2000);
				driver.close();
				driver.switchTo().window(winHandleBefore);
			}
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
