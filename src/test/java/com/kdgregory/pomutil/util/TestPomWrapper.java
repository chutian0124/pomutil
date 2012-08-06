// Copyright Keith D Gregory
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.kdgregory.pomutil.util;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.junit.Test;

import static org.junit.Assert.*;

import net.sf.practicalxml.DomUtil;
import net.sf.practicalxml.ParseUtil;
import net.sf.practicalxml.xpath.XPathWrapperFactory;


public class TestPomWrapper
{

    private XPathWrapperFactory xpFact = new XPathWrapperFactory()
                                         .bindNamespace("mvn", "http://maven.apache.org/POM/4.0.0");

//----------------------------------------------------------------------------
//  Support Code
//----------------------------------------------------------------------------


//----------------------------------------------------------------------------
//  Testcases
//----------------------------------------------------------------------------

    @Test
    public void testSelectAgainstPom() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper1.xml"));

        assertEquals("selectValue()",
                     "A simple POM with a few sections",
                     wrapper.selectValue("/mvn:project/mvn:description").trim());

        Element propElem = wrapper.selectElement("/mvn:project/mvn:properties");
        assertEquals("selectElement()",
                     "properties", DomUtil.getLocalName(propElem));

        List<Element> properties = wrapper.selectElements("/mvn:project/mvn:properties/*");
        assertEquals("selectElements()",
                     2, properties.size());
    }


    @Test
    public void testSelectAgainstNode() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper1.xml"));

        Element root = wrapper.getDom().getDocumentElement();

        Element propElem = wrapper.selectElement(root, "mvn:properties");
        assertEquals("selectElement(Node,path)", "properties", DomUtil.getLocalName(propElem));

        List<Element> allProps = wrapper.selectElements(propElem, "*");
        assertEquals("selectElements(Node,path)", 2, allProps.size());

        String value = wrapper.selectValue(propElem, "*[1]");
        assertEquals("selectValue(Node,path)", DomUtil.getText(allProps.get(0)), value);
    }


    @Test
    public void testSelectOrCreate() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper1.xml"));

        String path = "/mvn:project/mvn:name";
        assertNull("element doesn't exist at start", wrapper.selectElement(path));

        Element newElem = wrapper.selectOrCreateElement(path);
        assertNotNull("element was created", newElem);
        assertEquals("element created with correct namespace", "http://maven.apache.org/POM/4.0.0", newElem.getNamespaceURI());
        assertEquals("element was created with correct name",  "name", DomUtil.getLocalName(newElem));

        Element check = wrapper.selectElement(path);
        assertSame("element was selectable via path", newElem, check);
    }


    @Test
    public void testSelectOrCreateRecursive() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper1.xml"));

        String path = "/mvn:project/mvn:argle/mvn:bargle";
        assertNull("element doesn't exist at start", wrapper.selectElement(path));

        Element newElem = wrapper.selectOrCreateElement(path);
        assertNotNull("element was created", newElem);
        assertEquals("element created with correct namespace", "http://maven.apache.org/POM/4.0.0", newElem.getNamespaceURI());
        assertEquals("element was created with correct name",  "bargle", DomUtil.getLocalName(newElem));

        Element check = wrapper.selectElement(path);
        assertSame("element was selectable via path", newElem, check);
    }


    @Test
    public void testExtractGAV() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper1.xml"));

        Element elem = wrapper.selectElement("/mvn:project/mvn:dependencies/mvn:dependency[1]");
        GAV gav = wrapper.extractGAV(elem);

        assertEquals("groupId",     "junit",    gav.groupId);
        assertEquals("artifactId",  "junit",    gav.artifactId);
        assertEquals("version",     "4.10",     gav.version);
    }


    @Test
    public void testClear() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper1.xml"));

        Element propElem = wrapper.clear("/mvn:project/mvn:properties");
        assertEquals("element name", "properties", DomUtil.getLocalName(propElem));
        assertEquals("number of children", 0, propElem.getChildNodes().getLength());
    }


    @Test
    public void testGetAndSetProperties() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper1.xml"));

        Map<String,String> allProps1 = wrapper.getProperties();
        assertEquals("property count, initial", 2, allProps1.size());
        assertEquals("known property, initial", "UTF-8", allProps1.get("project.build.sourceEncoding"));

        assertEquals("getProperty(), initial", "UTF-8", wrapper.getProperty("project.build.sourceEncoding"));
        assertEquals("getProperty(), missing", "", wrapper.getProperty("foo"));

        wrapper.setProperty("project.build.sourceEncoding", "US-ASCII");
        assertEquals("getProperty(), adter set", "US-ASCII", wrapper.getProperty("project.build.sourceEncoding"));

        Map<String,String> allProps2 = wrapper.getProperties();
        assertEquals("property count, after set", 2, allProps2.size());
        assertEquals("known property, after set", "US-ASCII", allProps2.get("project.build.sourceEncoding"));

        assertEquals("cross-check, after set", "US-ASCII",
                     xpFact.newXPath("//mvn:project.build.sourceEncoding").evaluateAsString(wrapper.getDom()));

        wrapper.deleteProperty("project.build.sourceEncoding");
        assertEquals("getProperty(), after delete", "", wrapper.getProperty("project.build.sourceEncoding"));

        Map<String,String> allProps3 = wrapper.getProperties();
        assertEquals("property count, after delete", 1, allProps3.size());
        assertEquals("known property, after delete", null, allProps3.get("project.build.sourceEncoding"));
    }


    @Test
    public void testSetPropertiesAddsIfNecessary() throws Exception
    {
        PomWrapper wrapper = new PomWrapper(ParseUtil.parseFromClasspath("PomWrapper2.xml"));

        // this first check simply verifies that we don't blow up if there's no properties section
        Map<String,String> allProps1 = wrapper.getProperties();
        assertEquals("property count, initial", 0, allProps1.size());
        assertEquals("getProperty(), initial", "", wrapper.getProperty("foo"));

        wrapper.setProperty("foo", "bar");

        Map<String,String> allProps2 = wrapper.getProperties();
        assertEquals("property count, after set", 1, allProps2.size());
        assertEquals("allProps retrieve, after set", "bar", allProps2.get("foo"));
        assertEquals("getProperty(), after set", "bar", wrapper.getProperty("foo"));

        assertEquals("cross-check", "bar", xpFact.newXPath("//mvn:foo").evaluateAsString(wrapper.getDom()));
    }

}