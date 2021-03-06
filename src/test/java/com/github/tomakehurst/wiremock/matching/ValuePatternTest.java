/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.matching;

import com.github.tomakehurst.wiremock.common.LocalNotifier;
import com.github.tomakehurst.wiremock.common.Notifier;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JMock.class)
public class ValuePatternTest {
	
	private ValuePattern valuePattern;
    private Mockery context;
	
	@Before
	public void init() {
		valuePattern = new ValuePattern();
        context = new Mockery();
	}

    @After
    public void cleanUp() {
        LocalNotifier.set(null);
    }

	@Test
	public void matchesOnEqualTo() {
		valuePattern.setEqualTo("text/plain");
		assertTrue(valuePattern.isMatchFor("text/plain"));
	}
	
	@Test
	public void matchesOnRegex() {
		valuePattern.setMatches("[0-9]{6}");
		assertTrue(valuePattern.isMatchFor("938475"));
		assertFalse(valuePattern.isMatchFor("abcde"));
	}
	
	@Test
	public void matchesOnNegativeRegex() {
		valuePattern.setDoesNotMatch("[0-9]{6}");
		assertFalse(valuePattern.isMatchFor("938475"));
		assertTrue(valuePattern.isMatchFor("abcde"));
	}
	
	@Test
	public void matchesOnContains() {
		valuePattern.setContains("some text");
		assertFalse(valuePattern.isMatchFor("Nothing to see here"));
		assertTrue(valuePattern.isMatchFor("There's some text here"));
	}

    @Test
    public void matchesOnAbsent() {
        valuePattern = ValuePattern.absent();
        assertFalse("Absent value should not be a match for a string", valuePattern.isMatchFor("Something"));
        assertTrue("Absent value should be match for null", valuePattern.isMatchFor(null));
        assertTrue("isAbsent() should be true", valuePattern.isAbsent());
    }

    @Test
    public void matchesOnBasicJsonPaths() {
        valuePattern.setMatchesJsonPaths("$.one");
        assertTrue("Expected match when JSON attribute is present", valuePattern.isMatchFor("{ \"one\": 1 }"));
        assertFalse("Expected no match when JSON attribute is absent", valuePattern.isMatchFor("{ \"two\": 2 }"));
    }

    @Test
    public void matchesOnJsonPathsWithFilters() {
        valuePattern.setMatchesJsonPaths("$.numbers[?(@.number == '2')]");
        assertTrue("Expected match when JSON attribute is present", valuePattern.isMatchFor("{ \"numbers\": [ {\"number\": 1}, {\"number\": 2} ]}"));
        assertFalse("Expected no match when JSON attribute is absent", valuePattern.isMatchFor("{ \"numbers\": [{\"number\": 7} ]}"));
    }

    @Test
    public void matchesOnJsonPathsWithFiltersOnNestedObjects() {
        valuePattern.setMatchesJsonPaths("$..*[?(@.innerOne == 11)]");
        assertTrue("Expected match", valuePattern.isMatchFor("{ \"things\": { \"thingOne\": { \"innerOne\": 11 }, \"thingTwo\": 2 }}"));
    }

    @Test
    public void providesSensibleNotificationWhenJsonMatchFailsDueToInvalidJson() {
        expectInfoNotification("Warning: JSON path expression '$.something' failed to match document 'Not a JSON document' because the JSON document couldn't be parsed");

        valuePattern.setMatchesJsonPaths("$.something");
        assertFalse("Expected the match to fail", valuePattern.isMatchFor("Not a JSON document"));
    }

    @Test
    public void providesSensibleNotificationWhenJsonMatchFailsDueToMissingAttributeJson() {
        expectInfoNotification("Warning: JSON path expression '$.something' failed to match document '{ \"nothing\": 1 }' because the JSON path didn't match the document structure");

        valuePattern.setMatchesJsonPaths("$.something");
        assertFalse("Expected the match to fail", valuePattern.isMatchFor("{ \"nothing\": 1 }"));
    }

    @Test(expected=IllegalStateException.class)
	public void doesNotPermitMoreThanOneTypeOfMatch() {
		valuePattern.setEqualTo("my-value");
		valuePattern.setMatches("[0-9]{6}");
	}

	@Test(expected=IllegalStateException.class)
	public void doesNotPermitMoreThanOneTypeOfMatchWithOtherOrdering() {
		valuePattern.setMatches("[0-9]{6}");
		valuePattern.setEqualTo("my-value");
	}

	@Test(expected=IllegalStateException.class)
	public void doesNotPermitMoreThanOneTypeOfMatchWithOtherDoesNotMatch() {
		valuePattern.setEqualTo("my-value");
		valuePattern.setDoesNotMatch("[0-9]{6}");
	}

	@Test(expected=IllegalStateException.class)
	public void doesNotPermitZeroMatchTypes() {
		valuePattern.isMatchFor("blah");
	}

    private void expectInfoNotification(final String message) {
        final Notifier notifier = context.mock(Notifier.class);
        context.checking(new Expectations() {{
            one(notifier).info(message);
        }});
        LocalNotifier.set(notifier);
    }
}
