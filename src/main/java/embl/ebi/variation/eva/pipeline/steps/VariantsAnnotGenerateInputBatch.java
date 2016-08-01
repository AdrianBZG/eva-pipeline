/*
 * Copyright 2016 EMBL - European Bioinformatics Institute
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
package embl.ebi.variation.eva.pipeline.steps;

import com.mongodb.DBObject;
import embl.ebi.variation.eva.pipeline.utils.ConnectionHelper;
import embl.ebi.variation.eva.pipeline.annotation.generateInput.VariantAnnotationItemProcessor;
import embl.ebi.variation.eva.pipeline.annotation.generateInput.VariantWrapper;
import embl.ebi.variation.eva.pipeline.jobs.VariantJobArgsConfig;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.opencga.storage.core.variant.VariantStorageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Diego Poggioli
 *
 * Dump a list of variants without annotations from mongo
 *
 * Step class that:
 * - READ: read the variants without annotations from mongo
 * - PROCESS convert, filter, validate... the {@link VariantWrapper}
 * - LOAD: write the {@link VariantWrapper} into a flatfile
 *
 * TODO:
 * - Handle the overwrite
 * - The variant list should be compressed. It is not possible to write into a zipped file with FlatFile item writer
 *  see jmmut comment at https://github.com/EBIvariation/eva-v2/pull/22
 *  We can create an extra step to convert the file and remove the nonp-zipped one
 *  https://www.mkyong.com/java/how-to-compress-a-file-in-gzip-format/
 *  https://examples.javacodegeeks.com/core-java/io/fileinputstream/compress-a-file-in-gzip-format-in-java/
 *  http://www.journaldev.com/966/java-gzip-example-compress-and-decompress-file-in-gzip-format-in-java
 */

@Configuration
@EnableBatchProcessing
@Import(VariantJobArgsConfig.class)
public class VariantsAnnotGenerateInputBatch {

    private static final Logger logger = LoggerFactory.getLogger(VariantsAnnotGenerateInputBatch.class);

    @Autowired
    private StepBuilderFactory steps;

    @Autowired
    private ObjectMap pipelineOptions;

/*
    public static final String jobName = "variantsAnnotGenerateInputJob";

    @Bean
    public Step variantsAnnotGenerateInputBatchStep(ItemReader<DBObject> reader,
                      ItemProcessor<DBObject, VepInputLine> processor,
                      ItemWriter<VepInputLine> writer) {
        return steps.get("step1").<DBObject, VepInputLine> chunk(10)
                .reader(reader)
                .processor(processor)
                .writer(writer).allowStartIfComplete(false)
                .build();
    }*/

    @Bean
    @Qualifier("variantsAnnotGenerateInputBatchStep")
    public Step variantsAnnotGenerateInputBatchStep() throws Exception {
        return steps.get("variantsAnnotGenerateInputBatchStep").<DBObject, VariantWrapper> chunk(10)
                .reader(variantReader())
                .processor(vepInputLineProcessor())
                .writer(vepInputWriter()).allowStartIfComplete(false)
                .build();
    }

    @Bean
    public ItemReader<DBObject> variantReader() throws Exception {
        return initReader(pipelineOptions.getString("dbCollectionVariantsName"), mongoOperations());
    }

    public MongoItemReader<DBObject> initReader(String collection, MongoOperations template){
        MongoItemReader<DBObject> reader = new MongoItemReader<>();
        reader.setCollection(collection);

        reader.setQuery("{ annot : { $exists : false } }");
        reader.setFields("{ chr : 1, start : 1, end : 1, ref : 1, alt : 1, type : 1}");
        reader.setTargetType(DBObject.class);
        reader.setTemplate(template);

        Map<String, Sort.Direction> coordinatesSort = new HashMap<>();
        coordinatesSort.put("chr", Sort.Direction.ASC);
        coordinatesSort.put("start", Sort.Direction.ASC);
        reader.setSort(coordinatesSort);
        return reader;
    }

    @Bean
    public ItemProcessor<DBObject, VariantWrapper> vepInputLineProcessor() {
        return new VariantAnnotationItemProcessor();
    }

    /**
     * @return must return a {@link FlatFileItemWriter} and not a {@link org.springframework.batch.item.ItemWriter}
     * {@see https://jira.spring.io/browse/BATCH-2097
     *
     * TODO: The variant list should be compressed
     */
    @Bean
    public FlatFileItemWriter<VariantWrapper> vepInputWriter() throws Exception {
        return initWriter(new FileSystemResource(pipelineOptions.getString("vepInput")));
    }

    public static FlatFileItemWriter<VariantWrapper> initWriter(Resource resource){
        BeanWrapperFieldExtractor<VariantWrapper> fieldExtractor = new BeanWrapperFieldExtractor<>();
        fieldExtractor.setNames(new String[] {"chr", "start", "end", "refAlt", "strand"});

        DelimitedLineAggregator<VariantWrapper> delLineAgg = new DelimitedLineAggregator<>();
        delLineAgg.setDelimiter("\t");
        delLineAgg.setFieldExtractor(fieldExtractor);

        FlatFileItemWriter<VariantWrapper> writer = new FlatFileItemWriter<>();

        writer.setResource(resource);
        writer.setAppendAllowed(false);
        writer.setShouldDeleteIfExists(true);
        writer.setLineAggregator(delLineAgg);
        return writer;
    }

    @Bean
    public MongoOperations mongoOperations() {
        MongoTemplate mongoTemplate;
        try {
            mongoTemplate = getMongoTemplate();
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to initialize MongoDB", e);
        }
        return mongoTemplate;
    }

    private MongoTemplate getMongoTemplate() throws UnknownHostException {
        MongoTemplate mongoTemplate;
        if(pipelineOptions.getString("dbAuthenticationDb").isEmpty()){
            mongoTemplate = ConnectionHelper.getMongoTemplate(
                    pipelineOptions.getString(VariantStorageManager.DB_NAME)
            );
        }else {
            mongoTemplate = ConnectionHelper.getMongoTemplate(
                    pipelineOptions.getString(VariantStorageManager.DB_NAME),
                    pipelineOptions.getString("dbHosts"),
                    pipelineOptions.getString("dbAuthenticationDb"),
                    pipelineOptions.getString("dbUser"),
                    pipelineOptions.getString("dbPassword").toCharArray()
            );
        }
        return mongoTemplate;
    }
}