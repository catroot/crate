/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.operation.reference.doc;

import io.crate.operation.reference.doc.lucene.FloatColumnReference;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.mapper.FieldMapper;
import org.junit.Test;

import static org.hamcrest.core.Is.is;

public class FloatColumnReferenceTest extends DocLevelExpressionsTest {
    @Override
    protected void insertValues(IndexWriter writer) throws Exception {
        for (float f = -0.5f; f<10.0f; f++) {
            Document doc = new Document();
            doc.add(new StringField("_id", Float.toString(f), Field.Store.NO));
            doc.add(new FloatField(fieldName().name(), f, Field.Store.NO));
            writer.addDocument(doc);
        }
    }

    @Override
    protected FieldMapper.Names fieldName() {
        return new FieldMapper.Names("f");
    }

    @Override
    protected FieldDataType fieldType() {
        return new FieldDataType("float");
    }

    @Test
    public void testFieldCacheExpression() throws Exception {
        FloatColumnReference floatColumn = new FloatColumnReference(fieldName().name());
        floatColumn.startCollect(ctx);
        floatColumn.setNextReader(readerContext);
        IndexSearcher searcher = new IndexSearcher(readerContext.reader());
        TopDocs topDocs = searcher.search(new MatchAllDocsQuery(), 10);
        float f = -0.5f;
        for (ScoreDoc doc : topDocs.scoreDocs) {
            floatColumn.setNextDocId(doc.doc);
            assertThat(floatColumn.value(), is(f));
            f++;
        }
    }
}
