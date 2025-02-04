/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * user-defined enum "types"
 * 
 * @param <T>
 *            the enum type
 *
 */
public abstract class PersistentEnumUserType<T extends PersistentEnum>
    implements UserType, Serializable
{
    private static final long serialVersionUID = -3080625439869047088L;

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException
    {
        return cached;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException
    {
        return value;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException
    {
        return x == y;
    }

    @Override
    public int hashCode(Object x) throws HibernateException
    {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names,
            SharedSessionContractImplementor session, Object owner)
        throws HibernateException, SQLException
    {
        String name = rs.getString(names[0]);
        if (rs.wasNull()) {
            return null;
        }
        for (PersistentEnum value : returnedClass().getEnumConstants()) {
            if (name.equals(value.getId())) {
                return value;
            }
        }
        throw new IllegalStateException(
                "Unknown " + returnedClass().getSimpleName() + " value [" + name + "]");
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
            SharedSessionContractImplementor session)
        throws HibernateException, SQLException
    {
        if (value == null) {
            st.setNull(index, Types.INTEGER);
        }
        else {
            st.setString(index, ((PersistentEnum) value).getId());
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException
    {
        return original;
    }

    @Override
    public abstract Class<T> returnedClass();

    @Override
    public int[] sqlTypes()
    {
        return new int[] { Types.VARCHAR };
    }

}
