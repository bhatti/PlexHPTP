package com.plexobject.hptp.domain;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Triple<FIRST, SECOND, THIRD> implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    public final FIRST first;
    public final SECOND second;
    public final THIRD third;

    public Triple(FIRST first, SECOND second, THIRD third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    /**
     * @see java.lang.Object#equals(Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Triple)) {
            return false;
        }
        Triple<FIRST, SECOND, THIRD> rhs = (Triple<FIRST, SECOND, THIRD>) object;

        EqualsBuilder eqBuilder = new EqualsBuilder();
        eqBuilder.append(first, rhs.first);
        eqBuilder.append(second, rhs.second);
        eqBuilder.append(third, rhs.third);
        return eqBuilder.isEquals();
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(786529047, 1924536713).append(first).append(
                second).append(third).toHashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).append(first).append(second).append(
                third).toString();
    }
}
