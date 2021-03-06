/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package io.crate.integrationtests;

import io.crate.action.sql.SQLActionException;
import io.crate.test.integration.CrateIntegrationTest;
import io.crate.testing.TestingHelpers;
import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.is;

@CrateIntegrationTest.ClusterScope(scope = CrateIntegrationTest.Scope.GLOBAL)
public class QueryThenFetchIntegrationTest extends SQLTransportIntegrationTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testCrateSearchServiceSupportsOrderByOnFunctionWithBooleanReturnType() throws Exception {
        execute("create table t (name string, b byte) with (number_of_replicas = 0)");
        execute("insert into t (name, b) values ('Marvin', 0), ('Trillian', 1), ('Arthur', 2), ('Max', 3)");
        execute("refresh table t");
        ensureGreen();

        execute("select * from t order by substr(name, 1, 1) = 'M', b");
        assertThat(TestingHelpers.printedTable(response.rows()), is(
                "1| Trillian\n" +
                "2| Arthur\n" +
                "0| Marvin\n" +
                "3| Max\n"));
    }

    @Test
    public void testThatErrorsInSearchResponseCallbackAreNotSwallowed() throws Exception {
        expectedException.expect(SQLActionException.class);
        expectedException.expectMessage(is("d != java.lang.String"));

        execute("create table t (s string) clustered into 1 shards with (number_of_replicas = 0)");
        ensureYellow();
        execute("insert into t (s) values ('foo')");
        execute("refresh table t");

        execute("select format('%d', s) from t");
    }

    @Test
    public void testTestWithTimestampThatIsInIntegerRange() throws Exception {
        execute("create table t (ts timestamp) clustered into 1 shards with (number_of_replicas = 0)");
        ensureYellow();
        execute("insert into t (ts) values (0)");
        execute("insert into t (ts) values (1425980155)");
        execute("refresh table t");

        execute("select extract(day from ts) from t order by 1");
        assertThat((Integer) response.rows()[0][0], is(1));
        assertThat((Integer) response.rows()[1][0], is(17));
    }

    @Test
    public void testOrderBySortTypes() throws Exception {
        execute("create table xxx (" +
                "  b byte," +
                "  s short," +
                "  i integer," +
                "  l long," +
                "  f float," +
                "  d double," +
                "  st string," +
                "  boo boolean," +
                "  t timestamp," +
                "  ipp ip" +
                ")");
        ensureYellow();
        execute("insert into xxx (b, s, i, l, f, d, st, boo, t, ipp) values (?, ?, ?, ?, ?, ?, ? ,?, ?, ?)", new Object[][]{
                {1, 2, 3, 4L, 1.5f, -0.5d, "hallo", true, "1970-01-01", "127.0.0.1"},
                {null, null, null, null, null, null, null, null, null, null},
                {2, 4, 6, 8L, 3.1f, -4.5d, "goodbye", false, "2088-01-01", "10.0.0.1"},
        });
        execute("refresh table xxx");
        execute("select b, s, i, l, f, d, st, boo, t, ipp " +
                "from xxx " +
                "order by b, s, i, l, f, d, st, boo, t, ipp");
        assertThat(TestingHelpers.printedTable(response.rows()), Is.is(
                "1| 2| 3| 4| 1.5| -0.5| hallo| true| 0| 127.0.0.1\n" +
                        "2| 4| 6| 8| 3.1| -4.5| goodbye| false| 3723753600000| 10.0.0.1\n" +
                        "NULL| NULL| NULL| NULL| NULL| NULL| NULL| NULL| NULL| NULL\n"));

        execute("select b, s, i, l, f, d, st, boo, t, ipp " +
                "from xxx " +
                "order by b desc nulls first, s, i, l, f, d, st, boo, t, ipp");
        assertThat(TestingHelpers.printedTable(response.rows()), Is.is(
                "NULL| NULL| NULL| NULL| NULL| NULL| NULL| NULL| NULL| NULL\n" +
                        "2| 4| 6| 8| 3.1| -4.5| goodbye| false| 3723753600000| 10.0.0.1\n" +
                        "1| 2| 3| 4| 1.5| -0.5| hallo| true| 0| 127.0.0.1\n"));
    }
}
