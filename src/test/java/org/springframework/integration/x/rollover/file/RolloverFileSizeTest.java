/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.x.rollover.file;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RolloverFileSizeTest {

	@Autowired
	ConfigurableApplicationContext applicationContext;

	@Autowired
	MessageChannel input;

	File tmpDir = new File("./test_results");

	@Before
	public void before() {
		tmpDir.mkdir();
	}

	@Test
	public void testRolloverFileSink() throws IOException, InterruptedException {
		applicationContext.start();

		input.send(new GenericMessage<String>("ABCD"));
		input.send(new GenericMessage<String>("BINGO666666")); // Second message should land in a new file.

		TreeSet<File> files = new TreeSet<File>(FileUtils.listFiles(tmpDir, null, false));

//		assertEquals(2, files.size());
//
//		Iterator<File> iterator = files.iterator();
//		//iterator.next();
//		
//		File firstFile = iterator.next();
//		System.out.println(IOUtils.toString(firstFile.toURI()));
//		
//		assertTrue(firstFile.toString().startsWith("./test_results/test666_"));
//		assertEquals("ABCD", IOUtils.toString(firstFile.toURI()));
//
//		
//		File secondFile = iterator.next();
//		assertTrue(secondFile.toString().startsWith("./test_results/test666_"));
//		assertEquals("BINGO666666", IOUtils.toString(secondFile.toURI()));
		
	}

	@After
	public void cleanUp() throws IOException {
		FileUtils.deleteDirectory(tmpDir);
	}
}
