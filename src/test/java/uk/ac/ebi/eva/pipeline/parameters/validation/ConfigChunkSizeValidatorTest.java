/*
 * Copyright 2015-2017 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.ebi.eva.pipeline.parameters.validation;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

import uk.ac.ebi.eva.pipeline.parameters.JobParametersNames;

public class ConfigChunkSizeValidatorTest {
    private ConfigChunkSizeValidator validator;

    private JobParametersBuilder jobParametersBuilder;

    @Before
    public void setUp() throws Exception {
        validator = new ConfigChunkSizeValidator();
    }

    @Test
    public void chunkSizeIsValid() throws JobParametersInvalidException {
        jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString(JobParametersNames.CONFIG_CHUNK_SIZE, "11");
        validator.validate(jobParametersBuilder.toJobParameters());
    }

    @Test(expected = JobParametersInvalidException.class)
    public void chunkSizeIsZero() throws JobParametersInvalidException {
        jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString(JobParametersNames.CONFIG_CHUNK_SIZE, "0");
        validator.validate(jobParametersBuilder.toJobParameters());
    }

    @Test(expected = JobParametersInvalidException.class)
    public void chunkSizeIsNegative() throws JobParametersInvalidException {
        jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString(JobParametersNames.CONFIG_CHUNK_SIZE, "-1");
        validator.validate(jobParametersBuilder.toJobParameters());
    }

    @Test(expected = JobParametersInvalidException.class)
    public void chunkSizeIsNotValid() throws JobParametersInvalidException {
        jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString(JobParametersNames.CONFIG_CHUNK_SIZE, "hello");
        validator.validate(jobParametersBuilder.toJobParameters());
    }

    @Test(expected = JobParametersInvalidException.class)
    public void chunkSizeIsEmpty() throws JobParametersInvalidException {
        jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString(JobParametersNames.CONFIG_CHUNK_SIZE, "");
        validator.validate(jobParametersBuilder.toJobParameters());
    }

    @Test(expected = JobParametersInvalidException.class)
    public void chunkSizeIsNull() throws JobParametersInvalidException {
        jobParametersBuilder = new JobParametersBuilder();
        jobParametersBuilder.addString(JobParametersNames.CONFIG_CHUNK_SIZE, null);
        validator.validate(jobParametersBuilder.toJobParameters());
    }
}
