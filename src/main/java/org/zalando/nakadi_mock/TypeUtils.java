package org.zalando.nakadi_mock;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.coekie.gentyref.GenericTypeReflector;
import com.jayway.jsonpath.TypeRef;

class TypeUtils {

    static <T> TypeRef<List<T>> getListTypeRefFromCallback(EventSubmissionCallback<T> callback) {
        Type superType = GenericTypeReflector.getExactSuperType(callback.getClass(), EventSubmissionCallback.class);
        Type paramType = ((ParameterizedType) superType).getActualTypeArguments()[0];
        return getTypeRefForList(paramType);
    }

    static <T> TypeRef<List<T>> getListTypeRef(TypeRef<T> componentType) {
        return getTypeRefForList(componentType.getType());
    }

    static <T> TypeRef<List<T>> getListTypeRef(Class<T> componentType) {
        return getTypeRefForList(componentType);
    }

    private static <T> TypeRef<List<T>> getTypeRefForList(Type componentType) {
        return new TypeRef<List<T>>() {
            @Override
            public Type getType() {
                return new ParameterizedType() {
                    @Override
                    public Type[] getActualTypeArguments() {
                        return new Type[] { componentType };
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
