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
import org.junit.Assert;
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
public class RolloverFileMessageHandlerTest {

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
	public void test1() throws IOException, InterruptedException {
		applicationContext.start();

		input.send(new GenericMessage<String>("foo"));
		Thread.sleep(1100);
		input.send(new GenericMessage<String>("bar"));

		TreeSet<File> files = new TreeSet<File>(FileUtils.listFiles(tmpDir,
				null, false));

		Assert.assertEquals(2, files.size());

		Iterator<File> iterator = files.iterator();

		File firstFile = iterator.next();
		Assert.assertTrue(firstFile.toString().startsWith(
				"./test_results/test666_"));
		Assert.assertEquals("foo", IOUtils.toString(firstFile.toURI()));

		File secondFile = iterator.next();
		Assert.assertTrue(secondFile.toString().startsWith(
				"./test_results/test666_"));

		Assert.assertEquals("bar", IOUtils.toString(secondFile.toURI()));
	}

	@After
	public void cleanUp() throws IOException {
		FileUtils.deleteDirectory(tmpDir);
	}
}
