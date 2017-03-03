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

package uk.ac.ebi.eva.pipeline.jobs.steps;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import uk.ac.ebi.eva.pipeline.Application;
import uk.ac.ebi.eva.pipeline.configuration.BeanNames;
import uk.ac.ebi.eva.pipeline.jobs.DropStudyJob;
import uk.ac.ebi.eva.test.configuration.BatchTestConfiguration;
import uk.ac.ebi.eva.test.data.VariantData;
import uk.ac.ebi.eva.test.rules.TemporaryMongoRule;
import uk.ac.ebi.eva.utils.EvaJobParameterBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.eva.commons.models.converters.data.VariantSourceEntryToDBObjectConverter.STUDYID_FIELD;
import static uk.ac.ebi.eva.commons.models.converters.data.VariantToDBObjectConverter.FILES_FIELD;

/**
 * Test for {@link DropSingleStudyVariantsStep}
 */
@RunWith(SpringRunner.class)
@ActiveProfiles({Application.VARIANT_WRITER_MONGO_PROFILE, Application.VARIANT_ANNOTATION_MONGO_PROFILE})
@TestPropertySource({"classpath:common-configuration.properties", "classpath:test-mongo.properties"})
@ContextConfiguration(classes = {DropStudyJob.class, BatchTestConfiguration.class})
public class DropSingleStudyVariantsStepTest {

    private static final String COLLECTION_VARIANTS_NAME = "variants";

    private static final long EXPECTED_VARIANTS_AFTER_DROP_STUDY = 2;

    private static final String STUDY_ID_TO_DROP = "studyIdToDrop";

    @Rule
    public TemporaryMongoRule mongoRule = new TemporaryMongoRule();

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Test
    public void testNoVariantsToDrop() throws Exception {
        String databaseName = mongoRule.insertDocuments(COLLECTION_VARIANTS_NAME, Arrays.asList(
                VariantData.getVariantWithOneStudy(),
                VariantData.getVariantWithTwoStudies()));

        checkDrop(databaseName, EXPECTED_VARIANTS_AFTER_DROP_STUDY);
    }

    @Test
    public void testOneVariantToDrop() throws Exception {
        String databaseName = mongoRule.insertDocuments(COLLECTION_VARIANTS_NAME, Arrays.asList(
                VariantData.getVariantWithOneStudyToDrop(),
                VariantData.getVariantWithOneStudy(),
                VariantData.getVariantWithTwoStudies()));

        checkDrop(databaseName, EXPECTED_VARIANTS_AFTER_DROP_STUDY);
    }

    @Test
    public void testSeveralVariantsToDrop() throws Exception {
        String databaseName = mongoRule.insertDocuments(COLLECTION_VARIANTS_NAME, Arrays.asList(
                VariantData.getVariantWithOneStudyToDrop(),
                VariantData.getOtherVariantWithOneStudyToDrop(),
                VariantData.getVariantWithOneStudy(),
                VariantData.getVariantWithTwoStudies()));

        checkDrop(databaseName, EXPECTED_VARIANTS_AFTER_DROP_STUDY);
    }

    private void checkDrop(String databaseName, long expectedVariantsAfterDropStudy) {
        JobParameters jobParameters = new EvaJobParameterBuilder()
                .collectionVariantsName(COLLECTION_VARIANTS_NAME)
                .databaseName(databaseName)
                .inputStudyId(STUDY_ID_TO_DROP)
                .toJobParameters();

        // When the execute method in variantsLoad is executed
        JobExecution jobExecution = jobLauncherTestUtils.launchStep(BeanNames.DROP_SINGLE_STUDY_VARIANTS_STEP,
                jobParameters);

        //Then variantsLoad step should complete correctly
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // And the documents in the DB should not contain the study removed
        DBCollection variantsCollection = mongoRule.getCollection(databaseName, COLLECTION_VARIANTS_NAME);
        assertEquals(expectedVariantsAfterDropStudy, variantsCollection.count());

        String filesStudyIdField = String.format("%s.%s", FILES_FIELD, STUDYID_FIELD);
        BasicDBObject singleStudyVariants = new BasicDBObject(filesStudyIdField, STUDY_ID_TO_DROP)
                .append(FILES_FIELD, new BasicDBObject("$size", 1));
        assertEquals(0, variantsCollection.count(singleStudyVariants));
    }

}
