package uk.ac.ebi.eva.t2d.jobs.steps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import uk.ac.ebi.eva.pipeline.Application;
import uk.ac.ebi.eva.pipeline.parameters.JobOptions;
import uk.ac.ebi.eva.t2d.configuration.readers.SummaryStatisticsReaderConfiguration;
import uk.ac.ebi.eva.t2d.configuration.writers.TsvWriterConfiguration;
import uk.ac.ebi.eva.t2d.jobs.readers.TsvReader;
import uk.ac.ebi.eva.t2d.jobs.writers.TsvWriter;

import java.util.List;

import static uk.ac.ebi.eva.t2d.BeanNames.T2D_LOAD_SUMMARY_STATISTICS_STEP;
import static uk.ac.ebi.eva.t2d.BeanNames.T2D_SUMMARY_STATISTICS_READER;
import static uk.ac.ebi.eva.t2d.BeanNames.T2D_TSV_WRITER;

@Configuration
@Profile(Application.T2D_PROFILE)
@EnableBatchProcessing
@Import({SummaryStatisticsReaderConfiguration.class, TsvWriterConfiguration.class})
public class LoadSummaryStatisticsStep {

    private static final Logger logger = LoggerFactory.getLogger(LoadSummaryStatisticsStep.class);

    @Bean(T2D_LOAD_SUMMARY_STATISTICS_STEP)
    public Step prepareDatabaseT2d(StepBuilderFactory stepBuilderFactory, JobOptions jobOptions,
                                   @Qualifier(T2D_SUMMARY_STATISTICS_READER) TsvReader loadSamplesFileReader,
                                   @Qualifier(T2D_TSV_WRITER) TsvWriter tsvWriter) {
        logger.debug("Building '" + T2D_LOAD_SUMMARY_STATISTICS_STEP + "'");
        return stepBuilderFactory.get(T2D_LOAD_SUMMARY_STATISTICS_STEP)
                .<List<String>, List<String>>chunk(100)
                .reader(loadSamplesFileReader)
                .writer(tsvWriter)
                .allowStartIfComplete(jobOptions.isAllowStartIfComplete())
                .build();
    }

}