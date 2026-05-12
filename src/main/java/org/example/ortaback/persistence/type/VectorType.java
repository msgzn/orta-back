package org.example.ortaback.persistence.type;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * Hibernate {@link UserType} for the pgvector {@code vector(N)} column type.
 *
 * <p>Java representation: {@code float[]}.
 *
 * <p>Binding strategy: serialise the array to the canonical pgvector text form
 * (e.g. {@code "[0.1,0.2,0.3]"}) and bind it as {@link Types#OTHER}. PostgreSQL
 * then performs the implicit text → vector cast on the server side. This avoids
 * the {@code PGvector.addVectorType(connection)} dance pgvector-java otherwise
 * requires on every pooled connection — that path is brittle behind HikariCP
 * because Hikari has no public per-connection-init Java hook.
 *
 * <p>Usage on an entity field:
 * <pre>
 *   &#64;Type(VectorType.class)
 *   &#64;Column(name = "event_embedding", columnDefinition = "vector(384)")
 *   private float[] eventEmbedding;
 * </pre>
 */
public class VectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return x == null ? 0 : Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs,
                               int position,
                               SharedSessionContractImplementor session,
                               Object owner) throws SQLException {
        Object raw = rs.getObject(position);
        if (raw == null) {
            return null;
        }
        return parse(raw.toString());
    }

    @Override
    public void nullSafeSet(PreparedStatement st,
                            float[] value,
                            int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, format(value), Types.OTHER);
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : value.clone();
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return deepCopy((float[]) cached);
    }

    private static String format(float[] arr) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (float f : arr) {
            sj.add(Float.toString(f));
        }
        return sj.toString();
    }

    private static float[] parse(String s) {
        String trimmed = s.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isEmpty()) {
            return new float[0];
        }
        String[] parts = trimmed.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}
