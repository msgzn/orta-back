package org.example.ortaback.persistence.type;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VectorTypeTest {

    private final VectorType type = new VectorType();

    // ----- sqlType / class ------------------------------------------------

    @Test
    void sqlType_isOther_soPostgresInfersTextToVectorCast() {
        assertThat(type.getSqlType()).isEqualTo(Types.OTHER);
    }

    @Test
    void returnedClass_isFloatArray() {
        assertThat(type.returnedClass()).isEqualTo(float[].class);
    }

    // ----- nullSafeSet ----------------------------------------------------

    @Test
    void nullSafeSet_null_bindsNullAsOther() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        type.nullSafeSet(ps, null, 1, null);
        verify(ps).setNull(1, Types.OTHER);
    }

    @Test
    void nullSafeSet_array_bindsCanonicalPgvectorText() throws Exception {
        PreparedStatement ps = mock(PreparedStatement.class);
        type.nullSafeSet(ps, new float[]{0.1f, -0.2f, 0.5f}, 7, null);

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(ps).setObject(eq(7), textCaptor.capture(), eq(Types.OTHER));

        String bound = textCaptor.getValue();
        assertThat(bound).startsWith("[").endsWith("]");
        // Все три значения присутствуют (форматирование float может дать
        // "-0.2" или "-0.2f"-подобное представление, но точные точки запятые
        // важны).
        assertThat(bound).contains("0.1", "-0.2", "0.5").contains(",");
    }

    // ----- nullSafeGet ----------------------------------------------------

    @Test
    void nullSafeGet_nullColumn_returnsNull() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyInt())).thenReturn(null);

        float[] result = type.nullSafeGet(rs, 1, null, null);
        assertThat(result).isNull();
    }

    @Test
    void nullSafeGet_parsesCanonicalText() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("[0.1,-0.2,0.5]");

        float[] result = type.nullSafeGet(rs, 1, null, null);
        assertThat(result).containsExactly(0.1f, -0.2f, 0.5f);
    }

    @Test
    void nullSafeGet_parsesUnbracketedText() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("1.0, 2.0, 3.0");

        float[] result = type.nullSafeGet(rs, 1, null, null);
        assertThat(result).containsExactly(1.0f, 2.0f, 3.0f);
    }

    @Test
    void nullSafeGet_parsesEmptyArrayText() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1)).thenReturn("[]");

        float[] result = type.nullSafeGet(rs, 1, null, null);
        assertThat(result).isEmpty();
    }

    // ----- deepCopy / mutability -----------------------------------------

    @Test
    void deepCopy_returnsClone() {
        float[] original = {1.0f, 2.0f, 3.0f};
        float[] copy = type.deepCopy(original);

        assertThat(copy).isNotSameAs(original).containsExactly(original);
    }

    @Test
    void deepCopy_null_returnsNull() {
        assertThat(type.deepCopy(null)).isNull();
    }

    @Test
    void equals_treatsArraysByContent() {
        assertThat(type.equals(new float[]{1f, 2f}, new float[]{1f, 2f})).isTrue();
        assertThat(type.equals(new float[]{1f, 2f}, new float[]{1f, 3f})).isFalse();
        assertThat(type.equals(null, null)).isTrue();
    }
}
