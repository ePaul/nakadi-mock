package org.zalando.nakadi_mock;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.coekie.gentyref.GenericTypeReflector;
import com.jayway.jsonpath.TypeRef;

class TypeUtils {

    static <T> TypeRef<List<T>> getTypeRefFromCallback(EventSubmissionCallback<T> callback) {
        Type superType = GenericTypeReflector.getExactSuperType(callback.getClass(), EventSubmissionCallback.class);
        Type paramType = ((ParameterizedType) superType).getActualTypeArguments()[0];
        return new TypeRef<List<T>>() {
            @Override
            public Type getType() {
                return new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[] { paramType };
                    }

                    @Override
                    public Type getRawType() {
                        return List.class;
                    }

                    @Override
                    public Type getOwnerType() {
                        return null;
                    }
                };
            }
        };
    }

}
