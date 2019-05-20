package com.katalon.plugin.keyword.calendar

import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.MatchResult;
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.By

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.checkpoint.Checkpoint
import com.kms.katalon.core.configuration.RunConfiguration
import com.kms.katalon.core.exception.StepFailedException
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.keyword.internal.WebUIKeywordMain
import com.kms.katalon.core.webui.common.WebUiCommonHelper
import com.kms.katalon.core.webui.driver.DriverFactory

import groovy.transform.CompileStatic
import internal.GlobalVariable

public class SetDateCalendarKeyword {
	//May 30 2019
	static final String DATE_PATTERN_1 = "(\\b\\d{1,2}\\D{0,3})?\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|(Nov|Dec)(?:ember)?)\\D?(\\d{1,2}\\D?)?\\D?((19[7-9]\\d|20\\d{2})|\\d{2})"
	//2019-05-31
	static final String DATE_PATTERN_2 = "([0-9]{4}[-/]?((0[13-9]|1[012])[-/]?(0[1-9]|[12][0-9]|30)|(0[13578]|1[02])[-/]?31|02[-/]?(0[1-9]|1[0-9]|2[0-8]))|([0-9]{2}(([2468][048]|[02468][48])|[13579][26])|([13579][26]|[02468][048]|0[0-9]|1[0-6])00)[-/]?02[-/]?29)"
	//<a class="myDay shadow" data-day="3" data-month="5" data-year="2019" onclick="">3</a>
	static final String DATE_PATTERN_3 = '(?<=day=")([0-9]{1}|1[0-9]{1}|2[0-9]{1}|3[0-1]{1})(?=")|(?<=month=")([0-9]{1}|1[0-2]{1})(?=")|(?<=year=")([0-9]{4})(?=")'
	
	String Used_Date_Pattern = "";

	@Keyword
	def setDate(TestObject to, int day, int month, int year,int slideTimeOut, FailureHandling flowControl) throws StepFailedException {
		WebUIKeywordMain.runKeyword( {
			boolean isSwitchIntoFrame = false
			try {
				if (to == null) {
					to = new TestObject("tempBody").addProperty("css", ConditionType.EQUALS, "body")
				}
				isSwitchIntoFrame = WebUiCommonHelper.switchToParentFrame(to)

				//convert the TestObject to WebElement
				WebElement calendar = WebUiCommonHelper.findWebElement(to, RunConfiguration.getTimeOut());

				//get current Day Month Year
				Calendar cal = Calendar.getInstance();
				int curMonth = cal.get(Calendar.MONTH) + 1;
				int curYear = cal.get(Calendar.YEAR);


				//get all child element in calendar object
				List<WebElement> allChildElement = calendar.findElements(By.xpath(".//*"))

				//TO-DO: find solution for filter the displayed element more exactly
				List<WebElement> displayedElements = filterTheElementsDisplayed(allChildElement);

				//get next and previous month button
				WebElement nextBtn = getNextMonthElement(displayedElements);
				WebElement prevBtn = getPreviousMonthElement(displayedElements);

				String firstDate = getFirstDateElementVisible(displayedElements);

				if (firstDate.length() == 0)
					WebUIKeywordMain.stepFailed("This calendar is not supported!", flowControl, null, true)

				JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver();

				curMonth = js.executeScript("return new Date('" + firstDate + "').getMonth() + 1") as Integer
				curYear =  js.executeScript("return new Date('" + firstDate + "').getFullYear()") as Integer

				//calculate move Month
				int moveMonth = (year - curYear - 1) * 12 + 12 + (month - curMonth);

				//moving the calendar
				if (moveMonth > 0){
					for (int i = 0; i < moveMonth; i++){
						nextBtn.click();
						Thread.sleep(slideTimeOut);
					}

				}
				else
					for (int i = 0; i < 0 - moveMonth; i++){
						prevBtn.click();
						Thread.sleep(slideTimeOut);
					}

				//calendar object is changed after move. we have update our element
				allChildElement = calendar.findElements(By.xpath(".//*"))
				displayedElements = filterTheElementsDisplayed(allChildElement)
				List<WebElement> dateObjects = getAllElementsHasDateValue(displayedElements)

				//we just need click to the object has text equal the input day

				List<WebElement> dayObjects = getDateElementsWithMonthValue(dateObjects, month);

				dayObjects = getDateElementsWithDayValue(dayObjects, day)

				if (dayObjects.size() == 1)
					dayObjects[0].click();

				if (dayObjects.size() == 0)
					WebUIKeywordMain.stepFailed("Cannot detect the date.", flowControl, null, true)

			}
			finally {
				if (isSwitchIntoFrame) {
					WebUiCommonHelper.switchToDefaultContent()
				}
			}
		},
		flowControl,
		true, // screenshot should be taken
		"Something wrong! Cannot set the calendar date." // error message in case of failure
		)
	}

	public String getElementTagAttribute(WebElement we){
		if (we == null)
			return ""
		String innerHTML = we.getAttribute("innerHTML");
		String outerHTML = we.getAttribute("outerHTML");
		return outerHTML.replace(">" + innerHTML + "<", "")
	}

	public WebElement getPreviousMonthElement(List<WebElement> listWE){
		for(WebElement we : listWE){
			if (getElementTagAttribute(we).toUpperCase().contains("PREV"))
				return we
		}
		return null
	}

	public WebElement getNextMonthElement(List<WebElement> listWE){
		for(WebElement we : listWE){
			if (getElementTagAttribute(we).toUpperCase().contains("NEXT"))
				return we
		}
		return null
	}

	public String getDateFormatOfElement(WebElement we){

		if (Used_Date_Pattern.length() ==0){
			List<String> regExPatterns = new ArrayList<String>();
			//add all date pattern we have
			regExPatterns.add(DATE_PATTERN_1);
			regExPatterns.add(DATE_PATTERN_2);
			regExPatterns.add(DATE_PATTERN_3);
	
			for(String regEx : regExPatterns){
				def date = getDateWithPattern(we, regEx);			
				if (date.length() > 0)
					return date;
			}
	
			return "";
		}
		else
			return getDateWithPattern(we, Used_Date_Pattern);
	}
	
	public String getDateWithPattern(WebElement we, String datePattern){
		
		Pattern pattern = Pattern.compile(datePattern);
		Matcher matcher = pattern.matcher(getElementTagAttribute(we));

		List<String> matchValue = new ArrayList<String>();
		while (matcher.find()){
			matchValue.add((String) matcher.group())
		}
		
		//saving the pattern to use latter.
		if (matchValue.size > 0 && Used_Date_Pattern.length() == 0)
			Used_Date_Pattern = datePattern;
		
		if (matchValue.size() == 1)
			return matchValue[0];
		if (matchValue.size() == 3 & datePattern.equals(DATE_PATTERN_3))
			return matchValue[1] + "-" + matchValue[0] + "-"+ matchValue[2];
		
		return "";
		
	}

	public List<WebElement> getAllElementsHasDateValue(List<WebElement> listWE){

		List<WebElement> tmpList = new ArrayList<WebElement>();
		for(WebElement we : listWE){
			def d1 = getElementTagAttribute(we)
			def d2 = getDateFormatOfElement(we)
			if (getDateFormatOfElement(we).length() > 1)
				tmpList.add(we)
		}

		return tmpList
	}

	public List<WebElement> filterTheElementsDisplayed(List<WebElement> listWE){

		List<WebElement> tmpList = new ArrayList<WebElement>();
		for(WebElement we : listWE){
			if (we.isDisplayed())
				tmpList.add(we)
		}

		return tmpList;
	}

	public String getFirstDateElementVisible(List<WebElement> listWE){
		for(WebElement we : listWE){
			String weDate =  getDateFormatOfElement(we);
			
			if (weDate.length() > 1)
				if (getDate(weDate) == 1)
					return weDate;
		}
		return "";
	}

	public List<WebElement> getDateElementsWithDayValue(List<WebElement> listWE, int day){

		List<WebElement> tmpList = new ArrayList<WebElement>();
		for(WebElement we : listWE){
			if (we.getText() == day.toString())
				tmpList.add(we)
		}

		return tmpList;
	}

	public List<WebElement> getDateElementsWithMonthValue(List<WebElement> listWE, int month){

		List<WebElement> tmpList = new ArrayList<WebElement>();
		for(WebElement we : listWE){
			String date = getDateFormatOfElement(we);
			JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver();
			int objectMonth = (int) js.executeScript("return new Date('" + date + "').getMonth() + 1") as Integer;
			if (objectMonth == month)
				tmpList.add(we);
		}

		return tmpList;
	}
	
	public int getDate(String Day){
		JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver();	
		return (js.executeScript("return new Date('" + Day + "').getDate()") as Integer);
	}
	
	public int getMonth(String Day){
		JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver();
		return (js.executeScript("return new Date('" + Day + "').getMonth() + 1") as Integer);
	}
	
	public int getYear(String Day){
		JavascriptExecutor js = (JavascriptExecutor) DriverFactory.getWebDriver();
		return (js.executeScript("return new Date('" + Day + "').getFullYear()") as Integer);
	}
}
