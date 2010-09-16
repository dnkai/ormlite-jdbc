package com.j256.ormlite.db;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.j256.ormlite.TestUtils;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.stmt.StatementBuilder;
import com.j256.ormlite.table.TableInfo;

public class HsqldbDatabaseTypeTest extends BaseDatabaseTest {

	private final static String GENERATED_ID_SEQ = "genId_seq";

	@Override
	protected void setDatabaseParams() throws SQLException {
		databaseUrl = "jdbc:hsqldb:ormlite";
		connectionSource = DatabaseTypeUtils.createJdbcConnectionSource(DEFAULT_DATABASE_URL);
	}

	@Override
	protected boolean isDriverClassExpected() {
		return false;
	}

	@Test
	public void testGetDriverClassName() {
		assertEquals("org.hsqldb.jdbcDriver", databaseType.getDriverClassName());
	}

	@Override
	@Test
	public void testEscapedEntityName() throws Exception {
		String word = "word";
		assertEquals("\"" + word + "\"", TestUtils.appendEscapedEntityName(databaseType, word));
	}

	@Test(expected = IllegalStateException.class)
	public void testBadGeneratedId() throws Exception {
		Field field = GeneratedId.class.getField("id");
		DatabaseType mockDb = createMock(DatabaseType.class);
		expect(mockDb.isIdSequenceNeeded()).andReturn(false);
		expect(mockDb.getFieldConverter(isA(DataType.class))).andReturn(null);
		expect(mockDb.convertColumnName(isA(String.class))).andReturn("id");
		expect(mockDb.isEntityNamesMustBeUpCase()).andReturn(true);
		replay(mockDb);
		FieldType fieldType = FieldType.createFieldType(mockDb, "foo", field);
		verify(mockDb);
		StringBuilder sb = new StringBuilder();
		List<String> statementsBefore = new ArrayList<String>();
		databaseType.appendColumnArg(sb, fieldType, null, statementsBefore, null, null);
	}

	@Test
	public void testDropSequence() throws Exception {
		Field field = GeneratedId.class.getField("id");
		FieldType fieldType = FieldType.createFieldType(databaseType, "foo", field);
		List<String> statementsBefore = new ArrayList<String>();
		List<String> statementsAfter = new ArrayList<String>();
		databaseType.dropColumnArg(fieldType, statementsBefore, statementsAfter);
		assertEquals(0, statementsBefore.size());
		assertEquals(1, statementsAfter.size());
		assertTrue(statementsAfter.get(0).contains("DROP SEQUENCE "));
	}

	@Override
	@Test
	public void testGeneratedIdSequence() throws Exception {
		TableInfo<GeneratedIdSequence> tableInfo =
				new TableInfo<GeneratedIdSequence>(databaseType, GeneratedIdSequence.class);
		assertEquals(2, tableInfo.getFieldTypes().length);
		StringBuilder sb = new StringBuilder();
		List<String> additionalArgs = new ArrayList<String>();
		List<String> statementsBefore = new ArrayList<String>();
		databaseType.appendColumnArg(sb, tableInfo.getFieldTypes()[0], additionalArgs, statementsBefore, null, null);
		assertTrue(sb + " should contain autoincrement stuff", sb.toString().contains(
				" GENERATED BY DEFAULT AS IDENTITY "));
		// sequence, sequence table, insert
		assertEquals(1, statementsBefore.size());
		assertTrue(statementsBefore.get(0).contains(GENERATED_ID_SEQ.toUpperCase()));
		assertEquals(1, additionalArgs.size());
		assertTrue(additionalArgs.get(0).contains("PRIMARY KEY"));
	}

	@Test
	public void testGeneratedIdSequenceAutoName() throws Exception {
		TableInfo<GeneratedIdSequenceAutoName> tableInfo =
				new TableInfo<GeneratedIdSequenceAutoName>(databaseType, GeneratedIdSequenceAutoName.class);
		assertEquals(2, tableInfo.getFieldTypes().length);
		FieldType idField = tableInfo.getFieldTypes()[0];
		StringBuilder sb = new StringBuilder();
		List<String> additionalArgs = new ArrayList<String>();
		List<String> statementsBefore = new ArrayList<String>();
		databaseType.appendColumnArg(sb, idField, additionalArgs, statementsBefore, null, null);
		String seqName =
				databaseType.generateIdSequenceName(GeneratedIdSequenceAutoName.class.getSimpleName().toLowerCase(),
						idField);
		assertTrue(sb + " should contain gen-id-seq-name stuff", sb.toString().contains(
				" GENERATED BY DEFAULT AS IDENTITY "));
		// sequence, sequence table, insert
		assertEquals(1, statementsBefore.size());
		assertTrue(statementsBefore.get(0).contains(seqName.toUpperCase()));
		assertEquals(1, additionalArgs.size());
		assertTrue(additionalArgs.get(0).contains("PRIMARY KEY"));
	}

	@Override
	@Test
	public void testFieldWidthSupport() throws Exception {
		assertFalse(databaseType.isVarcharFieldWidthSupported());
	}

	@Override
	@Test
	public void testLimitAfterSelect() throws Exception {
		assertTrue(databaseType.isLimitAfterSelect());
	}

	@Override
	@Test
	public void testLimitFormat() throws Exception {
		BaseDaoImpl<Foo, String> dao = new BaseDaoImpl<Foo, String>(databaseType, Foo.class) {
		};
		dao.setConnectionSource(connectionSource);
		dao.initialize();
		StatementBuilder<Foo, String> qb = dao.statementBuilder();
		int limit = 1232;
		qb.limit(limit);
		String query = qb.prepareStatementString();
		assertTrue(query + " should start with stuff", query.startsWith("SELECT LIMIT 0 " + limit + " "));
	}

	@Test
	public void testBoolean() throws Exception {
		TableInfo<AllTypes> tableInfo = new TableInfo<AllTypes>(databaseType, AllTypes.class);
		assertEquals(9, tableInfo.getFieldTypes().length);
		FieldType booleanField = tableInfo.getFieldTypes()[1];
		assertEquals("booleanField", booleanField.getDbColumnName());
		StringBuilder sb = new StringBuilder();
		List<String> additionalArgs = new ArrayList<String>();
		List<String> statementsBefore = new ArrayList<String>();
		databaseType.appendColumnArg(sb, booleanField, additionalArgs, statementsBefore, null, null);
		assertTrue(sb.toString().contains("BIT"));
	}

	private final static String LONG_SEQ_NAME = "longseq";

	@Test
	public void testGneratedIdLong() throws Exception {
		TableInfo<GeneratedIdLong> tableInfo = new TableInfo<GeneratedIdLong>(databaseType, GeneratedIdLong.class);
		assertEquals(2, tableInfo.getFieldTypes().length);
		FieldType idField = tableInfo.getFieldTypes()[0];
		assertEquals("genId", idField.getDbColumnName());
		StringBuilder sb = new StringBuilder();
		List<String> additionalArgs = new ArrayList<String>();
		List<String> statementsBefore = new ArrayList<String>();
		databaseType.appendColumnArg(sb, idField, additionalArgs, statementsBefore, null, null);
		assertEquals(1, statementsBefore.size());
		StringBuilder sb2 = new StringBuilder();
		sb2.append("CREATE SEQUENCE ");
		databaseType.appendEscapedEntityName(sb2, LONG_SEQ_NAME.toUpperCase());
		sb2.append(" AS BIGINT");
		assertTrue(statementsBefore.get(0) + " should contain the right stuff", statementsBefore.get(0).contains(
				sb2.toString()));
	}

	protected static class GeneratedIdLong {
		@DatabaseField(generatedIdSequence = LONG_SEQ_NAME)
		long genId;
		@DatabaseField
		public String stuff;
	}
}
