/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.neo4j.server.qa;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.InvocationTargetException;

import org.neo4j.server.qa.web.ElementReference;
import org.neo4j.server.qa.web.WebDriverFacade;
import org.neo4j.server.qa.web.WebadminWebdriverLibrary;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import cuke4duke.annotation.I18n.EN.Given;
import cuke4duke.annotation.I18n.EN.Then;
import cuke4duke.annotation.I18n.EN.When;
import cuke4duke.spring.StepDefinitions;

@StepDefinitions
public class DataBrowserSteps
{
    private final WebDriver d;
    private final ElementReference itemHeadline;
    private final ElementReference saveButton;
    
    private final WebadminWebdriverLibrary wl;
    
    public DataBrowserSteps( WebDriverFacade facade ) throws InvocationTargetException, InstantiationException, IllegalAccessException
    {
        d = facade.getWebDriver();
        wl = new WebadminWebdriverLibrary(d);
        
        itemHeadline = new ElementReference(d, By.xpath( "//div[@id='data-area']//h1" ));
        saveButton = new ElementReference(d, By.xpath( "//button[@class='data-save-properties']" ));
    }
    
    @Given("^I have created a node through webadmin$")
    public void iHaveCreatedANode() throws Exception {
        wl.goToWebadminStartPage();
        wl.clickOnTab( "Data browser" );
        String prevItemHeadline = itemHeadline.getText();
        
        wl.clickOnButton( "Node" );
        
        itemHeadline.waitForTextToChangeFrom( prevItemHeadline );
    }
    
    @When("^I look at the webadmin data browser in a web browser$") 
    public void iLookAtDatabrowserInABrowser() throws Exception
    {
        wl.goToWebadminStartPage();
        wl.clickOnTab( "Data browser" );
    }
    
    @When("I enter (.+) into the data browser search field")
    public void iEnterXIntoTheDatabrowserSearchField(String keysToSend) {
        wl.searchForInDataBrowser( keysToSend );
    }
    
    @Then("^The data browser item headline should be (.+)$")
    public void theDataBrowserItemHeadlineShouldBe(String expected) {
        itemHeadline.waitForTextToChangeTo( expected );
    }
    
    @Then("^The data browser item headline should change from (.+)$")
    public void theDataBrowserShouldChangeFrom(String expected) {
        itemHeadline.waitForTextToChangeFrom( expected );
    }
    
    @Then("^The databrowser save button should say (.+)$")
    public void theDataBrowserSaveButtonShouldSay(String text) {
        assertThat( saveButton.getText(), is( text ));
    }
    
    @Then("^The databrowser save button should change to saying (.+)$")
    public void theDataBrowserSaveButtonChangeToSaying(String text) {
        saveButton.waitForTextToChangeTo( text );
    }
    
}
