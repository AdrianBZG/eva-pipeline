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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.eva.pipeline.jobs.steps.tasklets.PullFilesAndStatisticsFromVariantGivenStudyIdStep;
import uk.ac.ebi.eva.pipeline.parameters.JobOptions;
import uk.ac.ebi.eva.utils.TaskletUtils;

import static uk.ac.ebi.eva.pipeline.configuration.BeanNames.PULL_FILES_AND_STATISTICS_BY_STUDY_STEP;

/**
 * Step that pulls the file and statistics in a variant based on their study id.
 */
@Configuration
@EnableBatchProcessing
public class PullFilesAndStatisticsByStudyStep {

    private static final Logger logger = LoggerFactory.getLogger(PullFilesAndStatisticsByStudyStep.class);

    @Bean
    @StepScope
    public PullFilesAndStatisticsFromVariantGivenStudyIdStep variantsAndStatisticsDropperTasklet() {
        return new PullFilesAndStatisticsFromVariantGivenStudyIdStep();
    }

    @Bean(PULL_FILES_AND_STATISTICS_BY_STUDY_STEP)
    public TaskletStep dropVariantsAndStatisticsByStudioStep(StepBuilderFactory stepBuilderFactory,
                                                             JobOptions jobOptions) {
        return TaskletUtils.generateStep(stepBuilderFactory, PULL_FILES_AND_STATISTICS_BY_STUDY_STEP,
                variantsAndStatisticsDropperTasklet(), jobOptions.isAllowStartIfComplete());
    }

}
